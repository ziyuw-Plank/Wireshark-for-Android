package org.sharkdroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Starts tshark jobs. Preferred path: inside the isolated DissectorService sandbox.
 * Fallback (if the device's SELinux policy forbids exec from isolated processes):
 * spawn directly as the app UID — still never as root.
 */
object Dissector {
    private const val TAG = "SharkDissector"

    @Volatile private var binder: IBinder? = null
    @Volatile var sandboxUid: Int = -1; private set
    /** null = unknown yet, true = isolated sandbox works, false = fallback in app UID */
    @Volatile var sandboxed: Boolean? = null; private set
    @Volatile var sandboxError: String? = null; private set
    private val lock = Any()

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { binder = service }
        override fun onServiceDisconnected(name: ComponentName?) { binder = null }
        override fun onBindingDied(name: ComponentName?) { binder = null }
    }

    private fun bind(ctx: Context): IBinder? {
        binder?.let { if (it.isBinderAlive) return it }
        synchronized(lock) {
            binder?.let { if (it.isBinderAlive) return it }
            val app = ctx.applicationContext
            val latch = CountDownLatch(1)
            val c = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    conn.onServiceConnected(name, service); latch.countDown()
                }
                override fun onServiceDisconnected(name: ComponentName?) { conn.onServiceDisconnected(name) }
                override fun onBindingDied(name: ComponentName?) { conn.onBindingDied(name); latch.countDown() }
            }
            val ok = try {
                app.bindService(Intent(app, DissectorService::class.java), c, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) { Log.w(TAG, "bind failed", e); false }
            if (!ok) return null
            latch.await(8, TimeUnit.SECONDS)
            val b = binder ?: return null
            if (sandboxUid < 0) {
                val d = Parcel.obtain(); val r = Parcel.obtain()
                try {
                    d.writeInterfaceToken(DissectorService.DESCRIPTOR)
                    b.transact(DissectorService.TX_PING, d, r, 0)
                    sandboxUid = r.readInt()
                } catch (_: Exception) { } finally { d.recycle(); r.recycle() }
            }
            return b
        }
    }

    class Job(
        val stdout: InputStream,
        val stderr: InputStream,
        private val pid: Int,
        private val remote: IBinder?,
    ) {
        @Volatile private var exit: Int? = null

        fun waitFor(): Int {
            exit?.let { return it }
            val rc = if (remote != null) {
                val d = Parcel.obtain(); val r = Parcel.obtain()
                try {
                    d.writeInterfaceToken(DissectorService.DESCRIPTOR); d.writeInt(pid)
                    remote.transact(DissectorService.TX_WAIT, d, r, 0); r.readInt()
                } catch (e: Exception) { -1 } finally { d.recycle(); r.recycle() }
            } else NativeSpawn.waitPid(pid)
            exit = rc
            return rc
        }

        fun kill() {
            if (exit != null) return
            try {
                if (remote != null) {
                    val d = Parcel.obtain(); val r = Parcel.obtain()
                    try {
                        d.writeInterfaceToken(DissectorService.DESCRIPTOR); d.writeInt(pid)
                        remote.transact(DissectorService.TX_KILL, d, r, 0)
                    } finally { d.recycle(); r.recycle() }
                } else NativeSpawn.kill(pid, 9)
            } catch (_: Exception) { }
            try { stdout.close() } catch (_: Exception) { }
            try { stderr.close() } catch (_: Exception) { }
        }
    }

    /**
     * Start `tshark <args>` with [input] as stdin. Ownership of [input] passes to this call.
     */
    fun start(ctx: Context, args: List<String>, input: ParcelFileDescriptor): Job {
        val outPipe = ParcelFileDescriptor.createPipe()
        val errPipe = ParcelFileDescriptor.createPipe()
        try {
            if (sandboxed != false) {
                val b = bind(ctx)
                if (b != null) {
                    val d = Parcel.obtain(); val r = Parcel.obtain()
                    var pid = -1
                    try {
                        d.writeInterfaceToken(DissectorService.DESCRIPTOR)
                        d.writeStringArray(args.toTypedArray())
                        d.writeFileDescriptor(input.fileDescriptor)
                        d.writeFileDescriptor(outPipe[1].fileDescriptor)
                        d.writeFileDescriptor(errPipe[1].fileDescriptor)
                        b.transact(DissectorService.TX_SPAWN, d, r, 0)
                        pid = r.readInt()
                    } catch (e: Exception) {
                        Log.w(TAG, "sandbox spawn failed", e)
                    } finally { d.recycle(); r.recycle() }
                    if (pid > 0) {
                        sandboxed = true
                        return Job(ParcelFileDescriptor.AutoCloseInputStream(outPipe[0]),
                            ParcelFileDescriptor.AutoCloseInputStream(errPipe[0]), pid, b)
                    }
                    sandboxError = "isolated spawn returned $pid"
                    Log.w(TAG, "sandbox unavailable ($pid), falling back to app uid")
                    sandboxed = false
                } else {
                    sandboxError = "bindService failed"
                    sandboxed = false
                }
            }
            val conf = File(ctx.filesDir, "wsconf").apply { mkdirs() }.path
            val tshark = Tools.tshark(ctx)
            val pid = NativeSpawn.spawn(tshark, arrayOf(tshark) + args.toTypedArray(),
                Tools.tsharkEnv(conf), input.fd, outPipe[1].fd, errPipe[1].fd)
            if (pid <= 0) throw IllegalStateException("tshark spawn failed (errno ${-pid})")
            return Job(ParcelFileDescriptor.AutoCloseInputStream(outPipe[0]),
                ParcelFileDescriptor.AutoCloseInputStream(errPipe[0]), pid, null)
        } finally {
            // Our copies of the child's ends must be closed so EOF propagates.
            try { input.close() } catch (_: Exception) { }
            try { outPipe[1].close() } catch (_: Exception) { }
            try { errPipe[1].close() } catch (_: Exception) { }
        }
    }

    /** Convenience: run tshark over a whole capture file and collect stdout/stderr. */
    fun runOnFile(ctx: Context, file: File, args: List<String>, maxBytes: Int = 8 * 1024 * 1024): Triple<Int, String, String> {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val job = start(ctx, listOf("-n", "-r", "-") + args, pfd)
        val err = StringBuilder()
        val t = Thread { try { err.append(job.stderr.bufferedReader().readText().take(65536)) } catch (_: Exception) { } }
        t.start()
        val out = StringBuilder()
        try {
            val r = job.stdout.bufferedReader()
            val buf = CharArray(16384)
            while (true) {
                val n = r.read(buf); if (n < 0) break
                if (out.length < maxBytes) out.append(buf, 0, n)
                else { out.append("\n… [output truncated]"); job.kill(); break }
            }
        } catch (_: Exception) { }
        t.join(3000)
        val rc = job.waitFor()
        return Triple(rc, out.toString(), err.toString())
    }

    /**
     * Compile-check a display filter with tshark itself (same dissector registry as the
     * packet list): tshark gets an empty pcap on stdin, so it only parses the filter.
     * Returns null when the filter is valid, otherwise tshark's error message.
     */
    fun checkFilter(ctx: Context, filter: String): String? {
        if (filter.isBlank()) return null
        val pipe = ParcelFileDescriptor.createPipe()
        // pcap global header (LE, v2.4, snaplen 65535, DLT_EN10MB) and no packets.
        val hdr = byteArrayOf(0xd4.toByte(), 0xc3.toByte(), 0xb2.toByte(), 0xa1.toByte(), 2, 0, 4, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0xff.toByte(), 0xff.toByte(), 0, 0, 1, 0, 0, 0)
        val w = pipe[1]
        val job = start(ctx, listOf("-n", "-r", "-", "-Y", filter), pipe[0])
        try { ParcelFileDescriptor.AutoCloseOutputStream(w).use { it.write(hdr) } } catch (_: Exception) { }
        val err = StringBuilder()
        val t = Thread { try { err.append(job.stderr.bufferedReader().readText().take(4096)) } catch (_: Exception) { } }
        t.start()
        try { job.stdout.readBytes() } catch (_: Exception) { }
        t.join(5000)
        val rc = job.waitFor()
        if (rc == 0) return null
        return cleanStderr(err.toString()).lineSequence().map { it.removePrefix("tshark: ").trim() }
            .filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { "exit $rc" }
    }

    /** Remove noise that tshark prints on Android for missing optional data files. */
    fun cleanStderr(s: String): String = s.lineSequence()
        .filter { it.isNotBlank() && !it.contains("tzdata") }
        .joinToString("\n")
}

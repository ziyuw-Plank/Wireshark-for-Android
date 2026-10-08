package org.sharkdroid

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

data class PacketRow(
    val num: Int,
    val time: String,
    val src: String,
    val dst: String,
    val proto: String,
    val len: Int,
    val info: String,
    val protocols: String,
    val severity: Int,
    val tcpBad: Boolean,
)

/**
 * Owns the capture session (root dumpcap stream -> private pcapng file) and the
 * packet-list dissection job (sandboxed tshark reading the file as it grows).
 */
object CaptureManager {
    private const val TAG = "SharkCapture"
    private const val MAX_ROWS = 1_000_000

    enum class State { IDLE, STARTING, CAPTURING, STOPPING }

    interface Listener {
        fun onRowsChanged(reset: Boolean)
        fun onStateChanged()
        fun onMessage(msg: String)
    }

    /** Per-capture byte counter shared by the writer and the tail feeders. */
    class Sink(val file: File) {
        @Volatile var written = 0L
        @Volatile var complete = false
        val lock = Object()
        fun bump(n: Int) { written += n; synchronized(lock) { lock.notifyAll() } }
        fun finish() { complete = true; synchronized(lock) { lock.notifyAll() } }
    }

    private val main = Handler(Looper.getMainLooper())
    private val listeners = LinkedHashSet<Listener>()

    @Volatile var state = State.IDLE; private set
    var file: File? = null; private set
    var iface: String? = null; private set
    var displayFilter: String = ""; private set
    val rows = ArrayList<PacketRow>()            // main thread only
    var rowsTruncated = false; private set
    var dissecting = false; private set
    @Volatile var sink: Sink? = null; private set
    var dumpcapLog: String = ""; private set

    private var suProc: Process? = null
    private var listJob: Dissector.Job? = null
    @Volatile private var generation = 0

    fun addListener(l: Listener) { listeners += l }
    fun removeListener(l: Listener) { listeners -= l }

    private fun post(block: () -> Unit) = main.post(block)
    private fun fireState() = post { listeners.toList().forEach { it.onStateChanged() } }
    private fun fireMsg(m: String) = post { listeners.toList().forEach { it.onMessage(m) } }

    // ---------------------------------------------------------------- capture

    fun startCapture(ctx: Context, ifc: String, captureFilter: String, dfilter: String, snaplen: Int = Tools.DEFAULT_SNAPLEN) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (state != State.IDLE) return
        if (!Tools.validCaptureFilter(captureFilter)) { fireMsg("捕获过滤器只能包含可打印 ASCII 字符（最长 2048）"); return }
        if (!Tools.validDisplayFilter(dfilter)) { fireMsg("显示过滤器含非法字符"); return }
        val app = ctx.applicationContext
        val f = Tools.newCaptureFile(app, "cap_" + Tools.safeFileName(ifc))
        val s = Sink(f)
        sink = s; file = f; iface = ifc; displayFilter = dfilter; dumpcapLog = ""
        state = State.STARTING
        fireState()
        try {
            app.startForegroundService(Intent(app, CaptureService::class.java).putExtra("iface", ifc))
        } catch (e: Exception) { Log.w(TAG, "fgs", e) }

        thread(name = "capture-root") {
            val p = try {
                RootHelper.startCapture(app, ifc, captureFilter, snaplen)
            } catch (e: Exception) {
                fireMsg("无法启动 su：${e.message}\n请确认已 root 并在 Magisk/KernelSU 中授权本应用。")
                s.finish(); post { state = State.IDLE; fireState(); stopService(app) }
                return@thread
            }
            suProc = p
            val errBuf = StringBuilder()
            val errT = thread(name = "capture-stderr") {
                try {
                    p.errorStream.bufferedReader().forEachLine { line ->
                        synchronized(errBuf) { if (errBuf.length < 32768) errBuf.append(line).append('\n') }
                        if (line.startsWith("Capturing on")) post { if (state == State.STARTING) { state = State.CAPTURING; fireState() } }
                    }
                } catch (_: Exception) { }
            }
            post { if (state == State.STARTING) { state = State.CAPTURING; fireState() }; startDissection(app, f, s) }
            var limitHit = false
            try {
                FileOutputStream(f).use { out ->
                    val buf = ByteArray(65536)
                    val input = p.inputStream
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        s.bump(n)
                        if (!limitHit && s.written > Tools.MAX_CAPTURE_BYTES) {
                            limitHit = true
                            fireMsg("捕获文件已达 ${Tools.humanBytes(Tools.MAX_CAPTURE_BYTES)} 上限，自动停止")
                            RootHelper.stopCapture(p)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "capture read", e)
            }
            try { p.waitFor() } catch (_: Exception) { }
            errT.join(2000)
            s.finish()
            suProc = null
            val log = synchronized(errBuf) { errBuf.toString() }
            val rc = try { p.exitValue() } catch (_: Exception) { -1 }
            post {
                dumpcapLog = log
                state = State.IDLE
                stopService(app)
                fireState()
                if (s.written == 0L) {
                    f.delete()
                    fireMsg("抓包没有开始（退出码 $rc）：\n" + log.trim().ifEmpty { "su 被拒绝或不可用。请在 Magisk/KernelSU 中授予 SharkDroid root 权限。" })
                } else if (rc != 0 && rc != 143 && !log.contains("Packets captured")) {
                    fireMsg("dumpcap 退出码 $rc：\n" + log.trim())
                }
            }
        }
    }

    fun stopCapture() {
        val p = suProc ?: return
        if (state == State.STOPPING) return
        state = State.STOPPING
        fireState()
        thread(name = "capture-stop") {
            RootHelper.stopCapture(p)
            Thread.sleep(6000)
            if (suProc === p) { try { p.destroy() } catch (_: Exception) { } }
        }
    }

    private fun stopService(ctx: Context) {
        try { ctx.stopService(Intent(ctx, CaptureService::class.java)) } catch (_: Exception) { }
    }

    // ---------------------------------------------------------------- open file / dissection

    fun openFile(ctx: Context, f: File, dfilter: String) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (state != State.IDLE) { fireMsg("请先停止当前抓包"); return }
        if (!Tools.validDisplayFilter(dfilter)) { fireMsg("显示过滤器含非法字符"); return }
        file = f; iface = null; displayFilter = dfilter; sink = null
        startDissection(ctx.applicationContext, f, null)
    }

    fun applyDisplayFilter(ctx: Context, dfilter: String) {
        if (!Tools.validDisplayFilter(dfilter)) { fireMsg("显示过滤器含非法字符"); return }
        displayFilter = dfilter
        val f = file ?: return
        val s = sink
        startDissection(ctx.applicationContext, f, if (s != null && !s.complete) s else null)
    }

    fun clear() {
        listJob?.kill(); listJob = null
        generation++
        rows.clear(); rowsTruncated = false; dissecting = false
        if (state == State.IDLE) { file = null; sink = null }
        post { listeners.toList().forEach { it.onRowsChanged(true) } }
        fireState()
    }

    private val FIELDS = listOf("frame.number", "frame.time_relative", "_ws.col.def_src", "_ws.col.def_dst",
        "_ws.col.protocol", "frame.len", "frame.protocols", "_ws.expert.severity", "tcp.analysis.flags", "_ws.col.info")

    /** (Re)start the packet-list job. For a live capture, feed the growing file through a pipe. */
    private fun startDissection(app: Context, f: File, live: Sink?) {
        listJob?.kill(); listJob = null
        val gen = ++generation
        rows.clear(); rowsTruncated = false; dissecting = true
        listeners.toList().forEach { it.onRowsChanged(true) }
        fireState()
        val dfilter = displayFilter
        thread(name = "dissect-$gen") {
            val input: ParcelFileDescriptor = if (live != null) {
                val pipe = ParcelFileDescriptor.createPipe()
                startTailFeeder(live, pipe[1], gen)
                pipe[0]
            } else {
                try { ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY) }
                catch (e: Exception) { fireMsg("无法打开文件：${e.message}"); post { if (gen == generation) { dissecting = false; fireState() } }; return@thread }
            }
            val args = ArrayList<String>()
            args += listOf("-n", "-l", "-r", "-", "-T", "fields", "-E", "separator=/t", "-E", "occurrence=f", "-E", "quote=n")
            for (e in FIELDS) { args += "-e"; args += e }
            if (dfilter.isNotBlank()) { args += "-Y"; args += dfilter }
            val job = try { Dissector.start(app, args, input) } catch (e: Exception) {
                fireMsg("无法启动 tshark：${e.message}"); post { if (gen == generation) { dissecting = false; fireState() } }; return@thread
            }
            post { if (gen == generation) listJob = job else job.kill() }
            val err = StringBuilder()
            val errT = thread(name = "dissect-err-$gen") {
                try { job.stderr.bufferedReader().forEachLine { if (err.length < 16384) err.append(it).append('\n') } } catch (_: Exception) { }
            }
            var batch = ArrayList<PacketRow>(256)
            var lastPost = System.currentTimeMillis()
            var count = 0
            try {
                job.stdout.bufferedReader().forEachLine { line ->
                    if (gen != generation) throw InterruptedException()
                    parseRow(line)?.let { batch.add(it); count++ }
                    val now = System.currentTimeMillis()
                    if (batch.size >= 2000 || (batch.isNotEmpty() && now - lastPost > 120)) {
                        val b = batch; batch = ArrayList(256); lastPost = now
                        post { appendRows(gen, b) }
                    }
                }
            } catch (_: InterruptedException) {
            } catch (_: Exception) { }
            if (batch.isNotEmpty()) { val b = batch; post { appendRows(gen, b) } }
            errT.join(3000)
            val rc = job.waitFor()
            post {
                if (gen != generation) return@post
                dissecting = false
                listJob = null
                fireState()
                val e = Dissector.cleanStderr(err.toString())
                if (rc != 0 && rc != 137 && e.isNotEmpty()) fireMsg("tshark（退出码 $rc）：\n$e")
                else if (count == 0 && e.isNotEmpty()) fireMsg(e)
            }
        }
    }

    private fun startTailFeeder(s: Sink, w: ParcelFileDescriptor, gen: Int) {
        thread(name = "feeder-$gen") {
            try {
                FileInputStream(s.file).use { fis ->
                    ParcelFileDescriptor.AutoCloseOutputStream(w).use { os ->
                        val buf = ByteArray(65536)
                        var pos = 0L
                        while (gen == generation) {
                            val avail = s.written - pos
                            if (avail > 0) {
                                val n = fis.read(buf, 0, minOf(avail, buf.size.toLong()).toInt())
                                if (n > 0) { os.write(buf, 0, n); pos += n } else Thread.sleep(50)
                            } else if (s.complete) {
                                break
                            } else {
                                os.flush()
                                synchronized(s.lock) { s.lock.wait(250) }
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun appendRows(gen: Int, b: List<PacketRow>) {
        if (gen != generation) return
        val room = MAX_ROWS - rows.size
        if (room <= 0) { rowsTruncated = true; return }
        if (b.size > room) { rows.addAll(b.subList(0, room)); rowsTruncated = true } else rows.addAll(b)
        listeners.toList().forEach { it.onRowsChanged(false) }
    }

    private fun parseRow(line: String): PacketRow? {
        val f = line.split('\t', limit = 10)
        if (f.size < 10) return null
        val num = f[0].toIntOrNull() ?: return null
        val t = f[1].let { s -> val dot = s.indexOf('.'); if (dot >= 0 && s.length > dot + 7) s.substring(0, dot + 7) else s }
        return PacketRow(num, t, f[2], f[3], f[4], f[5].toIntOrNull() ?: 0, f[9], f[6],
            f[7].toIntOrNull() ?: 0, f[8].isNotEmpty())
    }
}

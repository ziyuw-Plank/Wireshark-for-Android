package org.sharkdroid

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Tools {
    const val WIRESHARK_VERSION = "4.6.9"
    const val MAX_CAPTURE_BYTES = 2L * 1024 * 1024 * 1024 // auto-stop at 2 GiB
    const val DEFAULT_SNAPLEN = 262144

    private val SAFE_PATH = Regex("^/[A-Za-z0-9/._~=+-]+$")
    private val SAFE_NAME = Regex("^[A-Za-z0-9._-]{1,128}$")

    fun libDir(ctx: Context): String = ctx.applicationInfo.nativeLibraryDir
    fun tshark(ctx: Context): String = File(libDir(ctx), "libtshark.so").path
    fun dumpcap(ctx: Context): String = File(libDir(ctx), "libdumpcap.so").path
    fun helper(ctx: Context): String = File(libDir(ctx), "libwsexec.so").path

    fun capturesDir(ctx: Context): File = File(ctx.filesDir, "captures").apply { mkdirs() }

    fun newCaptureFile(ctx: Context, prefix: String): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(capturesDir(ctx), "${prefix}_$ts.pcapng")
    }

    /** Is [f] a regular file directly inside our private captures directory? */
    fun isCaptureFile(ctx: Context, f: File): Boolean {
        val dir = capturesDir(ctx).canonicalFile
        val c = f.canonicalFile
        return c.parentFile == dir && SAFE_NAME.matches(c.name) && c.isFile
    }

    fun safeFileName(name: String): String {
        val base = name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_')
        return (if (base.isEmpty()) "capture" else base).take(80)
    }

    /**
     * The only string ever handed to `su -c`: a single-quoted absolute path chosen by the
     * package manager. Anything else (filters, interface names) goes over stdin.
     */
    fun shellQuotedPath(path: String): String {
        require(SAFE_PATH.matches(path)) { "unexpected characters in path: $path" }
        return "'$path'"
    }

    /** Display filters: printable text, no control characters, bounded length. */
    fun validDisplayFilter(f: String): Boolean =
        f.length <= 4096 && f.none { it.code < 0x20 || it.code == 0x7f }

    /** Capture (BPF) filters: printable ASCII only (also enforced by the root helper). */
    fun validCaptureFilter(f: String): Boolean =
        f.length <= 2048 && f.all { it.code in 0x20..0x7e }

    /** Environment for tshark: minimal and explicit. */
    fun tsharkEnv(configDir: String): Array<String> {
        val keep = listOf("ANDROID_ROOT", "ANDROID_DATA", "ANDROID_TZDATA_ROOT", "ANDROID_I18N_ROOT",
            "ANDROID_ART_ROOT", "ANDROID_RUNTIME_ROOT")
        val env = ArrayList<String>()
        env += "PATH=/system/bin"
        env += "HOME=$configDir"
        env += "WIRESHARK_CONFIG_DIR=$configDir"
        env += "LANG=C.UTF-8"
        for (k in keep) System.getenv(k)?.let { env += "$k=$it" }
        return env.toTypedArray()
    }

    fun humanBytes(n: Long): String = when {
        n >= 1L shl 30 -> String.format(Locale.US, "%.2f GiB", n / (1L shl 30).toDouble())
        n >= 1L shl 20 -> String.format(Locale.US, "%.1f MiB", n / (1L shl 20).toDouble())
        n >= 1L shl 10 -> String.format(Locale.US, "%.1f KiB", n / 1024.0)
        else -> "$n B"
    }
}

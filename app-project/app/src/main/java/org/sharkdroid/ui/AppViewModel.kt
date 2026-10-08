package org.sharkdroid.ui

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sharkdroid.CaptureManager
import org.sharkdroid.Dissector
import org.sharkdroid.IfaceInfo
import org.sharkdroid.PacketRow
import org.sharkdroid.R
import org.sharkdroid.RootHelper
import org.sharkdroid.Tools
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RootStatus { CHECKING, OK, DENIED }
enum class FilterValidity { EMPTY, CHECKING, VALID, INVALID }

/** A snackbar message; [detail] (if any) is shown in a dialog via the "Details" action. */
data class UiMessage(val text: String, val detail: String? = null, val undo: (() -> Unit)? = null, val onTimeout: (() -> Unit)? = null)

data class MainUiState(
    val capture: CaptureManager.State = CaptureManager.State.IDLE,
    val root: RootStatus = RootStatus.CHECKING,
    val iface: IfaceInfo = IfaceInfo.ANY,
    val fileName: String? = null,
    val appliedFilter: String = "",
    val dissecting: Boolean = false,
    val truncated: Boolean = false,
    val packets: Int = 0,
    val bytes: Long = 0,
    val durationMs: Long = 0,
    val written: Long = -1,
)

data class SavedCapture(val file: File, val size: Long, val modified: Long, val isCurrent: Boolean, val isLive: Boolean)

/** User preferences (app-private SharedPreferences), exposed as Compose state. */
class Settings(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("ui", Context.MODE_PRIVATE)
    var themeMode by mutableStateOf(runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM)); private set
    var dynamicColor by mutableStateOf(prefs.getBoolean("dynamic", true)); private set
    var snaplen by mutableIntStateOf(prefs.getInt("snaplen", Tools.DEFAULT_SNAPLEN)); private set
    var autoScroll by mutableStateOf(prefs.getBoolean("autoscroll", true)); private set
    var captureFilter by mutableStateOf(prefs.getString("cfilter", "") ?: ""); private set
    var lastIface by mutableStateOf(prefs.getString("iface", null)); private set
    var filterHistory by mutableStateOf(prefs.getString("dhistory", "")!!.split('\n').filter { it.isNotBlank() }); private set

    fun updateTheme(m: ThemeMode) { themeMode = m; prefs.edit { putString("theme", m.name) } }
    fun updateDynamic(b: Boolean) { dynamicColor = b; prefs.edit { putBoolean("dynamic", b) } }
    fun updateSnaplen(v: Int) { snaplen = v; prefs.edit { putInt("snaplen", v) } }
    fun updateAutoScroll(b: Boolean) { autoScroll = b; prefs.edit { putBoolean("autoscroll", b) } }
    fun updateCaptureFilter(s: String) { captureFilter = s; prefs.edit { putString("cfilter", s) } }
    fun updateLastIface(s: String) { lastIface = s; prefs.edit { putString("iface", s) } }
    fun remember(filter: String) {
        if (filter.isBlank()) return
        filterHistory = (listOf(filter) + filterHistory.filter { it != filter }).take(12)
        prefs.edit { putString("dhistory", filterHistory.joinToString("\n")) }
    }
    fun clearHistory() { filterHistory = emptyList(); prefs.edit { remove("dhistory") } }

    companion object {
        val SNAPLENS = listOf(Tools.DEFAULT_SNAPLEN, 65535, 1514, 256, 128)
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app), CaptureManager.Listener {
    private val ctx: Context get() = getApplication()
    val settings = Settings(app)

    var ui by mutableStateOf(MainUiState()); private set
    /** Bumped whenever [rows] changes; read it to observe the (main-thread) row list. */
    var rowsVersion by mutableIntStateOf(0); private set
    val rows: List<PacketRow> get() = CaptureManager.rows
    var ifaces by mutableStateOf(listOf(IfaceInfo.ANY)); private set
    var ifacesLoading by mutableStateOf(false); private set
    var rootDetail by mutableStateOf(""); private set

    var filterQuery by mutableStateOf(CaptureManager.displayFilter); private set
    var filterValidity by mutableStateOf(FilterValidity.EMPTY); private set
    var filterError by mutableStateOf<String?>(null); private set
    private var filterJob: Job? = null
    private val filterCache = HashMap<String, String?>()

    private val messages = Channel<UiMessage>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    init {
        CaptureManager.addListener(this)
        refresh()
        checkRoot()
    }

    override fun onCleared() { CaptureManager.removeListener(this); super.onCleared() }

    // ---------------------------------------------------------------- CaptureManager.Listener
    override fun onRowsChanged(reset: Boolean) { rowsVersion++; refresh() }
    override fun onStateChanged() = refresh()
    override fun onMessage(msg: String) { post(msg) }

    fun post(msg: String) {
        val first = msg.lineSequence().first().trimEnd(':', '：')
        val detail = if (msg.contains('\n') || msg.length > 90) msg else null
        messages.trySend(UiMessage(if (first.length > 90) first.take(88) + "…" else first, detail))
    }
    fun post(m: UiMessage) { messages.trySend(m) }
    fun postRes(res: Int, vararg args: Any) = post(ctx.getString(res, *args))

    /** Recompute the derived screen state from CaptureManager (main thread). */
    fun refresh() {
        val st = CaptureManager.state
        val dur = when {
            CaptureManager.startedAt > 0 && st != CaptureManager.State.IDLE -> SystemClock.elapsedRealtime() - CaptureManager.startedAt
            CaptureManager.startedAt > 0 && CaptureManager.endedAt > 0 -> CaptureManager.endedAt - CaptureManager.startedAt
            else -> (CaptureManager.lastRelTime * 1000).toLong()
        }
        val sel = ifaces.firstOrNull { it.name == (CaptureManager.iface ?: settings.lastIface) } ?: ui.iface
        ui = ui.copy(
            capture = st,
            iface = sel,
            fileName = CaptureManager.file?.name,
            appliedFilter = CaptureManager.displayFilter,
            dissecting = CaptureManager.dissecting,
            truncated = CaptureManager.rowsTruncated,
            packets = CaptureManager.rows.size,
            bytes = CaptureManager.rowsBytes,
            durationMs = dur,
            written = CaptureManager.sink?.takeIf { !it.complete }?.written ?: -1,
        )
    }

    // ---------------------------------------------------------------- root & interfaces
    fun checkRoot() {
        ui = ui.copy(root = RootStatus.CHECKING)
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { RootHelper.run(ctx, "id") }
            val ok = r.exit == 0 && r.stdout.contains("uid=0")
            rootDetail = if (ok) r.stdout.trim() else r.stderr.trim().ifEmpty { r.stdout.trim() }.ifEmpty { "exit=${r.exit}" }
            ui = ui.copy(root = if (ok) RootStatus.OK else RootStatus.DENIED)
            if (ok) loadInterfaces()
        }
    }

    fun loadInterfaces() {
        ifacesLoading = true
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { RootHelper.run(ctx, "list") }
            ifacesLoading = false
            val parsed = IfaceInfo.parse(r.stdout)
            if (r.exit != 0 || parsed.isEmpty()) {
                if (ui.root == RootStatus.OK) postRes(R.string.msg_iface_failed, r.stderr.trim().take(200))
                return@launch
            }
            ifaces = listOf(IfaceInfo.ANY) + parsed
            val saved = settings.lastIface
            val pick = ifaces.firstOrNull { it.name == (CaptureManager.iface ?: saved) && (it.isUp || CaptureManager.iface != null) }
                ?: IfaceInfo.pickDefault(parsed)
            ui = ui.copy(iface = pick)
            refresh()
        }
    }

    fun selectIface(i: IfaceInfo) {
        if (ui.capture != CaptureManager.State.IDLE) return
        settings.updateLastIface(i.name)
        ui = ui.copy(iface = i)
    }

    // ---------------------------------------------------------------- capture
    fun startCapture(captureFilter: String) {
        val cf = captureFilter.trim()
        if (!Tools.validCaptureFilter(cf)) { postRes(R.string.msg_bad_capture_filter); return }
        settings.updateCaptureFilter(cf)
        CaptureManager.startCapture(ctx, ui.iface.name, cf, CaptureManager.displayFilter, settings.snaplen)
        refresh()
    }

    fun stopCapture() = CaptureManager.stopCapture()

    fun clear() = CaptureManager.clear()

    // ---------------------------------------------------------------- display filter
    fun onFilterQuery(q: String) {
        filterQuery = q
        filterJob?.cancel()
        val f = q.trim()
        if (f.isEmpty()) { filterValidity = FilterValidity.EMPTY; filterError = null; return }
        if (!Tools.validDisplayFilter(f)) { filterValidity = FilterValidity.INVALID; filterError = ctx.getString(R.string.msg_bad_display_filter); return }
        if (filterCache.containsKey(f)) { setValidity(filterCache[f]); return }
        filterValidity = FilterValidity.CHECKING
        filterJob = viewModelScope.launch {
            delay(450)
            val err = withContext(Dispatchers.IO) { runCatching { Dissector.checkFilter(ctx, f) }.getOrElse { null } }
            filterCache[f] = err
            if (filterQuery.trim() == f) setValidity(err)
        }
    }

    private fun setValidity(err: String?) {
        filterValidity = if (err == null) FilterValidity.VALID else FilterValidity.INVALID
        filterError = err
    }

    fun applyFilter(f: String = filterQuery) {
        val df = f.trim()
        if (df != filterQuery) onFilterQuery(df)
        if (!Tools.validDisplayFilter(df)) { postRes(R.string.msg_bad_display_filter); return }
        if (filterValidity == FilterValidity.INVALID && filterCache.containsKey(df)) { post(filterError ?: ""); return }
        settings.remember(df)
        if (CaptureManager.file == null) { postRes(R.string.msg_no_file); return }
        CaptureManager.applyDisplayFilter(ctx, df)
    }

    // ---------------------------------------------------------------- files
    fun savedCaptures(): List<SavedCapture> {
        val files = (Tools.capturesDir(ctx).listFiles() ?: emptyArray()).filter { it.isFile }.sortedByDescending { it.lastModified() }
        val live = CaptureManager.state != CaptureManager.State.IDLE
        return files.map { SavedCapture(it, it.length(), it.lastModified(), it == CaptureManager.file, live && it == CaptureManager.file) }
    }

    fun openFile(f: File) {
        if (!Tools.isCaptureFile(ctx, f)) return
        CaptureManager.openFile(ctx, f, CaptureManager.displayFilter)
    }

    /** Delete a private capture; returns false if it is the live capture file. */
    fun deleteFile(f: File): Boolean {
        if (CaptureManager.state != CaptureManager.State.IDLE && CaptureManager.file == f) return false
        if (CaptureManager.file == f) CaptureManager.clear()
        if (Tools.isCaptureFile(ctx, f)) f.delete()
        return true
    }

    fun importUri(uri: Uri) {
        if (CaptureManager.state != CaptureManager.State.IDLE) { postRes(R.string.msg_stop_first); return }
        var name = "imported"
        try {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0) ?: name
            }
        } catch (_: Exception) { }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dest = File(Tools.capturesDir(ctx), "imp_${ts}_" + Tools.safeFileName(name))
        postRes(R.string.msg_importing, name)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    ctx.contentResolver.openInputStream(uri)!!.use { inp -> dest.outputStream().use { inp.copyTo(it, 65536) } }
                    true
                } catch (e: Exception) { dest.delete(); postRes(R.string.msg_import_failed, e.message ?: ""); false }
            }
            if (ok) CaptureManager.openFile(ctx, dest, CaptureManager.displayFilter)
        }
    }

    fun exportTo(f: File, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    ctx.contentResolver.openOutputStream(uri, "w")!!.use { out -> f.inputStream().use { it.copyTo(out, 65536) } }
                    postRes(R.string.msg_exported, f.name)
                } catch (e: Exception) { postRes(R.string.msg_export_failed, e.message ?: "") }
            }
        }
    }

    /** System share sheet for a private capture, via the non-exported read-only provider. */
    fun shareIntent(f: File): Intent? {
        if (!Tools.isCaptureFile(ctx, f)) return null
        val uri = Uri.parse("content://${ctx.packageName}.captures/${Uri.encode(f.name)}")
        val send = Intent(Intent.ACTION_SEND).setType("application/vnd.tcpdump.pcap")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(f.name, uri)
        return Intent.createChooser(send, ctx.getString(R.string.share_title, f.name))
    }

    fun streamCommand(): String {
        val ifc = ui.iface.name.takeIf { it != "any" } ?: "wlan0"
        return "adb exec-out \"su -c '${Tools.dumpcap(ctx)} -i $ifc -F pcapng -q -w -'\" | wireshark -k -i -"
    }

    suspend fun runPcapList(): String = withContext(Dispatchers.IO) {
        val r = RootHelper.run(ctx, "pcaplist"); (r.stdout + "\n" + r.stderr).trim()
    }
}

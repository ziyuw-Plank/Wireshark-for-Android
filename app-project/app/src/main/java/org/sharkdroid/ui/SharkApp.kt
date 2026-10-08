package org.sharkdroid.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.sharkdroid.BuildConfig
import org.sharkdroid.CaptureManager
import org.sharkdroid.Dissector
import org.sharkdroid.Pdml
import org.sharkdroid.R
import org.sharkdroid.Tools
import java.io.File
import java.io.StringReader

/** A pending tshark text report (statistics or follow stream). */
data class ReportRequest(val title: String, val args: List<String>, val file: File)

@Composable
fun SharkApp(vm: AppViewModel) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    var detailText by remember { mutableStateOf<String?>(null) }
    var showStream by remember { mutableStateOf(false) }
    var exportFile by remember { mutableStateOf<File?>(null) }
    var report by remember { mutableStateOf<ReportRequest?>(null) }
    val copiedMsg = stringResource(R.string.msg_copied)
    val detailsLabel = stringResource(R.string.action_details)
    val undoLabel = stringResource(R.string.action_undo)
    fun copy(s: String) {
        clipboard.setText(AnnotatedString(s))
        // Android 13+ shows its own clipboard confirmation.
        if (Build.VERSION.SDK_INT < 33) vm.post(copiedMsg)
    }

    // Snackbars for everything CaptureManager / the ViewModel reports.
    LaunchedEffect(Unit) {
        vm.messageFlow.collect { m ->
            val r = snackbar.showSnackbar(m.text, actionLabel = when {
                m.undo != null -> undoLabel
                m.detail != null -> detailsLabel
                else -> null
            }, withDismissAction = m.detail != null, duration = if (m.detail != null || m.undo != null) SnackbarDuration.Long else SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) { m.undo?.invoke(); if (m.undo == null) detailText = m.detail }
            else m.onTimeout?.invoke()
        }
    }
    // Live duration / byte counter while a capture runs.
    val ui = vm.ui
    LaunchedEffect(ui.capture) {
        while (ui.capture != CaptureManager.State.IDLE) { vm.refresh(); delay(1000) }
    }

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importUri(it) } }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val f = exportFile; if (uri != null && f != null) vm.exportTo(f, uri); exportFile = null
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun export(f: File) { exportFile = f; createDoc.launch(f.name) }
    fun share(f: File) { vm.shareIntent(f)?.let { ctx.startActivity(it) } }

    NavHost(navController = nav, startDestination = "main") {
        composable("main") {
            MainScreen(
                state = vm.ui, ifaces = vm.ifaces, ifacesLoading = vm.ifacesLoading,
                rows = vm.rows, rowsVersion = vm.rowsVersion,
                filterQuery = vm.filterQuery, filterValidity = vm.filterValidity, filterError = vm.filterError,
                filterHistory = vm.settings.filterHistory,
                captureFilter = vm.settings.captureFilter, snaplen = vm.settings.snaplen,
                autoScrollDefault = vm.settings.autoScroll,
                snackbar = snackbar,
                onFilterQuery = vm::onFilterQuery,
                onApplyFilter = { vm.applyFilter(it) },
                onClearHistory = { vm.settings.clearHistory() },
                onSelectIface = vm::selectIface,
                onRefreshIfaces = vm::loadInterfaces,
                onRetryRoot = vm::checkRoot,
                onStart = { cf ->
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    vm.startCapture(cf)
                },
                onStop = vm::stopCapture,
                onPacketClick = { r -> nav.navigate("detail/${r.num}?s=" + android.net.Uri.encode("${r.proto} · ${r.src} → ${r.dst}")) },
                onMenu = { m ->
                    val cur = CaptureManager.file
                    when (m) {
                        MainMenu.OPEN -> openDoc.launch(arrayOf("*/*"))
                        MainMenu.SAVED -> nav.navigate("saved")
                        MainMenu.STATS -> nav.navigate("stats")
                        MainMenu.EXPORT -> cur?.takeIf { it.exists() }?.let { export(it) }
                        MainMenu.SHARE -> cur?.takeIf { it.exists() }?.let { share(it) }
                        MainMenu.STREAM -> showStream = true
                        MainMenu.DIAGNOSTICS -> nav.navigate("diagnostics")
                        MainMenu.SETTINGS -> nav.navigate("settings")
                        MainMenu.CLEAR -> vm.clear()
                    }
                },
                onMessage = { vm.post(it) },
            )
        }
        composable("detail/{frame}?s={s}", arguments = listOf(
            navArgument("frame") { type = NavType.IntType },
            navArgument("s") { type = NavType.StringType; defaultValue = "" },
        )) { entry ->
            val frame = entry.arguments?.getInt("frame") ?: 0
            val file = remember { CaptureManager.file }
            val sourceLabel = stringResource(R.string.hex_source)
            val data by produceState(DetailData(), frame) {
                if (file == null || !Tools.isCaptureFile(ctx, file) || frame <= 0) { value = DetailData(loading = false); return@produceState }
                val sel = listOf("-c", frame.toString(), "-Y", "frame.number == $frame")
                value = withContext(Dispatchers.IO) {
                    val (rc, out, err) = Dissector.runOnFile(ctx, file, sel + listOf("-T", "pdml"))
                    val roots = runCatching { Pdml.parse(StringReader(out)) }.getOrDefault(emptyList())
                    val (_, hexOut, _) = Dissector.runOnFile(ctx, file, sel + listOf("-x"))
                    val (bytes, extra) = parseHex(hexOut) { i -> "$sourceLabel $i" }
                    DetailData(roots, bytes, extra, loading = false,
                        error = if (roots.isEmpty()) "tshark ($rc): " + Dissector.cleanStderr(err).take(300) else null)
                }
            }
            DetailScreen(
                frame = frame, summary = entry.arguments?.getString("s") ?: "", data = data, snackbar = snackbar,
                onBack = { nav.popBackStack() },
                onApplyFilter = { f -> vm.onFilterQuery(f); vm.applyFilter(f); nav.popBackStack("main", false) },
                onFollow = { proto, id ->
                    if (file != null) {
                        report = ReportRequest(ctx.getString(if (proto == "tcp") R.string.follow_tcp_title else R.string.follow_udp_title, id),
                            listOf("-q", "-z", "follow,$proto,ascii,$id"), file)
                        nav.navigate("report")
                    }
                },
                onMessage = { vm.post(it) },
            )
        }
        composable("saved") {
            var version by remember { mutableStateOf(0) }
            val items = remember(version, vm.ui.capture, vm.ui.fileName) { vm.savedCaptures() }
            SavedScreen(items, snackbar,
                onBack = { nav.popBackStack() },
                onOpen = { vm.openFile(it); nav.popBackStack("main", false) },
                onShare = ::share, onExport = ::export,
                onDelete = { f -> if (!vm.deleteFile(f)) vm.postRes(R.string.msg_cannot_delete_live); version++ })
        }
        composable("stats") {
            val counts = remember(vm.rowsVersion) {
                vm.rows.groupingBy { it.proto.ifEmpty { "?" } }.eachCount().entries.sortedByDescending { it.value }.take(8).map { it.key to it.value }
            }
            StatsScreen(vm.ui.fileName, vm.ui.packets, vm.ui.bytes, vm.ui.durationMs, counts, LocalExtraColors.current.dark,
                onBack = { nav.popBackStack() },
                onReport = { r ->
                    val f = CaptureManager.file
                    if (f == null || !f.exists()) vm.postRes(R.string.msg_no_file)
                    else { report = ReportRequest(ctx.getString(r.title), r.args, f); nav.navigate("report") }
                })
        }
        composable("report") {
            val req = report
            val text by produceState<String?>(null, req) {
                if (req == null || !Tools.isCaptureFile(ctx, req.file)) { value = ""; return@produceState }
                value = withContext(Dispatchers.IO) {
                    val (rc, out, err) = Dissector.runOnFile(ctx, req.file, req.args)
                    val e = Dissector.cleanStderr(err)
                    when {
                        out.isNotBlank() -> out
                        e.isNotEmpty() -> "tshark exit $rc\n$e"
                        else -> ctx.getString(R.string.report_empty)
                    }
                }
            }
            ReportScreen(req?.title ?: "", req?.file?.name, text, onBack = { nav.popBackStack() }, onCopy = ::copy)
        }
        composable("settings") {
            val s = vm.settings
            SettingsScreen(s.themeMode, s.dynamicColor, s.snaplen, s.autoScroll, BuildConfig.VERSION_NAME,
                onBack = { nav.popBackStack() }, onTheme = s::updateTheme, onDynamic = s::updateDynamic, onSnaplen = s::updateSnaplen,
                onAutoScroll = s::updateAutoScroll, onLicenses = { nav.navigate("licenses") }, onDiagnostics = { nav.navigate("diagnostics") })
        }
        composable("licenses") { LicensesScreen(onBack = { nav.popBackStack() }) }
        composable("diagnostics") {
            val sandbox = when (Dissector.sandboxed) {
                true -> "✓ " + stringResource(R.string.diag_sandbox_ok, Dissector.sandboxUid)
                false -> stringResource(R.string.diag_sandbox_fallback, Dissector.sandboxError ?: "")
                null -> stringResource(R.string.diag_sandbox_unused)
            }
            DiagnosticsScreen(vm.ui.root, vm.rootDetail, sandbox, Tools.libDir(ctx), vm.ifaces, CaptureManager.dumpcapLog,
                vm.streamCommand(), snackbar, onBack = { nav.popBackStack() }, onRetryRoot = vm::checkRoot, onCopy = ::copy,
                runPcapList = vm::runPcapList)
        }
    }

    detailText?.let { MessageDialog(it, onDismiss = { detailText = null }, onCopy = ::copy) }
    if (showStream) StreamDialog(vm.streamCommand(), onDismiss = { showStream = false }, onCopy = ::copy)
}

package org.sharkdroid.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sharkdroid.IfaceInfo
import org.sharkdroid.R
import org.sharkdroid.Tools
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackTopBar(title: String, onBack: () -> Unit, subtitle: String? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = {
            Column {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
        actions = { actions() },
    )
}

@Composable
private fun bottomInset() = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

// ====================================================================================== saved

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    items: List<SavedCapture>,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit,
    onExport: (File) -> Unit,
    onDelete: (File) -> Unit,
) {
    val pending = remember { mutableStateListOf<File>() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val currentDelete by rememberUpdatedState(onDelete)
    // Anything still waiting for "Undo" when the screen goes away is deleted for real.
    DisposableEffect(Unit) { onDispose { pending.toList().forEach { currentDelete(it) } } }
    fun requestDelete(f: File) {
        if (f in pending) return
        pending.add(f)
        scope.launch {
            val r = snackbar.showSnackbar(ctx.getString(R.string.msg_deleted, f.name), ctx.getString(R.string.action_undo),
                withDismissAction = false, duration = SnackbarDuration.Long)
            if (pending.remove(f) && r != SnackbarResult.ActionPerformed) currentDelete(f)
        }
    }
    val shown = items.filter { it.file !in pending }
    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.saved_title), onBack, stringResource(R.string.saved_subtitle)) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        if (shown.isEmpty()) {
            EmptyState(Icons.Outlined.Inventory2, stringResource(R.string.saved_empty_title), stringResource(R.string.saved_empty_body),
                Modifier.padding(inner))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(top = inner.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = bottomInset() + 16.dp)) {
                items(shown, key = { it.file.name }) { c ->
                    SavedRow(c, onOpen, onShare, onExport, ::requestDelete, Modifier.animateItem())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedRow(c: SavedCapture, onOpen: (File) -> Unit, onShare: (File) -> Unit, onExport: (File) -> Unit,
                     onDelete: (File) -> Unit, modifier: Modifier) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) { onDelete(c.file); state.snapTo(SwipeToDismissBoxValue.Settled) }
    }
    SwipeToDismissBox(
        state = state, modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !c.isLive,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Filled.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        var menu by remember { mutableStateOf(false) }
        val name = c.file.name
        val icon = when {
            name.startsWith("cap_wlan") -> Icons.Outlined.Wifi
            name.startsWith("cap_rmnet") || name.startsWith("cap_ccmni") -> Icons.Outlined.SignalCellularAlt
            name.startsWith("imp_") -> Icons.Outlined.Description
            else -> Icons.AutoMirrored.Filled.ListAlt
        }
        ListItem(
            headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.MiddleEllipsis) },
            supportingContent = {
                Column {
                    Text(Tools.humanBytes(c.size) + " · " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(c.modified)),
                        style = MaterialTheme.typography.bodySmall)
                    if (c.isLive || c.isCurrent) Row(Modifier.padding(top = 4.dp)) {
                        if (c.isLive) TagChip(stringResource(R.string.saved_live), leading = { RecDot(6) })
                        else TagChip(stringResource(R.string.saved_current))
                    }
                }
            },
            leadingContent = {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                }
            },
            trailingContent = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options)) }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_open)) }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
                            enabled = !c.isLive, onClick = { menu = false; onOpen(c.file) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_share)) }, leadingIcon = { Icon(Icons.Outlined.Share, null) },
                            onClick = { menu = false; onShare(c.file) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_export)) }, leadingIcon = { Icon(Icons.Outlined.SaveAlt, null) },
                            onClick = { menu = false; onExport(c.file) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            enabled = !c.isLive, onClick = { menu = false; onDelete(c.file) })
                    }
                }
            },
            modifier = Modifier.clickable(enabled = !c.isLive) { onOpen(c.file) },
        )
    }
}

// ====================================================================================== statistics

data class Report(val title: Int, val desc: Int, val icon: ImageVector, val args: List<String>)

val REPORTS = listOf(
    Report(R.string.rep_phs, R.string.rep_phs_d, Icons.Outlined.AccountTree, listOf("-q", "-z", "io,phs")),
    Report(R.string.rep_conv_ip, R.string.rep_conv_d, Icons.Outlined.People, listOf("-q", "-z", "conv,ip")),
    Report(R.string.rep_conv_ipv6, R.string.rep_conv_d, Icons.Outlined.People, listOf("-q", "-z", "conv,ipv6")),
    Report(R.string.rep_conv_tcp, R.string.rep_conv_d, Icons.Outlined.People, listOf("-q", "-z", "conv,tcp")),
    Report(R.string.rep_conv_udp, R.string.rep_conv_d, Icons.Outlined.People, listOf("-q", "-z", "conv,udp")),
    Report(R.string.rep_endpoints, R.string.rep_endpoints_d, Icons.Outlined.Place, listOf("-q", "-z", "endpoints,ip")),
    Report(R.string.rep_expert, R.string.rep_expert_d, Icons.Outlined.ReportProblem, listOf("-q", "-z", "expert")),
    Report(R.string.rep_dns_q, R.string.rep_dns_q_d, Icons.Outlined.Dns, listOf("-Y", "dns.flags.response == 0", "-T", "fields", "-E", "separator=/t",
        "-e", "frame.number", "-e", "frame.time_relative", "-e", "ip.src", "-e", "dns.qry.name")),
    Report(R.string.rep_sni, R.string.rep_sni_d, Icons.Outlined.Lock, listOf("-Y", "tls.handshake.extensions_server_name", "-T", "fields", "-E", "separator=/t",
        "-e", "frame.number", "-e", "ip.dst", "-e", "ipv6.dst", "-e", "tls.handshake.extensions_server_name")),
    Report(R.string.rep_http, R.string.rep_http_d, Icons.Outlined.Http, listOf("-q", "-z", "http_req,tree")),
    Report(R.string.rep_dns_stats, R.string.rep_dns_stats_d, Icons.Outlined.QueryStats, listOf("-q", "-z", "dns,tree")),
    Report(R.string.rep_io, R.string.rep_io_d, Icons.Outlined.Timeline, listOf("-q", "-z", "io,stat,1")),
)

@Composable
fun StatsScreen(
    fileName: String?, packets: Int, bytes: Long, durationMs: Long, protoCounts: List<Pair<String, Int>>, dark: Boolean,
    onBack: () -> Unit, onReport: (Report) -> Unit,
) {
    Scaffold(topBar = { BackTopBar(stringResource(R.string.stats_title), onBack, fileName) }) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(top = inner.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomInset() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(stringResource(R.string.stat_packets), formatCount(packets.toLong()), Modifier.weight(1f))
                    SummaryCard(stringResource(R.string.stat_bytes), Tools.humanBytes(bytes), Modifier.weight(1f))
                }
            }
            item {
                val secs = durationMs / 1000.0
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(stringResource(R.string.stat_duration), formatDuration(durationMs), Modifier.weight(1f))
                    SummaryCard(stringResource(R.string.stat_rate),
                        if (secs > 0) stringResource(R.string.stat_rate_value, formatCount((packets / secs).toLong())) else "—", Modifier.weight(1f))
                }
            }
            if (protoCounts.isNotEmpty()) item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.stats_protocols), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        val max = protoCounts.maxOf { it.second }.coerceAtLeast(1)
                        protoCounts.forEach { (p, n) ->
                            val color = ProtoColors.colorFor(org.sharkdroid.PacketRow(0, "", "", "", p, 0, "", "eth:ip:" + guessLayer(p), 0, false), dark)
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(p, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp), maxLines = 1)
                                Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(n.toFloat() / max).clip(RoundedCornerShape(5.dp)).background(color))
                                }
                                Text(formatCount(n.toLong()), style = Mono.small, modifier = Modifier.width(64.dp).padding(start = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                        }
                        Text(stringResource(R.string.stats_protocols_note), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            item { Text(stringResource(R.string.stats_reports), style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
            items(REPORTS) { r ->
                OutlinedCard(onClick = { onReport(r) }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(stringResource(r.title)) },
                        supportingContent = { Text(stringResource(r.desc)) },
                        leadingContent = { Icon(r.icon, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
            }
        }
    }
}

private fun guessLayer(p: String): String = when (val l = p.lowercase()) {
    "tlsv1.2", "tlsv1.3", "tls", "ssl" -> "tcp:tls"
    "http", "http2" -> "tcp:$l"
    "quic" -> "udp:quic"
    "dns", "mdns" -> "udp:dns"
    "tcp" -> "tcp"
    "udp" -> "udp"
    else -> l
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier) {
    Card(modifier.height(96.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(4.dp))
            Text(value, style = Mono.stat.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
        }
    }
}

@Composable
fun ReportScreen(title: String, subtitle: String?, text: String?, onBack: () -> Unit, onCopy: (String) -> Unit) {
    Scaffold(topBar = {
        BackTopBar(title, onBack, subtitle) {
            if (!text.isNullOrEmpty()) IconButton(onClick = { onCopy(text) }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.action_copy_all)) }
        }
    }) { inner ->
        Box(Modifier.fillMaxSize().padding(top = inner.calculateTopPadding())) {
            if (text == null) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.report_running), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                SelectionContainer {
                    Text(text, style = Mono.small, softWrap = false,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomInset() + 16.dp))
                }
            }
        }
    }
}

// ====================================================================================== settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode, dynamicColor: Boolean, snaplen: Int, autoScroll: Boolean, versionName: String,
    onBack: () -> Unit, onTheme: (ThemeMode) -> Unit, onDynamic: (Boolean) -> Unit, onSnaplen: (Int) -> Unit,
    onAutoScroll: (Boolean) -> Unit, onLicenses: () -> Unit, onDiagnostics: () -> Unit,
) {
    Scaffold(topBar = { BackTopBar(stringResource(R.string.settings_title), onBack) }) { inner ->
        Column(Modifier.fillMaxSize().padding(top = inner.calculateTopPadding()).verticalScroll(rememberScrollState())
            .padding(bottom = bottomInset() + 16.dp)) {
            SectionHeader(R.string.settings_appearance)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                leadingContent = { Icon(Icons.Outlined.DarkMode, null) },
                supportingContent = {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        val opts = listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
                        opts.forEachIndexed { i, (m, label) ->
                            SegmentedButton(selected = themeMode == m, onClick = { onTheme(m) },
                                shape = SegmentedButtonDefaults.itemShape(i, opts.size)) { Text(stringResource(label)) }
                        }
                    }
                },
            )
            val dynAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            SwitchItem(Icons.Outlined.Palette, R.string.settings_dynamic,
                if (dynAvailable) R.string.settings_dynamic_d else R.string.settings_dynamic_na, dynamicColor && dynAvailable, dynAvailable, onDynamic)

            SectionHeader(R.string.settings_capture)
            var snapMenu by remember { mutableStateOf(false) }
            Box {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_snaplen)) },
                    supportingContent = { Text(stringResource(R.string.settings_snaplen_d, formatCount(snaplen.toLong()))) },
                    leadingContent = { Icon(Icons.Outlined.Straighten, null) },
                    trailingContent = { Icon(Icons.Filled.ExpandMore, null) },
                    modifier = Modifier.clickable { snapMenu = true },
                )
                DropdownMenu(snapMenu, onDismissRequest = { snapMenu = false }, offset = androidx.compose.ui.unit.DpOffset(56.dp, 0.dp)) {
                    Settings.SNAPLENS.forEach { v ->
                        DropdownMenuItem(text = { Text(stringResource(R.string.bytes_value, formatCount(v.toLong()))) },
                            onClick = { snapMenu = false; onSnaplen(v) })
                    }
                }
            }
            SwitchItem(Icons.Outlined.VerticalAlignBottom, R.string.settings_autoscroll, R.string.settings_autoscroll_d, autoScroll, true, onAutoScroll)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_resolution)) },
                supportingContent = { Text(stringResource(R.string.settings_resolution_d)) },
                leadingContent = { Icon(Icons.Outlined.Translate, null) },
            )

            SectionHeader(R.string.settings_privacy)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_security)) },
                supportingContent = { Text(stringResource(R.string.settings_security_d)) },
                leadingContent = { Icon(Icons.Outlined.Shield, null) },
            )

            SectionHeader(R.string.settings_about)
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = { Text(stringResource(R.string.settings_version, versionName, Tools.WIRESHARK_VERSION)) },
                leadingContent = { Icon(Icons.Outlined.Info, null) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.licenses_title)) },
                supportingContent = { Text(stringResource(R.string.licenses_d)) },
                leadingContent = { Icon(Icons.Outlined.Gavel, null) },
                modifier = Modifier.clickable(onClick = onLicenses),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_diagnostics)) },
                supportingContent = { Text(stringResource(R.string.diag_d)) },
                leadingContent = { Icon(Icons.Outlined.MonitorHeart, null) },
                modifier = Modifier.clickable(onClick = onDiagnostics),
            )
        }
    }
}

@Composable
private fun SwitchItem(icon: ImageVector, title: Int, desc: Int, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(desc)) },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange, enabled = enabled) },
        modifier = Modifier.clickable(enabled = enabled) { onChange(!checked) },
    )
}

// ====================================================================================== licenses

data class Component(val name: String, val version: String, val license: String, val asset: String)

val COMPONENTS = listOf(
    Component("SharkDroid", "", "GPL-2.0-or-later", "NOTICE.txt"),
    Component("Wireshark (tshark, dumpcap)", Tools.WIRESHARK_VERSION, "GPL-2.0-or-later", "GPL-2.0.txt"),
    Component("libpcap", "1.11.0", "BSD-3-Clause", "libpcap.txt"),
    Component("GLib", "2.88.3", "LGPL-2.1-or-later", "LGPL-2.1.txt"),
    Component("libgcrypt", "1.12.4", "LGPL-2.1-or-later", "LGPL-2.1.txt"),
    Component("libgpg-error", "1.61", "LGPL-2.1-or-later", "LGPL-2.1.txt"),
    Component("c-ares", "1.34.8", "MIT", "c-ares.txt"),
    Component("PCRE2", "10.49", "BSD-3-Clause WITH PCRE2-exception", "pcre2.txt"),
    Component("libxml2", "2.15.4", "MIT", "libxml2.txt"),
    Component("libffi", "3.8.0", "MIT", "libffi.txt"),
    Component("AndroidX, Jetpack Compose, Material Icons, Kotlin", "", "Apache-2.0", "Apache-2.0.txt"),
)

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf<Int?>(null) }
    Scaffold(topBar = { BackTopBar(stringResource(R.string.licenses_title), onBack) }) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(top = inner.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = bottomInset() + 16.dp)) {
            item {
                Text(stringResource(R.string.licenses_intro), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }
            items(COMPONENTS.size) { i ->
                val c = COMPONENTS[i]
                val expanded = open == i
                Column {
                    ListItem(
                        headlineContent = { Text(c.name) },
                        supportingContent = { Text(listOf(c.version, c.license).filter { it.isNotEmpty() }.joinToString(" · ")) },
                        trailingContent = { Icon(Icons.Filled.ExpandMore, null, Modifier.rotate(if (expanded) 180f else 0f)) },
                        modifier = Modifier.clickable { open = if (expanded) null else i },
                    )
                    AnimatedVisibility(expanded) {
                        var text by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(c.asset) {
                            text = withContext(Dispatchers.IO) {
                                runCatching { ctx.assets.open("licenses/" + c.asset).bufferedReader().readText() }.getOrDefault("")
                            }
                        }
                        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SelectionContainer {
                                Text(text ?: "…", style = Mono.small, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

// ====================================================================================== diagnostics

@Composable
fun DiagnosticsScreen(
    root: RootStatus, rootDetail: String, sandbox: String, libDir: String, ifaces: List<IfaceInfo>, dumpcapLog: String,
    streamCommand: String, snackbar: SnackbarHostState,
    onBack: () -> Unit, onRetryRoot: () -> Unit, onCopy: (String) -> Unit, runPcapList: suspend () -> String,
) {
    var pcap by remember { mutableStateOf<String?>(null) }
    var pcapRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val full = buildString {
        append("SharkDroid / Wireshark ${Tools.WIRESHARK_VERSION} arm64\nroot: $root $rootDetail\nsandbox: $sandbox\nlibDir: $libDir\n\n")
        ifaces.forEach { append("${it.name}\t${it.type}\ttype=${it.arpType}\tstate=${it.operState}\tflags=0x${Integer.toHexString(it.flags)}\t${it.addrs.joinToString(",")}\n") }
        append("\ndumpcap:\n").append(dumpcapLog)
    }
    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.action_diagnostics), onBack) {
            IconButton(onClick = { onCopy(full) }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.action_copy_all)) }
        } },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(top = inner.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomInset() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                DiagCard(Icons.Outlined.AdminPanelSettings, stringResource(R.string.diag_root),
                    stringResource(when (root) { RootStatus.OK -> R.string.diag_root_ok; RootStatus.DENIED -> R.string.diag_root_denied; else -> R.string.diag_root_checking }),
                    ok = root == RootStatus.OK) {
                    if (rootDetail.isNotEmpty()) Text(rootDetail, style = Mono.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (root != RootStatus.OK) TextButton(onClick = onRetryRoot) { Text(stringResource(R.string.action_retry)) }
                }
            }
            item { DiagCard(Icons.Outlined.Security, stringResource(R.string.diag_sandbox), sandbox, ok = sandbox.startsWith("✓")) {} }
            item {
                DiagCard(Icons.Outlined.Terminal, stringResource(R.string.diag_libdir), libDir, ok = null) {
                    FilledTonalButton(onClick = {
                        pcapRunning = true
                        scope.launch { pcap = runPcapList(); pcapRunning = false }
                    }, enabled = !pcapRunning) {
                        if (pcapRunning) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text("dumpcap -D")
                    }
                    pcap?.let { Text(it, style = Mono.small, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
            item {
                DiagCard(Icons.Outlined.Cast, stringResource(R.string.action_stream_pc), stringResource(R.string.stream_body), ok = null) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(8.dp)) {
                        SelectionContainer { Text(streamCommand, style = Mono.small, modifier = Modifier.padding(10.dp)) }
                    }
                    TextButton(onClick = { onCopy(streamCommand) }) {
                        Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_copy_command))
                    }
                }
            }
            item {
                DiagCard(Icons.Outlined.Insights, stringResource(R.string.diag_ifaces), stringResource(R.string.diag_ifaces_count, ifaces.size), ok = null) {
                    ifaces.forEach {
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text(it.name, style = Mono.small, modifier = Modifier.width(110.dp), maxLines = 1)
                            Text("${stringResource(it.type.label())} · ${it.operState} · ${it.displayAddrs.joinToString(" ")}",
                                style = Mono.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                DiagCard(Icons.Outlined.Policy, stringResource(R.string.diag_dumpcap_log), "", ok = null) {
                    Text(dumpcapLog.ifEmpty { stringResource(R.string.diag_none) }, style = Mono.small)
                }
            }
        }
    }
}

@Composable
private fun DiagCard(icon: ImageVector, title: String, value: String, ok: Boolean?, content: @Composable () -> Unit) {
    val ex = LocalExtraColors.current
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, modifier = Modifier.size(36.dp), color = when (ok) {
                    true -> ex.validContainer; false -> MaterialTheme.colorScheme.errorContainer; null -> MaterialTheme.colorScheme.secondaryContainer
                }) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp)) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.padding(top = 8.dp)) { content() }
        }
    }
}

@Composable
fun MessageDialog(text: String, onDismiss: () -> Unit, onCopy: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = { onCopy(text) }) { Text(stringResource(R.string.action_copy)) } },
        text = {
            SelectionContainer {
                Text(text, style = Mono.small, modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        },
    )
}

@Composable
fun StreamDialog(command: String, onDismiss: () -> Unit, onCopy: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Cast, null) },
        title = { Text(stringResource(R.string.action_stream_pc)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.stream_body))
                Spacer(Modifier.height(12.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(8.dp)) {
                    SelectionContainer { Text(command, style = Mono.small, modifier = Modifier.padding(10.dp)) }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.stream_note), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onCopy(command); onDismiss() }) { Text(stringResource(R.string.action_copy_command)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

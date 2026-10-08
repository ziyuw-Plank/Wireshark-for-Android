package org.sharkdroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import org.sharkdroid.CaptureManager
import org.sharkdroid.IfaceInfo
import org.sharkdroid.IfaceType
import org.sharkdroid.PacketRow
import org.sharkdroid.R
import org.sharkdroid.Tools

enum class MainMenu { OPEN, SAVED, STATS, EXPORT, SHARE, STREAM, DIAGNOSTICS, SETTINGS, CLEAR }
enum class MainSheet { NONE, IFACES, START }

/** Common display filters offered as suggestions (descriptions in R.array.filter_suggestion_desc). */
val FILTER_SUGGESTIONS = listOf(
    "dns", "tls.handshake.type == 1", "http.request", "quic", "tcp.port == 443",
    "tcp.analysis.flags", "icmp || icmpv6", "arp", "!(arp || dns || icmp)", "frame.len > 1000",
)
val CAPTURE_FILTER_SUGGESTIONS = listOf("tcp port 443", "udp port 53", "not port 5555", "icmp or icmp6", "tcp", "udp")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    ifaces: List<IfaceInfo>,
    ifacesLoading: Boolean,
    rows: List<PacketRow>,
    rowsVersion: Int,
    filterQuery: String,
    filterValidity: FilterValidity,
    filterError: String?,
    filterHistory: List<String>,
    captureFilter: String,
    snaplen: Int,
    autoScrollDefault: Boolean,
    snackbar: SnackbarHostState,
    onFilterQuery: (String) -> Unit,
    onApplyFilter: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSelectIface: (IfaceInfo) -> Unit,
    onRefreshIfaces: () -> Unit,
    onRetryRoot: () -> Unit,
    onStart: (String) -> Unit,
    onStop: () -> Unit,
    onPacketClick: (PacketRow) -> Unit,
    onMenu: (MainMenu) -> Unit,
    onMessage: (String) -> Unit,
    initialSheet: MainSheet = MainSheet.NONE,
    initialFilterExpanded: Boolean = false,
) {
    var sheet by rememberSaveable { mutableStateOf(initialSheet) }
    var follow by rememberSaveable { mutableStateOf(autoScrollDefault) }
    val idle = state.capture == CaptureManager.State.IDLE
    val listState = rememberLazyListState()
    val dark = LocalExtraColors.current.dark

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), maxLines = 1)
                        Text(
                            state.fileName ?: stringResource(R.string.subtitle_version, Tools.WIRESHARK_VERSION),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.MiddleEllipsis,
                        )
                    }
                },
                actions = { MainOverflow(state, onMenu) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            CaptureFab(state) {
                when (state.capture) {
                    CaptureManager.State.IDLE ->
                        if (state.root == RootStatus.OK) sheet = MainSheet.START
                        else onRetryRoot()
                    CaptureManager.State.STARTING, CaptureManager.State.CAPTURING -> onStop()
                    CaptureManager.State.STOPPING -> {}
                }
            }
        },
    ) { inner ->
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(Modifier.fillMaxSize().padding(top = inner.calculateTopPadding())) {
            AnimatedVisibility(state.root == RootStatus.DENIED) { RootBanner(onRetryRoot) }
            InterfaceSelector(state.iface, enabled = idle, loading = ifacesLoading || state.root == RootStatus.CHECKING) {
                sheet = MainSheet.IFACES
            }
            DisplayFilterBar(
                query = filterQuery, validity = filterValidity, error = filterError, history = filterHistory,
                applied = state.appliedFilter, initialExpanded = initialFilterExpanded,
                onQuery = onFilterQuery, onApply = onApplyFilter, onClearHistory = onClearHistory,
            )
            StatsBar(state, follow, onFollow = { follow = it })
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (rows.isEmpty() && rowsVersion >= 0) {
                    when {
                        state.capture != CaptureManager.State.IDLE -> EmptyState(
                            Icons.Outlined.WifiFind, stringResource(R.string.empty_waiting_title),
                            stringResource(R.string.empty_waiting_body, state.iface.name))
                        state.fileName != null && state.dissecting -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        state.fileName != null -> EmptyState(Icons.Filled.FilterAlt, stringResource(R.string.empty_filtered_title),
                            stringResource(R.string.empty_filtered_body))
                        else -> EmptyState(
                            Icons.Outlined.WifiFind, stringResource(R.string.empty_title), stringResource(R.string.empty_body),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { onMenu(MainMenu.OPEN) }) {
                                    Icon(Icons.Outlined.FileOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.action_open_file))
                                }
                                OutlinedButton(onClick = { onMenu(MainMenu.SAVED) }) {
                                    Icon(Icons.Outlined.Inventory2, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.action_saved))
                                }
                            }
                        }
                    }
                } else {
                    PacketList(rows, rowsVersion, listState, follow, dark, bottomInset,
                        onUserScrollUp = { follow = false }, onClick = onPacketClick, onApplyFilter = onApplyFilter, onMessage = onMessage)
                }
                val showJump by remember { derivedStateOf { listState.canScrollForward } }
                androidx.compose.animation.AnimatedVisibility(
                    visible = !follow && showJump && rows.isNotEmpty(),
                    enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = bottomInset + 92.dp),
                ) {
                    SmallFloatingActionButton(onClick = { follow = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(Icons.Filled.ArrowDownward, stringResource(R.string.action_jump_latest))
                    }
                }
            }
        }
    }

    when (sheet) {
        MainSheet.IFACES -> InterfaceSheet(ifaces, state.iface, ifacesLoading,
            onSelect = { onSelectIface(it); sheet = MainSheet.NONE }, onRefresh = onRefreshIfaces,
            onDismiss = { sheet = MainSheet.NONE })
        MainSheet.START -> StartCaptureSheet(state.iface, captureFilter, snaplen,
            onChangeIface = { sheet = MainSheet.IFACES },
            onStart = { onStart(it); sheet = MainSheet.NONE },
            onDismiss = { sheet = MainSheet.NONE })
        MainSheet.NONE -> {}
    }
}

@Composable
private fun MainOverflow(state: MainUiState, onMenu: (MainMenu) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val hasFile = state.fileName != null
    val idle = state.capture == CaptureManager.State.IDLE
    IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        @Composable
        fun item(text: Int, icon: ImageVector, m: MainMenu, enabled: Boolean = true) = DropdownMenuItem(
            text = { Text(stringResource(text)) }, leadingIcon = { Icon(icon, null) }, enabled = enabled,
            onClick = { open = false; onMenu(m) })
        item(R.string.action_open_file, Icons.Outlined.FileOpen, MainMenu.OPEN, idle)
        item(R.string.action_saved, Icons.Outlined.Inventory2, MainMenu.SAVED)
        HorizontalDivider()
        item(R.string.action_stats, Icons.Outlined.Analytics, MainMenu.STATS, hasFile)
        item(R.string.action_export, Icons.Outlined.SaveAlt, MainMenu.EXPORT, hasFile)
        item(R.string.action_share, Icons.Outlined.Share, MainMenu.SHARE, hasFile)
        item(R.string.action_clear, Icons.Outlined.DeleteSweep, MainMenu.CLEAR, hasFile && idle)
        HorizontalDivider()
        item(R.string.action_stream_pc, Icons.Outlined.Cast, MainMenu.STREAM)
        item(R.string.action_diagnostics, Icons.Outlined.MonitorHeart, MainMenu.DIAGNOSTICS)
        item(R.string.action_settings, Icons.Outlined.Settings, MainMenu.SETTINGS)
    }
}

@Composable
private fun RootBanner(onRetry: () -> Unit) {
    ElevatedCard(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.root_needed_title), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
                Text(stringResource(R.string.root_needed_body), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

@Composable
private fun InterfaceSelector(iface: IfaceInfo, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IfaceAvatar(iface, 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.label_interface), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(iface.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    TagChip(stringResource(iface.type.label()))
                    Spacer(Modifier.width(6.dp))
                    if (iface.type != IfaceType.ANY) StateChip(iface)
                }
            }
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Filled.ExpandMore, null, Modifier.alpha(if (enabled) 1f else 0.38f))
        }
    }
}

@Composable
fun IfaceAvatar(iface: IfaceInfo, sizeDp: Int) {
    val active = iface.type == IfaceType.ANY || (iface.isUp && iface.hasAddr)
    Surface(shape = CircleShape, modifier = Modifier.size(sizeDp.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
        Box(contentAlignment = Alignment.Center) {
            Icon(iface.type.icon(), null, Modifier.size((sizeDp * 0.55f).dp),
                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TagChip(text: String, container: Color = MaterialTheme.colorScheme.secondaryContainer,
            content: Color = MaterialTheme.colorScheme.onSecondaryContainer, leading: (@Composable () -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(8.dp), color = container) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) { leading(); Spacer(Modifier.width(4.dp)) }
            Text(text, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1)
        }
    }
}

@Composable
fun StateChip(iface: IfaceInfo) {
    val up = iface.isUp
    val ex = LocalExtraColors.current
    val (txt, bg, fg) = when {
        up && iface.hasAddr -> Triple(R.string.ifstate_up, ex.validContainer, ex.onValidContainer)
        up -> Triple(R.string.ifstate_noaddr, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        else -> Triple(R.string.ifstate_down, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    TagChip(stringResource(txt), bg, fg, leading = {
        Box(Modifier.size(6.dp).clip(CircleShape).background(fg))
    })
}

// ------------------------------------------------------------------------------------ filter bar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DisplayFilterBar(
    query: String, validity: FilterValidity, error: String?, history: List<String>, applied: String,
    initialExpanded: Boolean,
    onQuery: (String) -> Unit, onApply: (String) -> Unit, onClearHistory: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initialExpanded) }
    val ex = LocalExtraColors.current
    val cs = MaterialTheme.colorScheme
    val container = when (validity) {
        FilterValidity.VALID -> ex.validContainer
        FilterValidity.INVALID -> cs.errorContainer
        else -> cs.surfaceContainerHigh
    }
    val onContainer = when (validity) {
        FilterValidity.VALID -> ex.onValidContainer
        FilterValidity.INVALID -> cs.onErrorContainer
        else -> cs.onSurface
    }
    val descs = stringArrayResource(R.array.filter_suggestion_desc)
    fun submit(f: String) { onQuery(f); onApply(f); expanded = false }

    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQuery,
                onSearch = { submit(it) },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                placeholder = { Text(stringResource(R.string.filter_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    if (expanded) IconButton(onClick = { expanded = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    } else Icon(Icons.Filled.FilterList, null)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (validity) {
                            FilterValidity.CHECKING -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            FilterValidity.VALID -> Icon(Icons.Filled.CheckCircle, stringResource(R.string.filter_valid), tint = onContainer)
                            FilterValidity.INVALID -> Icon(Icons.Filled.ErrorOutline, stringResource(R.string.filter_invalid), tint = onContainer)
                            FilterValidity.EMPTY -> {}
                        }
                        if (query.isNotEmpty() || applied.isNotEmpty()) IconButton(onClick = { onQuery(""); if (applied.isNotEmpty()) onApply("") }) {
                            Icon(Icons.Filled.Clear, stringResource(R.string.action_clear_filter))
                        }
                    }
                },
                colors = SearchBarDefaults.inputFieldColors(focusedTextColor = onContainer, unfocusedTextColor = onContainer,
                    focusedContainerColor = container, unfocusedContainerColor = container),
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        colors = SearchBarDefaults.colors(containerColor = if (expanded) cs.surfaceContainerHigh else container),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
            if (validity == FilterValidity.INVALID && error != null) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = cs.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(error, style = MaterialTheme.typography.bodySmall, color = cs.error)
                }
            } else if (validity == FilterValidity.VALID) {
                Text(stringResource(R.string.filter_valid_hint), style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            if (history.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.filter_recent), style = MaterialTheme.typography.labelLarge,
                        color = cs.primary, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearHistory) { Text(stringResource(R.string.action_clear_history)) }
                }
                history.take(6).forEach { h ->
                    ListItem(
                        headlineContent = { Text(h, style = Mono.body, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(Icons.Filled.History, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.combinedClickableCompat { submit(h) },
                    )
                }
            }
            Text(stringResource(R.string.filter_suggestions), style = MaterialTheme.typography.labelLarge, color = cs.primary,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
            FILTER_SUGGESTIONS.forEachIndexed { i, f ->
                ListItem(
                    headlineContent = { Text(f, style = Mono.body) },
                    supportingContent = descs.getOrNull(i)?.let { d -> { Text(d) } },
                    leadingContent = { Icon(Icons.Filled.Lightbulb, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.combinedClickableCompat { onQuery(f) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit) = this.combinedClickable(onClick = onClick)

// ------------------------------------------------------------------------------------ stats bar

@Composable
private fun StatsBar(state: MainUiState, follow: Boolean, onFollow: (Boolean) -> Unit) {
    val capturing = state.capture != CaptureManager.State.IDLE
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Column {
            Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Stat(stringResource(R.string.stat_packets), formatCount(state.packets.toLong()) + if (state.truncated) "+" else "", Modifier.weight(1f))
                Stat(stringResource(R.string.stat_bytes), Tools.humanBytes(if (state.written >= 0) state.written else state.bytes), Modifier.weight(1f))
                Stat(stringResource(R.string.stat_duration), formatDuration(state.durationMs), Modifier.weight(1f),
                    leading = if (capturing) ({ RecDot() }) else null)
                IconToggleButton(checked = follow, onCheckedChange = onFollow) {
                    Icon(Icons.Filled.VerticalAlignBottom, stringResource(R.string.action_autoscroll),
                        tint = if (follow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.dissecting && !capturing) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
            else HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, leading: (@Composable () -> Unit)? = null) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) { leading(); Spacer(Modifier.width(6.dp)) }
            Text(value, style = Mono.stat, maxLines = 1)
        }
    }
}

@Composable
fun RecDot(sizeDp: Int = 8) {
    val t = rememberInfiniteTransition(label = "rec")
    val a by t.animateFloat(1f, 0.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "recAlpha")
    Box(Modifier.size(sizeDp.dp).alpha(a).clip(CircleShape).background(LocalExtraColors.current.recording))
}

// ------------------------------------------------------------------------------------ packet list

@Composable
private fun PacketList(
    rows: List<PacketRow>, rowsVersion: Int, listState: androidx.compose.foundation.lazy.LazyListState,
    follow: Boolean, dark: Boolean, bottomInset: androidx.compose.ui.unit.Dp,
    onUserScrollUp: () -> Unit, onClick: (PacketRow) -> Unit, onApplyFilter: (String) -> Unit, onMessage: (String) -> Unit,
) {
    val count = rows.size
    LaunchedEffect(rowsVersion, follow) {
        if (follow && count > 0) listState.scrollToItem(count - 1)
    }
    val nested = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) onUserScrollUp()
                return Offset.Zero
            }
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().nestedScroll(nested),
        contentPadding = PaddingValues(bottom = bottomInset + 96.dp),
    ) {
        items(count = count, key = { i -> rows.getOrNull(i)?.num ?: (-i - 1) }, contentType = { 0 }) { i ->
            val r = rows.getOrNull(i) ?: return@items
            PacketRowItem(r, dark, onClick, onApplyFilter, onMessage)
        }
    }
}

private val IPV4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
private val IPV6 = Regex("^[0-9A-Fa-f:]+:[0-9A-Fa-f:.]*$")
private val PROTO = Regex("^[A-Za-z][A-Za-z0-9.]*$")

/** Build a display filter for a packet-list address (IPv4/IPv6 only). */
fun addrFilter(a: String): String? = when {
    IPV4.matches(a) -> "ip.addr == $a"
    IPV6.matches(a) -> "ipv6.addr == $a"
    else -> null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PacketRowItem(r: PacketRow, dark: Boolean, onClick: (PacketRow) -> Unit, onApplyFilter: (String) -> Unit, onMessage: (String) -> Unit) {
    val color = ProtoColors.colorFor(r, dark)
    val tint = color.copy(alpha = if (dark) 0.10f else 0.06f)
    val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    var menu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val copied = stringResource(R.string.msg_copied)
    Box {
        Column(
            Modifier.fillMaxWidth()
                .combinedClickable(onClick = { onClick(r) }, onLongClick = { menu = true })
                .drawBehind {
                    drawRect(tint)
                    drawRect(color, size = Size(4.dp.toPx(), size.height))
                    val h = 0.5.dp.toPx()
                    drawRect(divider, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                }
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.num.toString(), style = Mono.small, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp), maxLines = 1)
                Text(r.time, style = Mono.small, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = if (dark) 0.22f else 0.14f)) {
                    Text(r.proto.ifEmpty { "?" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = color, maxLines = 1, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(r.len.toString(), style = Mono.small, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp), maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Text("${r.src}  →  ${r.dst}", style = Mono.small, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp))
            Text(r.info, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            val p = r.proto.lowercase()
            if (PROTO.matches(p)) DropdownMenuItem(text = { Text(stringResource(R.string.ctx_filter_value, p)) },
                leadingIcon = { Icon(Icons.Filled.FilterAlt, null) }, onClick = { menu = false; onApplyFilter(p) })
            addrFilter(r.src)?.let { f -> DropdownMenuItem(text = { Text(stringResource(R.string.ctx_filter_value, f)) },
                leadingIcon = { Icon(Icons.Filled.FilterAlt, null) }, onClick = { menu = false; onApplyFilter(f) }) }
            addrFilter(r.dst)?.let { f -> if (r.dst != r.src) DropdownMenuItem(text = { Text(stringResource(R.string.ctx_filter_value, f)) },
                leadingIcon = { Icon(Icons.Filled.FilterAlt, null) }, onClick = { menu = false; onApplyFilter(f) }) }
            DropdownMenuItem(text = { Text(stringResource(R.string.ctx_copy_row)) }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                onClick = {
                    menu = false
                    clipboard.setText(AnnotatedString("${r.num}\t${r.time}\t${r.src}\t${r.dst}\t${r.proto}\t${r.len}\t${r.info}"))
                    onMessage(copied)
                })
        }
    }
}

// ------------------------------------------------------------------------------------ FAB

@Composable
private fun CaptureFab(state: MainUiState, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val (container, content) = when (state.capture) {
        CaptureManager.State.IDLE -> cs.primaryContainer to cs.onPrimaryContainer
        else -> cs.errorContainer to cs.onErrorContainer
    }
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = container, contentColor = content,
        modifier = Modifier.padding(bottom = 4.dp),
        icon = {
            when (state.capture) {
                CaptureManager.State.IDLE -> Icon(Icons.Filled.FiberManualRecord, null, tint = LocalExtraColors.current.recording)
                CaptureManager.State.CAPTURING -> Icon(Icons.Filled.Stop, null)
                else -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = content)
            }
        },
        text = {
            when (state.capture) {
                CaptureManager.State.IDLE -> Text(stringResource(R.string.action_start_capture))
                CaptureManager.State.STARTING -> Text(stringResource(R.string.state_starting))
                CaptureManager.State.CAPTURING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.action_stop))
                    Spacer(Modifier.width(10.dp))
                    Text(formatDuration(state.durationMs), style = Mono.stat)
                }
                CaptureManager.State.STOPPING -> Text(stringResource(R.string.state_stopping))
            }
        },
    )
}

// ------------------------------------------------------------------------------------ sheets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceSheet(
    ifaces: List<IfaceInfo>, selected: IfaceInfo, loading: Boolean,
    onSelect: (IfaceInfo) -> Unit, onRefresh: () -> Unit, onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        InterfaceSheetContent(ifaces, selected, loading, onSelect, onRefresh)
    }
}

@Composable
fun InterfaceSheetContent(ifaces: List<IfaceInfo>, selected: IfaceInfo, loading: Boolean,
                          onSelect: (IfaceInfo) -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.sheet_iface_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.sheet_iface_body), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (loading) CircularProgressIndicator(Modifier.size(24.dp).padding(2.dp), strokeWidth = 2.dp)
            else IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, stringResource(R.string.action_refresh)) }
        }
        Spacer(Modifier.height(8.dp))
        val active = ifaces.filter { it.type == IfaceType.ANY || it.isUp }
        val inactive = ifaces.filter { it.type != IfaceType.ANY && !it.isUp }
        LazyColumn {
            item { SectionHeader(R.string.iface_group_active) }
            items(active.size, key = { "a" + active[it].name }) { IfaceRow(active[it], active[it].name == selected.name, onSelect) }
            if (inactive.isNotEmpty()) {
                item { SectionHeader(R.string.iface_group_inactive) }
                items(inactive.size, key = { "i" + inactive[it].name }) { IfaceRow(inactive[it], inactive[it].name == selected.name, onSelect) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IfaceRow(i: IfaceInfo, selected: Boolean, onSelect: (IfaceInfo) -> Unit) {
    ListItem(
        headlineContent = { Text(i.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)) {
                    TagChip(stringResource(i.type.label()))
                    if (i.type != IfaceType.ANY) StateChip(i)
                }
                val a = if (i.type == IfaceType.ANY) stringResource(R.string.iface_any_desc)
                    else i.displayAddrs.take(2).joinToString("  ").ifEmpty { stringResource(R.string.iface_no_addr) }
                Text(a, style = Mono.small, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        },
        leadingContent = { IfaceAvatar(i, 40) },
        trailingContent = { RadioButton(selected = selected, onClick = { onSelect(i) }) },
        colors = ListItemDefaults.colors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f) else Color.Transparent),
        modifier = Modifier.combinedClickableCompat { onSelect(i) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartCaptureSheet(
    iface: IfaceInfo, initialFilter: String, snaplen: Int,
    onChangeIface: () -> Unit, onStart: (String) -> Unit, onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        StartCaptureContent(iface, initialFilter, snaplen, onChangeIface, onStart, onDismiss)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartCaptureContent(
    iface: IfaceInfo, initialFilter: String, snaplen: Int,
    onChangeIface: () -> Unit, onStart: (String) -> Unit, onCancel: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(initialFilter) }
    val bad = !Tools.validCaptureFilter(filter)
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.sheet_start_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, onClick = onChangeIface) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IfaceAvatar(iface, 40)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(iface.name, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(iface.type.label()), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onChangeIface) { Text(stringResource(R.string.action_change)) }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = filter, onValueChange = { filter = it },
            label = { Text(stringResource(R.string.capture_filter_label)) },
            placeholder = { Text("tcp port 443", style = Mono.body) },
            leadingIcon = { Icon(Icons.Filled.FilterAlt, null) },
            trailingIcon = { if (filter.isNotEmpty()) IconButton(onClick = { filter = "" }) { Icon(Icons.Filled.Clear, stringResource(R.string.action_clear_filter)) } },
            supportingText = { Text(stringResource(if (bad) R.string.msg_bad_capture_filter else R.string.capture_filter_help)) },
            isError = bad, singleLine = true, textStyle = Mono.body,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (!bad) onStart(filter) }),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CAPTURE_FILTER_SUGGESTIONS.forEach { s ->
                SuggestionChip(onClick = { filter = s }, label = { Text(s, style = Mono.small) })
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.AutoMirrored.Filled.ShowChart, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.start_info, formatCount(snaplen.toLong())), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onStart(filter) }, enabled = !bad) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_start))
            }
        }
    }
}


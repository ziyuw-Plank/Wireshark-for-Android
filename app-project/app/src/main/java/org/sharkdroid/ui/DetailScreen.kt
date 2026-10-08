package org.sharkdroid.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.sharkdroid.Pdml
import org.sharkdroid.PdmlNode
import org.sharkdroid.R
import java.util.Locale

data class DetailData(
    val roots: List<PdmlNode> = emptyList(),
    val bytes: ByteArray = ByteArray(0),
    val extraHex: String = "",
    val loading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    frame: Int,
    summary: String,
    data: DetailData,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onApplyFilter: (String) -> Unit,
    onFollow: (proto: String, stream: Int) -> Unit,
    onMessage: (String) -> Unit,
    initialSelected: PdmlNode? = null,
    hexExpanded: Boolean = false,
) {
    var version by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(initialSelected) }
    val visible = remember(data.roots, version) { Pdml.flatten(data.roots) }
    val clipboard = LocalClipboardManager.current
    val copiedMsg = stringResource(R.string.msg_copied)
    fun copy(s: String) { clipboard.setText(AnnotatedString(s)); onMessage(copiedMsg) }
    val tcp = remember(data.roots) { Pdml.find(data.roots, "tcp.stream")?.show?.toIntOrNull() }
    val udp = remember(data.roots) { Pdml.find(data.roots, "udp.stream")?.show?.toIntOrNull() }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = if (hexExpanded) SheetValue.Expanded else SheetValue.PartiallyExpanded))

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(snackbar) },
        sheetPeekHeight = 240.dp,
        sheetContent = { HexPanel(data, selected) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.detail_title, frame), maxLines = 1)
                        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                actions = {
                    if (tcp != null || udp != null) IconButton(onClick = { if (tcp != null) onFollow("tcp", tcp) else onFollow("udp", udp!!) }) {
                        Icon(Icons.Outlined.Forum, stringResource(if (tcp != null) R.string.action_follow_tcp else R.string.action_follow_udp))
                    }
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options)) }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_expand_all)) }, leadingIcon = { Icon(Icons.Filled.UnfoldMore, null) },
                            onClick = { menu = false; setAll(data.roots, true); version++ })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_collapse_all)) }, leadingIcon = { Icon(Icons.Filled.UnfoldLess, null) },
                            onClick = { menu = false; setAll(data.roots, false); version++ })
                        if (udp != null && tcp != null) DropdownMenuItem(text = { Text(stringResource(R.string.action_follow_udp)) },
                            leadingIcon = { Icon(Icons.Outlined.Forum, null) }, onClick = { menu = false; onFollow("udp", udp) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_copy_details)) }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                            onClick = {
                                menu = false
                                val sb = StringBuilder()
                                fun walk(l: List<PdmlNode>) { l.forEach { sb.append("    ".repeat(it.depth)).append(it.label).append('\n'); walk(it.children) } }
                                walk(data.roots); sb.append('\n').append(hexDump(data.bytes))
                                copy(sb.toString())
                            })
                    }
                },
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                data.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                data.roots.isEmpty() -> EmptyState(Icons.Outlined.ErrorOutline, stringResource(R.string.detail_failed),
                    data.error ?: "")
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 260.dp)) {
                    items(visible.size) { i ->
                        val n = visible[i]
                        TreeRow(n, n === selected,
                            onClick = {
                                if (n.children.isNotEmpty()) { n.expanded = !n.expanded; version++ }
                                selected = n
                            },
                            onApply = { f -> onApplyFilter(f) },
                            onCopy = { copy(it) })
                    }
                }
            }
        }
    }
}

private fun setAll(l: List<PdmlNode>, v: Boolean) { l.forEach { it.expanded = v; setAll(it.children, v) } }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeRow(n: PdmlNode, selected: Boolean, onClick: () -> Unit, onApply: (String) -> Unit, onCopy: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (n.expanded) 90f else 0f, label = "chev")
    val cs = MaterialTheme.colorScheme
    Box {
        Row(
            Modifier.fillMaxWidth()
                .background(if (selected) cs.secondaryContainer else Color.Transparent)
                .combinedClickable(onClick = onClick, onLongClick = { onClick(); menu = true })
                .padding(start = (8 + n.depth * 16).dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (n.children.isNotEmpty()) Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp).rotate(rot), tint = cs.onSurfaceVariant)
            else Spacer(Modifier.width(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(n.label,
                style = if (n.depth == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                color = if (selected) cs.onSecondaryContainer else cs.onSurface)
        }
        DropdownMenu(menu, onDismissRequest = { menu = false }) {
            val f = Pdml.filterFor(n)
            if (f != null) {
                DropdownMenuItem(text = { Text(stringResource(R.string.ctx_apply_filter)) }, leadingIcon = { Icon(Icons.Filled.FilterAlt, null) },
                    onClick = { menu = false; onApply(f) })
                DropdownMenuItem(text = { Text(stringResource(R.string.ctx_copy_filter)) }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                    onClick = { menu = false; onCopy(f) })
            }
            n.show?.let { v -> DropdownMenuItem(text = { Text(stringResource(R.string.ctx_copy_value)) }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                onClick = { menu = false; onCopy(v) }) }
            DropdownMenuItem(text = { Text(stringResource(R.string.ctx_copy_line)) }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                onClick = { menu = false; onCopy(n.label) })
        }
    }
}

@Composable
private fun HexPanel(data: DetailData, selected: PdmlNode?) {
    val cs = MaterialTheme.colorScheme
    val b = data.bytes
    val hl = selected?.takeIf { it.pos >= 0 && it.size > 0 && it.pos + it.size <= b.size }
    val hiBg = cs.primary.copy(alpha = 0.28f)
    val dim = cs.onSurfaceVariant
    Column(Modifier.fillMaxWidth().height(560.dp)) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.hex_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(if (hl != null) stringResource(R.string.hex_selection, hl.pos, hl.size) else stringResource(R.string.hex_total, b.size),
                style = Mono.small, color = dim)
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            // 16 bytes per row needs ~72 monospace columns; phones in portrait get 8.
            val per = if (maxWidth >= 600.dp) 16 else 8
            val lines = (b.size + per - 1) / per
            val state = rememberLazyListState()
            LaunchedEffect(hl, per) { if (hl != null) state.animateScrollToItem((hl.pos / per - 1).coerceAtLeast(0)) }
            LazyColumn(state = state, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), modifier = Modifier.fillMaxSize()) {
                items(lines) { line ->
                    val text = remember(b, line, hl, per) {
                        buildAnnotatedString {
                            val off = line * per
                            withStyle(SpanStyle(color = dim)) { append(String.format(Locale.US, "%04x  ", off)) }
                            for (j in 0 until per) {
                                val i = off + j
                                val gap = if (j == 7 && per == 16) "  " else " "
                                if (i < b.size) {
                                    val on = hl != null && i >= hl.pos && i < hl.pos + hl.size
                                    val hx = String.format(Locale.US, "%02x", b[i].toInt() and 0xff)
                                    if (on) withStyle(SpanStyle(background = hiBg)) { append(hx) } else append(hx)
                                    append(gap)
                                } else append("  $gap")
                            }
                            append("  ")
                            for (j in 0 until per) {
                                val i = off + j
                                if (i >= b.size) break
                                val c = b[i].toInt() and 0xff
                                val ch = if (c in 0x20..0x7e) c.toChar().toString() else "·"
                                val on = hl != null && i >= hl.pos && i < hl.pos + hl.size
                                if (on) withStyle(SpanStyle(background = hiBg)) { append(ch) }
                                else withStyle(SpanStyle(color = if (c in 0x20..0x7e) cs.onSurface else dim)) { append(ch) }
                            }
                        }
                    }
                    Text(text, style = Mono.body, softWrap = false, modifier = Modifier.padding(vertical = 1.dp))
                }
                if (data.extraHex.isNotEmpty()) item {
                    Text(data.extraHex, style = Mono.small, softWrap = false, color = dim,
                        modifier = Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()))
                }
            }
        }
    }
}

fun hexDump(b: ByteArray): String {
    val sb = StringBuilder()
    var off = 0
    while (off < b.size) {
        sb.append(String.format(Locale.US, "%04x  ", off))
        for (j in 0 until 16) {
            if (off + j < b.size) sb.append(String.format(Locale.US, "%02x ", b[off + j].toInt() and 0xff)) else sb.append("   ")
        }
        sb.append(' ')
        for (j in 0 until 16) {
            if (off + j >= b.size) break
            val c = b[off + j].toInt() and 0xff
            sb.append(if (c in 0x20..0x7e) c.toChar() else '.')
        }
        sb.append('\n')
        off += 16
    }
    return sb.toString()
}

/** Parse `tshark -x` output: first block = frame bytes; other data sources kept as text. */
fun parseHex(out: String, sourceLabel: (Int) -> String): Pair<ByteArray, String> {
    val lineRe = Regex("^([0-9a-f]{4,8})  ((?:[0-9a-f]{2} ?){1,16})")
    val blocks = ArrayList<Pair<String, java.io.ByteArrayOutputStream>>()
    var cur: java.io.ByteArrayOutputStream? = null
    var title = ""
    for (line in out.lineSequence()) {
        val m = lineRe.find(line)
        if (m != null) {
            if (cur == null) { cur = java.io.ByteArrayOutputStream(); blocks.add(title to cur) }
            m.groupValues[2].trim().split(' ').filter { it.length == 2 }.forEach { cur.write(it.toInt(16)) }
        } else if (line.isBlank()) {
            cur = null; title = ""
        } else { cur = null; title = line.trim() }
    }
    if (blocks.isEmpty()) return ByteArray(0) to ""
    val extra = StringBuilder()
    for (i in 1 until blocks.size) {
        extra.append('\n').append(blocks[i].first.ifEmpty { sourceLabel(i) }).append('\n')
        extra.append(hexDump(blocks[i].second.toByteArray()))
    }
    return blocks[0].second.toByteArray() to extra.toString()
}

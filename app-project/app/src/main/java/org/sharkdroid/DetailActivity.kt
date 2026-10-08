package org.sharkdroid

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.StringReader
import java.util.Locale
import kotlin.concurrent.thread

class DetailActivity : Activity() {
    private lateinit var file: File
    private var frame = 0
    private var roots: List<PdmlNode> = emptyList()
    private var visible: List<PdmlNode> = emptyList()
    private var selected: PdmlNode? = null
    private var frameBytes: ByteArray = ByteArray(0)
    private var extraHex: String = ""

    private lateinit var tree: ListView
    private lateinit var hex: TextView
    private lateinit var progress: ProgressBar
    private val treeAdapter = TreeAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        file = File(intent.getStringExtra("file") ?: run { finish(); return })
        frame = intent.getIntExtra("frame", 0)
        if (!Tools.isCaptureFile(this, file) || frame <= 0) { finish(); return }
        title = "第 $frame 包 · " + (intent.getStringExtra("summary") ?: "")

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { isIndeterminate = true }
        root.addView(progress, lp(MATCH, WRAP))
        tree = ListView(this)
        tree.adapter = treeAdapter
        tree.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val n = visible[pos]
            if (n.children.isNotEmpty()) n.expanded = !n.expanded
            selected = n
            refreshTree(); renderHex()
        }
        tree.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ -> nodeMenu(visible[pos]); true }
        root.addView(tree, lp(MATCH, 0, 3f))
        val div = View(this).apply { setBackgroundColor(Color.GRAY) }
        root.addView(div, lp(MATCH, dp(1)))
        hex = mono(11f).apply { setPadding(dp(6), dp(4), dp(6), dp(4)); setTextIsSelectable(true) }
        val hs = HorizontalScrollView(this).apply { addView(hex) }
        val vs = ScrollView(this).apply { addView(hs) }
        root.addView(vs, lp(MATCH, 0, 2f))
        setContentView(root)
        load()
    }

    private fun load() {
        val sel = listOf("-c", frame.toString(), "-Y", "frame.number == $frame")
        var pdmlDone = false; var hexDone = false
        fun finishOne() { if (pdmlDone && hexDone) progress.visibility = View.GONE }
        thread {
            val (rc, out, err) = Dissector.runOnFile(this, file, sel + listOf("-T", "pdml"))
            val parsed = try { Pdml.parse(StringReader(out)) } catch (e: Exception) { emptyList() }
            runOnUiThread {
                roots = parsed
                if (parsed.isEmpty()) toast("解析失败（$rc）：" + Dissector.cleanStderr(err).take(300))
                refreshTree(); pdmlDone = true; finishOne(); invalidateOptionsMenu()
            }
        }
        thread {
            val (_, out, _) = Dissector.runOnFile(this, file, sel + listOf("-x"))
            val (bytes, extra) = parseHex(out)
            runOnUiThread { frameBytes = bytes; extraHex = extra; renderHex(); hexDone = true; finishOne() }
        }
    }

    private fun refreshTree() { visible = Pdml.flatten(roots); treeAdapter.notifyDataSetChanged() }

    /** Parse `tshark -x` output: first block = frame bytes; other data sources kept as text. */
    private fun parseHex(out: String): Pair<ByteArray, String> {
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
            extra.append('\n').append(blocks[i].first.ifEmpty { "数据源 $i" }).append('\n')
            extra.append(hexDump(blocks[i].second.toByteArray()))
        }
        return blocks[0].second.toByteArray() to extra.toString()
    }

    private fun hexDump(b: ByteArray): String {
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

    private fun renderHex() {
        val main = hexDump(frameBytes)
        val s = SpannableString(main + extraHex)
        val n = selected
        if (n != null && n.pos >= 0 && n.size > 0 && n.pos + n.size <= frameBytes.size) {
            val color = if (isNight()) Color.parseColor("#664FC3F7") else Color.parseColor("#994FC3F7")
            for (i in n.pos until n.pos + n.size) {
                val line = i / 16; val col = i % 16
                val lineStart = line * (6 + 48 + 1 + 16 + 1) - 0
                // all lines except the last are full width (6+48+1+16+1 = 72 chars)
                val hexAt = lineStart + 6 + col * 3
                val ascAt = lineStart + 6 + 48 + 1 + col
                if (hexAt + 2 <= s.length) s.setSpan(BackgroundColorSpan(color), hexAt, hexAt + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (ascAt + 1 <= s.length) s.setSpan(BackgroundColorSpan(color), ascAt, ascAt + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        hex.text = s
    }

    private fun nodeMenu(n: PdmlNode) {
        val f = Pdml.filterFor(n)
        val items = ArrayList<String>()
        if (f != null) { items += "设为显示过滤器：$f"; items += "复制过滤表达式" }
        items += "复制此行"
        AlertDialog.Builder(this).setTitle(n.label.take(80)).setItems(items.toTypedArray()) { _, which ->
            when (items[which]) {
                "复制此行" -> copy(n.label)
                "复制过滤表达式" -> copy(f!!)
                else -> { setResult(RESULT_OK, Intent().putExtra("filter", f)); finish() }
            }
        }.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (Pdml.find(roots, "tcp.stream") != null) menu.add(0, 1, 0, "追踪 TCP 流")
        if (Pdml.find(roots, "udp.stream") != null) menu.add(0, 2, 0, "追踪 UDP 流")
        menu.add(0, 3, 0, "全部展开")
        menu.add(0, 4, 0, "复制详情文本")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1, 2 -> {
                val proto = if (item.itemId == 1) "tcp" else "udp"
                val id = Pdml.find(roots, "$proto.stream")?.show?.toIntOrNull() ?: return true
                startActivity(Intent(this, TextActivity::class.java)
                    .putExtra("title", "追踪 ${proto.uppercase()} 流 #$id")
                    .putExtra("file", file.path)
                    .putExtra("args", arrayOf("-q", "-z", "follow,$proto,ascii,$id")))
            }
            3 -> { fun all(l: List<PdmlNode>) { l.forEach { it.expanded = true; all(it.children) } }; all(roots); refreshTree() }
            4 -> {
                val sb = StringBuilder()
                fun walk(l: List<PdmlNode>) { l.forEach { sb.append("    ".repeat(it.depth)).append(it.label).append('\n'); walk(it.children) } }
                walk(roots); sb.append('\n').append(hexDump(frameBytes)); copy(sb.toString())
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun copy(s: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("SharkDroid", s))
        toast("已复制")
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    private inner class TreeAdapter : BaseAdapter() {
        override fun getCount() = visible.size
        override fun getItem(position: Int) = visible[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val tv = (convertView as? TextView) ?: mono(12f).apply { setPadding(dp(6), dp(5), dp(6), dp(5)) }
            val n = visible[position]
            val arrow = if (n.children.isEmpty()) "  " else if (n.expanded) "▾ " else "▸ "
            tv.text = "    ".repeat(n.depth) + arrow + n.label
            tv.setBackgroundColor(if (n === selected) Color.parseColor("#334FC3F7") else Color.TRANSPARENT)
            return tv
        }
    }
}

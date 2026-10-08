package org.sharkdroid

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), CaptureManager.Listener {

    companion object {
        private const val REQ_OPEN = 1
        private const val REQ_EXPORT = 2
        private const val REQ_DETAIL = 3
        private const val PREFS = "ui"
    }

    private lateinit var ifaceSpinner: Spinner
    private lateinit var captureFilter: EditText
    private lateinit var displayFilter: EditText
    private lateinit var startStop: Button
    private lateinit var status: TextView
    private lateinit var list: ListView
    private lateinit var adapter: PacketAdapter

    private var ifaces: List<IfaceInfo> = listOf(IfaceInfo.ANY)
    private var rootInfo: String = "正在检测 root…"
    private var rootOk: Boolean? = null
    private var pendingExport: File? = null
    private val ticker = object : Runnable {
        override fun run() {
            if (CaptureManager.state != CaptureManager.State.IDLE) refreshStatusLine()
            list.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "SharkDroid · Wireshark ${Tools.WIRESHARK_VERSION}"
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), 0)
        }

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        ifaceSpinner = Spinner(this)
        row1.addView(ifaceSpinner, lp(0, WRAP, 1f))
        row1.addView(smallButton("↻") { loadInterfaces() }, lp(WRAP, WRAP))
        root.addView(row1, lp(MATCH, WRAP))

        captureFilter = EditText(this).apply {
            hint = "捕获过滤器 (BPF)，如 tcp port 443 或 host 1.2.3.4"
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            textSize = 14f
            setText(prefs.getString("cfilter", ""))
        }
        root.addView(captureFilter, lp(MATCH, WRAP))

        val row3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        displayFilter = EditText(this).apply {
            hint = "显示过滤器，如 dns || tls.handshake"
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imeOptions = EditorInfo.IME_ACTION_DONE
            textSize = 14f
            setText(prefs.getString("dfilter", ""))
            setOnEditorActionListener { _, _, _ -> applyDisplayFilter(); true }
        }
        row3.addView(displayFilter, lp(0, WRAP, 1f))
        row3.addView(smallButton("应用") { applyDisplayFilter() }, lp(WRAP, WRAP))
        root.addView(row3, lp(MATCH, WRAP))

        val row4 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        startStop = smallButton("开始抓包") { toggleCapture() }
        row4.addView(startStop, lp(0, WRAP, 1f))
        row4.addView(smallButton("打开") { openDocument() }, lp(WRAP, WRAP))
        row4.addView(smallButton("清空") { CaptureManager.clear() }, lp(WRAP, WRAP))
        root.addView(row4, lp(MATCH, WRAP))

        status = mono(11f).apply { setPadding(dp(2), dp(2), dp(2), dp(2)) }
        root.addView(status, lp(MATCH, WRAP))

        list = ListView(this).apply {
            isFastScrollEnabled = true
            transcriptMode = AbsListView.TRANSCRIPT_MODE_NORMAL
        }
        adapter = PacketAdapter(this)
        list.adapter = adapter
        list.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ -> openDetail(adapter.getItem(pos)) }
        list.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ ->
            val r = adapter.getItem(pos)
            copyText("${r.num}\t${r.time}\t${r.src}\t${r.dst}\t${r.proto}\t${r.len}\t${r.info}")
            true
        }
        root.addView(list, lp(MATCH, 0, 1f))
        setContentView(root)

        CaptureManager.addListener(this)
        refreshState()
        checkRootAndLoad()
        list.postDelayed(ticker, 1000)
    }

    override fun onDestroy() {
        CaptureManager.removeListener(this)
        list.removeCallbacks(ticker)
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString("cfilter", captureFilter.text.toString())
            .putString("dfilter", displayFilter.text.toString())
            .putString("iface", selectedIface()?.name)
            .apply()
        super.onDestroy()
    }

    // ------------------------------------------------------------------ root & interfaces

    private fun checkRootAndLoad() {
        thread {
            val r = RootHelper.run(this, "id")
            val ok = r.exit == 0 && r.stdout.contains("uid=0")
            runOnUiThread {
                rootOk = ok
                rootInfo = if (ok) "root ✓ " + r.stdout.trim().substringAfter("ctx=", "").let { if (it.isNotEmpty()) "($it)" else "" }
                else "未获得 root"
                refreshState()
                if (ok) loadInterfaces()
                else AlertDialog.Builder(this)
                    .setTitle("需要 root 权限")
                    .setMessage("实时抓包需要 root。请在 Magisk / KernelSU / APatch 中允许 SharkDroid 使用超级用户，然后点 ↻ 重试。\n\n没有 root 时仍可打开并分析已有的 pcap/pcapng 文件。\n\n详情：" +
                        (r.stderr.trim().ifEmpty { r.stdout.trim() }.ifEmpty { "exit=${r.exit}" }))
                    .setPositiveButton("重试") { _, _ -> checkRootAndLoad() }
                    .setNegativeButton("关闭", null)
                    .show()
            }
        }
    }

    private fun loadInterfaces() {
        thread {
            val r = RootHelper.run(this, "list")
            val parsed = IfaceInfo.parse(r.stdout)
            runOnUiThread {
                if (r.exit != 0 || parsed.isEmpty()) {
                    if (rootOk == true) toast("读取接口失败：" + r.stderr.trim().take(200))
                    return@runOnUiThread
                }
                ifaces = listOf(IfaceInfo.ANY) + parsed
                val ad = ArrayAdapter(this, android.R.layout.simple_spinner_item, ifaces.map { it.label() })
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                ifaceSpinner.adapter = ad
                val saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString("iface", null)
                val pick = ifaces.firstOrNull { it.name == saved && it.isUp } ?: IfaceInfo.pickDefault(parsed)
                ifaceSpinner.setSelection(ifaces.indexOfFirst { it.name == pick.name }.coerceAtLeast(0))
            }
        }
    }

    private fun selectedIface(): IfaceInfo? = ifaces.getOrNull(ifaceSpinner.selectedItemPosition)

    // ------------------------------------------------------------------ actions

    private fun toggleCapture() {
        when (CaptureManager.state) {
            CaptureManager.State.IDLE -> {
                val ifc = selectedIface() ?: IfaceInfo.ANY
                val cf = captureFilter.text.toString().trim()
                val df = displayFilter.text.toString().trim()
                if (!Tools.validCaptureFilter(cf)) { toast("捕获过滤器只能包含可打印 ASCII 字符"); return }
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 9)
                }
                hideKeyboard()
                CaptureManager.startCapture(this, ifc.name, cf, df)
            }
            CaptureManager.State.CAPTURING, CaptureManager.State.STARTING -> CaptureManager.stopCapture()
            CaptureManager.State.STOPPING -> {}
        }
    }

    private fun applyDisplayFilter() {
        hideKeyboard()
        val df = displayFilter.text.toString().trim()
        if (!Tools.validDisplayFilter(df)) { toast("显示过滤器含非法字符"); return }
        if (CaptureManager.file == null) { toast("还没有抓包或打开文件"); return }
        CaptureManager.applyDisplayFilter(this, df)
    }

    private fun openDetail(r: PacketRow) {
        val f = CaptureManager.file ?: return
        startActivityForResult(Intent(this, DetailActivity::class.java)
            .putExtra("file", f.path).putExtra("frame", r.num)
            .putExtra("summary", "${r.proto} ${r.src} → ${r.dst}"), REQ_DETAIL)
    }

    private fun openDocument() {
        if (CaptureManager.state != CaptureManager.State.IDLE) { toast("请先停止抓包"); return }
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
        startActivityForResult(i, REQ_OPEN)
    }

    private fun exportFile(f: File) {
        pendingExport = f
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE, f.name)
        startActivityForResult(i, REQ_EXPORT)
    }

    private fun shareFile(f: File) {
        if (!Tools.isCaptureFile(this, f)) return
        val uri = Uri.parse("content://$packageName.captures/${Uri.encode(f.name)}")
        val send = Intent(Intent.ACTION_SEND).setType("application/vnd.tcpdump.pcap")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(f.name, uri)
        startActivity(Intent.createChooser(send, "分享 ${f.name}"))
    }

    @Deprecated("framework API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            REQ_OPEN -> data?.data?.let { importUri(it) }
            REQ_EXPORT -> { val f = pendingExport; val u = data?.data; if (f != null && u != null) copyOut(f, u) }
            REQ_DETAIL -> data?.getStringExtra("filter")?.let { displayFilter.setText(it); applyDisplayFilter() }
        }
    }

    private fun importUri(uri: Uri) {
        var name = "imported"
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0) ?: name
            }
        } catch (_: Exception) { }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dest = File(Tools.capturesDir(this), "imp_${ts}_" + Tools.safeFileName(name))
        status.text = "正在导入 $name …"
        thread {
            try {
                contentResolver.openInputStream(uri)!!.use { inp -> dest.outputStream().use { inp.copyTo(it, 65536) } }
                runOnUiThread { CaptureManager.openFile(this, dest, displayFilter.text.toString().trim()) }
            } catch (e: Exception) {
                dest.delete()
                runOnUiThread { toast("导入失败：${e.message}"); refreshState() }
            }
        }
    }

    private fun copyOut(f: File, uri: Uri) {
        thread {
            try {
                contentResolver.openOutputStream(uri, "w")!!.use { out -> f.inputStream().use { it.copyTo(out, 65536) } }
                runOnUiThread { toast("已导出 ${f.name}") }
            } catch (e: Exception) { runOnUiThread { toast("导出失败：${e.message}") } }
        }
    }

    // ------------------------------------------------------------------ menu

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "已保存的抓包…")
        menu.add(0, 2, 0, "导出当前文件…")
        menu.add(0, 3, 0, "分享当前文件…")
        val st = menu.addSubMenu(0, 4, 0, "统计")
        STATS.forEachIndexed { i, s -> st.add(1, 100 + i, i, s.first) }
        menu.add(0, 5, 0, "推送到电脑 Wireshark")
        menu.add(0, 6, 0, "抓包日志 / 诊断")
        menu.add(0, 7, 0, "关于与安全")
        return true
    }

    private val STATS: List<Pair<String, List<String>>> = listOf(
        "协议分级" to listOf("-q", "-z", "io,phs"),
        "会话 IPv4" to listOf("-q", "-z", "conv,ip"),
        "会话 IPv6" to listOf("-q", "-z", "conv,ipv6"),
        "会话 TCP" to listOf("-q", "-z", "conv,tcp"),
        "会话 UDP" to listOf("-q", "-z", "conv,udp"),
        "端点 IPv4" to listOf("-q", "-z", "endpoints,ip"),
        "专家信息" to listOf("-q", "-z", "expert"),
        "DNS 查询列表" to listOf("-Y", "dns.flags.response == 0", "-T", "fields", "-E", "separator=/t",
            "-e", "frame.number", "-e", "frame.time_relative", "-e", "ip.src", "-e", "dns.qry.name"),
        "TLS 服务器名 (SNI)" to listOf("-Y", "tls.handshake.extensions_server_name", "-T", "fields", "-E", "separator=/t",
            "-e", "frame.number", "-e", "ip.dst", "-e", "ipv6.dst", "-e", "tls.handshake.extensions_server_name"),
        "HTTP 请求" to listOf("-q", "-z", "http_req,tree"),
        "DNS 统计" to listOf("-q", "-z", "dns,tree"),
        "IO 统计 (每秒)" to listOf("-q", "-z", "io,stat,1"),
    )

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val cur = CaptureManager.file
        when (item.itemId) {
            1 -> showSavedCaptures()
            2 -> if (cur != null && cur.exists()) exportFile(cur) else toast("没有当前文件")
            3 -> if (cur != null && cur.exists()) shareFile(cur) else toast("没有当前文件")
            5 -> showStreamToPc()
            6 -> showDiagnostics()
            7 -> showAbout()
            in 100..199 -> {
                val s = STATS[item.itemId - 100]
                if (cur == null || !cur.exists()) toast("没有当前文件")
                else startActivity(Intent(this, TextActivity::class.java).putExtra("title", s.first)
                    .putExtra("file", cur.path).putExtra("args", s.second.toTypedArray()))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showSavedCaptures() {
        val files = (Tools.capturesDir(this).listFiles() ?: emptyArray()).filter { it.isFile }.sortedByDescending { it.lastModified() }
        if (files.isEmpty()) { toast("还没有保存的抓包"); return }
        val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.US)
        val labels = files.map { "${it.name}\n${Tools.humanBytes(it.length())} · ${fmt.format(Date(it.lastModified()))}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("已保存的抓包（应用私有目录）")
            .setItems(labels) { _, which ->
                val f = files[which]
                AlertDialog.Builder(this).setTitle(f.name)
                    .setItems(arrayOf("打开", "导出到…", "分享…", "删除")) { _, op ->
                        when (op) {
                            0 -> CaptureManager.openFile(this, f, displayFilter.text.toString().trim())
                            1 -> exportFile(f)
                            2 -> shareFile(f)
                            3 -> confirmDelete(f)
                        }
                    }.show()
            }.setNegativeButton("关闭", null).show()
    }

    private fun confirmDelete(f: File) {
        if (CaptureManager.state != CaptureManager.State.IDLE && CaptureManager.file == f) { toast("正在抓包，不能删除"); return }
        AlertDialog.Builder(this).setMessage("删除 ${f.name}？")
            .setPositiveButton("删除") { _, _ ->
                if (CaptureManager.file == f) CaptureManager.clear()
                if (Tools.isCaptureFile(this, f)) f.delete()
            }.setNegativeButton("取消", null).show()
    }

    private fun showStreamToPc() {
        val ifc = selectedIface()?.name ?: "wlan0"
        val dumpcap = Tools.dumpcap(this)
        val cmd = "adb exec-out \"su -c '$dumpcap -i $ifc -F pcapng -q -w -'\" | wireshark -k -i -"
        val msg = "用 USB 数据线连接电脑并开启 USB 调试，在电脑上运行（Windows 用 PowerShell 时请改用 cmd，或把 wireshark 换成 Wireshark.exe 的完整路径）：\n\n$cmd\n\n" +
            "说明：数据只经过已授权的 adb 通道，本应用不会开放任何网络端口。停止时在电脑上关闭 Wireshark 或 Ctrl+C。"
        val tv = mono(12f).apply { text = msg; setTextIsSelectable(true); setPadding(dp(16), dp(8), dp(16), dp(8)) }
        AlertDialog.Builder(this).setTitle("推送到电脑 Wireshark").setView(ScrollView(this).apply { addView(tv) })
            .setPositiveButton("复制命令") { _, _ -> copyText(cmd) }.setNegativeButton("关闭", null).show()
    }

    private fun showDiagnostics() {
        val sb = StringBuilder()
        sb.append("Wireshark ${Tools.WIRESHARK_VERSION} (tshark/dumpcap, arm64)\n")
        sb.append("Root：$rootInfo\n")
        sb.append("解析沙箱：").append(when (Dissector.sandboxed) {
            true -> "隔离进程 ✓ (uid ${Dissector.sandboxUid})"
            false -> "不可用，已回退为应用 UID 运行（${Dissector.sandboxError ?: ""}）"
            null -> "尚未使用"
        }).append('\n')
        sb.append("原生库目录：${Tools.libDir(this)}\n\n接口：\n")
        ifaces.forEach { sb.append("  ").append(it.label()).append("  [type=${it.arpType} state=${it.operState} flags=0x${Integer.toHexString(it.flags)}]\n") }
        sb.append("\n最近一次 dumpcap 输出：\n").append(CaptureManager.dumpcapLog.ifEmpty { "（无）" })
        val tv = mono(11f).apply { text = sb; setTextIsSelectable(true); setPadding(dp(16), dp(8), dp(16), dp(8)) }
        AlertDialog.Builder(this).setTitle("诊断").setView(ScrollView(this).apply { addView(tv) })
            .setPositiveButton("复制") { _, _ -> copyText(sb.toString()) }
            .setNeutralButton("dumpcap -D") { _, _ -> runPcapList() }
            .setNegativeButton("关闭", null).show()
    }

    private fun runPcapList() {
        thread {
            val r = RootHelper.run(this, "pcaplist")
            runOnUiThread {
                val tv = mono(11f).apply { text = (r.stdout + "\n" + r.stderr).trim(); setTextIsSelectable(true); setPadding(dp(16), dp(8), dp(16), dp(8)) }
                AlertDialog.Builder(this).setTitle("dumpcap -D").setView(ScrollView(this).apply { addView(tv) }).setPositiveButton("关闭", null).show()
            }
        }
    }

    private fun showAbout() {
        val msg = """
            SharkDroid：Wireshark ${Tools.WIRESHARK_VERSION} 的 tshark / dumpcap 移植到 Android arm64，带一个手机界面。

            安全设计：
            • 只有 dumpcap 以 root 运行。应用只把固定路径交给 su，接口名和过滤器经 stdin 以 NUL 分隔传给 root 辅助程序，由它校验后直接 execv，全程不经过 shell。
            • 包解析 (tshark) 在隔离进程里运行：随机 UID、没有任何权限、读不到应用数据，也调用不了 su。
            • 不申请网络权限，没有统计或遥测；DNS 反查默认关闭 (-n)。
            • 抓包文件存在应用私有目录，只有你手动导出或分享时才会离开应用。
            • 没有导出不必要的组件，并且关闭了备份。

            许可证：Wireshark、libpcap 等组件分别遵循 GPL-2.0-or-later、BSD 等许可证，源码地址见 README。
        """.trimIndent()
        AlertDialog.Builder(this).setTitle("关于").setMessage(msg).setPositiveButton("好", null).show()
    }

    // ------------------------------------------------------------------ listener

    override fun onRowsChanged(reset: Boolean) {
        adapter.notifyDataSetChanged()
        refreshStatusLine()
    }

    override fun onStateChanged() = refreshState()

    override fun onMessage(msg: String) {
        if (isFinishing) return
        val tv = mono(12f).apply { text = msg; setTextIsSelectable(true); setPadding(dp(16), dp(8), dp(16), dp(8)) }
        AlertDialog.Builder(this).setView(ScrollView(this).apply { addView(tv) }).setPositiveButton("好", null).show()
    }

    private fun refreshState() {
        val st = CaptureManager.state
        startStop.text = when (st) {
            CaptureManager.State.IDLE -> "开始抓包"
            CaptureManager.State.STARTING -> "启动中…（点击停止）"
            CaptureManager.State.CAPTURING -> "停止抓包"
            CaptureManager.State.STOPPING -> "正在停止…"
        }
        startStop.isEnabled = st != CaptureManager.State.STOPPING && (rootOk != false || st != CaptureManager.State.IDLE)
        ifaceSpinner.isEnabled = st == CaptureManager.State.IDLE
        captureFilter.isEnabled = st == CaptureManager.State.IDLE
        refreshStatusLine()
    }

    private fun refreshStatusLine() {
        val f = CaptureManager.file
        val sb = StringBuilder()
        sb.append(rootInfo).append(" · ")
        sb.append("显示 ${CaptureManager.rows.size} 包")
        if (CaptureManager.rowsTruncated) sb.append("（已截断）")
        CaptureManager.sink?.let { if (!it.complete) sb.append(" · 已写入 ${Tools.humanBytes(it.written)}") }
        if (CaptureManager.dissecting) sb.append(" · 解析中")
        if (f != null) sb.append("\n").append(f.name)
        if (CaptureManager.displayFilter.isNotEmpty()) sb.append(" · 过滤: ").append(CaptureManager.displayFilter)
        status.text = sb
    }

    // ------------------------------------------------------------------ utils

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    private fun copyText(s: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("SharkDroid", s))
        toast("已复制")
    }

    private fun hideKeyboard() {
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(window.decorView.windowToken, 0)
        currentFocus?.clearFocus()
    }
}

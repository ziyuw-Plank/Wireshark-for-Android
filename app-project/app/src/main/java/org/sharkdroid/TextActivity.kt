package org.sharkdroid

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import kotlin.concurrent.thread

/** Shows the text output of a tshark statistics / follow-stream job. */
class TextActivity : Activity() {
    private lateinit var text: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val f = File(intent.getStringExtra("file") ?: run { finish(); return })
        val args = intent.getStringArrayExtra("args")?.toList() ?: run { finish(); return }
        if (!Tools.isCaptureFile(this, f)) { finish(); return }
        title = intent.getStringExtra("title") ?: "tshark"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { isIndeterminate = true }
        root.addView(progress, lp(MATCH, WRAP))
        text = mono(11f).apply { setPadding(dp(8), dp(6), dp(8), dp(6)); setTextIsSelectable(true) }
        root.addView(ScrollView(this).apply { addView(HorizontalScrollView(this@TextActivity).apply { addView(text) }) }, lp(MATCH, 0, 1f))
        setContentView(root)
        thread {
            val (rc, out, err) = Dissector.runOnFile(this, f, args)
            val e = Dissector.cleanStderr(err)
            runOnUiThread {
                progress.visibility = View.GONE
                text.text = when {
                    out.isNotBlank() -> out
                    e.isNotEmpty() -> "tshark 退出码 $rc\n$e"
                    else -> "（没有结果）"
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { menu.add(0, 1, 0, "复制全部"); return true }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("SharkDroid", text.text))
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

package org.sharkdroid

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

fun Context.dp(v: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
fun Context.isNight(): Boolean = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)
const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun Context.mono(sizeSp: Float = 12f): TextView = TextView(this).apply {
    typeface = Typeface.MONOSPACE
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
}

fun Context.smallButton(text: String, onClick: (View) -> Unit): Button = Button(this).apply {
    this.text = text
    isAllCaps = false
    minWidth = 0; minimumWidth = 0
    setPadding(dp(12), 0, dp(12), 0)
    setOnClickListener(onClick)
}

/** Simplified Wireshark-like protocol colouring. */
object ProtoColors {
    fun colorFor(r: PacketRow): Int {
        val p = r.protocols
        return when {
            r.severity >= 0x800000 -> Color.parseColor("#E53935")
            r.tcpBad -> Color.parseColor("#FF7043")
            p.contains(":arp") || p.startsWith("arp") -> Color.parseColor("#FBC02D")
            p.contains("icmp") -> Color.parseColor("#E040FB")
            p.contains(":dns") || p.contains(":mdns") -> Color.parseColor("#42A5F5")
            p.contains(":http") && !p.contains(":http2") -> Color.parseColor("#66BB6A")
            p.contains(":tls") || p.contains(":quic") || p.contains(":http2") -> Color.parseColor("#7E57C2")
            p.contains(":tcp") -> Color.parseColor("#B39DDB")
            p.contains(":udp") -> Color.parseColor("#4FC3F7")
            else -> Color.parseColor("#9E9E9E")
        }
    }
}

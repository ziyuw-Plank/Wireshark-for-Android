package org.sharkdroid

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView

class PacketAdapter(private val ctx: Context) : BaseAdapter() {
    private class Holder(val bar: View, val l1: TextView, val l2: TextView)

    override fun getCount(): Int = CaptureManager.rows.size
    override fun getItem(position: Int): PacketRow = CaptureManager.rows[position]
    override fun getItemId(position: Int): Long = CaptureManager.rows[position].num.toLong()
    override fun hasStableIds(): Boolean = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val v: View
        val h: Holder
        if (convertView == null) {
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            val bar = View(ctx)
            row.addView(bar, lp(ctx.dp(5), MATCH))
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(ctx.dp(6), ctx.dp(3), ctx.dp(4), ctx.dp(3))
            }
            val l1 = ctx.mono(11.5f).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
            val l2 = ctx.mono(12f).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }
            col.addView(l1); col.addView(l2)
            row.addView(col, lp(0, WRAP, 1f))
            h = Holder(bar, l1, l2)
            row.tag = h
            v = row
        } else { v = convertView; h = v.tag as Holder }
        val r = getItem(position)
        val color = ProtoColors.colorFor(r)
        h.bar.setBackgroundColor(color)
        h.l1.text = String.format("%-6d %11s  %s → %s", r.num, r.time, r.src, r.dst)
        val sb = SpannableStringBuilder()
        val p0 = sb.length
        sb.append(r.proto.ifEmpty { "?" })
        sb.setSpan(StyleSpan(Typeface.BOLD), p0, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(color), p0, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append(" ").append(r.len.toString()).append("  ").append(r.info)
        h.l2.text = sb
        return v
    }
}

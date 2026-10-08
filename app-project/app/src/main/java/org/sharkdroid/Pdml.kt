package org.sharkdroid

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.Reader

class PdmlNode(
    val label: String,
    val name: String,
    val show: String?,
    val pos: Int,
    val size: Int,
    val depth: Int,
) {
    val children = ArrayList<PdmlNode>()
    var expanded = false
}

/** Parses tshark -T pdml output (DOCTYPE/entity processing stays disabled). */
object Pdml {
    fun parse(r: Reader): List<PdmlNode> {
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(r)
        val roots = ArrayList<PdmlNode>()
        val stack = ArrayList<PdmlNode?>()   // null = skipped subtree / wrapper
        var inPacket = false
        var done = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT && !done) {
            when (ev) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "packet" -> if (!inPacket) inPacket = true
                    "proto", "field" -> if (inPacket) {
                        val name = p.getAttributeValue(null, "name") ?: ""
                        val hide = p.getAttributeValue(null, "hide") == "yes"
                        val parentSkipped = stack.isNotEmpty() && stack.last() == null && !isWrapperOnly(stack)
                        if (hide || name == "geninfo" || parentSkipped) {
                            stack.add(null)
                        } else if (name == "fake-field-wrapper") {
                            stack.add(WRAPPER)
                        } else {
                            val showname = p.getAttributeValue(null, "showname")
                            val show = p.getAttributeValue(null, "show")
                            val label = showname ?: show ?: name
                            val parent = stack.lastOrNull { it != null && it !== WRAPPER }
                            val node = PdmlNode(label, name, show,
                                p.getAttributeValue(null, "pos")?.toIntOrNull() ?: -1,
                                p.getAttributeValue(null, "size")?.toIntOrNull() ?: 0,
                                (parent?.depth ?: -1) + 1)
                            if (parent == null) roots.add(node) else parent.children.add(node)
                            stack.add(node)
                        }
                    }
                }
                XmlPullParser.END_TAG -> when (p.name) {
                    "proto", "field" -> if (inPacket && stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                    "packet" -> if (inPacket) done = true
                }
            }
            ev = p.next()
        }
        return roots
    }

    private val WRAPPER = PdmlNode("", "fake-field-wrapper", null, -1, 0, -1)
    private fun isWrapperOnly(stack: List<PdmlNode?>) = false

    fun flatten(roots: List<PdmlNode>): List<PdmlNode> {
        val out = ArrayList<PdmlNode>()
        fun walk(n: PdmlNode) { out.add(n); if (n.expanded) n.children.forEach { walk(it) } }
        roots.forEach { walk(it) }
        return out
    }

    fun find(roots: List<PdmlNode>, field: String): PdmlNode? {
        for (n in roots) {
            if (n.name == field) return n
            find(n.children, field)?.let { return it }
        }
        return null
    }

    /** Build a display filter for a tree item, e.g. ip.src == 1.2.3.4 */
    fun filterFor(n: PdmlNode): String? {
        if (n.name.isEmpty() || n.name == "fake-field-wrapper") return null
        val s = n.show
        if (s == null || n.children.isNotEmpty() && !n.name.contains('.')) return n.name
        if (!n.name.contains('.')) return n.name
        val plain = Regex("^[A-Za-z0-9.:_/-]+$")
        val v = if (plain.matches(s)) s else "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        return "${n.name} == $v"
    }
}

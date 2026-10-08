package org.sharkdroid

/** One network interface as reported by the root helper's `list` command. */
data class IfaceInfo(
    val name: String,
    val arpType: Int,
    val operState: String,
    val flags: Int,
    val carrier: Boolean,
    val wireless: Boolean,
    val addrs: List<String>,
) {
    val isUp: Boolean get() = (flags and 0x1) != 0 && operState != "down"
    val hasAddr: Boolean get() = addrs.any { !it.startsWith("fe80") }

    val kind: String
        get() = when {
            name == "any" -> "全部接口 (Linux cooked)"
            arpType == 772 -> "回环"
            wireless || name.startsWith("wlan") -> if (name.startsWith("p2p") || name.contains("aware")) "Wi‑Fi 直连" else "Wi‑Fi"
            name.startsWith("p2p") -> "Wi‑Fi 直连"
            name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("seth") ||
                name.startsWith("rmnet_data") || arpType == 519 -> "蜂窝 (原始 IP)"
            name.startsWith("tun") || arpType == 65534 -> "VPN / 隧道"
            name.startsWith("rndis") || name.startsWith("ncm") || name.startsWith("usb") -> "USB 共享"
            name.startsWith("bt-pan") || name.startsWith("bnep") -> "蓝牙共享"
            name.startsWith("dummy") || name.startsWith("sit") || name.startsWith("ip6") ||
                name.startsWith("ip_vti") || name.startsWith("ip6_vti") || name.startsWith("tunl") -> "虚拟"
            arpType == 1 -> "以太网"
            arpType == 803 || arpType == 801 || arpType == 802 -> "Wi‑Fi 监听"
            else -> "类型 $arpType"
        }

    fun label(): String {
        val dot = if (name == "any") "◆" else if (isUp && hasAddr) "●" else if (isUp) "◐" else "○"
        val a = addrs.filter { !it.startsWith("fe80") }.take(2).joinToString(" ")
        return "$dot $name · $kind" + if (a.isNotEmpty()) "  $a" else ""
    }

    companion object {
        val ANY = IfaceInfo("any", 0, "up", 1, true, false, emptyList())

        fun parse(out: String): List<IfaceInfo> {
            val list = ArrayList<IfaceInfo>()
            for (line in out.lineSequence()) {
                val f = line.split('\t')
                if (f.size < 8 || f[0] != "IF") continue
                list += IfaceInfo(
                    name = f[1],
                    arpType = f[2].toIntOrNull() ?: -1,
                    operState = f[3],
                    flags = f[4].removePrefix("0x").toIntOrNull(16) ?: 0,
                    carrier = f[5] == "1",
                    wireless = f[6] == "1",
                    addrs = f[7].split(',').filter { it.isNotBlank() },
                )
            }
            return sort(list)
        }

        private fun score(i: IfaceInfo): Int {
            var s = 0
            if (i.isUp) s += 100
            if (i.hasAddr) s += 100
            when {
                i.name == "wlan0" -> s += 50
                i.name.startsWith("rmnet_data") || i.name.startsWith("ccmni") -> s += 40
                i.name.startsWith("tun") -> s += 30
                i.name.startsWith("wlan") -> s += 20
                i.name == "lo" -> s -= 10
            }
            if (i.kind == "虚拟") s -= 150
            if (i.name.startsWith("rmnet_ipa") || i.name.startsWith("r_rmnet")) s -= 120
            return s
        }

        fun sort(l: List<IfaceInfo>): List<IfaceInfo> = l.sortedWith(compareByDescending<IfaceInfo> { score(it) }.thenBy { it.name })

        /** Sensible default: active Wi‑Fi, else active cellular, else "any". */
        fun pickDefault(l: List<IfaceInfo>): IfaceInfo {
            l.firstOrNull { it.name == "wlan0" && it.isUp && it.hasAddr }?.let { return it }
            l.firstOrNull { (it.name.startsWith("rmnet_data") || it.name.startsWith("ccmni") || it.arpType == 519) && it.isUp && it.hasAddr }?.let { return it }
            return ANY
        }
    }
}

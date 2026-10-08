package org.sharkdroid

import java.io.ByteArrayOutputStream

/** Deterministic, made-up data for UI screenshot tests (no real traffic). */
object FakeData {
    val ifaces: List<IfaceInfo> = listOf(
        IfaceInfo.ANY,
        IfaceInfo("wlan0", 1, "up", 0x1043, true, true, listOf("192.168.31.105", "2408:8207:1851:3c40::5e1", "fe80::8c3a:4ff:fe12:9a01")),
        IfaceInfo("rmnet_data2", 519, "unknown", 0x10c1, true, false, listOf("10.172.46.9", "2409:8a1e:6c31:51a0::1")),
        IfaceInfo("tun0", 65534, "unknown", 0x10d1, true, false, listOf("172.19.0.1")),
        IfaceInfo("lo", 772, "unknown", 0x49, true, false, listOf("127.0.0.1", "::1")),
        IfaceInfo("rmnet_data0", 519, "down", 0x1080, false, false, emptyList()),
        IfaceInfo("p2p0", 1, "down", 0x1002, false, true, emptyList()),
        IfaceInfo("dummy0", 1, "down", 0x82, false, false, emptyList()),
    )

    private val templates = listOf(
        Triple("DNS", "eth:ethertype:ip:udp:dns", "Standard query 0x3f1a A api.example.com"),
        Triple("DNS", "eth:ethertype:ip:udp:dns", "Standard query response 0x3f1a A api.example.com A 93.184.215.14"),
        Triple("TCP", "eth:ethertype:ip:tcp", "51544 → 443 [SYN] Seq=0 Win=65535 Len=0 MSS=1460 SACK_PERM TSval=1849021 WS=512"),
        Triple("TCP", "eth:ethertype:ip:tcp", "443 → 51544 [SYN, ACK] Seq=0 Ack=1 Win=65160 Len=0 MSS=1400"),
        Triple("TLSv1.3", "eth:ethertype:ip:tcp:tls", "Client Hello (SNI=api.example.com)"),
        Triple("TLSv1.3", "eth:ethertype:ip:tcp:tls", "Server Hello, Change Cipher Spec, Application Data"),
        Triple("QUIC", "eth:ethertype:ipv6:udp:quic", "Initial, DCID=5f2e9a1b0c7d4e33, PKN: 1, CRYPTO, PADDING"),
        Triple("HTTP", "eth:ethertype:ip:tcp:http", "GET /generate_204 HTTP/1.1"),
        Triple("HTTP", "eth:ethertype:ip:tcp:http", "HTTP/1.1 204 No Content"),
        Triple("ICMP", "eth:ethertype:ip:icmp", "Echo (ping) request  id=0x0003, seq=12/3072, ttl=64"),
        Triple("ARP", "eth:ethertype:arp", "Who has 192.168.31.1? Tell 192.168.31.105"),
        Triple("TCP", "eth:ethertype:ip:tcp", "[TCP Retransmission] 51544 → 443 [PSH, ACK] Seq=518 Ack=4033 Len=86"),
        Triple("MDNS", "eth:ethertype:ip:udp:mdns", "Standard query 0x0000 PTR _googlecast._tcp.local"),
        Triple("TLSv1.3", "eth:ethertype:ip:tcp:tls", "Application Data"),
        Triple("TCP", "eth:ethertype:ip:tcp", "443 → 51544 [ACK] Seq=4033 Ack=604 Win=64128 Len=0"),
        Triple("UDP", "eth:ethertype:ip:udp", "40211 → 3478 Len=96"),
    )

    fun rows(n: Int = 64): List<PacketRow> {
        val out = ArrayList<PacketRow>()
        var t = 0.0
        for (i in 1..n) {
            val (proto, protos, info) = templates[(i * 7 + i / 3) % templates.size]
            t += 0.0137 * ((i % 5) + 1)
            val v6 = protos.contains("ipv6")
            val out1 = i % 2 == 0
            val me = if (v6) "2408:8207:1851:3c40::5e1" else "192.168.31.105"
            val peer = when (proto) {
                "DNS" -> "192.168.31.1"; "ARP" -> "8e:3a:04:12:9a:01"; "MDNS" -> "224.0.0.251"
                "QUIC" -> "2606:4700::6810:84e5"; else -> "93.184.215.14"
            }
            val src = if (proto == "ARP") "8e:3a:04:12:9a:01" else if (out1) me else peer
            val dst = if (proto == "ARP") "Broadcast" else if (out1) peer else me
            val len = when (proto) { "TLSv1.3" -> 583 + (i * 37) % 900; "QUIC" -> 1292; "TCP" -> 66 + (i % 3) * 20; "ARP" -> 42; else -> 74 + (i * 13) % 300 }
            out += PacketRow(i, String.format(java.util.Locale.US, "%.6f", t), src, dst, proto, len, info, protos,
                if (info.contains("Retransmission")) 0x600000 else 0, info.contains("Retransmission"), t)
        }
        return out
    }

    /** A TLS Client Hello frame (Ethernet/IPv4/TCP/TLS) and its protocol tree. */
    fun detail(): Pair<List<PdmlNode>, ByteArray> {
        val b = ByteArrayOutputStream()
        fun w(vararg x: Int) = x.forEach { b.write(it) }
        w(0x28, 0x6c, 0x07, 0x5a, 0x1e, 0x02, 0x8e, 0x3a, 0x04, 0x12, 0x9a, 0x01, 0x08, 0x00)          // eth 0..13
        w(0x45, 0x00, 0x02, 0x3d, 0x5c, 0x21, 0x40, 0x00, 0x40, 0x06, 0x1b, 0x7e, 192, 168, 31, 105, 93, 184, 215, 14) // ip 14..33
        w(0xc9, 0x58, 0x01, 0xbb, 0x8f, 0x2a, 0x11, 0x04, 0x3c, 0x91, 0x7e, 0x55, 0x50, 0x18, 0x01, 0xf6, 0x9d, 0x40, 0x00, 0x00) // tcp 34..53
        w(0x16, 0x03, 0x01, 0x02, 0x10, 0x01, 0x00, 0x02, 0x0c, 0x03, 0x03)                          // tls 54..64
        for (i in 0 until 32) w((i * 53 + 17) and 0xff)                                              // random 65..96
        w(0x20); for (i in 0 until 32) w((i * 29 + 101) and 0xff)                                   // session id 97..129
        w(0x00, 0x08, 0x13, 0x01, 0x13, 0x02, 0x13, 0x03, 0xc0, 0x2b, 0x01, 0x00)                    // 130..141
        w(0x01, 0xfb, 0x00, 0x00, 0x00, 0x14, 0x00, 0x12, 0x00, 0x00, 0x0f)                          // sni ext hdr 142..152
        "api.example.com".forEach { w(it.code) }                                                     // 153..167
        w(0x00, 0x2b, 0x00, 0x03, 0x02, 0x03, 0x04, 0x00, 0x0a, 0x00, 0x04, 0x00, 0x02, 0x00, 0x1d)
        val bytes = b.toByteArray()

        fun n(label: String, name: String, show: String?, pos: Int, size: Int, depth: Int, vararg kids: PdmlNode) =
            PdmlNode(label, name, show, pos, size, depth).also { it.children.addAll(kids) }
        val frame = n("Frame 42: 573 bytes on wire (4584 bits), 573 bytes captured (4584 bits) on interface wlan0", "frame", null, 0, bytes.size, 0,
            n("Arrival Time: Oct  8, 2026 13:02:11.482113000 CST", "frame.time", "Oct  8, 2026 13:02:11.482113000 CST", 0, 0, 1),
            n("Protocols in frame: eth:ethertype:ip:tcp:tls", "frame.protocols", "eth:ethertype:ip:tcp:tls", 0, 0, 1))
        val eth = n("Ethernet II, Src: 8e:3a:04:12:9a:01, Dst: 28:6c:07:5a:1e:02", "eth", null, 0, 14, 0)
        val ip = n("Internet Protocol Version 4, Src: 192.168.31.105, Dst: 93.184.215.14", "ip", null, 14, 20, 0,
            n("Source Address: 192.168.31.105", "ip.src", "192.168.31.105", 26, 4, 1),
            n("Destination Address: 93.184.215.14", "ip.dst", "93.184.215.14", 30, 4, 1),
            n("Time to Live: 64", "ip.ttl", "64", 22, 1, 1))
        val tcp = n("Transmission Control Protocol, Src Port: 51544, Dst Port: 443, Seq: 1, Ack: 1, Len: 519", "tcp", null, 34, 20, 0,
            n("Source Port: 51544", "tcp.srcport", "51544", 34, 2, 1),
            n("Destination Port: 443", "tcp.dstport", "443", 36, 2, 1),
            n("[Stream index: 7]", "tcp.stream", "7", 0, 0, 1))
        tcp.expanded = false
        val sni = n("Server Name: api.example.com", "tls.handshake.extensions_server_name", "api.example.com", 153, 15, 4)
        val ext = n("Extension: server_name (len=20) name=api.example.com", "tls.handshake.extension", null, 142, 24, 3,
            n("Type: server_name (0)", "tls.handshake.extension.type", "0", 142, 2, 4), sni)
        ext.expanded = true
        val hs = n("Handshake Protocol: Client Hello", "tls.handshake", null, 59, 514, 2,
            n("Handshake Type: Client Hello (1)", "tls.handshake.type", "1", 59, 1, 3),
            n("Version: TLS 1.2 (0x0303)", "tls.handshake.version", "0x0303", 63, 2, 3),
            n("Cipher Suites (4 suites)", "tls.handshake.ciphersuites", null, 132, 8, 3),
            ext)
        hs.expanded = true
        val rec = n("TLSv1.3 Record Layer: Handshake Protocol: Client Hello", "tls.record", null, 54, 519, 1,
            n("Content Type: Handshake (22)", "tls.record.content_type", "22", 54, 1, 2), hs)
        rec.expanded = true
        val tls = n("Transport Layer Security", "tls", null, 54, 519, 0, rec)
        tls.expanded = true
        frame.children.forEach { }
        return listOf(frame, eth, ip, tcp, tls) to bytes
    }

    fun selectedSni(roots: List<PdmlNode>): PdmlNode = Pdml.find(roots, "tls.handshake.extensions_server_name")!!
}

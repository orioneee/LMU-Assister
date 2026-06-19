package com.orioooneee.lmuasister.data.steam

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress

// Same rationale as JVM: resolve up front, IPv4 ahead of IPv6, to dodge the IPv6 connect
// stall. (Android only uses this tunnel on non-JavaSteam paths, but keep it consistent.)
internal actual suspend fun resolveDialAddresses(host: String): List<String> =
    withContext(Dispatchers.IO) {
        val all = InetAddress.getAllByName(host).toList()
        val (v4, rest) = all.partition { it is Inet4Address }
        (v4 + rest).mapNotNull { it.hostAddress }.ifEmpty { listOf(host) }
    }

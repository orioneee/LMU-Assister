package com.orioooneee.lmuasister.data.steam

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress

// JVM nio can otherwise hand back an AAAA first and hang the connect on a routeless IPv6
// path. Resolve here and put IPv4 ahead of IPv6 (order preserved within each family).
// An IP-literal host short-circuits getAllByName (no lookup), so this is cheap either way.
internal actual suspend fun resolveDialAddresses(host: String): List<String> =
    withContext(Dispatchers.IO) {
        val all = InetAddress.getAllByName(host).toList()
        val (v4, rest) = all.partition { it is Inet4Address }
        (v4 + rest).mapNotNull { it.hostAddress }.ifEmpty { listOf(host) }
    }

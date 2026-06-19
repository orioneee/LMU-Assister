package com.orioooneee.lmuasister.data.steam

// Darwin's connect path implements Happy Eyeballs (RFC 8305) natively, so we hand the
// hostname straight to Ktor/Darwin and let the OS race the address families itself.
internal actual suspend fun resolveDialAddresses(host: String): List<String> = listOf(host)

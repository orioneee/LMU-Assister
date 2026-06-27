package com.orioooneee.lmuasister.data.steam

/*
 * TUNNEL_DISABLED:
 * Resolver support for the old device egress tunnel. Kept only as commented reference.
 *
/**
 * Resolves [host] to literal IP addresses to dial, **IPv4-first** so a dead/black-holed
 * IPv6 route can't stall the TCP connect (~35s on the first AAAA). Returning literals
 * also keeps the socket layer from re-running DNS at connect time.
 *
 * Platforms whose OS already does Happy Eyeballs (Darwin) may just return [host] and let
 * the system pick a working family.
 */
internal expect suspend fun resolveDialAddresses(host: String): List<String>
*/

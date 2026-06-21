package com.orioooneee.lmuasister.ui

/**
 * Best-effort circuit-emblem lookup by track name.
 *
 * The schedule's track logos resolve fine (the client derives `…/logo.svg` from the
 * minimap path), but the profile's race cards come back with `track_logo = null`
 * (the backend only emits a logo when its asset manifest lists one). So we cache the
 * working schedule logos keyed by a normalised track name and let the profile fall
 * back to them. Missing entries simply mean no emblem — never an error.
 */
object TrackLogoIndex {
    private var map: Map<String, String> = emptyMap()

    fun populate(entries: List<Pair<String?, String?>>) {
        val m = HashMap<String, String>()
        for ((name, url) in entries) {
            val key = normalize(name) ?: continue
            val u = url ?: continue
            if (key !in m) m[key] = u
        }
        if (m.isNotEmpty()) map = m
    }

    fun lookup(name: String?): String? = normalize(name)?.let { map[it] }

    /** Mirror of the backend's `_track_base`: 'LeMansWEC' & 'Le Mans' both → 'lemans'. */
    private fun normalize(s: String?): String? {
        if (s.isNullOrBlank()) return null
        var x = s.lowercase()
        x = Regex("_\\d{4}.*$").replace(x, "")
        x = Regex("(wec|elms)$").replace(x, "")
        x = Regex("[^a-z0-9]").replace(x, "")
        return x.ifBlank { null }
    }
}

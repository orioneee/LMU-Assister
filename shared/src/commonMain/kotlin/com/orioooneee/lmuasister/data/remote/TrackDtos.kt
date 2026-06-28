package com.orioooneee.lmuasister.data.remote

import kotlinx.serialization.Serializable

// Decoded with [AppJson] (global SnakeCase strategy) — every wire key is snake_case, so camelCase
// Kotlin names map automatically; `best_by_class` keys are class names (a Map, untouched by the
// strategy). The `default_weather` block is intentionally omitted (ignoreUnknownKeys drops it).

/** GET /api/v2/tracks — full reference list of official tracks (public, static). */
@Serializable
data class TracksResponse(
    val count: Int = 0,
    val tracks: List<TrackFullDto> = emptyList(),
)

/** One track's reference block — shared by /tracks and /profile/track/<id>. */
@Serializable
data class TrackFullDto(
    val id: String = "",
    val code: String? = null,        // raw cmpName
    val base: String? = null,        // slug
    val name: String? = null,
    val eventName: String? = null,
    val grandPrix: String? = null,
    val fullName: String? = null,
    val detailedScheme: String? = null,
    val countryCode: String? = null,
    val lengthKm: String? = null,    // string per contract ("6.019")
    val corners: Int? = null,
    val type: String? = null,        // Permanent / Street / …
    val openingYear: Int? = null,
    val official: Boolean = false,
    val assets: TrackAssetsDto? = null,
)

/** Absolute CDN (R2) asset URLs — any may be null; loaded directly, no resolution. */
@Serializable
data class TrackAssetsDto(
    val scheme: String? = null,
    val detailedScheme: String? = null,
    val logo: String? = null,
    val cover: String? = null,
    val background: String? = null,
)

/** GET /api/v2/profile/track/<id> — the track block + the caller's personal record on it. */
@Serializable
data class TrackDetailResponse(
    val track: TrackFullDto,
    val personal: TrackPersonalDto? = null,
)

/** The caller's history on this track (absent/empty when they've never raced it). */
@Serializable
data class TrackPersonalDto(
    val races: Int = 0,
    // Career laps + distance on this track (snake_case `laps` / `distance_km` via AppJson).
    val laps: Int = 0,
    val distanceKm: Double = 0.0,
    val currentPatch: GameVersionDto? = null,
    val bestLapEver: TrackAttemptDto? = null,
    val bestLapCurrentPatch: TrackAttemptDto? = null,
    // Absolute best lap across all classes (may be a faster prototype class).
    val bestLap: TrackAttemptDto? = null,
    // Best lap per class the driver has run — key is the class name ("GT3", "LMP2"…).
    val bestByClass: Map<String, TrackAttemptDto> = emptyMap(),
    // Up to 10 most recent attempts on this track (mini-log).
    val recent: List<TrackAttemptDto> = emptyList(),
)

/** One lap attempt on a track (same shape in bestLap / bestByClass / recent). */
@Serializable
data class TrackAttemptDto(
    val bestLapMs: Long? = null,
    val session: String? = null,     // practice | qualifying | race
    val car: String? = null,
    val carClass: String? = null,
    val date: String? = null,        // ISO-8601 UTC slot
    val eventId: String? = null,
    val eventTitle: String? = null,
    val tier: String? = null,
    val official: Boolean = false,
    val split: Int? = null,
    val position: Int? = null,
    val finishStatus: String? = null,
    val gameVersion: GameVersionDto? = null,
)

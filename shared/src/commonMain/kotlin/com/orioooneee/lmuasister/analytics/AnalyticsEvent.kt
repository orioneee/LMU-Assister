package com.orioooneee.lmuasister.analytics

/**
 * Every analytics event the app emits. Each carries a Firebase-safe [name]
 * (snake_case, ≤40 chars, starts with a letter) and a typed [params] map.
 *
 * Grouped by the question they answer:
 *   - Schedule (anonymous funnel): who browses, which weeks/tiers, where it errors
 *   - Race detail / leaderboard:   do users go deeper than the schedule list
 *   - Login funnel:                the main drop-off (Steam creds → 2FA → tunnel → success)
 *   - Profile:                     what signed-in (high-value) users do
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any?> = emptyMap(),
) {
    // ── Schedule (anonymous) ─────────────────────────────────────────────
    data object ScheduleViewed : AnalyticsEvent("schedule_viewed")

    class WeekSelected(weekKey: String, isCached: Boolean) :
        AnalyticsEvent("schedule_week_selected", mapOf("week_key" to weekKey, "is_cached" to isCached))

    class ScheduleError(reason: String) :
        AnalyticsEvent("schedule_error", mapOf("reason" to reason))

    // ── Race detail / leaderboard ────────────────────────────────────────
    /** [source] = home_grid | profile_recent | all_races. */
    class RaceDetailOpened(raceId: String, source: String) :
        AnalyticsEvent("race_detail_opened", mapOf("race_id" to raceId, "source" to source))

    class LeaderboardOpened(leaderboardId: String) :
        AnalyticsEvent("leaderboard_full_opened", mapOf("leaderboard_id" to leaderboardId))

    // ── Login funnel (Steam) ─────────────────────────────────────────────
    data object LoginFormShown : AnalyticsEvent("login_form_shown")

    class LoginSubmitted(has2fa: Boolean) :
        AnalyticsEvent("login_submitted", mapOf("has_2fa" to has2fa))

    /** [kind] = email | device. */
    class Login2faRequired(kind: String) :
        AnalyticsEvent("login_2fa_required", mapOf("kind" to kind))

    class LoginSuccess(restored: Boolean) :
        AnalyticsEvent("login_success", mapOf("restored" to restored))

    /** [reason] is normalized into a small set of buckets, not the raw error text. */
    class LoginFailed(reason: String) :
        AnalyticsEvent("login_failed", mapOf("reason" to reason))

    data object LoginTunnelRequired : AnalyticsEvent("login_tunnel_required")

    data object ProfileReauthTriggered : AnalyticsEvent("profile_reauth_triggered")

    // ── Profile (signed-in) ──────────────────────────────────────────────
    class ProfileLoaded(fromCache: Boolean) :
        AnalyticsEvent("profile_loaded", mapOf("from_cache" to fromCache))

    data object ProfileSignedOut : AnalyticsEvent("profile_sign_out")

    data object AllRacesOpened : AnalyticsEvent("all_races_opened")

    class SuspensionsOpened(active: Boolean) :
        AnalyticsEvent("suspensions_opened", mapOf("active" to active))

    data object PrivacyOpened : AnalyticsEvent("privacy_opened")
}

/** Shared user-property keys (also mirrored as Crashlytics custom keys). */
object UserProperties {
    const val PLATFORM = "platform"
    const val IS_LOGGED_IN = "is_logged_in"
    const val DRIVER_RANK = "driver_rank"
    const val SAFETY_RANK = "safety_rank"
}

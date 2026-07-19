package com.orioooneee.lmuasister.analytics

/**
 * Every analytics event the app emits. Each carries a Firebase-safe [name]
 * (snake_case, ≤40 chars, starts with a letter) and a typed [params] map.
 *
 * Grouped by the question they answer:
 *   - Schedule (anonymous funnel): who browses, which weeks/tiers, where it errors
 *   - Race detail / leaderboard:   do users go deeper than the schedule list
 *   - Public drivers:              search, profile opens, public drill-downs
 *   - Login funnel:                the main drop-off (Steam creds → 2FA → success)
 *   - Profile:                     what signed-in (high-value) users do
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any?> = emptyMap(),
) {
    data object ScheduleViewed : AnalyticsEvent("schedule_viewed")

    class WeekSelected(weekKey: String, isCached: Boolean) :
        AnalyticsEvent("schedule_week_selected", mapOf("week_key" to weekKey, "is_cached" to isCached))

    class ScheduleError(reason: String) :
        AnalyticsEvent("schedule_error", mapOf("reason" to reason))

    data object StartNotificationCtaOpened : AnalyticsEvent("start_notif_cta_opened")

    class StartNotificationSheetShown(canDevicePush: Boolean, canEmail: Boolean) :
        AnalyticsEvent(
            "start_notif_sheet_shown",
            mapOf("can_device_push" to canDevicePush, "can_email" to canEmail),
        )

    class StartNotificationChannelToggled(channel: String, enabled: Boolean) :
        AnalyticsEvent("start_notif_ch_toggled", mapOf("channel" to channel, "enabled" to enabled))

    class StartNotificationOffsetSelected(minutes: Int) :
        AnalyticsEvent("start_notif_offset_selected", mapOf("minutes" to minutes))

    class StartNotificationSubmit(devicePush: Boolean, email: Boolean, minutes: Int) :
        AnalyticsEvent(
            "start_notif_submit",
            mapOf("device_push" to devicePush, "email" to email, "minutes" to minutes),
        )

    class StartNotificationResult(channel: String, success: Boolean, reason: String? = null) :
        AnalyticsEvent(
            "start_notif_result",
            mapOf("channel" to channel, "success" to success, "reason" to reason),
        )

    class NotificationPermissionResult(source: String, state: String) :
        AnalyticsEvent("notif_permission_result", mapOf("source" to source, "state" to state))

    class FcmTokenResult(stage: String, success: Boolean, reason: String? = null) :
        AnalyticsEvent(
            "fcm_token_result",
            mapOf("stage" to stage, "success" to success, "reason" to reason),
        )

    data object ScheduleUpdatesOpened : AnalyticsEvent("schedule_updates_opened")

    class ScheduleUpdatesLoaded(devicePush: Boolean, email: Boolean) :
        AnalyticsEvent(
            "schedule_updates_loaded",
            mapOf("device_push" to devicePush, "email" to email),
        )

    class ScheduleUpdatesLoadFailed(reason: String) :
        AnalyticsEvent("schedule_updates_load_failed", mapOf("reason" to reason))

    class ScheduleUpdatesChannelToggled(channel: String, enabled: Boolean) :
        AnalyticsEvent("schedule_updates_ch_toggled", mapOf("channel" to channel, "enabled" to enabled))

    class ScheduleUpdatesSubmit(deviceChanged: Boolean, emailChanged: Boolean) :
        AnalyticsEvent(
            "schedule_updates_submit",
            mapOf("device_changed" to deviceChanged, "email_changed" to emailChanged),
        )

    class ScheduleUpdatesResult(channel: String, subscribed: Boolean, success: Boolean, reason: String? = null) :
        AnalyticsEvent(
            "schedule_updates_result",
            mapOf("channel" to channel, "subscribed" to subscribed, "success" to success, "reason" to reason),
        )

    class PushNotificationReceived(notificationType: String, hasNotification: Boolean, hasBody: Boolean) :
        AnalyticsEvent(
            "push_notification_received",
            mapOf(
                "notification_type" to notificationType,
                "has_notification" to hasNotification,
                "has_body" to hasBody,
            ),
        )

    class PushNotificationDisplayed(notificationType: String, channelId: String) :
        AnalyticsEvent(
            "push_notification_displayed",
            mapOf("notification_type" to notificationType, "channel_id" to channelId),
        )

    class PushNotificationSkipped(notificationType: String, reason: String) :
        AnalyticsEvent(
            "push_notification_skipped",
            mapOf("notification_type" to notificationType, "reason" to reason),
        )

    class PushNotificationOpened(notificationType: String, notificationId: Int?) :
        AnalyticsEvent(
            "push_notification_opened",
            mapOf("notification_type" to notificationType, "notification_id" to notificationId),
        )

    /** [source] = home_grid | profile_recent | all_races. */
    class RaceDetailOpened(raceId: String, source: String) :
        AnalyticsEvent("race_detail_opened", mapOf("race_id" to raceId, "source" to source))

    class LeaderboardOpened(leaderboardId: String) :
        AnalyticsEvent("leaderboard_full_opened", mapOf("leaderboard_id" to leaderboardId))

    data object DriversViewed : AnalyticsEvent("drivers_viewed")

    class DriversLoaded(total: Int, fromCache: Boolean) :
        AnalyticsEvent("drivers_loaded", mapOf("total" to total, "from_cache" to fromCache))

    data object DriversSearchOpened : AnalyticsEvent("drivers_search_opened")

    class DriversSearchSubmitted(queryLength: Int) :
        AnalyticsEvent("drivers_search_submitted", mapOf("query_length" to queryLength))

    class DriversSearchResults(queryLength: Int, page: Int, results: Int, total: Int, hasMore: Boolean) :
        AnalyticsEvent(
            "drivers_search_results",
            mapOf(
                "query_length" to queryLength,
                "page" to page,
                "results" to results,
                "total" to total,
                "has_more" to hasMore,
            ),
        )

    class DriversSearchFailed(queryLength: Int, page: Int, reason: String) :
        AnalyticsEvent(
            "drivers_search_failed",
            mapOf("query_length" to queryLength, "page" to page, "reason" to reason),
        )

    /** [source] = top_safety | search. */
    class PublicUserOpened(source: String) :
        AnalyticsEvent("public_user_opened", mapOf("source" to source))

    class PublicProfileLoaded(fromCache: Boolean, externalData: Boolean) :
        AnalyticsEvent("public_profile_loaded", mapOf("from_cache" to fromCache, "external_data" to externalData))

    class PublicProfileFailed(reason: String) :
        AnalyticsEvent("public_profile_failed", mapOf("reason" to reason))

    data object PublicAllRacesOpened : AnalyticsEvent("public_all_races_opened")

    class PublicCategoryRacesOpened(category: String) :
        AnalyticsEvent("public_category_races_opened", mapOf("category" to category))

    data object PublicTrackBreakdownOpened : AnalyticsEvent("public_tracks_opened")

    class PublicTrackOpened(trackId: String) :
        AnalyticsEvent("public_track_opened", mapOf("track_id" to trackId))

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

    /*
     * TUNNEL_DISABLED:
     * The tunnel-required event belonged to the old device-egress auth path.
     *
     * data object LoginTunnelRequired : AnalyticsEvent("login_tunnel_required")
     */

    data object ProfileReauthTriggered : AnalyticsEvent("profile_reauth_triggered")

    class ProfileLoaded(fromCache: Boolean) :
        AnalyticsEvent("profile_loaded", mapOf("from_cache" to fromCache))

    data object ProfileSignedOut : AnalyticsEvent("profile_sign_out")

    /** User explicitly deleted their server-side data (App Review 5.1.1(v) flow). */
    data object ProfileDataCleared : AnalyticsEvent("profile_data_cleared")

    data object AllRacesOpened : AnalyticsEvent("all_races_opened")

    class CategoryRacesOpened(category: String) :
        AnalyticsEvent("category_races_opened", mapOf("category" to category))

    class SuspensionsOpened(active: Boolean) :
        AnalyticsEvent("suspensions_opened", mapOf("active" to active))

    data object PrivacyOpened : AnalyticsEvent("privacy_opened")
}

object UserProperties {
    const val PLATFORM = "platform"
    const val IS_LOGGED_IN = "is_logged_in"
    const val DRIVER_RANK = "driver_rank"
    const val SAFETY_RANK = "safety_rank"
}

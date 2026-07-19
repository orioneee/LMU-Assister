package com.orioooneee.lmuasister

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry

internal const val EXTRA_NOTIFICATION_TYPE = "lmu_notification_type"
internal const val EXTRA_NOTIFICATION_ID = "lmu_notification_id"

class LMUMessagingService : FirebaseMessagingService() {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        FcmTokenRegistrar.persistAndRegisterIfPossible(applicationContext, token)
    }

    @Suppress("DEPRECATION")
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"]
        if (type != SCHEDULE_NOTIFICATION_TYPE && type != SCHEDULE_UPDATED_TYPE) return
        Telemetry.log(
            AnalyticsEvent.PushNotificationReceived(
                notificationType = type,
                hasNotification = message.notification != null,
                hasBody = message.notification?.body?.isNotBlank() == true || data["body"]?.isNotBlank() == true,
            ),
        )
        if (!canPostNotifications()) {
            Telemetry.log(AnalyticsEvent.PushNotificationSkipped(type, "permission_denied"))
            return
        }

        when (type) {
            SCHEDULE_NOTIFICATION_TYPE -> showScheduleStartNotification(message)
            SCHEDULE_UPDATED_TYPE -> showScheduleUpdatedNotification(message)
        }
    }

    @Suppress("DEPRECATION")
    private fun showScheduleStartNotification(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title
            ?.takeIf { it.isNotBlank() }
            ?: data["event_name"]?.takeIf { it.isNotBlank() }
            ?: return Telemetry.log(AnalyticsEvent.PushNotificationSkipped(SCHEDULE_NOTIFICATION_TYPE, "missing_title"))
        val body = message.notification?.body
            ?.takeIf { it.isNotBlank() && it != title }
            ?: data["body"]?.takeIf { it.isNotBlank() && it != title }
        val notificationId = data["notification_id"].notificationId(title)
        val channelId = message.notification?.channelId
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.schedule_notification_channel_id)

        val builder = newNotificationBuilder(channelId)
            .setSmallIcon(R.drawable.ic_notification_schedule)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(launchPendingIntent(notificationId, SCHEDULE_NOTIFICATION_TYPE))
        if (body != null) {
            builder
                .setContentText(body)
                .setStyle(Notification.BigTextStyle().bigText(body))
        }
        val notification = builder.build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
        Telemetry.log(AnalyticsEvent.PushNotificationDisplayed(SCHEDULE_NOTIFICATION_TYPE, channelId))
    }

    @Suppress("DEPRECATION")
    private fun showScheduleUpdatedNotification(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title
            ?.takeIf { it.isNotBlank() }
            ?: data["title"]?.takeIf { it.isNotBlank() }
            ?: getString(R.string.schedule_updated_channel_name)
        val body = message.notification?.body
            ?.takeIf { it.isNotBlank() && it != title }
            ?: data["body"]?.takeIf { it.isNotBlank() && it != title }
        val notificationId = data["notification_id"].notificationId(title)
        val builder = newNotificationBuilder(getString(R.string.schedule_updated_channel_id))
            .setSmallIcon(R.drawable.ic_notification_schedule)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(launchPendingIntent(notificationId, SCHEDULE_UPDATED_TYPE))
        if (body != null) {
            builder
                .setContentText(body.lineSequence().firstOrNull() ?: body)
                .setStyle(Notification.BigTextStyle().bigText(body))
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
        Telemetry.log(
            AnalyticsEvent.PushNotificationDisplayed(
                notificationType = SCHEDULE_UPDATED_TYPE,
                channelId = getString(R.string.schedule_updated_channel_id),
            ),
        )
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun launchPendingIntent(requestCode: Int, notificationType: String): PendingIntent? {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_NOTIFICATION_TYPE, notificationType)
                putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            }
            ?: return null
        return PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun String?.notificationId(title: String): Int =
        this?.toIntOrNull() ?: title.hashCode()

    @Suppress("DEPRECATION")
    private fun newNotificationBuilder(channelId: String): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }

    private companion object {
        private const val SCHEDULE_NOTIFICATION_TYPE = "schedule_notification"
        private const val SCHEDULE_UPDATED_TYPE = "schedule_updated"
    }
}

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

class LMUMessagingService : FirebaseMessagingService() {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        FcmTokenRegistrar.persistAndRegisterIfPossible(applicationContext, token)
    }

    @Suppress("DEPRECATION")
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        if (data["type"] != SCHEDULE_NOTIFICATION_TYPE) return
        if (!canPostNotifications()) return

        val title = message.notification?.title
            ?.takeIf { it.isNotBlank() }
            ?: data["event_name"]?.takeIf { it.isNotBlank() }
            ?: return
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
            .setContentIntent(launchPendingIntent(notificationId))
        if (body != null) {
            builder
                .setContentText(body)
                .setStyle(Notification.BigTextStyle().bigText(body))
        }
        val notification = builder.build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun launchPendingIntent(requestCode: Int): PendingIntent? {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) }
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
    }
}

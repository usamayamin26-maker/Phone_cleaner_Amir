package com.phone.cleaner.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.phone.cleaner.R

/**
 * The FCM (Firebase Cloud Messaging) notification channel. Created from
 * [com.phone.cleaner.BaseApp] and from [AppFirebaseMessagingService], since FCM needs the channel
 * named by `default_notification_channel_id` to exist before a notification can be shown.
 */
object NotificationChannels {

    /** Must match `com.google.firebase.messaging.default_notification_channel_id` in the manifest. */
    const val GENERAL_ID = "general"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                GENERAL_ID,
                context.getString(R.string.notification_channel_general),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }
}

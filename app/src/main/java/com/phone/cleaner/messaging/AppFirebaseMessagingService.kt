package com.phone.cleaner.messaging

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.phone.cleaner.R
import com.phone.cleaner.presentation.onboarding.SplashActivity

/**
 * Handles FCM messages the SDK doesn't draw itself: data-only messages, and notification messages
 * that arrive while the app is in the foreground. (With the app in the background or closed, the
 * SDK shows notification messages itself, using the manifest's default channel and icon.)
 * Tapping the notification opens the app.
 *
 * The registration token is logged under the "FcmToken" tag so QA can send a test message to this
 * device from the Firebase console (`adb logcat -s FcmToken`).
 */
class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
    }

    override fun onNewToken(token: String) {
        Log.i(TAG, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SplashActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.GENERAL_ID)
            // A small icon is drawn from its alpha channel only, so it is a white silhouette.
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        // Fixed id: a newer message replaces the previous one instead of stacking up.
        // No-op when the user hasn't granted the notification permission.
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            Log.w(TAG, "Notification permission not granted")
        }
    }

    private companion object {
        const val TAG = "FcmToken"
        const val NOTIFICATION_ID = 1001
    }
}

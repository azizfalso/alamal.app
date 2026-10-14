package com.alamal.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alamal.app.MainActivity
import com.alamal.app.R

/**
 * Creates the notification channel and posts the "long shift" notification.
 */
object NotificationHelper {

    const val CHANNEL_ID = "alamal_long_shift"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تنبيه ساعات العمل الطويلة",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "يُنبّهك عند نسيان تسجيل الخروج من العمل"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showLongShiftNotification(context: Context, hoursElapsed: Double) {
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val hoursText = if (hoursElapsed == hoursElapsed.toLong().toDouble()) {
            hoursElapsed.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", hoursElapsed)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("نسيت تسجيل الخروج؟")
            .setContentText("منذ $hoursText ساعة وأنت في العمل. افتح التطبيق لتسجيل الخروج.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "منذ $hoursText ساعة وأنت في العمل. " +
                            "إذا كنت قد انتهيت، افتح التطبيق وسجّل الخروج."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked after enabling; nothing we can do.
        }
    }
}

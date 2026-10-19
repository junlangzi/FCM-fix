package com.fcmguard.app.helper

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.fcmguard.app.FcmGuardApp
import com.fcmguard.app.R
import com.fcmguard.app.receiver.NotificationActionReceiver
import com.fcmguard.app.ui.MainActivity

object NotificationHelper {

    fun buildForegroundNotification(
        context: Context,
        hideFromStatusBar: Boolean = true,
        paused: Boolean = false,
        successCount: Long = 0,
        batteryPct: Double = 0.0,
        usedMah: Double = 0.0
    ): Notification {
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (paused) {
            NotificationCompat.Action.Builder(
                0, "Tiếp tục",
                actionPending(context, NotificationActionReceiver.ACTION_RESUME, 1)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                0, "Tạm dừng",
                actionPending(context, NotificationActionReceiver.ACTION_PAUSE, 2)
            ).build()
        }

        val restartAction = NotificationCompat.Action.Builder(
            0, "Restart",
            actionPending(context, NotificationActionReceiver.ACTION_RESTART, 4)
        ).build()

        val stopAction = NotificationCompat.Action.Builder(
            0, "Tắt",
            actionPending(context, NotificationActionReceiver.ACTION_STOP, 3)
        ).build()

        val title = if (paused) "FCM fix đã tạm dừng" else "FCM fix đang chạy"
        val pinStr = String.format("%.2f%%", batteryPct)
        val mahStr = if (usedMah < 1) String.format("%.2fmAh", usedMah) else String.format("%.1fmAh", usedMah)
        // Không hiện CPU — chỉ Ping + Pin
        val statsLine = "📡 $successCount   🪫 $pinStr ($mahStr)"

        val channelId =
            if (hideFromStatusBar) FcmGuardApp.CHANNEL_ID_QUIET else FcmGuardApp.CHANNEL_ID_VISIBLE

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_yin_yang)
            .setContentTitle(title)
            .setContentText(statsLine)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)
            .setSilent(true)
            .setLocalOnly(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(statsLine)
            )
            .addAction(pauseResumeAction)
            .addAction(restartAction)
            .addAction(stopAction)

        if (hideFromStatusBar) {
            builder.setPriority(NotificationCompat.PRIORITY_MIN)
            builder.setVisibility(NotificationCompat.VISIBILITY_SECRET)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        return builder.build()
    }

    private fun actionPending(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

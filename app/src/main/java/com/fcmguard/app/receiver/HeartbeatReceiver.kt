package com.fcmguard.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.fcmguard.app.service.FcmGuardService

class HeartbeatReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Heartbeat alarm fired")
        // Alarm nổ → đánh thức service làm việc (service tự quyết định có ping hay không)
        FcmGuardService.triggerHeartbeat(context)
    }

    companion object {
        private const val TAG = "HeartbeatReceiver"
        private const val REQUEST_CODE = 2001

        fun schedule(context: Context, intervalMinutes: Int) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, HeartbeatReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)

            // Tối thiểu 1 phút
            val minutes = intervalMinutes.coerceAtLeast(1)
            val triggerAt = System.currentTimeMillis() + minutes * 60_000L

            try {
                // Android 12+ cần quyền exact alarm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!am.canScheduleExactAlarms()) {
                        Log.w(TAG, "No exact alarm permission – fallback inexact")
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                        return
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
                Log.d(TAG, "Next heartbeat in $minutes min (exact)")
            } catch (e: SecurityException) {
                try {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    Log.w(TAG, "Exact denied, using inexact", e)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to schedule alarm", e2)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Schedule failed", e)
            }
        }

        fun cancel(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, HeartbeatReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context, REQUEST_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.cancel(pi)
                pi.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Cancel alarm failed", e)
            }
        }

        /** Kiểm tra nhanh màn hình đang bật hay tắt */
        fun isScreenOn(context: Context): Boolean {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isInteractive
        }
    }
}

package com.fcmguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.fcmguard.app.data.AppDatabase
import com.fcmguard.app.data.PreferencesManager
import com.fcmguard.app.helper.BatteryEstimator

class FcmGuardApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        preferences = PreferencesManager(this)
        createNotificationChannels()
        // Khóa dung lượng pin thiết kế 1 lần (tránh số nhảy mỗi lần đọc)
        try {
            BatteryEstimator.refreshCapacityIfNeeded(this)
        } catch (_: Exception) {
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            listOf("fcm_guard_service", "fcm_fix_service_v2").forEach {
                try {
                    manager.deleteNotificationChannel(it)
                } catch (_: Exception) {
                }
            }

            val quiet = NotificationChannel(
                CHANNEL_ID_QUIET,
                "FCM fix (ẩn icon)",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Chạy nền, ẩn icon thanh trạng thái"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            val visible = NotificationChannel(
                CHANNEL_ID_VISIBLE,
                "FCM fix (hiện icon)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Hiện icon trên thanh trạng thái"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            manager.createNotificationChannel(quiet)
            manager.createNotificationChannel(visible)
        }
    }

    companion object {
        const val CHANNEL_ID_QUIET = "fcm_fix_quiet_v1"
        const val CHANNEL_ID_VISIBLE = "fcm_fix_visible_v1"
        const val CHANNEL_ID = CHANNEL_ID_QUIET
        lateinit var instance: FcmGuardApp
            private set
    }
}

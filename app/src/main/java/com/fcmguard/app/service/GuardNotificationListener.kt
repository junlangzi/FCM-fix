package com.fcmguard.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * NotificationListenerService acts as a strong anchor.
 * system_server binds to it, making it much harder for ColorOS Freezer / MIUI to kill the process.
 */
class GuardNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "NotificationListenerService connected – strong anchor active")
        // Optionally start the main guard service if not running
        FcmGuardService.start(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "NotificationListenerService disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // We don't need to process notifications, just stay bound
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    companion object {
        private const val TAG = "GuardNLS"
    }
}

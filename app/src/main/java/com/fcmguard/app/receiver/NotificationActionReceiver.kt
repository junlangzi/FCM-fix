package com.fcmguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fcmguard.app.FcmGuardApp
import com.fcmguard.app.service.FcmGuardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_PAUSE -> {
                Log.i(TAG, "Pause from notification")
                CoroutineScope(Dispatchers.IO).launch {
                    FcmGuardApp.instance.preferences.setPaused(true)
                }
                FcmGuardService.applyMode(context)
            }
            ACTION_RESUME -> {
                Log.i(TAG, "Resume from notification")
                CoroutineScope(Dispatchers.IO).launch {
                    FcmGuardApp.instance.preferences.setPaused(false)
                }
                FcmGuardService.start(context)
                FcmGuardService.applyMode(context)
            }
            ACTION_STOP -> {
                Log.i(TAG, "Stop from notification")
                CoroutineScope(Dispatchers.IO).launch {
                    FcmGuardApp.instance.preferences.setPaused(false)
                    FcmGuardApp.instance.preferences.setRunMode("OFF")
                }
                FcmGuardService.stop(context)
            }
            ACTION_RESTART -> {
                Log.i(TAG, "Restart from notification")
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        FcmGuardApp.instance.preferences.setPaused(false)
                        val mode = FcmGuardApp.instance.preferences.runMode.first()
                        FcmGuardService.stop(context)
                        delay(500)
                        if (mode == "OFF") {
                            FcmGuardApp.instance.preferences.setRunMode("ALWAYS")
                        }
                        FcmGuardService.start(context)
                        FcmGuardService.applyMode(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "NotifAction"
        const val ACTION_PAUSE = "com.fcmguard.app.ACTION_PAUSE"
        const val ACTION_RESUME = "com.fcmguard.app.ACTION_RESUME"
        const val ACTION_STOP = "com.fcmguard.app.ACTION_STOP"
        const val ACTION_RESTART = "com.fcmguard.app.ACTION_RESTART"
    }
}

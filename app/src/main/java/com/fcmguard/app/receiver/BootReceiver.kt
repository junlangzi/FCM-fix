package com.fcmguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fcmguard.app.FcmGuardApp
import com.fcmguard.app.service.FcmGuardService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "Received: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val (autoStart, mode) = runBlocking {
                val prefs = FcmGuardApp.instance.preferences
                prefs.autoStartBoot.first() to prefs.runMode.first()
            }
            if (autoStart && mode != "OFF") {
                FcmGuardService.start(context)
                HeartbeatReceiver.schedule(context, 2)
                Log.i(TAG, "Service restarted after boot/update (mode=$mode)")
            } else {
                Log.i(TAG, "Skip auto-start (autoStart=$autoStart, mode=$mode)")
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

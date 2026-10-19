package com.fcmguard.app.helper

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.util.Log

/**
 * Theo dõi mạng + chu kỳ heartbeat tiết kiệm pin.
 *
 * NAT timeout di động ~3–5 phút → cellular tối thiểu ~3 phút khi màn tắt.
 * WiFi NAT thường dài hơn → 8–15 phút là đủ.
 * Pin thấp / tiết kiệm pin hệ thống → kéo dài chu kỳ thêm.
 */
object NetworkHelper {

    private const val TAG = "NetworkHelper"

    enum class NetType { WIFI, CELLULAR, OTHER, NONE }

    fun currentType(context: Context): NetType {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return NetType.NONE
        val caps = cm.getNetworkCapabilities(network) ?: return NetType.NONE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetType.CELLULAR
            else -> NetType.OTHER
        }
    }

    fun batteryLevelPercent(context: Context): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        } catch (_: Exception) {
            try {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                if (level >= 0 && scale > 0) (level * 100 / scale) else 50
            } catch (_: Exception) {
                50
            }
        }
    }

    fun isSystemPowerSave(context: Context): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isPowerSaveMode
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Chu kỳ (phút) cân bằng NAT keep-alive vs pin.
     *
     * @param userPreference khoảng user chọn (phút)
     * @param screenOn màn hình đang bật
     * @param ecoMode bật tiết kiệm thêm
     */
    fun adaptiveIntervalMinutes(
        context: Context,
        userPreference: Int,
        screenOn: Boolean = false,
        ecoMode: Boolean = true
    ): Int {
        val net = currentType(context)
        val batt = batteryLevelPercent(context)
        val powerSave = isSystemPowerSave(context)

        // Cơ sở theo mạng
        var minutes = when (net) {
            // Di động: NAT drop 3–5 phút — không được > 4 khi màn tắt
            NetType.CELLULAR -> {
                if (screenOn) userPreference.coerceIn(4, 8) else userPreference.coerceIn(3, 4)
            }
            // WiFi: NAT ổn định hơn — ưu tiên pin
            NetType.WIFI -> {
                if (screenOn) userPreference.coerceIn(8, 15) else userPreference.coerceIn(6, 12)
            }
            else -> userPreference.coerceIn(3, 10)
        }

        // Eco: kéo dài thêm khi không phải cellular màn tắt (vẫn giữ floor NAT)
        if (ecoMode) {
            when {
                net == NetType.WIFI -> minutes = (minutes + 2).coerceAtMost(15)
                net == NetType.CELLULAR && screenOn -> minutes = (minutes + 1).coerceAtMost(8)
            }
        }

        // Pin thấp hoặc Power Save hệ thống → nới chu kỳ (trừ cellular màn tắt tối thiểu 3)
        if (batt in 1..20 || powerSave) {
            minutes = if (net == NetType.CELLULAR && !screenOn) {
                maxOf(3, minutes)
            } else {
                (minutes + 3).coerceAtMost(18)
            }
        }

        return minutes.coerceAtLeast(2)
    }

    fun registerCallback(
        context: Context,
        onNetworkChanged: (NetType) -> Unit
    ): ConnectivityManager.NetworkCallback {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val type = currentType(context)
                Log.i(TAG, "Network available: $type")
                onNetworkChanged(type)
            }

            override fun onLost(network: Network) {
                Log.i(TAG, "Network lost")
                onNetworkChanged(NetType.NONE)
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val type = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetType.WIFI
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetType.CELLULAR
                    else -> NetType.OTHER
                }
                onNetworkChanged(type)
            }
        }
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e(TAG, "registerNetworkCallback failed", e)
        }
        return callback
    }

    fun unregisterCallback(context: Context, callback: ConnectivityManager.NetworkCallback?) {
        if (callback == null) return
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }
}

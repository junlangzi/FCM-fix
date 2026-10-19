package com.fcmguard.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.util.Calendar

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fcm_guard_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val HEARTBEAT_INTERVAL = intPreferencesKey("heartbeat_interval")
        val INTERVAL_SCREEN_ON = intPreferencesKey("interval_screen_on")
        val INTERVAL_SCREEN_OFF = intPreferencesKey("interval_screen_off")
        val STRONG_PROTECTION = booleanPreferencesKey("strong_protection")
        val HIDE_NOTIFICATION = booleanPreferencesKey("hide_notification")
        val SHOW_STATUS_ICON = booleanPreferencesKey("show_status_icon")
        val ADAPTIVE_INTERVAL = booleanPreferencesKey("adaptive_interval")
        val AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
        val RUN_MODE = stringPreferencesKey("run_mode")
        val WHITELIST_APP_AUTO = booleanPreferencesKey("whitelist_app_auto")
        val WHITELIST_GMS_AUTO = booleanPreferencesKey("whitelist_gms_auto")
        val WHITELIST_APP_BATTERY = booleanPreferencesKey("whitelist_app_battery")
        val WHITELIST_GMS_BATTERY = booleanPreferencesKey("whitelist_gms_battery")
        val WHITELIST_BACKGROUND = booleanPreferencesKey("whitelist_background")
        val TOTAL_SUCCESS = longPreferencesKey("total_success")
        val TOTAL_FAIL = longPreferencesKey("total_fail")
        val ENERGY_UAH = longPreferencesKey("energy_uah") // microamp-hours tích lũy
        val ACTIVE_MS = longPreferencesKey("active_ms") // thời gian active thực (cho ước lượng)
        val LAST_PING_TIME = longPreferencesKey("last_ping_time")
        val LAST_PING_TARGET = stringPreferencesKey("last_ping_target")
        val LAST_PING_LATENCY = longPreferencesKey("last_ping_latency")
        val SERVICE_START_TIME = longPreferencesKey("service_start_time")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PAUSED = booleanPreferencesKey("paused")
        val PING_MODE = stringPreferencesKey("ping_mode")
        val FIXED_PING_HOST = stringPreferencesKey("fixed_ping_host")
        val CUSTOM_PING_HOSTS = stringPreferencesKey("custom_ping_hosts") // JSON array of "host:port"
        val STATS_RESET_HOUR = intPreferencesKey("stats_reset_hour") // 0-23
        val STATS_RESET_MINUTE = intPreferencesKey("stats_reset_minute") // 0-59
        val LAST_STATS_RESET_DAY = longPreferencesKey("last_stats_reset_day") // yyyyMMdd
        val STATS_PERIOD_START = longPreferencesKey("stats_period_start") // ms bắt đầu chu kỳ thống kê
        val CACHED_BATTERY_CAPACITY = doublePreferencesKey("cached_battery_capacity_mah")
    }

    val runMode: Flow<String> = context.dataStore.data.map { it[Keys.RUN_MODE] ?: "ALWAYS" }
    val heartbeatInterval: Flow<Int> = context.dataStore.data.map { it[Keys.HEARTBEAT_INTERVAL] ?: 2 }
    val intervalScreenOn: Flow<Int> = context.dataStore.data.map { it[Keys.INTERVAL_SCREEN_ON] ?: 8 }
    val intervalScreenOff: Flow<Int> = context.dataStore.data.map { it[Keys.INTERVAL_SCREEN_OFF] ?: 0 }
    val strongProtection: Flow<Boolean> = context.dataStore.data.map { it[Keys.STRONG_PROTECTION] ?: false }
    val hideNotification: Flow<Boolean> = context.dataStore.data.map { it[Keys.HIDE_NOTIFICATION] ?: true }
    val showStatusIcon: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_STATUS_ICON] ?: true }
    val adaptiveInterval: Flow<Boolean> = context.dataStore.data.map { it[Keys.ADAPTIVE_INTERVAL] ?: true }
    val autoStartBoot: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_START_BOOT] ?: true }

    val whitelistAppAuto: Flow<Boolean> = context.dataStore.data.map { it[Keys.WHITELIST_APP_AUTO] ?: false }
    val whitelistGmsAuto: Flow<Boolean> = context.dataStore.data.map { it[Keys.WHITELIST_GMS_AUTO] ?: false }
    val whitelistAppBattery: Flow<Boolean> = context.dataStore.data.map { it[Keys.WHITELIST_APP_BATTERY] ?: false }
    val whitelistGmsBattery: Flow<Boolean> = context.dataStore.data.map { it[Keys.WHITELIST_GMS_BATTERY] ?: false }
    val whitelistBackground: Flow<Boolean> = context.dataStore.data.map { it[Keys.WHITELIST_BACKGROUND] ?: false }

    val totalSuccess: Flow<Long> = context.dataStore.data.map { it[Keys.TOTAL_SUCCESS] ?: 0L }
    val totalFail: Flow<Long> = context.dataStore.data.map { it[Keys.TOTAL_FAIL] ?: 0L }
    val energyUah: Flow<Long> = context.dataStore.data.map { it[Keys.ENERGY_UAH] ?: 0L }
    val activeMs: Flow<Long> = context.dataStore.data.map { it[Keys.ACTIVE_MS] ?: 0L }
    val lastPingTime: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_PING_TIME] ?: 0L }
    val lastPingTarget: Flow<String> = context.dataStore.data.map { it[Keys.LAST_PING_TARGET] ?: "1.1.1.1:53" }
    val lastPingLatency: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_PING_LATENCY] ?: -1L }
    val serviceStartTime: Flow<Long> = context.dataStore.data.map { it[Keys.SERVICE_START_TIME] ?: 0L }
    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "SYSTEM" }
    val paused: Flow<Boolean> = context.dataStore.data.map { it[Keys.PAUSED] ?: false }
    val pingMode: Flow<String> = context.dataStore.data.map { it[Keys.PING_MODE] ?: "ROTATE" }
    val fixedPingHost: Flow<String> = context.dataStore.data.map { it[Keys.FIXED_PING_HOST] ?: "1.1.1.1" }
    val statsResetHour: Flow<Int> = context.dataStore.data.map { it[Keys.STATS_RESET_HOUR] ?: 0 }
    val statsResetMinute: Flow<Int> = context.dataStore.data.map { it[Keys.STATS_RESET_MINUTE] ?: 0 }

    val statsPeriodStart: Flow<Long> = context.dataStore.data.map { it[Keys.STATS_PERIOD_START] ?: 0L }
    val cachedBatteryCapacityMah: Flow<Double> = context.dataStore.data.map {
        it[Keys.CACHED_BATTERY_CAPACITY] ?: 0.0
    }

    val customPingHosts: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.CUSTOM_PING_HOSTS]
        if (raw.isNullOrBlank()) defaultPingHosts()
        else parseHosts(raw)
    }

    suspend fun setHeartbeatInterval(minutes: Int) {
        context.dataStore.edit { it[Keys.HEARTBEAT_INTERVAL] = minutes }
    }

    suspend fun setIntervalScreenOn(minutes: Int) {
        context.dataStore.edit { it[Keys.INTERVAL_SCREEN_ON] = minutes.coerceIn(0, 120) }
    }

    suspend fun setIntervalScreenOff(minutes: Int) {
        context.dataStore.edit {
            it[Keys.INTERVAL_SCREEN_OFF] = minutes.coerceIn(0, 120)
            it[Keys.HEARTBEAT_INTERVAL] = minutes
        }
    }

    suspend fun setServiceStartTime(time: Long) {
        context.dataStore.edit { it[Keys.SERVICE_START_TIME] = time }
    }

    suspend fun setLastPingInfo(target: String, latencyMs: Long) {
        context.dataStore.edit {
            it[Keys.LAST_PING_TARGET] = target
            it[Keys.LAST_PING_LATENCY] = latencyMs
            it[Keys.LAST_PING_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setPaused(paused: Boolean) {
        context.dataStore.edit { it[Keys.PAUSED] = paused }
    }

    suspend fun setPingMode(mode: String) {
        context.dataStore.edit { it[Keys.PING_MODE] = mode }
    }

    suspend fun setFixedPingHost(host: String) {
        context.dataStore.edit { it[Keys.FIXED_PING_HOST] = host }
    }

    suspend fun setStrongProtection(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STRONG_PROTECTION] = enabled }
    }

    suspend fun setHideNotification(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_NOTIFICATION] = enabled }
    }

    suspend fun setShowStatusIcon(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_STATUS_ICON] = enabled }
    }

    suspend fun setAdaptiveInterval(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ADAPTIVE_INTERVAL] = enabled }
    }

    suspend fun setAutoStartBoot(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_START_BOOT] = enabled }
    }

    suspend fun setRunMode(mode: String) {
        context.dataStore.edit { it[Keys.RUN_MODE] = mode }
    }

    suspend fun setStatsResetHour(hour: Int) {
        context.dataStore.edit { it[Keys.STATS_RESET_HOUR] = hour.coerceIn(0, 23) }
    }

    suspend fun setCachedBatteryCapacityMah(mah: Double) {
        context.dataStore.edit { it[Keys.CACHED_BATTERY_CAPACITY] = mah }
    }

    suspend fun setStatsResetMinute(minute: Int) {
        context.dataStore.edit { it[Keys.STATS_RESET_MINUTE] = minute.coerceIn(0, 59) }
    }

    suspend fun setStatsResetTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.STATS_RESET_HOUR] = hour.coerceIn(0, 23)
            it[Keys.STATS_RESET_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setCustomPingHosts(hosts: List<String>) {
        val arr = JSONArray()
        hosts.forEach { arr.put(it) }
        context.dataStore.edit { it[Keys.CUSTOM_PING_HOSTS] = arr.toString() }
    }

    suspend fun addPingHost(hostPort: String) {
        val current = customPingHosts.first().toMutableList()
        val normalized = hostPort.trim()
        if (normalized.isNotEmpty() && !current.contains(normalized)) {
            current.add(normalized)
            setCustomPingHosts(current)
        }
    }

    suspend fun removePingHost(hostPort: String) {
        val current = customPingHosts.first().toMutableList()
        current.remove(hostPort)
        if (current.isEmpty()) setCustomPingHosts(defaultPingHosts())
        else setCustomPingHosts(current)
    }

    suspend fun setWhitelistStep(key: String, done: Boolean) {
        context.dataStore.edit {
            when (key) {
                "app_auto" -> it[Keys.WHITELIST_APP_AUTO] = done
                "gms_auto" -> it[Keys.WHITELIST_GMS_AUTO] = done
                "app_battery" -> it[Keys.WHITELIST_APP_BATTERY] = done
                "gms_battery" -> it[Keys.WHITELIST_GMS_BATTERY] = done
                "background" -> it[Keys.WHITELIST_BACKGROUND] = done
            }
        }
    }

    suspend fun incrementSuccess() {
        context.dataStore.edit {
            it[Keys.TOTAL_SUCCESS] = (it[Keys.TOTAL_SUCCESS] ?: 0L) + 1
            it[Keys.LAST_PING_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun incrementFail() {
        context.dataStore.edit {
            it[Keys.TOTAL_FAIL] = (it[Keys.TOTAL_FAIL] ?: 0L) + 1
        }
    }

    /**
     * Ghi nhận năng lượng tiêu thụ của một chu kỳ heartbeat.
     * @param activeMs thời gian active thực (wake + network)
     * @param avgCurrentMa dòng trung bình ước tính trong chu kỳ (mA)
     */
    suspend fun addEnergySample(activeMs: Long, avgCurrentMa: Double = 35.0) {
        if (activeMs <= 0) return
        // µAh = mA * hours * 1000
        val uah = (avgCurrentMa * (activeMs / 1000.0 / 3600.0) * 1000.0).toLong().coerceAtLeast(1)
        context.dataStore.edit {
            it[Keys.ENERGY_UAH] = (it[Keys.ENERGY_UAH] ?: 0L) + uah
            it[Keys.ACTIVE_MS] = (it[Keys.ACTIVE_MS] ?: 0L) + activeMs
            if ((it[Keys.STATS_PERIOD_START] ?: 0L) == 0L) {
                it[Keys.STATS_PERIOD_START] = System.currentTimeMillis()
            }
        }
    }

    /** Xóa thống kê ping — không xóa pin. */
    suspend fun clearStats() {
        context.dataStore.edit {
            it[Keys.TOTAL_SUCCESS] = 0L
            it[Keys.TOTAL_FAIL] = 0L
        }
    }

    /** Reset đầu ngày: ping + pin. */
    suspend fun clearStatsFullDaily() {
        context.dataStore.edit {
            it[Keys.TOTAL_SUCCESS] = 0L
            it[Keys.TOTAL_FAIL] = 0L
            it[Keys.ENERGY_UAH] = 0L
            it[Keys.ACTIVE_MS] = 0L
            it[Keys.STATS_PERIOD_START] = System.currentTimeMillis()
        }
    }

    /**
     * Reset thống kê nếu đã qua mốc giờ reset trong ngày (và chưa reset hôm nay).
     */
    suspend fun maybeDailyReset() {
        val hour = statsResetHour.first()
        val minute = statsResetMinute.first()
        val cal = Calendar.getInstance()
        val nowHour = cal.get(Calendar.HOUR_OF_DAY)
        val nowMin = cal.get(Calendar.MINUTE)
        val dayKey = cal.get(Calendar.YEAR) * 10000L +
            (cal.get(Calendar.MONTH) + 1) * 100L +
            cal.get(Calendar.DAY_OF_MONTH)

        val lastReset = context.dataStore.data.first()[Keys.LAST_STATS_RESET_DAY] ?: 0L
        val pastResetTime = nowHour > hour || (nowHour == hour && nowMin >= minute)
        if (pastResetTime && lastReset < dayKey) {
            clearStatsFullDaily()
            context.dataStore.edit { it[Keys.LAST_STATS_RESET_DAY] = dayKey }
        }
    }

    companion object {
        fun defaultPingHosts(): List<String> = listOf(
            "1.1.1.1:53",
            "1.0.0.1:53",
            "8.8.8.8:53",
            "8.8.4.4:53",
            "208.67.222.222:53",
            "208.67.220.220:53",
            "94.140.14.14:53",
            "76.76.2.0:53",
            "185.228.168.9:53"
        )

        fun parseHosts(raw: String): List<String> = try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        } catch (_: Exception) {
            defaultPingHosts()
        }
    }
}

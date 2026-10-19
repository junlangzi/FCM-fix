package com.fcmguard.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fcmguard.app.FcmGuardApp
import com.fcmguard.app.data.ActivityLog
import com.fcmguard.app.helper.BatteryEstimator
import com.fcmguard.app.helper.BatteryHelper
import com.fcmguard.app.helper.HeartbeatHelper
import com.fcmguard.app.helper.PingTargets
import com.fcmguard.app.helper.WhitelistHelper
import com.fcmguard.app.service.FcmGuardService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = FcmGuardApp.instance.preferences
    private val dao = FcmGuardApp.instance.database.statsDao()

    val totalSuccess = prefs.totalSuccess.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val totalFail = prefs.totalFail.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val energyUah = prefs.energyUah.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val statsPeriodStart = prefs.statsPeriodStart.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val runMode = prefs.runMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ALWAYS")
    val heartbeatInterval = prefs.heartbeatInterval.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)
    val intervalScreenOn = prefs.intervalScreenOn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)
    val intervalScreenOff = prefs.intervalScreenOff.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val strongProtection = prefs.strongProtection.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hideNotification = prefs.hideNotification.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoStartBoot = prefs.autoStartBoot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val adaptiveInterval = prefs.adaptiveInterval.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        val showStatusIcon = prefs.showStatusIcon.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val lastPingTarget = prefs.lastPingTarget.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1.1.1.1:53")
    val lastPingLatency = prefs.lastPingLatency.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1L)
    val serviceStartTime = prefs.serviceStartTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val themeMode = prefs.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")
    val paused = prefs.paused.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val pingMode = prefs.pingMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ROTATE")
    val fixedPingHost = prefs.fixedPingHost.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1.1.1.1")
    val statsResetHour = prefs.statsResetHour.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val statsResetMinute = prefs.statsResetMinute.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val customPingHosts = prefs.customPingHosts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _logs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val logs: StateFlow<List<ActivityLog>> = _logs.asStateFlow()

    val isIgnoringBattery = MutableStateFlow(BatteryHelper.isIgnoringBatteryOptimizations(application))
    val isNlsEnabled = MutableStateFlow(WhitelistHelper.isNotificationListenerEnabled(application))

    init {
        viewModelScope.launch {
            prefs.maybeDailyReset()
            val hosts = prefs.customPingHosts.first()
            PingTargets.updateCache(hosts)
            dao.getLogsSince(System.currentTimeMillis() - 24 * 3600_000L).collect { _logs.value = it }
        }
    }

    fun loadLogs24h() {
        viewModelScope.launch {
            val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            dao.getLogsSince(since).collect { _logs.value = it }
        }
    }

    fun refreshStatus() {
        isIgnoringBattery.value = BatteryHelper.isIgnoringBatteryOptimizations(getApplication())
        isNlsEnabled.value = WhitelistHelper.isNotificationListenerEnabled(getApplication())
        viewModelScope.launch { prefs.maybeDailyReset() }
    }

    fun batteryReport(): BatteryEstimator.BatteryReport {
        val energy = energyUah.value
        val start = statsPeriodStart.value
        return BatteryEstimator.report(getApplication(), energy, start)
    }

    fun setRunMode(mode: String) = viewModelScope.launch {
        prefs.setRunMode(mode)
        when (mode) {
            "OFF" -> FcmGuardService.stop(getApplication())
            else -> {
                FcmGuardService.start(getApplication())
                FcmGuardService.applyMode(getApplication())
            }
        }
    }

    fun setInterval(minutes: Int) = viewModelScope.launch {
        prefs.setHeartbeatInterval(minutes)
        prefs.setIntervalScreenOff(minutes)
    }

    fun setIntervalScreenOn(minutes: Int) = viewModelScope.launch {
        prefs.setIntervalScreenOn(minutes)
    }

    fun setIntervalScreenOff(minutes: Int) = viewModelScope.launch {
        prefs.setIntervalScreenOff(minutes)
    }

    fun setStrongProtection(enabled: Boolean) = viewModelScope.launch {
        prefs.setStrongProtection(enabled)
    }

    fun setHideNotification(enabled: Boolean) = viewModelScope.launch {
        prefs.setHideNotification(enabled)
    }

    fun setAutoStartBoot(enabled: Boolean) = viewModelScope.launch {
        prefs.setAutoStartBoot(enabled)
    }

    fun setAdaptiveInterval(enabled: Boolean) = viewModelScope.launch {
        prefs.setAdaptiveInterval(enabled)
    }

    fun setShowStatusIcon(enabled: Boolean) = viewModelScope.launch {
        prefs.setShowStatusIcon(enabled)
    }

    fun setThemeMode(mode: String) = viewModelScope.launch {
        prefs.setThemeMode(mode)
    }

    fun setPingMode(mode: String) = viewModelScope.launch {
        prefs.setPingMode(mode)
    }

    fun setFixedPingHost(host: String) = viewModelScope.launch {
        prefs.setFixedPingHost(host)
    }

    fun setStatsResetHour(hour: Int) = viewModelScope.launch {
        prefs.setStatsResetHour(hour)
    }

    fun setStatsResetMinute(minute: Int) = viewModelScope.launch {
        prefs.setStatsResetMinute(minute)
    }

    fun setStatsResetTime(hour: Int, minute: Int) = viewModelScope.launch {
        prefs.setStatsResetTime(hour, minute)
    }

    fun addPingHost(hostPort: String) = viewModelScope.launch {
        prefs.addPingHost(hostPort)
        PingTargets.updateCache(prefs.customPingHosts.first())
    }

    fun removePingHost(hostPort: String) = viewModelScope.launch {
        prefs.removePingHost(hostPort)
        PingTargets.updateCache(prefs.customPingHosts.first())
    }

    fun setPaused(paused: Boolean) = viewModelScope.launch {
        prefs.setPaused(paused)
        if (!paused) {
            FcmGuardService.start(getApplication())
            FcmGuardService.applyMode(getApplication())
        } else {
            FcmGuardService.applyMode(getApplication())
        }
    }

    fun manualPingTest() = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Gửi GMS trước (giống service)
            HeartbeatHelper.sendGmsHeartbeat(getApplication())
            val mode = prefs.pingMode.first()
            val host = prefs.fixedPingHost.first()
            val hosts = prefs.customPingHosts.first()
            PingTargets.updateCache(hosts)
            // Timeout dài hơn, thử nhiều IP hơn khi test tay
            val result = HeartbeatHelper.smartPing(
                mode = mode,
                preferredHost = host,
                hosts = hosts,
                maxAttempts = 5,
                timeoutMs = 3000
            )
            val target = "${result.host}:${result.port}"
            if (result.success) {
                prefs.incrementSuccess()
                prefs.setLastPingInfo(target, result.latencyMs)
                dao.insertLog(
                    ActivityLog(
                        timestamp = System.currentTimeMillis(),
                        message = "Test thủ công OK → $target (${result.latencyMs}ms, ${result.attempts} thử)",
                        type = "SUCCESS"
                    )
                )
            } else {
                prefs.incrementFail()
                prefs.setLastPingInfo(target, -1)
                dao.insertLog(
                    ActivityLog(
                        timestamp = System.currentTimeMillis(),
                        message = "Test thủ công FAILED sau ${result.attempts} IP ($target)",
                        type = "FAIL"
                    )
                )
            }
        } catch (e: Exception) {
            dao.insertLog(
                ActivityLog(
                    timestamp = System.currentTimeMillis(),
                    message = "Test thủ công lỗi: ${e.message}",
                    type = "FAIL"
                )
            )
        }
    }

    fun clearStats() = viewModelScope.launch {
        prefs.clearStats()
    }

    fun startService() {
        FcmGuardService.start(getApplication())
    }

    fun openNlsSettings() {
        WhitelistHelper.openNotificationListenerSettings(getApplication())
    }

    fun requestBatteryOpt() {
        BatteryHelper.requestIgnoreBatteryOptimizations(getApplication())
    }

    fun openColorOsBattery() {
        BatteryHelper.openColorOsBatteryStats(getApplication())
    }

    fun openAutoStart() {
        WhitelistHelper.openAutoStartSettings(getApplication())
    }

    fun openGmsBattery() {
        WhitelistHelper.openGmsBatterySettings(getApplication())
    }
}

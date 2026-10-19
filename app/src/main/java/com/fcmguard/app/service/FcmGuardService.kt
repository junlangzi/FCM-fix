package com.fcmguard.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.fcmguard.app.FcmGuardApp
import com.fcmguard.app.data.ActivityLog
import com.fcmguard.app.helper.BatteryEstimator
import com.fcmguard.app.helper.HeartbeatHelper
import com.fcmguard.app.helper.NetworkHelper
import com.fcmguard.app.helper.NotificationHelper
import com.fcmguard.app.helper.PingTargets
import com.fcmguard.app.receiver.HeartbeatReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Logic mới (quan trọng):
 * - AlarmManager LUÔN được lên lịch khi mode != OFF (kể cả lúc màn hình bật).
 * - Khi alarm nổ → kiểm tra mode + trạng thái màn hình:
 *     + ALWAYS        → luôn ping
 *     + SCREEN_OFF    → chỉ ping khi màn hình TẮT
 *     + OFF           → không làm gì
 * - Không còn hủy alarm khi màn hình bật (đây là lỗi khiến tắt màn hình không chạy).
 */
class FcmGuardService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private var lastNetType: NetworkHelper.NetType = NetworkHelper.NetType.NONE
    /** Thời điểm heartbeat thành công gần nhất (ms) — tránh ping thừa */
    @Volatile private var lastSuccessAt: Long = 0L
    /** Debounce heartbeat do đổi mạng */
    @Volatile private var lastForcedAt: Long = 0L
    @Volatile private var notifRefreshCounter: Int = 0

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    log("Screen OFF detected → kích hoạt keep-alive ngay.", "SCREEN")
                    // Đánh thức ngay 1 nhịp + đảm bảo alarm đang được lên lịch
                    scope.launch {
                        val mode = FcmGuardApp.instance.preferences.runMode.first()
                        if (mode != "OFF") {
                            ensureAlarmScheduled()
                            performHeartbeat(force = true)
                        }
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    log("Screen ON detected.", "SCREEN")
                    // Không hủy alarm — chỉ bỏ qua ping trong lần chạy tiếp theo
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        // Đăng ký theo dõi đổi mạng → heartbeat ngay (chống NAT drop khi 4G↔WiFi)
        lastNetType = NetworkHelper.currentType(this)
        networkCallback = NetworkHelper.registerCallback(this) { newType ->
            if (newType != NetworkHelper.NetType.NONE && newType != lastNetType) {
                val now = System.currentTimeMillis()
                // Debounce: tối thiểu 45s giữa các forced heartbeat do đổi mạng
                if (now - lastForcedAt < 45_000L) {
                    lastNetType = newType
                    return@registerCallback
                }
                log("Mạng đổi: $lastNetType → $newType → heartbeat.", "INFO")
                lastNetType = newType
                lastForcedAt = now
                scope.launch {
                    val mode = FcmGuardApp.instance.preferences.runMode.first()
                    if (mode != "OFF") {
                        ensureAlarmScheduled()
                        performHeartbeat(force = true)
                    }
                }
            } else {
                lastNetType = newType
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_PERFORM_HEARTBEAT -> {
                scope.launch { performHeartbeat(force = false) }
                return START_STICKY
            }
            ACTION_APPLY_MODE -> {
                scope.launch { applyRunMode() }
                return START_STICKY
            }
            ACTION_STOP -> {
                stopSelfSafely()
                return START_NOT_STICKY
            }
        }

        if (!isRunning) {
            isRunning = true
            startForegroundSafe()
            log("Service started with NotificationListenerService anchor.")
            scope.launch {
                FcmGuardApp.instance.preferences.setServiceStartTime(System.currentTimeMillis())
                applyRunMode()
            }
        }
        return START_STICKY
    }

    private suspend fun applyRunMode() {
        val mode = FcmGuardApp.instance.preferences.runMode.first()
        when (mode) {
            "OFF" -> {
                HeartbeatReceiver.cancel(this)
                log("Chế độ TẮT → dừng heartbeat & service.", "INFO")
                stopSelfSafely()
            }
            "SCREEN_OFF" -> {
                // Luôn giữ alarm, không hủy khi màn hình bật
                ensureAlarmScheduled()
                if (isScreenCurrentlyOn()) {
                    log("Chế độ CHỈ TẮT MÀN HÌNH → alarm vẫn chạy, chỉ ping khi màn hình tắt.", "INFO")
                } else {
                    log("Chế độ CHỈ TẮT MÀN HÌNH → màn hình đang tắt, bắt đầu ping.", "INFO")
                    performHeartbeat(force = true)
                }
            }
            else -> { // ALWAYS
                ensureAlarmScheduled()
                log("Chế độ LUÔN CHẠY → heartbeat đang hoạt động.", "INFO")
                performHeartbeat(force = true)
            }
        }
    }

    private suspend fun ensureAlarmScheduled() {
        val prefs = FcmGuardApp.instance.preferences
        val mode = prefs.runMode.first()
        val adaptive = prefs.adaptiveInterval.first()
        val strong = prefs.strongProtection.first()
        val screenOn = isScreenCurrentlyOn()

        // ALWAYS: dùng interval riêng khi màn hình bật / tắt
        val userPref = when {
            mode == "ALWAYS" && screenOn -> prefs.intervalScreenOn.first()
            mode == "ALWAYS" && !screenOn -> prefs.intervalScreenOff.first()
            else -> prefs.intervalScreenOff.first()
        }

        // userPref == 0 → Auto: luôn adaptive theo mạng/pin/màn hình
        val useAuto = userPref <= 0 || adaptive
        val basePref = if (userPref <= 0) 3 else userPref
        var minutes = if (useAuto) {
            NetworkHelper.adaptiveIntervalMinutes(
                this, basePref, screenOn = screenOn, ecoMode = true
            )
        } else {
            val batt = NetworkHelper.batteryLevelPercent(this)
            if (batt in 1..15 && !screenOn) maxOf(basePref, 4) else basePref
        }
        // Bảo vệ mạnh: chỉ rút ngắn khi cellular / màn tắt, tối thiểu 2 phút (tránh tốn pin)
        if (strong) {
            val net = NetworkHelper.currentType(this)
            minutes = if (net == NetworkHelper.NetType.CELLULAR) {
                maxOf(2, minutes - 1)
            } else {
                maxOf(3, minutes - 1)
            }
        }

        val net = NetworkHelper.currentType(this)
        Log.d(TAG, "Schedule heartbeat: ${minutes}min (net=$net, screenOn=$screenOn, adaptive=$adaptive)")
        HeartbeatReceiver.schedule(this, minutes)
    }

    /**
     * @param force true = bỏ qua kiểm tra màn hình (dùng khi vừa nhận SCREEN_OFF)
     */
    private suspend fun performHeartbeat(force: Boolean) {
        val prefs = FcmGuardApp.instance.preferences
        val mode = prefs.runMode.first()
        if (mode == "OFF") return

        if (prefs.paused.first()) {
            log("Đang tạm dừng — bỏ qua ping.", "INFO")
            ensureAlarmScheduled()
            refreshNotification()
            return
        }

        val screenOn = isScreenCurrentlyOn()
        if (!force && mode == "SCREEN_OFF" && screenOn) {
            log("Bỏ qua ping (màn hình đang bật, mode=SCREEN_OFF). Alarm vẫn giữ.", "INFO")
            ensureAlarmScheduled()
            return
        }

        prefs.maybeDailyReset()

        // Bỏ qua nếu vừa ping thành công < 50s (trừ khi force) — giảm wake thừa
        val now = System.currentTimeMillis()
        if (!force && lastSuccessAt > 0 && now - lastSuccessAt < 50_000L) {
            log("Bỏ qua (vừa ping ${((now - lastSuccessAt) / 1000)}s trước).", "INFO")
            ensureAlarmScheduled()
            return
        }

        val hosts = prefs.customPingHosts.first()
        PingTargets.updateCache(hosts)

        acquireBriefWakeLock()
        try {
            val start = System.currentTimeMillis()
            // Luôn gửi GMS broadcast (rẻ, không mạng)
            val gmsOk = HeartbeatHelper.sendGmsHeartbeat(this)

            val net = NetworkHelper.currentType(this@FcmGuardService)
            // Màn hình BẬT + WiFi: chỉ GMS broadcast là đủ phần lớn trường hợp (tiết kiệm radio)
            val lightPath = screenOn && net == NetworkHelper.NetType.WIFI && !force

            val result = if (lightPath) {
                HeartbeatHelper.PingResult(true, "gms-only", 0, 0, 0)
            } else {
                val pingMode = prefs.pingMode.first()
                val fixedHost = prefs.fixedPingHost.first()
                HeartbeatHelper.smartPing(
                    mode = pingMode,
                    preferredHost = fixedHost,
                    hosts = hosts,
                    maxAttempts = 2
                )
            }

            val target = if (lightPath) "GMS-broadcast" else "${result.host}:${result.port}"
            if (result.success || gmsOk) {
                lastSuccessAt = System.currentTimeMillis()
                prefs.incrementSuccess()
                prefs.setLastPingInfo(target, result.latencyMs)
                val msg = when {
                    lightPath -> "Light keep-alive (GMS only, WiFi+screenOn) net=$net."
                    result.success -> "Socket ping OK → $target (${result.latencyMs}ms, ${result.attempts} thử) net=$net."
                    else -> "GMS heartbeat OK (net=$net)."
                }
                log(msg, "SUCCESS")
            } else {
                prefs.incrementFail()
                prefs.setLastPingInfo(target, -1)
                log("Ping FAILED sau ${result.attempts} IP (net=$net).", "FAIL")
            }

            val activeMs = System.currentTimeMillis() - start
            // Light path gần như không tốn radio
            val currentMa = if (lightPath) 8.0 else BatteryEstimator.DEFAULT_ACTIVE_CURRENT_MA
            prefs.addEnergySample(activeMs, currentMa)
            // Refresh notification thưa hơn (mỗi 3 lần) để giảm IPC
            notifRefreshCounter++
            if (notifRefreshCounter >= 3 || force) {
                notifRefreshCounter = 0
                refreshNotification()
            }
        } finally {
            releaseWakeLock()
        }

        if (mode != "OFF") {
            ensureAlarmScheduled()
        }
    }

    private suspend fun refreshNotification() {
        val prefs = FcmGuardApp.instance.preferences
        val hide = prefs.hideNotification.first()
        val paused = prefs.paused.first()
        val success = prefs.totalSuccess.first()
        val energy = prefs.energyUah.first()
        val periodStart = prefs.statsPeriodStart.first()
        val report = BatteryEstimator.report(this, energy, periodStart)
        val notification = NotificationHelper.buildForegroundNotification(
            this, hide, paused, success, report.usedPercent, report.usedMah
        )
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun isScreenCurrentlyOn(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isInteractive
    }

    private fun acquireBriefWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FcmGuard::HeartbeatWakelock"
            ).apply {
                setReferenceCounted(false)
                acquire(4_000L) // tối đa 4 giây — đủ cho 1–2 UDP ping
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock failed", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {
        } finally {
            wakeLock = null
        }
    }

    private fun startForegroundSafe() {
        scope.launch {
            val prefs = FcmGuardApp.instance.preferences
            val hide = try { prefs.hideNotification.first() } catch (_: Exception) { true }
            val paused = try { prefs.paused.first() } catch (_: Exception) { false }
            val success = try { prefs.totalSuccess.first() } catch (_: Exception) { 0L }
            val energy = try { prefs.energyUah.first() } catch (_: Exception) { 0L }
            val periodStart = try { prefs.statsPeriodStart.first() } catch (_: Exception) { 0L }
            val report = BatteryEstimator.report(this@FcmGuardService, energy, periodStart)
            val notification = NotificationHelper.buildForegroundNotification(
                this@FcmGuardService, hide, paused, success, report.usedPercent, report.usedMah
            )
            withContext(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceCompat.startForeground(
                        this@FcmGuardService, NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun log(message: String, type: String = "INFO") {
        scope.launch {
            try {
                FcmGuardApp.instance.database.statsDao().insertLog(
                    ActivityLog(
                        timestamp = System.currentTimeMillis(),
                        message = message,
                        type = type
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Log insert failed", e)
            }
            Log.i(TAG, message)
        }
    }

    private fun stopSelfSafely() {
        isRunning = false
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        NetworkHelper.unregisterCallback(this, networkCallback)
        networkCallback = null
        HeartbeatReceiver.cancel(this)
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isRunning = false
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        releaseWakeLock()
        scope.cancel()
        // Sticky: nếu bị kill ngoài ý muốn và mode != OFF thì alarm sẽ kéo lại
        scope.launch {
            try {
                val mode = FcmGuardApp.instance.preferences.runMode.first()
                if (mode != "OFF") {
                    HeartbeatReceiver.schedule(this@FcmGuardService, 1)
                }
            } catch (_: Exception) {
            }
        }
        super.onDestroy()
        Log.w(TAG, "Service destroyed")
    }

    companion object {
        private const val TAG = "FcmGuardService"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PERFORM_HEARTBEAT = "com.fcmguard.app.ACTION_PERFORM_HEARTBEAT"
        const val ACTION_APPLY_MODE = "com.fcmguard.app.ACTION_APPLY_MODE"
        const val ACTION_STOP = "com.fcmguard.app.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, FcmGuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun triggerHeartbeat(context: Context) {
            val intent = Intent(context, FcmGuardService::class.java).apply {
                action = ACTION_PERFORM_HEARTBEAT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun applyMode(context: Context) {
            val intent = Intent(context, FcmGuardService::class.java).apply {
                action = ACTION_APPLY_MODE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FcmGuardService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
                context.stopService(Intent(context, FcmGuardService::class.java))
            }
        }
    }
}

package com.fcmguard.app.helper

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.fcmguard.app.FcmGuardApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Ưu tiên dung lượng pin đầy (mAh):
 * 1. Cache đã khóa
 * 2. DeviceBatteryCatalog (MODEL/DEVICE → thông số hãng)
 * 3. PowerProfile hệ thống
 * 4. CHARGE_COUNTER khi pin ≥ 80%
 * 5. Fallback 4000 mAh
 */
object BatteryEstimator {

    private const val TAG = "BatteryEstimator"
    private const val DEFAULT_CAPACITY_MAH = 4000.0
    private const val MIN_CAPACITY = 1500.0
    private const val MAX_CAPACITY = 12000.0

    const val DEFAULT_ACTIVE_CURRENT_MA = 28.0

    @Volatile private var memoryCacheMah: Double = 0.0
    @Volatile private var capacitySource: String = ""

    data class BatteryReport(
        val capacityMah: Double,
        val usedMah: Double,
        val usedPercent: Double,
        val estimatedPercentPer24h: Double,
        val periodElapsedMs: Long,
        val capacitySource: String = ""
    )

    fun getBatteryCapacityMah(context: Context): Double {
        if (memoryCacheMah in MIN_CAPACITY..MAX_CAPACITY) return memoryCacheMah

        val stored = readStoredCapacity()
        if (stored in MIN_CAPACITY..MAX_CAPACITY) {
            memoryCacheMah = stored
            capacitySource = "cache"
            return stored
        }

        DeviceBatteryCatalog.lookup()?.let {
            persistCapacity(it.mah.toDouble(), "catalog:${it.label}")
            return it.mah.toDouble()
        }

        readPowerProfileCapacity(context)?.let {
            persistCapacity(it, "PowerProfile")
            return it
        }

        estimateFromChargeCounter(context)?.let {
            persistCapacity(it, "ChargeCounter")
            return it
        }

        readCapacityFromIntent(context)?.let {
            persistCapacity(it, "Intent")
            return it
        }

        memoryCacheMah = DEFAULT_CAPACITY_MAH
        capacitySource = "default"
        return DEFAULT_CAPACITY_MAH
    }

    fun refreshCapacityIfNeeded(context: Context) {
        val stored = memoryCacheMah.takeIf { it > 0 } ?: readStoredCapacity()
        if (stored in MIN_CAPACITY..MAX_CAPACITY && capacitySource.startsWith("catalog")) {
            memoryCacheMah = stored
            return
        }

        val catalog = DeviceBatteryCatalog.lookup()
        if (catalog != null) {
            persistCapacity(catalog.mah.toDouble(), "catalog:${catalog.label}")
            return
        }

        val profile = readPowerProfileCapacity(context)
        if (profile != null) {
            if (stored <= 0 || kotlin.math.abs(profile - stored) > 80) {
                persistCapacity(profile, "PowerProfile-refresh")
            } else {
                memoryCacheMah = stored
                capacitySource = "cache"
            }
            return
        }

        val est = estimateFromChargeCounter(context) ?: return
        if (est > stored + 100) persistCapacity(est, "ChargeCounter-up")
        else if (stored > 0) {
            memoryCacheMah = stored
            capacitySource = "cache"
        }
    }

    fun getCapacitySource(): String = capacitySource

    private fun readPowerProfileCapacity(context: Context): Double? {
        return try {
            val clazz = Class.forName("com.android.internal.os.PowerProfile")
            val profile = clazz.getConstructor(Context::class.java).newInstance(context)
            val value = clazz.getMethod("getBatteryCapacity").invoke(profile)
            val mah = when (value) {
                is Double -> value
                is Float -> value.toDouble()
                is Int -> value.toDouble()
                is Long -> value.toDouble()
                else -> return null
            }
            if (mah in MIN_CAPACITY..MAX_CAPACITY) mah else null
        } catch (e: Exception) {
            Log.w(TAG, "PowerProfile: ${e.message}")
            null
        }
    }

    private fun estimateFromChargeCounter(context: Context): Double? {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val counter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (counter <= 0 || pct !in 80..100) return null
            val asUah = (counter / 1000.0) / (pct / 100.0)
            val asMah = counter.toDouble() / (pct / 100.0)
            listOf(asUah, asMah).filter { it in MIN_CAPACITY..MAX_CAPACITY }.maxOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun readCapacityFromIntent(context: Context): Double? {
        return try {
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
            val counter = battery.getIntExtra("charge_counter", -1)
            val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (counter <= 0 || level < 80) return null
            val pct = level * 100.0 / scale
            val asUah = (counter / 1000.0) / (pct / 100.0)
            if (asUah in MIN_CAPACITY..MAX_CAPACITY) asUah else null
        } catch (_: Exception) {
            null
        }
    }

    private fun readStoredCapacity(): Double = try {
        runBlocking { FcmGuardApp.instance.preferences.cachedBatteryCapacityMah.first() }
    } catch (_: Exception) { 0.0 }

    private fun persistCapacity(mah: Double, source: String) {
        val clamped = mah.coerceIn(MIN_CAPACITY, MAX_CAPACITY)
        memoryCacheMah = clamped
        capacitySource = source
        Log.i(TAG, "Capacity locked=$clamped mAh ($source) ${DeviceBatteryCatalog.deviceLabel()}")
        try {
            runBlocking { FcmGuardApp.instance.preferences.setCachedBatteryCapacityMah(clamped) }
        } catch (e: Exception) {
            Log.w(TAG, "persist failed", e)
        }
    }

    fun report(context: Context, energyUah: Long, periodStartMs: Long): BatteryReport {
        refreshCapacityIfNeeded(context)
        val capacity = getBatteryCapacityMah(context)
        val usedMah = (energyUah / 1000.0).coerceAtLeast(0.0)
        val usedPct = if (capacity > 0) (usedMah / capacity) * 100.0 else 0.0
        val elapsed = if (periodStartMs > 0) (System.currentTimeMillis() - periodStartMs).coerceAtLeast(60_000L) else 60_000L
        val factor = (24.0 * 3600_000.0) / elapsed.toDouble()
        return BatteryReport(
            capacityMah = capacity,
            usedMah = usedMah,
            usedPercent = usedPct,
            estimatedPercentPer24h = (usedPct * factor).coerceIn(0.0, 100.0),
            periodElapsedMs = elapsed,
            capacitySource = capacitySource
        )
    }

    fun usedPercent(context: Context, energyUah: Long): Double = report(context, energyUah, 0).usedPercent
}

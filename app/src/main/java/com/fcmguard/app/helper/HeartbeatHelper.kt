package com.fcmguard.app.helper

import android.content.Context
import android.content.Intent
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object HeartbeatHelper {

    private const val TAG = "HeartbeatHelper"

    private const val ACTION_GTALK = "com.google.android.intent.action.GTALK_HEARTBEAT"
    private const val ACTION_MCS = "com.google.android.intent.action.MCS_HEARTBEAT"

    private val TARGET_PACKAGES = arrayOf(
        "com.google.android.gms",
        "com.google.android.gsf"
    )

    data class PingResult(
        val success: Boolean,
        val host: String,
        val port: Int,
        val latencyMs: Long,
        val attempts: Int
    )

    fun sendGmsHeartbeat(context: Context): Boolean {
        var sent = false
        for (pkg in TARGET_PACKAGES) {
            try {
                context.sendBroadcast(Intent(ACTION_GTALK).setPackage(pkg))
                context.sendBroadcast(Intent(ACTION_MCS).setPackage(pkg))
                sent = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send heartbeat to $pkg", e)
            }
        }
        return sent
    }

    fun socketPing(host: String = "1.1.1.1", port: Int = 53, timeoutMs: Int = 1500): Long {
        return try {
            val start = System.currentTimeMillis()
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                // Prefer IPv4
                val address = InetAddress.getAllByName(host).firstOrNull {
                    it is java.net.Inet4Address
                } ?: InetAddress.getByName(host)
                val data = byteArrayOf(
                    0x00, 0x00, 0x01, 0x00, 0x00, 0x01,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00
                )
                val packet = DatagramPacket(data, data.size, address, port)
                socket.send(packet)
                val buffer = ByteArray(512)
                socket.receive(DatagramPacket(buffer, buffer.size))
            }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            Log.w(TAG, "Socket ping failed $host:$port — ${e.javaClass.simpleName}: ${e.message}")
            -1L
        }
    }

    fun smartPing(
        mode: String = "ROTATE",
        preferredHost: String = "1.1.1.1",
        hosts: List<String> = emptyList(),
        maxAttempts: Int = 2,
        timeoutMs: Int = 1500
    ): PingResult {
        if (hosts.isNotEmpty()) {
            PingTargets.updateCache(hosts)
        }
        val list = PingTargets.all()
        if (list.isEmpty()) {
            return PingResult(false, preferredHost, 53, -1, 0)
        }

        // preferredHost có thể là "1.1.1.1" hoặc "1.1.1.1:53"
        val cleanPreferred = preferredHost.substringBefore(":").trim().ifBlank { list.first().host }

        var host = when (mode) {
            "FIXED" -> cleanPreferred
            "RANDOM" -> PingTargets.random().host
            else -> {
                // ROTATE: bắt đầu từ preferred nếu có trong list, không thì first
                if (list.any { it.host == cleanPreferred }) cleanPreferred else list.first().host
            }
        }

        var attempts = 0
        while (attempts < maxAttempts && list.isNotEmpty()) {
            attempts++
            val target = list.find { it.host == host } ?: list[attempts % list.size]
            val latency = socketPing(target.host, target.port, timeoutMs)
            if (latency >= 0) {
                return PingResult(true, target.host, target.port, latency, attempts)
            }
            host = PingTargets.nextAfter(host).host
            Log.i(TAG, "Ping fail, try next: $host (attempt $attempts)")
        }
        val last = list.find { it.host == host } ?: list.first()
        return PingResult(false, last.host, last.port, -1, attempts)
    }
}

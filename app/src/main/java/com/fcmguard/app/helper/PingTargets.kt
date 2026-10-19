package com.fcmguard.app.helper

/**
 * Danh sách IP/host DNS để NAT keep-alive.
 * Nguồn mặc định (không có 9.9.9.9). User có thể thêm/xóa qua Preferences.
 */
object PingTargets {

    data class Target(val host: String, val port: Int, val label: String = "") {
        fun display() = if (port > 0) "$host:$port" else host
    }

    /** Parse "1.1.1.1:53" hoặc "1.1.1.1" */
    fun parse(entry: String): Target? {
        val s = entry.trim()
        if (s.isEmpty()) return null
        return try {
            if (s.contains(":")) {
                val parts = s.split(":")
                Target(parts[0], parts.getOrNull(1)?.toIntOrNull() ?: 53)
            } else {
                Target(s, 53)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun fromList(hosts: List<String>): List<Target> {
        val list = hosts.mapNotNull { parse(it) }
        return list.ifEmpty {
            PreferencesManagerDefaults.mapNotNull { parse(it) }
        }
    }

    private val PreferencesManagerDefaults = listOf(
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

    @Volatile
    private var cached: List<Target> = PreferencesManagerDefaults.mapNotNull { parse(it) }

    fun updateCache(hosts: List<String>) {
        cached = fromList(hosts)
    }

    fun all(): List<Target> = cached.ifEmpty {
        PreferencesManagerDefaults.mapNotNull { parse(it) }
    }

    fun random(): Target = all().random()

    fun nextAfter(currentHost: String?): Target {
        val list = all()
        if (list.isEmpty()) return Target("1.1.1.1", 53)
        if (currentHost == null) return list.random()
        val idx = list.indexOfFirst { it.host == currentHost }
        if (idx < 0) return list.random()
        return list[(idx + 1) % list.size]
    }
}

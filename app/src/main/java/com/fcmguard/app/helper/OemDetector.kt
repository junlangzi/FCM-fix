package com.fcmguard.app.helper

import android.os.Build

object OemDetector {

    enum class Oem {
        COLOROS, // Oppo / Realme / OnePlus
        HYPEROS, // Xiaomi / Redmi / POCO
        ORIGINOS, // Vivo / iQOO
        EMUI, // Huawei / Honor
        SAMSUNG,
        STOCK
    }

    fun detect(): Oem {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return when {
            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") ||
                b.contains("oppo") || b.contains("realme") || b.contains("oneplus") -> Oem.COLOROS
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ||
                b.contains("xiaomi") || b.contains("redmi") || b.contains("poco") -> Oem.HYPEROS
            m.contains("vivo") || m.contains("iqoo") ||
                b.contains("vivo") || b.contains("iqoo") -> Oem.ORIGINOS
            m.contains("huawei") || m.contains("honor") ||
                b.contains("huawei") || b.contains("honor") -> Oem.EMUI
            m.contains("samsung") || b.contains("samsung") -> Oem.SAMSUNG
            else -> Oem.STOCK
        }
    }

    fun displayName(oem: Oem = detect()): String = when (oem) {
        Oem.COLOROS -> "ColorOS / OxygenOS (OPPO · Realme · OnePlus)"
        Oem.HYPEROS -> "HyperOS / MIUI (Xiaomi · Redmi · POCO)"
        Oem.ORIGINOS -> "OriginOS / Funtouch (Vivo · iQOO)"
        Oem.EMUI -> "EMUI / MagicOS (Huawei · Honor)"
        Oem.SAMSUNG -> "One UI (Samsung)"
        Oem.STOCK -> "Android gốc / hãng khác"
    }

    data class GuideStep(
        val title: String,
        val detail: String,
        val action: GuideAction
    )

    enum class GuideAction {
        AUTO_START, BATTERY_APP, BATTERY_GMS, NLS, APP_DETAILS
    }

    fun stepsFor(oem: Oem = detect()): List<GuideStep> {
        return when (oem) {
            Oem.COLOROS -> listOf(
                GuideStep("Bật Auto-launch cho FCM fix", "Cài đặt → Ứng dụng → Tự khởi chạy → FCM fix", GuideAction.AUTO_START),
                GuideStep("Bật Auto-launch cho Google Play Services", "Cùng mục Tự khởi chạy → Google Play services", GuideAction.AUTO_START),
                GuideStep("Cho phép chạy nền FCM fix", "Cài đặt → Pin → FCM fix → Cho phép hoạt động nền", GuideAction.BATTERY_APP),
                GuideStep("Bỏ tối ưu pin Google Play Services", "Pin → Google Play services → Không tối ưu", GuideAction.BATTERY_GMS),
                GuideStep("Bật Notification Listener", "Quyền truy cập thông báo → FCM fix", GuideAction.NLS),
                GuideStep("Khóa app trong Recent", "Vuốt gần đây → khóa FCM fix (không vuốt xóa)", GuideAction.APP_DETAILS)
            )
            Oem.HYPEROS -> listOf(
                GuideStep("Bật Tự khởi động FCM fix", "Cài đặt → Ứng dụng → Quản lý tự khởi động", GuideAction.AUTO_START),
                GuideStep("Tự khởi động Google Play Services", "Cùng mục quản lý tự khởi động", GuideAction.AUTO_START),
                GuideStep("Pin không hạn chế – FCM fix", "Cài đặt → Pin → Tiết kiệm pin cho app → Không hạn chế", GuideAction.BATTERY_APP),
                GuideStep("Pin không hạn chế – GMS", "Google Play services → Không hạn chế", GuideAction.BATTERY_GMS),
                GuideStep("Bật Notification Listener", "Quyền thông báo hệ thống", GuideAction.NLS),
                GuideStep("Khóa trong Recent apps", "Khóa FCM fix trên màn hình đa nhiệm", GuideAction.APP_DETAILS)
            )
            Oem.ORIGINOS -> listOf(
                GuideStep("Cho phép tự khởi động", "iManager / Cài đặt → Quyền → Tự khởi động", GuideAction.AUTO_START),
                GuideStep("Tự khởi động GMS", "Cùng danh sách tự khởi động", GuideAction.AUTO_START),
                GuideStep("Pin – High background power", "Cài đặt pin → FCM fix → High background power consumption", GuideAction.BATTERY_APP),
                GuideStep("Pin – GMS", "Google Play services → cho phép nền", GuideAction.BATTERY_GMS),
                GuideStep("Notification Listener", "Bật quyền đọc thông báo", GuideAction.NLS)
            )
            Oem.EMUI -> listOf(
                GuideStep("Bảo vệ ứng dụng / Tự khởi động", "Trình quản lý điện thoại → Khởi chạy ứng dụng", GuideAction.AUTO_START),
                GuideStep("GMS tự khởi động", "Cho phép thủ công Google Play services", GuideAction.AUTO_START),
                GuideStep("Bỏ tối ưu pin FCM fix", "Pin → Khởi chạy ứng dụng → Quản lý thủ công", GuideAction.BATTERY_APP),
                GuideStep("Notification Listener", "Bật quyền thông báo", GuideAction.NLS)
            )
            else -> listOf(
                GuideStep("Bỏ tối ưu pin FCM fix", "Cài đặt → Pin → Không tối ưu hóa", GuideAction.BATTERY_APP),
                GuideStep("Bỏ tối ưu pin GMS", "Google Play services → Không tối ưu hóa", GuideAction.BATTERY_GMS),
                GuideStep("Notification Listener", "Bật quyền truy cập thông báo", GuideAction.NLS),
                GuideStep("Chi tiết ứng dụng", "Kiểm tra quyền nền nếu có", GuideAction.APP_DETAILS)
            )
        }
    }
}

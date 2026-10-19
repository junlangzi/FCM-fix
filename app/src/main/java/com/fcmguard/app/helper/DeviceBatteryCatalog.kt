package com.fcmguard.app.helper

import android.os.Build
import android.util.Log

/**
 * Tra dung lượng pin (mAh) theo model máy — thông số thiết kế từ hãng.
 * Ưu tiên khớp Build.MODEL / DEVICE / PRODUCT (không phân biệt hoa thường, bỏ khoảng trắng).
 *
 * Không cover 100% mọi máy; kết hợp PowerProfile cho máy không có trong list.
 */
object DeviceBatteryCatalog {

    private const val TAG = "BatteryCatalog"

    data class Match(val mah: Int, val label: String)

    /** key đã normalize → mAh */
    private val CATALOG: Map<String, Int> = mapOf(
        // ——— OPPO / ColorOS ———
        "cph2573" to 5000, // Reno11 F
        "cph2609" to 5000,
        "cph2591" to 5000, // A78
        "cph2525" to 5000, // A78 5G
        "cph2471" to 5000, // A77s
        "cph2387" to 5000, // A77
        "cph2333" to 5000, // Reno8 T
        "cph2359" to 4500, // Find X5
        "cph2305" to 4800, // Find X5 Pro
        "cph2413" to 4800, // Find X5 Pro
        "cph2451" to 4800, // Find N2 Flip
        "cph2437" to 4500, // Find N2
        "cph2557" to 5000, // Reno10
        "cph2531" to 4600, // Reno10 Pro
        "cph2521" to 4600, // Reno10 Pro+
        "cph2527" to 5000, // A98
        "cph2565" to 5000, // A58
        "cph2577" to 5000, // A38
        "cph2599" to 5000, // A18
        "cph2669" to 5500, // Reno12 series approx
        "cph2687" to 5000,
        "cph2693" to 5800, // một số Find X8 series
        "cph2653" to 5400,
        "oppo reno11" to 5000,
        "oppo reno10" to 5000,
        "oppo reno12" to 5000,
        "oppo a78" to 5000,
        "oppo a58" to 5000,
        "oppo find x5" to 4800,
        "oppo find x6" to 4800,
        "oppo find x7" to 5000,
        "oppo find x8" to 5630,

        // ——— Realme ———
        "rmx3630" to 5000,
        "rmx3710" to 5000,
        "rmx3760" to 5000,
        "rmx3761" to 5000,
        "rmx3780" to 5000, // 11 Pro+
        "rmx3771" to 5000,
        "rmx3830" to 5000,
        "rmx3840" to 5000,
        "rmx3851" to 5400, // GT5
        "rmx3888" to 5500,
        "rmx3700" to 5000,
        "rmx3370" to 4500, // GT Neo2
        "rmx3371" to 4500,
        "rmx3461" to 5000,
        "rmx3085" to 5000, // 8
        "rmx3393" to 5000, // 9 Pro+
        "realme 12" to 5000,
        "realme 11" to 5000,
        "realme gt" to 5000,
        "realme c55" to 5000,
        "realme narzo" to 5000,

        // ——— OnePlus ———
        "cph2449" to 5400, // 11
        "cph2451" to 4800,
        "cph2581" to 5400, // 12
        "cph2611" to 5400, // 12R
        "cph2609" to 5500,
        "le2121" to 4500, // 9 Pro
        "le2111" to 4500, // 9
        "kb2001" to 4500, // 8T
        "oneplus 12" to 5400,
        "oneplus 11" to 5000,
        "oneplus nord" to 5000,

        // ——— Xiaomi / Redmi / POCO / HyperOS ———
        "2211133c" to 5000, // 13
        "2210132c" to 4600, // 13 Pro
        "2304fpn6dc" to 5000, // 13 Ultra
        "23127pn0cc" to 5000, // 14
        "23116pn5bc" to 4880, // 14 Pro
        "24031pn0dc" to 5300, // 14 Ultra
        "24129pn74c" to 6270, // 15 series approx
        "2201123c" to 4500, // 12
        "2201122c" to 4600, // 12 Pro
        "2107113sg" to 4500, // 11T
        "21081111rg" to 5000, // 11T Pro
        "22101316g" to 5000, // Redmi Note 12
        "23076rn4bi" to 5000, // Note 12 Pro
        "23090ra98g" to 5100, // Note 13
        "23117ra68g" to 5100, // Note 13 Pro
        "24090ra29g" to 5500, // Note 14
        "m2007j3sg" to 5000, // Poco F2 Pro
        "22021211rg" to 4700, // Poco F4
        "23049pcd8g" to 5000, // Poco F5
        "2311drk48g" to 5000, // Poco F6
        "redmi note 13" to 5100,
        "redmi note 12" to 5000,
        "redmi note 14" to 5500,
        "xiaomi 14" to 4610,
        "xiaomi 13" to 4500,
        "poco f5" to 5000,
        "poco x6" to 5100,

        // ——— Vivo / iQOO / OriginOS ———
        "v2241a" to 4800,
        "v2254a" to 5000,
        "v2307a" to 5000,
        "v2324a" to 5500,
        "v2352a" to 5500,
        "i2302" to 5000, // iQOO
        "i2212" to 4700,
        "vivo v30" to 5000,
        "vivo v29" to 4600,
        "vivo y36" to 5000,
        "vivo y27" to 5000,
        "iqoo neo" to 5000,
        "iqoo 12" to 5000,

        // ——— Samsung (một số phổ biến VN) ———
        "sm-a556e" to 5000, // A55
        "sm-a546e" to 5000, // A54
        "sm-a256e" to 5000, // A25
        "sm-a156e" to 5000, // A15
        "sm-s911b" to 3900, // S23
        "sm-s916b" to 4700, // S23+
        "sm-s918b" to 5000, // S23 Ultra
        "sm-s921b" to 4000, // S24
        "sm-s926b" to 4900, // S24+
        "sm-s928b" to 5000, // S24 Ultra
        "galaxy a55" to 5000,
        "galaxy a54" to 5000,
        "galaxy s24" to 4000,

        // ——— Google Pixel ———
        "pixel 8" to 4575,
        "pixel 8 pro" to 5050,
        "pixel 9" to 4700,
        "pixel 9 pro" to 5060,
        "pixel 7" to 4355,
        "pixel 7 pro" to 5000,
    )

    fun lookup(): Match? {
        val candidates = listOf(
            Build.MODEL,
            Build.DEVICE,
            Build.PRODUCT,
            Build.HARDWARE,
            "${Build.BRAND} ${Build.MODEL}",
            "${Build.MANUFACTURER} ${Build.MODEL}"
        ).map { normalize(it) }.filter { it.isNotBlank() }

        // Exact key
        for (c in candidates) {
            CATALOG[c]?.let {
                Log.i(TAG, "Exact match $c → $it mAh")
                return Match(it, c)
            }
        }

        // Contains (MODEL chứa key hoặc ngược lại)
        for ((key, mah) in CATALOG) {
            for (c in candidates) {
                if (c.contains(key) || key.contains(c)) {
                    if (key.length >= 4 && c.length >= 4) {
                        Log.i(TAG, "Fuzzy match $c ~ $key → $mah mAh")
                        return Match(mah, "$c~$key")
                    }
                }
            }
        }

        Log.i(TAG, "No catalog match for model=${Build.MODEL} device=${Build.DEVICE}")
        return null
    }

    fun deviceLabel(): String {
        return listOf(Build.MANUFACTURER, Build.BRAND, Build.MODEL, Build.DEVICE)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" / ")
    }

    private fun normalize(s: String?): String {
        if (s.isNullOrBlank()) return ""
        return s.lowercase()
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "")
            .trim()
    }
}

package com.fcmguard.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fcmguard.app.R
import com.fcmguard.app.data.ActivityLog
import com.fcmguard.app.ui.theme.GreenSuccess
import com.fcmguard.app.ui.theme.PurplePrimary
import com.fcmguard.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateWhitelist: () -> Unit,
    onNavigateStats: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val totalSuccess by viewModel.totalSuccess.collectAsState()
    val totalFail by viewModel.totalFail.collectAsState()
    val energyUah by viewModel.energyUah.collectAsState()
    val statsPeriodStart by viewModel.statsPeriodStart.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isNls by viewModel.isNlsEnabled.collectAsState()
    val isBattery by viewModel.isIgnoringBattery.collectAsState()
    val runMode by viewModel.runMode.collectAsState()
    val lastPingTarget by viewModel.lastPingTarget.collectAsState()
    val lastPingLatency by viewModel.lastPingLatency.collectAsState()
    val serviceStartTime by viewModel.serviceStartTime.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
        if (runMode != "OFF") viewModel.startService()
        viewModel.loadLogs24h()
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val modeLabel = when (runMode) {
        "ALWAYS" -> "Luôn chạy"
        "SCREEN_OFF" -> "Chỉ tắt màn hình"
        else -> "Đã tắt"
    }
    val modeColor = when (runMode) {
        "ALWAYS" -> GreenSuccess
        "SCREEN_OFF" -> PurplePrimary
        else -> MaterialTheme.colorScheme.error
    }

    val uptimeText = formatUptime(if (serviceStartTime > 0) now - serviceStartTime else 0)
    val report = remember(energyUah, statsPeriodStart, now) { viewModel.batteryReport() }

    val threeHoursAgo = now - 3 * 3600_000L
    val recentLogs = remember(logs, now) { logs.filter { it.timestamp >= threeHoursAgo } }
    val olderGrouped = remember(logs, now) {
        val older = logs.filter { it.timestamp < threeHoursAgo }
        val cal = Calendar.getInstance()
        older.groupBy { log ->
            cal.timeInMillis = log.timestamp
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            SimpleDateFormat("dd/MM HH:00", Locale.getDefault()).format(cal.time)
        }.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "FCM fix",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge.copy(
                                brush = com.fcmguard.app.ui.theme.RadiantBrush
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Cài đặt")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = modeColor.copy(alpha = 0.14f)),
                    border = BorderStroke(1.dp, modeColor.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (runMode == "OFF") Icons.Default.Close else Icons.Default.PlayArrow,
                            null, tint = modeColor
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Chế độ", style = MaterialTheme.typography.labelMedium)
                            Text(modeLabel, fontWeight = FontWeight.Bold, color = modeColor)
                        }
                        TextButton(onClick = onNavigateSettings) { Text("Đổi") }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Notifications,
                        title = "Truy cập thông báo",
                        enabled = isNls,
                        enabledText = "Đã bật",
                        disabledText = "Chưa bật",
                        onClick = { viewModel.openNlsSettings() }
                    )
                    StatusTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        title = "Bỏ tối ưu pin",
                        enabled = isBattery,
                        enabledText = "Đã bỏ qua",
                        disabledText = "Chưa bỏ qua",
                        onClick = { viewModel.requestBatteryOpt() }
                    )
                }
            }

            item {
                Text("Thống kê hôm nay", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SquareStat(Modifier.weight(1f).aspectRatio(1.15f), "📡", "Ping OK", "$totalSuccess", "", GreenSuccess)
                    SquareStat(Modifier.weight(1f).aspectRatio(1.15f), "⚠️", "Thất bại", "$totalFail", "", MaterialTheme.colorScheme.error)
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Pin hôm nay", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "So với 100% pin máy (~${report.capacityMah.toInt()} mAh)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🪫", fontSize = 20.sp)
                                Text(String.format("%.2f", report.usedMah), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PurplePrimary)
                                Text("mAh đã dùng", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total %", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                                Text(String.format("%.3f%%", report.usedPercent), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GreenSuccess)
                                Text("của 100% pin máy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Star, null, tint = PurplePrimary)
                            Spacer(Modifier.width(10.dp))
                            Text("Mục tiêu ping", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            FilledTonalButton(onClick = { viewModel.manualPingTest() }) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Test")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            lastPingTarget.ifBlank { "1.1.1.1:53" },
                            fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = PurplePrimary
                        )
                        if (lastPingLatency >= 0) {
                            Text("Latency: ${lastPingLatency}ms", style = MaterialTheme.typography.bodySmall, color = GreenSuccess)
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Chạy: ", style = MaterialTheme.typography.bodyMedium)
                            Text(uptimeText, fontWeight = FontWeight.Bold, color = PurplePrimary)
                        }
                        Text(
                            "Pin máy ~${report.capacityMah.toInt()} mAh",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            ">_ NHẬT KÝ (3 giờ gần nhất chi tiết)",
                            color = GreenSuccess, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        if (recentLogs.isEmpty() && olderGrouped.isEmpty()) {
                            Text("Chưa có nhật ký...", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            recentLogs.take(40).forEach { LogLine(it) }
                            olderGrouped.forEach { (hourLabel, hourLogs) ->
                                HourFold(hourLabel, hourLogs)
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onNavigateWhitelist,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                    ) {
                        Icon(Icons.Default.Favorite, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Whitelist")
                    }
                    OutlinedButton(onClick = onNavigateStats, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Menu, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Thống kê")
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatusTile(
    modifier: Modifier, icon: ImageVector, title: String,
    enabled: Boolean, enabledText: String, disabledText: String, onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Card(
        onClick = onClick,
        modifier = modifier.scale(if (pressed) 0.97f else 1f).shadow(
            elevation = if (pressed) 2.dp else 8.dp,
            shape = RoundedCornerShape(16.dp),
            ambientColor = PurplePrimary.copy(alpha = 0.25f),
            spotColor = PurplePrimary.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        interactionSource = interaction
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                tint = if (enabled) GreenSuccess else PurplePrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                Text(
                    if (enabled) "✓ $enabledText" else "○ $disabledText",
                    color = if (enabled) GreenSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp, fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SquareStat(
    modifier: Modifier, emoji: String, title: String, value: String, unit: String, color: Color
) {
    Card(
        modifier = modifier.shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(20.dp),
            ambientColor = color.copy(alpha = 0.2f),
            spotColor = color.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
            if (unit.isNotEmpty()) {
                Text(unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LogLine(log: ActivityLog) {
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    val color = when (log.type) {
        "SUCCESS" -> GreenSuccess
        "FAIL" -> Color(0xFFFF6B6B)
        else -> Color(0xFF8B949E)
    }
    Text(
        "[$time] ${log.message}", color = color, fontSize = 11.sp,
        fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun HourFold(label: String, logs: List<ActivityLog>) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "▸ $label (${logs.size})", color = Color(0xFF8B949E), fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
            )
            Text(if (expanded) "▲" else "▼", color = Color.Gray, fontSize = 10.sp)
        }
        if (expanded) logs.forEach { LogLine(it) }
    }
}

private fun formatUptime(ms: Long): String {
    if (ms <= 0) return "—"
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return when {
        h > 0 -> String.format("%d giờ %02d phút", h, m)
        m > 0 -> String.format("%d phút %02d giây", m, s)
        else -> String.format("%d giây", s)
    }
}

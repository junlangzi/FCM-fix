package com.fcmguard.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fcmguard.app.data.ActivityLog
import com.fcmguard.app.ui.theme.GreenSuccess
import com.fcmguard.app.ui.theme.PurplePrimary
import com.fcmguard.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val totalSuccess by viewModel.totalSuccess.collectAsState()
    val totalFail by viewModel.totalFail.collectAsState()
    val energyUah by viewModel.energyUah.collectAsState()
    val statsPeriodStart by viewModel.statsPeriodStart.collectAsState()
    val logs by viewModel.logs.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadLogs24h() }

    val report = remember(energyUah, statsPeriodStart) { viewModel.batteryReport() }

    val now = System.currentTimeMillis()
    val threeHoursAgo = now - 3 * 3600_000L
    val recentLogs = logs.filter { it.timestamp >= threeHoursAgo }
    val olderGrouped = logs.filter { it.timestamp < threeHoursAgo }.let { older ->
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
            TopAppBar(
                title = { Text("Thống kê & Nhật ký") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearStats() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa ping (giữ pin)")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EqualCard("📡 OK", "$totalSuccess", GreenSuccess, Modifier.weight(1f))
                    EqualCard("⚠️ Fail", "$totalFail", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Pin hôm nay (so với 100% pin máy)", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(String.format("%.2f mAh", report.usedMah), fontWeight = FontWeight.Bold, color = PurplePrimary)
                                Text("Đã dùng", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(String.format("%.3f%%", report.usedPercent), fontWeight = FontWeight.Bold, color = GreenSuccess)
                                Text("của ~${report.capacityMah.toInt()} mAh", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item { Text("3 giờ gần nhất (chi tiết)", fontWeight = FontWeight.Bold) }
            if (recentLogs.isEmpty()) {
                item { Text("Không có bản ghi.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(recentLogs.size) { i ->
                    val log = recentLogs[i]
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    Text(
                        "[$time] ${log.message}",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = when (log.type) {
                            "SUCCESS" -> GreenSuccess
                            "FAIL" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            item { Text("Các giờ trước (bấm để mở)", fontWeight = FontWeight.Bold) }
            olderGrouped.forEach { (label, hourLogs) ->
                item { HourGroupCard(label, hourLogs) }
            }
        }
    }
}

@Composable
private fun EqualCard(
    title: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier
) {
    Card(
        modifier = modifier.aspectRatio(1.2f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
        }
    }
}

@Composable
private fun HourGroupCard(label: String, logs: List<ActivityLog>) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(
                    "${logs.size} sự kiện ${if (expanded) "▲" else "▼"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                logs.forEach { log ->
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    Text(
                        "[$time] ${log.message}",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = when (log.type) {
                            "SUCCESS" -> GreenSuccess
                            "FAIL" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

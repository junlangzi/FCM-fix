package com.fcmguard.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fcmguard.app.helper.OemDetector
import com.fcmguard.app.ui.theme.PurplePrimary
import com.fcmguard.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val oem = remember { OemDetector.detect() }
    val oemName = remember { OemDetector.displayName(oem) }
    val steps = remember { OemDetector.stepsFor(oem) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hướng dẫn Whitelist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PurplePrimary.copy(alpha = 0.12f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = PurplePrimary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Đã nhận diện máy", style = MaterialTheme.typography.labelMedium)
                            Text(oemName, fontWeight = FontWeight.Bold, color = PurplePrimary)
                        }
                    }
                }
            }

            item {
                Text(
                    "Làm lần lượt các bước dưới đây (đã tối ưu theo hãng của bạn):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            itemsIndexed(steps) { index, step ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = PurplePrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${index + 1}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = PurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(step.title, fontWeight = FontWeight.Medium)
                            Text(
                                step.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            when (step.action) {
                                OemDetector.GuideAction.AUTO_START -> viewModel.openAutoStart()
                                OemDetector.GuideAction.BATTERY_APP -> viewModel.requestBatteryOpt()
                                OemDetector.GuideAction.BATTERY_GMS -> viewModel.openGmsBattery()
                                OemDetector.GuideAction.NLS -> viewModel.openNlsSettings()
                                OemDetector.GuideAction.APP_DETAILS -> viewModel.openAutoStart()
                            }
                        }) {
                            Icon(Icons.Default.Star, contentDescription = "Mở", tint = PurplePrimary)
                        }
                    }
                }
            }

            item {
                Text(
                    "Sau khi xong, quay lại trang chủ và kiểm tra trạng thái xanh (NLS + Pin).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

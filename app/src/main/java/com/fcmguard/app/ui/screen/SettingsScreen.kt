package com.fcmguard.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fcmguard.app.ui.theme.PurplePrimary
import com.fcmguard.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val runMode by viewModel.runMode.collectAsState()
    val intervalOn by viewModel.intervalScreenOn.collectAsState()
    val intervalOff by viewModel.intervalScreenOff.collectAsState()
    val strong by viewModel.strongProtection.collectAsState()
    val hideNoti by viewModel.hideNotification.collectAsState()
    val autoBoot by viewModel.autoStartBoot.collectAsState()
    val adaptive by viewModel.adaptiveInterval.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val pingMode by viewModel.pingMode.collectAsState()
    val fixedHost by viewModel.fixedPingHost.collectAsState()
    val statsResetHour by viewModel.statsResetHour.collectAsState()
    val statsResetMinute by viewModel.statsResetMinute.collectAsState()
    val customHosts by viewModel.customPingHosts.collectAsState()

    var newHost by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionTitle("Chế độ chạy")
            }
            item {
                SettingsCard {
                    ModeChip("Luôn chạy", "Ping cả khi màn bật/tắt", runMode == "ALWAYS") {
                        viewModel.setRunMode("ALWAYS")
                    }
                    Spacer(Modifier.height(8.dp))
                    ModeChip("Chỉ tắt màn hình", "Tiết kiệm pin", runMode == "SCREEN_OFF") {
                        viewModel.setRunMode("SCREEN_OFF")
                    }
                    Spacer(Modifier.height(8.dp))
                    ModeChip("Tắt chương trình", "Dừng service", runMode == "OFF") {
                        viewModel.setRunMode("OFF")
                    }
                }
            }

            if (runMode == "ALWAYS") {
                item { SectionTitle("Tần suất — màn hình BẬT") }
                item {
                    SettingsCard {
                        IntervalDropdown(intervalOn) { viewModel.setIntervalScreenOn(it) }
                    }
                }
                item { SectionTitle("Tần suất — màn hình TẮT") }
                item {
                    SettingsCard {
                        IntervalDropdown(intervalOff) { viewModel.setIntervalScreenOff(it) }
                    }
                }
            } else if (runMode == "SCREEN_OFF") {
                item { SectionTitle("Tần suất khi màn tắt") }
                item {
                    SettingsCard {
                        IntervalDropdown(intervalOff) { viewModel.setIntervalScreenOff(it) }
                    }
                }
            }

            item { SectionTitle("Tùy chọn") }
            item {
                SettingsCard {
                    SwitchRow(
                        "Chu kỳ thích ứng (khi không chọn Auto)",
                        "Bật thêm khi chọn phút cố định — Auto đã tự thích ứng",
                        adaptive,
                        runMode != "OFF"
                    ) {
                        viewModel.setAdaptiveInterval(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    SwitchRow("Bảo vệ mạnh", "Rút ngắn chu kỳ khi cần", strong, runMode != "OFF") {
                        viewModel.setStrongProtection(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    SwitchRow(
                        "Ẩn thông báo / icon status bar",
                        "Foreground Service vẫn bắt buộc có notification ẩn (IMPORTANCE_MIN). Android không cho tắt hoàn toàn khi service chạy.",
                        hideNoti,
                        runMode != "OFF"
                    ) {
                        viewModel.setHideNotification(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    SwitchRow("Tự khởi động khi boot", "Chạy lại sau khi mở máy", autoBoot, true) {
                        viewModel.setAutoStartBoot(it)
                    }
                }
            }

            item { SectionTitle("Reset thống kê hàng ngày") }
            item {
                SettingsCard {
                    Text(
                        "Ping về 0 mỗi ngày; pin cũng reset theo lịch này",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var hourExpanded by remember { mutableStateOf(false) }
                        var minuteExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = hourExpanded,
                            onExpandedChange = { hourExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = String.format("%02d", statsResetHour),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Giờ") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(hourExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = hourExpanded, onDismissRequest = { hourExpanded = false }) {
                                (0..23).forEach { h ->
                                    DropdownMenuItem(
                                        text = { Text(String.format("%02d", h)) },
                                        onClick = {
                                            viewModel.setStatsResetHour(h)
                                            hourExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Text(":", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        ExposedDropdownMenuBox(
                            expanded = minuteExpanded,
                            onExpandedChange = { minuteExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = String.format("%02d", statsResetMinute),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Phút") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(minuteExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = minuteExpanded, onDismissRequest = { minuteExpanded = false }) {
                                listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55).forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(String.format("%02d", m)) },
                                        onClick = {
                                            viewModel.setStatsResetMinute(m)
                                            minuteExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Reset lúc ${String.format("%02d:%02d", statsResetHour, statsResetMinute)}",
                        color = PurplePrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item { SectionTitle("Giao diện") }
            item {
                SettingsCard {
                    listOf("SYSTEM" to "Theo hệ thống", "LIGHT" to "Sáng", "DARK" to "Tối").forEach { (mode, label) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                            Text(label)
                        }
                    }
                }
            }

            item { SectionTitle("Chế độ ping IP") }
            item {
                SettingsCard {
                    listOf(
                        "ROTATE" to "Xoay vòng IP",
                        "RANDOM" to "Ngẫu nhiên",
                        "FIXED" to "Cố định một IP"
                    ).forEach { (mode, label) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = pingMode == mode, onClick = { viewModel.setPingMode(mode) })
                            Text(label)
                        }
                    }
                }
            }

            
            
            item { SectionTitle("Danh sách IP ping") }
            item {
                SettingsCard {
                    Text(
                        "Định dạng host:port (vd: 1.1.1.1:53)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    customHosts.forEach { host ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pingMode == "FIXED") {
                                RadioButton(
                                    selected = fixedHost == host.substringBefore(":"),
                                    onClick = { viewModel.setFixedPingHost(host.substringBefore(":")) }
                                )
                            }
                            Text(host, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.removePingHost(host) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa")
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newHost,
                            onValueChange = { newHost = it },
                            label = { Text("IP:port mới") },
                            placeholder = { Text("1.1.1.1:53") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(onClick = {
                            if (newHost.isNotBlank()) {
                                viewModel.addPingHost(newHost.trim())
                                newHost = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm")
                        }
                    }
                }
            }

            item {
                Text(
                    "Xóa thống kê thủ công chỉ xóa Ping OK/Thất bại — không xóa số liệu pin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = PurplePrimary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = PurplePrimary.copy(0.15f), spotColor = PurplePrimary.copy(0.2f)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun ModeChip(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PurplePrimary.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, if (selected) PurplePrimary.copy(0.5f) else PurplePrimary.copy(0.12f))
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(6.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Dropdown: Auto | 1–10 phút | Custom (tự gõ).
 * selectedMinutes == 0 → Auto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalDropdown(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    val presets = (1..10).toList()
    val isAuto = selectedMinutes <= 0
    val isPreset = selectedMinutes in presets
    val isCustom = !isAuto && !isPreset

    var expanded by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf(if (isCustom) selectedMinutes.toString() else "") }

    val label = when {
        isAuto -> "Auto (thông minh)"
        isPreset -> "$selectedMinutes phút"
        else -> "Custom: $selectedMinutes phút"
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Chu kỳ ping") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text("Auto (thông minh)", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Theo mạng 4G/WiFi, pin, màn hình",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                onClick = {
                    onSelect(0)
                    expanded = false
                }
            )
            presets.forEach { min ->
                DropdownMenuItem(
                    text = { Text("$min phút") },
                    onClick = {
                        onSelect(min)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Custom…") },
                onClick = {
                    // Giữ expanded=false nhưng hiện ô custom bên dưới qua isCustom
                    onSelect(if (isCustom) selectedMinutes else 15)
                    expanded = false
                }
            )
        }
    }

    if (isCustom || (!isAuto && !isPreset)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = customText,
                onValueChange = { v ->
                    customText = v.filter { it.isDigit() }.take(3)
                },
                label = { Text("Số phút (1–120)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val n = customText.toIntOrNull()?.coerceIn(1, 120)
                    if (n != null) onSelect(n)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("OK")
            }
        }
    }

    if (isAuto) {
        Text(
            "Auto: 4G ~3–4 phút · WiFi ~8–15 phút · pin yếu tự nới",
            style = MaterialTheme.typography.bodySmall,
            color = PurplePrimary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun SwitchRow(
    title: String, subtitle: String, checked: Boolean, enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

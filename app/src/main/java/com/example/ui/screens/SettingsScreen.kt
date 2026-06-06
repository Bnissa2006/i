package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    lang: String,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val downloadLocation by viewModel.downloadLocation.collectAsState()
    val storageInfo = remember { viewModel.getStorageMetrics() }

    var mockNotificationsActive by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Theme Selector Block
        SettingsGroupHeader(text = Translation.getString("settings_theme", lang))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                listOf("System", "Light", "Dark").forEach { mode ->
                    val isSel = themeMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.changeThemeMode(mode) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSel,
                            onClick = { viewModel.changeThemeMode(mode) },
                            modifier = Modifier.testTag("theme_button_$mode")
                        )
                        Text(
                            text = when (mode) {
                                "Light" -> if (lang == "Arabic") "فاتح" else if (lang == "French") "Clair" else "Light"
                                "Dark" -> if (lang == "Arabic") "داكن" else if (lang == "French") "Sombre" else "Dark"
                                else -> if (lang == "Arabic") "تلقائي (الخيار الافتراضي للهاتف)" else if (lang == "French") "Système" else "System"
                            },
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }

        // Language Select Toggles Block
        SettingsGroupHeader(text = Translation.getString("settings_language", lang))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                listOf("English", "Arabic", "French").forEach { item ->
                    val isSel = lang == item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.changeLanguage(item) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSel,
                            onClick = { viewModel.changeLanguage(item) },
                            modifier = Modifier.testTag("lang_button_$item")
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = when (item) {
                                    "Arabic" -> "العربية (Arabic)"
                                    "French" -> "Français (French)"
                                    else -> "English"
                                },
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Storage details card block
        SettingsGroupHeader(text = Translation.getString("settings_storage", lang))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "${Translation.getString("settings_location", lang)}:",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = downloadLocation,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Translation.getString("app_storage_used", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = formatBytes(storageInfo.usedByAppBytes),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val freeSpaceText = formatBytes(storageInfo.systemFreeBytes)
                val totalSpaceText = formatBytes(storageInfo.systemTotalBytes)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = Translation.getString("system_free", lang), fontSize = 11.sp, color = Color.Gray)
                    Text(text = "$freeSpaceText / $totalSpaceText", fontSize = 11.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom segmented Storage space visual bar mapping
                val fractionUsed = (storageInfo.usedByAppBytes.toFloat() / storageInfo.systemTotalBytes.toFloat()).coerceAtLeast(0.01f)

                LinearProgressIndicator(
                    progress = { fractionUsed },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Notification selection configurations
        SettingsGroupHeader(text = "App Notifications Settings")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Download Completes Alert", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Notify when background downloads complete", fontSize = 11.sp, color = Color.Gray)
                }
                Switch(
                    checked = mockNotificationsActive,
                    onCheckedChange = { mockNotificationsActive = it },
                    modifier = Modifier.testTag("notification_switch_completes")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsGroupHeader(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

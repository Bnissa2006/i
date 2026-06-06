package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = viewModel.language

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = Translation.getString("title_settings", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // 1. Language Toggle Row card
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "App Language / لغة التطبيق", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = if (lang == "ar") "العربية" else "English", fontSize = 12.sp, color = Color.Gray)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.changeLanguage("en") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lang == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "English", fontSize = 11.sp, color = if (lang == "en") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = { viewModel.changeLanguage("ar") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lang == "ar") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "العربية", fontSize = 11.sp, color = if (lang == "ar") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // 2. Theme Toggle Card
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = Translation.getString("setting_theme", lang), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Enable dynamic polished dark system theme", fontSize = 12.sp, color = Color.Gray)
                    }

                    Switch(
                        checked = viewModel.isDarkMode,
                        onCheckedChange = { viewModel.toggleTheme() }
                    )
                }
            }

            // 3. Clean history option
            SettingsCard(
                onClick = {
                    viewModel.clearHistory()
                    Toast.makeText(
                        context,
                        Translation.getString("toast_history_cleared", lang),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = Translation.getString("setting_clear", lang), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Resets in-app browser local cache & history states", fontSize = 12.sp, color = Color.Gray)
                    }

                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear",
                        tint = Color.Red.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .clip(RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        content()
    }
}

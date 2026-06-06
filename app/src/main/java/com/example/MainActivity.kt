package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.MediaPlayerContainer
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Translation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable full Edge to Edge bleed drawing
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val lang by viewModel.language.collectAsState()

            // Resolve color scheme state
            val darkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                var currentSelectedTab by remember { mutableIntStateOf(0) }
                val playingVideo by viewModel.playingVideo.collectAsState()
                val playingAudio by viewModel.playingAudio.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Display bottom navigation only when player screens are not occupying full state viewport
                        if (playingVideo == null && playingAudio == null) {
                            NavigationBar(
                                modifier = Modifier.testTag("app_navigation_bar")
                            ) {
                                NavigationBarItem(
                                    selected = currentSelectedTab == 0,
                                    onClick = { currentSelectedTab = 0 },
                                    icon = { Icon(imageVector = Icons.Default.Language, contentDescription = "Browser") },
                                    label = { Text(Translation.getString("nav_browser", lang)) },
                                    modifier = Modifier.testTag("nav_btn_browser")
                                )
                                NavigationBarItem(
                                    selected = currentSelectedTab == 1,
                                    onClick = { currentSelectedTab = 1 },
                                    icon = { Icon(imageVector = Icons.Default.Download, contentDescription = "Downloads") },
                                    label = { Text(Translation.getString("nav_downloads", lang)) },
                                    modifier = Modifier.testTag("nav_btn_downloads")
                                )
                                NavigationBarItem(
                                    selected = currentSelectedTab == 2,
                                    onClick = { currentSelectedTab = 2 },
                                    icon = { Icon(imageVector = Icons.Default.Folder, contentDescription = "Files") },
                                    label = { Text(Translation.getString("nav_files", lang)) },
                                    modifier = Modifier.testTag("nav_btn_files")
                                )
                                NavigationBarItem(
                                    selected = currentSelectedTab == 3,
                                    onClick = { currentSelectedTab = 3 },
                                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text(Translation.getString("nav_settings", lang)) },
                                    modifier = Modifier.testTag("nav_btn_settings")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val modifierWithPadding = Modifier.padding(innerPadding)

                    // Overlay built-in players immediately when media is selected to play
                    if (playingVideo != null || playingAudio != null) {
                        MediaPlayerContainer(
                            viewModel = viewModel,
                            lang = lang,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Switch active screen dynamically
                        when (currentSelectedTab) {
                            0 -> BrowserScreen(
                                viewModel = viewModel,
                                lang = lang,
                                modifier = modifierWithPadding
                            )
                            1 -> DownloadsScreen(
                                viewModel = viewModel,
                                lang = lang,
                                modifier = modifierWithPadding
                            )
                            2 -> FilesScreen(
                                viewModel = viewModel,
                                lang = lang,
                                modifier = modifierWithPadding
                            )
                            3 -> SettingsScreen(
                                viewModel = viewModel,
                                lang = lang,
                                modifier = modifierWithPadding
                            )
                        }
                    }
                }
            }
        }
    }
}

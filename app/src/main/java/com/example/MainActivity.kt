package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.MainViewModel
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SnapTubeTheme
import com.example.ui.theme.Translation

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnapTubeTheme(darkTheme = viewModel.isDarkMode) {
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val lang = viewModel.language

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(modifier = Modifier.testTag("app_navigation_bar")) {
                            NavigationBarItem(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                icon = { Icon(Icons.Default.Language, contentDescription = "") },
                                label = { Text(Translation.getString("title_browser", lang)) },
                                modifier = Modifier.testTag("nav_browser_tab")
                            )
                            NavigationBarItem(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                icon = { Icon(Icons.Default.Download, contentDescription = "") },
                                label = { Text(Translation.getString("title_downloads", lang)) },
                                modifier = Modifier.testTag("nav_downloads_tab")
                            )
                            NavigationBarItem(
                                selected = selectedTabIndex == 2,
                                onClick = { selectedTabIndex = 2 },
                                icon = { Icon(Icons.Default.Folder, contentDescription = "") },
                                label = { Text(Translation.getString("title_files", lang)) },
                                modifier = Modifier.testTag("nav_files_tab")
                            )
                            NavigationBarItem(
                                selected = selectedTabIndex == 3,
                                onClick = { selectedTabIndex = 3 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "") },
                                label = { Text(Translation.getString("title_settings", lang)) },
                                modifier = Modifier.testTag("nav_settings_tab")
                            )
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)
                    when (selectedTabIndex) {
                        0 -> BrowserScreen(viewModel = viewModel, modifier = contentModifier)
                        1 -> DownloadsScreen(viewModel = viewModel, modifier = contentModifier)
                        2 -> FilesScreen(viewModel = viewModel, modifier = contentModifier)
                        3 -> SettingsScreen(viewModel = viewModel, modifier = contentModifier)
                    }
                }
            }
        }
    }
}

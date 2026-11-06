package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.repository.ThemeMode
import com.example.data.repository.ThemeRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.DocToolkitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.utils.configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val themeRepository = remember { ThemeRepository(applicationContext) }
            val themeMode by themeRepository.currentTheme.collectAsState()
            val systemInDark = isSystemInDarkTheme()

            val isDarkMode = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDark
            }

            DocToolkitTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        isDarkMode = isDarkMode,
                        onToggleTheme = { themeRepository.toggleTheme() }
                    )
                }
            }
        }
    }
}

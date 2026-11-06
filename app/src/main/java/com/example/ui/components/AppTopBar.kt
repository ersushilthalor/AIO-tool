package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.ThemeMode
import com.example.utils.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    isDarkMode: Boolean = false,
    onToggleTheme: (() -> Unit)? = null,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onNavigateHistory: (() -> Unit)? = null
) {
    val rotation by animateFloatAsState(
        targetValue = if (isDarkMode) 360f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "theme_rotation"
    )

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.stretchBounceClickable { onNavigateBack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            // Dark / Light Mode Toggle Button
            if (onToggleTheme != null) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .testTag("theme_toggle_button")
                        .stretchBounceClickable { onToggleTheme() }
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Dark/Light Mode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Language Switcher Chip (English / हिन्दी)
            FilterChip(
                selected = currentLanguage == AppLanguage.HINDI,
                onClick = onToggleLanguage,
                label = {
                    Text(
                        text = if (currentLanguage == AppLanguage.HINDI) "हिन्दी" else "English",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Change Language",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.stretchBounceClickable { onToggleLanguage() }
            )

            if (onNavigateHistory != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onNavigateHistory,
                    modifier = Modifier.stretchBounceClickable { onNavigateHistory() }
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Recent Files",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

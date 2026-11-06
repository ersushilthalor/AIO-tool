package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ToolCategory
import com.example.data.model.ToolItem
import com.example.ui.components.AppTopBar
import com.example.ui.components.Card3D
import com.example.ui.components.StaggeredAnimatedItem
import com.example.ui.components.pulseGlow
import com.example.ui.components.stretchBounceClickable
import com.example.ui.navigation.AppRoutes
import com.example.ui.theme.LocalCustomTheme
import com.example.utils.AppLanguage
import com.example.utils.StringsUtil

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToTool: (String) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val currentLang by viewModel.currentLanguage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val customTheme = LocalCustomTheme.current

    val filteredTools = viewModel.allTools.filter { tool ->
        val matchesCategory = selectedCategory == ToolCategory.ALL || tool.category == selectedCategory
        val title = if (currentLang == AppLanguage.HINDI) tool.titleHi else tool.titleEn
        val desc = if (currentLang == AppLanguage.HINDI) tool.descriptionHi else tool.descriptionEn
        val matchesQuery = searchQuery.isBlank() ||
                title.contains(searchQuery, ignoreCase = true) ||
                desc.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, StringsUtil.appTitleEn, StringsUtil.appTitleHi),
                currentLanguage = currentLang,
                onToggleLanguage = { viewModel.toggleLanguage() },
                isDarkMode = isDarkMode,
                onToggleTheme = onToggleTheme,
                canNavigateBack = false,
                onNavigateHistory = onNavigateToHistory
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar with 3D Depth
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_tools_input"),
                placeholder = {
                    Text(
                        text = StringsUtil.get(currentLang, StringsUtil.searchHintEn, StringsUtil.searchHintHi),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Category Filter Chips with Tactile Stretch Bounce
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ToolCategory.values()) { category ->
                    val isSelected = selectedCategory == category
                    val title = if (currentLang == AppLanguage.HINDI) category.hiName else category.enName
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.stretchBounceClickable { viewModel.onCategorySelected(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Grid of 3D Tool Cards with Staggered Entrance & Open-Close Stretch
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Hero Card: Gemini AI Image Agent Banner
                if (searchQuery.isBlank() && selectedCategory == ToolCategory.ALL) {
                    item(span = { GridItemSpan(2) }) {
                        StaggeredAnimatedItem(index = 0) {
                            HeroAiAgentCard(
                                currentLang = currentLang,
                                onLaunch = { onNavigateToTool(AppRoutes.AI_ASSISTANT) }
                            )
                        }
                    }
                }

                // Recent Files Quick Banner
                if (recentFiles.isNotEmpty() && searchQuery.isBlank() && selectedCategory == ToolCategory.ALL) {
                    item(span = { GridItemSpan(2) }) {
                        StaggeredAnimatedItem(index = 1) {
                            Card3D(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .stretchBounceClickable { onNavigateToHistory() },
                                elevation = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = StringsUtil.get(currentLang, StringsUtil.recentFilesEn, StringsUtil.recentFilesHi),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${recentFiles.size} ${if (currentLang == AppLanguage.HINDI) "फ़ाइलें तैयार" else "files ready to open/share"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(filteredTools) { index, tool ->
                    StaggeredAnimatedItem(index = index + 2) {
                        Animated3DToolCard(
                            tool = tool,
                            currentLang = currentLang,
                            onClick = { onNavigateToTool(tool.route) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hero Card for Gemini AI Image Agent with pulsing glow & shimmer
 */
@Composable
fun HeroAiAgentCard(
    currentLang: AppLanguage,
    onLaunch: () -> Unit
) {
    val customTheme = LocalCustomTheme.current

    Card3D(
        modifier = Modifier
            .fillMaxWidth()
            .pulseGlow(customTheme.accentGlow)
            .stretchBounceClickable { onLaunch() },
        elevation = 10.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (currentLang == AppLanguage.HINDI) "Gemini AI इमेज चैटबॉट" else "Gemini AI Image Agent",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (currentLang == AppLanguage.HINDI) "फोटो डालें और बोलें, AI खुद ऐप से बदलाव करेगा!" else "Attach photo & speak: AI automatically edits using app tools!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // AI Badge without shiny light animation
                SuggestionChip(
                    onClick = onLaunch,
                    label = { Text("Gemini 3.8", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLaunch,
                modifier = Modifier.fillMaxWidth().testTag("launch_ai_agent_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentLang == AppLanguage.HINDI) "AI इमेज चैटबॉट शुरू करें" else "Launch AI Image Chatbot",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 3D Tool Grid Card with:
 * - Stretch bounce touch animation
 * - Open/Close expandable quick-actions drawer
 * - 3D depth gradients
 */
@Composable
fun Animated3DToolCard(
    tool: ToolItem,
    currentLang: AppLanguage,
    onClick: () -> Unit
) {
    val title = if (currentLang == AppLanguage.HINDI) tool.titleHi else tool.titleEn
    val desc = if (currentLang == AppLanguage.HINDI) tool.descriptionHi else tool.descriptionEn
    var isExpanded by remember { mutableStateOf(false) }

    Card3D(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tool_card_${tool.id}"),
        isExpandable = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getToolIcon(tool.iconName),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tool.badge != null) {
                        SuggestionChip(
                            onClick = onClick,
                            label = { Text(tool.badge, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    // Open / Close toggle icon button
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp).stretchBounceClickable { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand/Collapse",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (isExpanded) 5 else 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Open/Close Expandable Content with spring animation
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .stretchBounceClickable { onClick() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (currentLang == AppLanguage.HINDI) "खोलें" else "Open Tool", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .stretchBounceClickable { onClick() },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = if (currentLang == AppLanguage.HINDI) "शुरू करें" else "Start",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

private fun getToolIcon(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "aspect_ratio" -> Icons.Default.Crop
        "picture_as_pdf" -> Icons.Default.PictureAsPdf
        "badge", "face" -> Icons.Default.Face
        "swap_horiz", "transform" -> Icons.Default.Transform
        "document_scanner" -> Icons.Default.DocumentScanner
        "text_fields" -> Icons.Default.TextFields
        "lock" -> Icons.Default.Lock
        "branding_watermark" -> Icons.Default.Image
        "draw", "edit" -> Icons.Default.Edit
        "auto_awesome" -> Icons.Default.AutoAwesome
        "history" -> Icons.Default.History
        "smart_toy" -> Icons.Default.SmartToy
        else -> Icons.Default.Build
    }
}


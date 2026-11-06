package com.example.ui.resizer

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SizeUnit
import com.example.ui.components.AppTopBar
import com.example.ui.components.BeforeAfterCard
import com.example.ui.components.FilePickerCard
import com.example.ui.components.ProcessingOverlay
import com.example.utils.AppLanguage
import com.example.utils.StorageUtils
import com.example.utils.StringsUtil
import kotlin.math.roundToInt

@Composable
fun PhotoResizerScreen(
    viewModel: ResizerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsState()
    val state by viewModel.photoState.collectAsState()

    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onPhotosSelected(uris)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (currentLang == AppLanguage.HINDI) "फोटो रीसाइज़र और कंप्रेस" else "Photo Resizer & Target KB",
                currentLanguage = currentLang,
                onToggleLanguage = {},
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // File Picker
                if (state.originalBitmaps.isEmpty()) {
                    FilePickerCard(
                        title = StringsUtil.get(currentLang, "Select Photo to Resize", "रीसाइज़ करने के लिए फोटो चुनें"),
                        subtitle = StringsUtil.get(currentLang, "JPEG, PNG, WEBP supported (Single or Batch)", "सिंगल या कई फोटो चुनें"),
                        mimeTypes = arrayOf("image/*"),
                        onFileSelected = { uri -> viewModel.onPhotosSelected(listOf(uri)) },
                        icon = Icons.Default.AddPhotoAlternate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { multiPicker.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth().testTag("batch_picker_button")
                    ) {
                        Text(StringsUtil.get(currentLang, "Or Select Multiple for Batch Resize", "या बैच रीसाइज़ के लिए कई फोटो चुनें"))
                    }
                } else {
                    // Selected info banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.originalBitmaps.size > 1) "${state.originalBitmaps.size} Photos Selected (Batch Mode)" else "1 Photo Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { multiPicker.launch(arrayOf("image/*")) }
                        ) {
                            Text("Change")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Selection Tabs
                    val modes = ResizeMode.values()
                    TabRow(
                        selectedTabIndex = state.resizeMode.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        modes.forEach { mode ->
                            val label = when (mode) {
                                ResizeMode.TARGET_SIZE -> "Target KB"
                                ResizeMode.PIXELS -> "Pixels"
                                ResizeMode.UNITS -> "cm / mm / in"
                                ResizeMode.PERCENTAGE -> "Percentage"
                            }
                            Tab(
                                selected = state.resizeMode == mode,
                                onClick = { viewModel.setResizeMode(mode) },
                                text = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Options Container based on selected tab
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (state.resizeMode) {
                                ResizeMode.TARGET_SIZE -> {
                                    Text(
                                        text = "Target File Size (e.g. for exam/job forms):",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = state.targetKb,
                                        onValueChange = { viewModel.setTargetKb(it) },
                                        label = { Text("Max Size (KB)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("target_kb_input")
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Quick presets
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("20", "50", "100", "200", "500").forEach { kb ->
                                            FilterChip(
                                                selected = state.targetKb == kb,
                                                onClick = { viewModel.setTargetKb(kb) },
                                                label = { Text("$kb KB") }
                                            )
                                        }
                                    }
                                }

                                ResizeMode.PIXELS -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = state.targetWidthPx,
                                            onValueChange = { viewModel.setPixels(it, state.targetHeightPx) },
                                            label = { Text("Width (px)") },
                                            modifier = Modifier.weight(1f).testTag("width_px_input")
                                        )
                                        IconButton(onClick = { viewModel.toggleLockAspectRatio() }) {
                                            Icon(
                                                imageVector = if (state.lockAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = "Lock Aspect Ratio",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        OutlinedTextField(
                                            value = state.targetHeightPx,
                                            onValueChange = { viewModel.setPixels(state.targetWidthPx, it) },
                                            label = { Text("Height (px)") },
                                            modifier = Modifier.weight(1f).testTag("height_px_input")
                                        )
                                    }
                                }

                                ResizeMode.UNITS -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = state.unitValueW,
                                            onValueChange = { viewModel.setUnitParams(it, state.unitValueH, state.selectedUnit, state.selectedDpi) },
                                            label = { Text("Width") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = state.unitValueH,
                                            onValueChange = { viewModel.setUnitParams(state.unitValueW, it, state.selectedUnit, state.selectedDpi) },
                                            label = { Text("Height") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            SizeUnit.values().forEach { u ->
                                                FilterChip(
                                                    selected = state.selectedUnit == u,
                                                    onClick = { viewModel.setUnitParams(state.unitValueW, state.unitValueH, u, state.selectedDpi) },
                                                    label = { Text(u.label) }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("DPI: ${state.selectedDpi}", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(72, 150, 300, 600).forEach { dpi ->
                                            FilterChip(
                                                selected = state.selectedDpi == dpi,
                                                onClick = { viewModel.setUnitParams(state.unitValueW, state.unitValueH, state.selectedUnit, dpi) },
                                                label = { Text("$dpi DPI") }
                                            )
                                        }
                                    }
                                }

                                ResizeMode.PERCENTAGE -> {
                                    Text("Resize to: ${state.percentage.roundToInt()}%", style = MaterialTheme.typography.titleSmall)
                                    Slider(
                                        value = state.percentage,
                                        onValueChange = { viewModel.setPercentage(it) },
                                        valueRange = 10f..200f,
                                        steps = 18
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(25f, 50f, 75f, 100f, 150f).forEach { pct ->
                                            FilterChip(
                                                selected = state.percentage == pct,
                                                onClick = { viewModel.setPercentage(pct) },
                                                label = { Text("${pct.roundToInt()}%") }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.processPhotoResize() },
                                modifier = Modifier.fillMaxWidth().testTag("apply_resize_button")
                            ) {
                                Text(StringsUtil.get(currentLang, "Process Resize", "रीसाइज़ लागू करें"))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Before & After comparison card
                    if (state.processedBitmaps.isNotEmpty()) {
                        val savings = if (state.originalSizeBytes > 0 && state.processedSizeBytes > 0) {
                            (((state.originalSizeBytes - state.processedSizeBytes).toFloat() / state.originalSizeBytes) * 100).roundToInt()
                        } else null

                        BeforeAfterCard(
                            originalDimensions = "${state.originalWidth} x ${state.originalHeight} px",
                            originalSize = StorageUtils.formatFileSize(state.originalSizeBytes),
                            newDimensions = "${state.processedWidth} x ${state.processedHeight} px",
                            newSize = StorageUtils.formatFileSize(state.processedSizeBytes),
                            savingsPercentage = savings
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Preview of resized photo
                        Text(
                            text = "Preview Result:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = state.processedBitmaps.first().asImageBitmap(),
                                contentDescription = "Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save & Share actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveResult { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("save_resized_button")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(StringsUtil.get(currentLang, StringsUtil.saveEn, StringsUtil.saveHi))
                            }

                            if (state.lastSavedFile != null) {
                                OutlinedButton(
                                    onClick = {
                                        StorageUtils.shareFile(
                                            context,
                                            state.lastSavedFile!!,
                                            "image/jpeg",
                                            "Resized Photo"
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("share_resized_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(StringsUtil.get(currentLang, StringsUtil.shareEn, StringsUtil.shareHi))
                                }
                            }
                        }
                    }
                }
            }

            ProcessingOverlay(
                isProcessing = state.isProcessing,
                message = state.statusMessage
            )
        }
    }
}

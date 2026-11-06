package com.example.ui.passport

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PassportPreset
import com.example.ui.components.AppTopBar
import com.example.ui.components.FilePickerCard
import com.example.ui.components.ProcessingOverlay
import com.example.utils.AppLanguage
import com.example.utils.StorageUtils
import com.example.utils.StringsUtil
import kotlin.math.roundToInt

@Composable
fun PassportPhotoScreen(
    viewModel: PassportViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsState()
    val state by viewModel.state.collectAsState()

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        bmp?.let { viewModel.onBitmapCaptured(it) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "Passport & Exam Photo Maker", "पासपोर्ट व परीक्षा फोटो मेकर"),
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
                if (state.sourceBitmap == null) {
                    FilePickerCard(
                        title = StringsUtil.get(currentLang, "Select or Take Photo", "फोटो चुनें या कैमरे से लें"),
                        subtitle = StringsUtil.get(currentLang, "Take selfie or select from gallery", "सेल्फी लें या गैलरी से चुनें"),
                        mimeTypes = arrayOf("image/*"),
                        onFileSelected = { uri -> viewModel.onImageSelected(uri) },
                        allowCamera = true,
                        onCameraCapture = { takePictureLauncher.launch(null) },
                        icon = Icons.Default.Badge
                    )
                } else {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preset: ${if (currentLang == AppLanguage.HINDI) state.selectedPreset.nameHi else state.selectedPreset.nameEn}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { takePictureLauncher.launch(null) },
                            modifier = Modifier.testTag("retake_camera_button")
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Selector Chips
                    Text(
                        text = "Country & Exam Presets:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(PassportPreset.ALL_PRESETS) { preset ->
                            val isSel = state.selectedPreset.id == preset.id
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.selectPreset(preset) },
                                label = { Text(if (currentLang == AppLanguage.HINDI) preset.nameHi else preset.nameEn) },
                                leadingIcon = if (isSel) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // AI Background Remover & Color Palette
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI Background Remover",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Switch(
                                    checked = state.isBgRemoved,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleBgRemoval(checked, state.selectedBgColor)
                                    },
                                    modifier = Modifier.testTag("bg_remover_switch")
                                )
                            }

                            if (state.isBgRemoved) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Select Background Color:", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PassportBgColor.values().filter { it != PassportBgColor.ORIGINAL }.forEach { bgCol ->
                                        FilterChip(
                                            selected = state.selectedBgColor == bgCol,
                                            onClick = { viewModel.toggleBgRemoval(true, bgCol) },
                                            label = { Text(bgCol.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Name and Date strip for exams (SSC / UPSC / RRB)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Name & Date Strip (Exam Form)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = state.enableNameDateStrip,
                                    onCheckedChange = { checked ->
                                        viewModel.setNameDateStrip(checked, state.candidateName, state.photoDate)
                                    }
                                )
                            }

                            if (state.enableNameDateStrip) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = state.candidateName,
                                    onValueChange = { viewModel.setNameDateStrip(true, it, state.photoDate) },
                                    label = { Text("Candidate Name (e.g. RAHUL SHARMA)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = state.photoDate,
                                    onValueChange = { viewModel.setNameDateStrip(true, state.candidateName, it) },
                                    label = { Text("Date of Photo (e.g. 15/10/2026)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Print Sheet Options (Single vs 4x6 / A4)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Generate Print Sheet Grid",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Switch(
                                    checked = state.isPrintSheet,
                                    onCheckedChange = { checked ->
                                        viewModel.setPrintSheet(checked, state.isSheetA4)
                                    }
                                )
                            }

                            if (state.isPrintSheet) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = !state.isSheetA4,
                                        onClick = { viewModel.setPrintSheet(true, false) },
                                        label = { Text("4x6 inch (6-8 Photos)") }
                                    )
                                    FilterChip(
                                        selected = state.isSheetA4,
                                        onClick = { viewModel.setPrintSheet(true, true) },
                                        label = { Text("A4 Sheet (30+ Photos)") }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preview Card
                    if (state.previewBitmap != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Ready Output Preview:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${state.previewBitmap!!.width} x ${state.previewBitmap!!.height} px • ${StorageUtils.formatFileSize(state.finalSizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .size(if (state.isPrintSheet) 260.dp else 190.dp, 240.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = state.previewBitmap!!.asImageBitmap(),
                                        contentDescription = "Passport Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.savePassport { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("save_passport_button")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(StringsUtil.get(currentLang, StringsUtil.saveEn, StringsUtil.saveHi))
                            }

                            if (state.finalFile != null) {
                                OutlinedButton(
                                    onClick = {
                                        StorageUtils.shareFile(
                                            context,
                                            state.finalFile!!,
                                            "image/jpeg",
                                            "Passport Photo"
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("share_passport_button")
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

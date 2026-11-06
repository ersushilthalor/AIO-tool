package com.example.ui.tools

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FileHistoryItem
import com.example.data.repository.HistoryRepository
import com.example.ui.components.AppTopBar
import com.example.ui.components.FilePickerCard
import com.example.ui.components.ProcessingOverlay
import com.example.utils.AppLanguage
import com.example.utils.BitmapUtils
import com.example.utils.MlKitUtils
import com.example.utils.StorageUtils
import com.example.utils.StringsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun BgRemoverScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedColorInt by remember { mutableStateOf(Color.WHITE) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedFile by remember { mutableStateOf<File?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            sourceBitmap = bmp
            scope.launch {
                isProcessing = true
                statusMessage = "Removing background on-device (ML Kit)..."
                val out = withContext(Dispatchers.IO) {
                    MlKitUtils.removeBackground(bmp, selectedColorInt)
                }
                processedBitmap = out
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "AI Background Remover", "एआई बैकग्राउंड रिमूवर"),
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
                if (sourceBitmap == null) {
                    FilePickerCard(
                        title = StringsUtil.get(currentLang, "Select Photo for Cutout", "बैकग्राउंड हटाने के लिए फोटो चुनें"),
                        subtitle = StringsUtil.get(currentLang, "100% on-device AI person & portrait cutout", "ऑन-डिवाइस एआई कटआउट"),
                        mimeTypes = arrayOf("image/*"),
                        onFileSelected = { uri ->
                            scope.launch {
                                isProcessing = true
                                statusMessage = "Reading photo..."
                                val bmp = withContext(Dispatchers.IO) {
                                    BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1600, 1600)
                                }
                                if (bmp != null) {
                                    sourceBitmap = bmp
                                    statusMessage = "Removing background with ML Kit..."
                                    val out = withContext(Dispatchers.IO) {
                                        MlKitUtils.removeBackground(bmp, selectedColorInt)
                                    }
                                    processedBitmap = out
                                }
                                isProcessing = false
                            }
                        },
                        allowCamera = true,
                        onCameraCapture = { cameraLauncher.launch(null) },
                        icon = Icons.Default.AutoFixHigh
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Background Cutout Result",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(onClick = { sourceBitmap = null; processedBitmap = null }) {
                            Text("Change")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Background color options
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Choose Background:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    "White" to Color.WHITE,
                                    "Light Blue" to Color.rgb(176, 212, 255),
                                    "Red" to Color.rgb(217, 4, 41),
                                    "Dark Slate" to Color.rgb(30, 41, 59),
                                    "Transparent" to Color.TRANSPARENT
                                ).forEach { (lbl, col) ->
                                    FilterChip(
                                        selected = selectedColorInt == col,
                                        onClick = {
                                            selectedColorInt = col
                                            scope.launch {
                                                isProcessing = true
                                                statusMessage = "Applying new background color..."
                                                val out = withContext(Dispatchers.IO) {
                                                    MlKitUtils.removeBackground(sourceBitmap!!, col)
                                                }
                                                processedBitmap = out
                                                isProcessing = false
                                            }
                                        },
                                        label = { Text(lbl) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (processedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                .background(androidx.compose.ui.graphics.Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = processedBitmap!!.asImageBitmap(),
                                contentDescription = "Cutout",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        statusMessage = "Saving cutout..."
                                        val isTransparent = selectedColorInt == Color.TRANSPARENT
                                        val fmt = if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                                        val ext = if (isTransparent) "png" else "jpg"
                                        val mime = if (isTransparent) "image/png" else "image/jpeg"

                                        val uri = withContext(Dispatchers.IO) {
                                            val dest = File(context.cacheDir, "cutout_${System.currentTimeMillis()}.$ext")
                                            FileOutputStream(dest).use { out ->
                                                processedBitmap!!.compress(fmt, 95, out)
                                            }
                                            savedFile = dest

                                            val saved = StorageUtils.saveBitmapToGallery(
                                                context,
                                                processedBitmap!!,
                                                "cutout_photo",
                                                fmt,
                                                95
                                            )
                                            if (saved != null) {
                                                historyRepository.addHistoryItem(
                                                    FileHistoryItem(
                                                        id = "cutout_${System.currentTimeMillis()}",
                                                        title = "Background Cutout Photo",
                                                        filePath = dest.absolutePath,
                                                        uriString = saved.toString(),
                                                        mimeType = mime,
                                                        sizeBytes = dest.length(),
                                                        formattedSize = StorageUtils.formatFileSize(dest.length()),
                                                        toolTag = "Background Remover"
                                                    )
                                                )
                                            }
                                            saved
                                        }
                                        isProcessing = false
                                        if (uri != null) {
                                            Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("save_cutout_button")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(StringsUtil.get(currentLang, StringsUtil.saveEn, StringsUtil.saveHi))
                            }

                            if (savedFile != null) {
                                OutlinedButton(
                                    onClick = {
                                        val isTransparent = selectedColorInt == Color.TRANSPARENT
                                        val mime = if (isTransparent) "image/png" else "image/jpeg"
                                        StorageUtils.shareFile(context, savedFile!!, mime, "Cutout Photo")
                                    },
                                    modifier = Modifier.weight(1f).testTag("share_cutout_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(StringsUtil.get(currentLang, StringsUtil.shareEn, StringsUtil.shareHi))
                                }
                            }
                        }
                    }
                }
            }

            ProcessingOverlay(isProcessing = isProcessing, message = statusMessage)
        }
    }
}

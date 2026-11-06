package com.example.ui.converter

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.components.BeforeAfterCard
import com.example.ui.components.FilePickerCard
import com.example.ui.components.ProcessingOverlay
import com.example.utils.AppLanguage
import com.example.utils.BitmapUtils
import com.example.utils.StorageUtils
import com.example.utils.StringsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class ImageOutputFormat(val extension: String, val mimeType: String, val format: Bitmap.CompressFormat) {
    JPG("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG),
    PNG("png", "image/png", Bitmap.CompressFormat.PNG),
    WEBP("webp", "image/webp", Bitmap.CompressFormat.WEBP),
    BMP("bmp", "image/bmp", Bitmap.CompressFormat.PNG) // Wrapped lossless
}

@Composable
fun ImageConverterScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalSize by remember { mutableStateOf(0L) }
    var selectedFormat by remember { mutableStateOf(ImageOutputFormat.JPG) }
    var quality by remember { mutableFloatStateOf(90f) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var convertedFile by remember { mutableStateOf<File?>(null) }
    var convertedSize by remember { mutableStateOf(0L) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "Image Format Converter", "इमेज फॉर्मेट कन्वर्टर"),
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
                if (originalBitmap == null) {
                    FilePickerCard(
                        title = StringsUtil.get(currentLang, "Select Image to Convert", "कन्वर्ट करने के लिए फोटो चुनें"),
                        subtitle = StringsUtil.get(currentLang, "Supports JPG, PNG, WEBP, BMP, GIF, HEIC", "सभी मुख्य इमेज फॉर्मेट समर्थित हैं"),
                        mimeTypes = arrayOf("image/*"),
                        onFileSelected = { uri ->
                            selectedUri = uri
                            originalSize = StorageUtils.getFileSizeFromUri(context, uri)
                            val bmp = BitmapUtils.decodeSampledBitmapFromUri(context, uri)
                            originalBitmap = bmp
                            convertedFile = null
                        },
                        icon = Icons.Default.Transform
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected: ${originalBitmap!!.width} x ${originalBitmap!!.height} px",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(onClick = { originalBitmap = null; convertedFile = null }) {
                            Text("Change")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Select Target Format:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ImageOutputFormat.values().forEach { fmt ->
                                    FilterChip(
                                        selected = selectedFormat == fmt,
                                        onClick = { selectedFormat = fmt },
                                        label = { Text(fmt.name) }
                                    )
                                }
                            }

                            if (selectedFormat == ImageOutputFormat.JPG || selectedFormat == ImageOutputFormat.WEBP) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Quality: ${quality.roundToInt()}%", style = MaterialTheme.typography.titleSmall)
                                Slider(
                                    value = quality,
                                    onValueChange = { quality = it },
                                    valueRange = 10f..100f,
                                    steps = 18
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        statusMessage = "Converting to ${selectedFormat.name}..."
                                        withContext(Dispatchers.IO) {
                                            val outFile = File(context.cacheDir, "converted_${System.currentTimeMillis()}.${selectedFormat.extension}")
                                            FileOutputStream(outFile).use { out ->
                                                originalBitmap!!.compress(selectedFormat.format, quality.roundToInt(), out)
                                            }
                                            if (outFile.exists()) {
                                                convertedFile = outFile
                                                convertedSize = outFile.length()
                                            }
                                        }
                                        isProcessing = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("convert_image_button")
                            ) {
                                Text(StringsUtil.get(currentLang, "Convert Image", "इमेज कन्वर्ट करें"))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (convertedFile != null && convertedSize > 0) {
                        BeforeAfterCard(
                            originalDimensions = "${originalBitmap!!.width} x ${originalBitmap!!.height} px",
                            originalSize = StorageUtils.formatFileSize(originalSize),
                            newDimensions = "${originalBitmap!!.width} x ${originalBitmap!!.height} px (${selectedFormat.name})",
                            newSize = StorageUtils.formatFileSize(convertedSize)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        statusMessage = "Saving converted image..."
                                        val uri = withContext(Dispatchers.IO) {
                                            val saved = StorageUtils.saveBitmapToGallery(
                                                context = context,
                                                bitmap = originalBitmap!!,
                                                title = "converted_img",
                                                format = selectedFormat.format,
                                                quality = quality.roundToInt()
                                            )
                                            if (saved != null) {
                                                historyRepository.addHistoryItem(
                                                    FileHistoryItem(
                                                        id = "img_${System.currentTimeMillis()}",
                                                        title = "Converted Image (${selectedFormat.name})",
                                                        filePath = convertedFile!!.absolutePath,
                                                        uriString = saved.toString(),
                                                        mimeType = selectedFormat.mimeType,
                                                        sizeBytes = convertedSize,
                                                        formattedSize = StorageUtils.formatFileSize(convertedSize),
                                                        toolTag = "Image Converter"
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
                                modifier = Modifier.weight(1f).testTag("save_converted_button")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(StringsUtil.get(currentLang, StringsUtil.saveEn, StringsUtil.saveHi))
                            }

                            OutlinedButton(
                                onClick = {
                                    StorageUtils.shareFile(
                                        context,
                                        convertedFile!!,
                                        selectedFormat.mimeType,
                                        "Converted Image"
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("share_converted_button")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(StringsUtil.get(currentLang, StringsUtil.shareEn, StringsUtil.shareHi))
                            }
                        }
                    }
                }
            }

            ProcessingOverlay(isProcessing = isProcessing, message = statusMessage)
        }
    }
}

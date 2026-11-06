package com.example.ui.tools

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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.FileHistoryItem
import com.example.data.repository.HistoryRepository
import com.example.ui.components.AppTopBar
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

@Composable
fun ExifStripperScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val exifData = remember { mutableStateMapOf<String, String>() }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var strippedFile by remember { mutableStateOf<File?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "Remove EXIF Metadata", "EXIF और मेटाडेटा हटाएं"),
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
                if (selectedUri == null) {
                    FilePickerCard(
                        title = "Select Photo to Inspect & Strip",
                        subtitle = "Wipe GPS location, camera info & device privacy tags",
                        mimeTypes = arrayOf("image/*"),
                        onFileSelected = { uri ->
                            selectedUri = uri
                            scope.launch {
                                isProcessing = true
                                statusMessage = "Reading EXIF tags..."
                                withContext(Dispatchers.IO) {
                                    sourceBitmap = BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1200, 1200)
                                    exifData.clear()
                                    try {
                                        context.contentResolver.openInputStream(uri)?.use { stream ->
                                            val exif = ExifInterface(stream)
                                            val tags = listOf(
                                                "Camera Make" to ExifInterface.TAG_MAKE,
                                                "Camera Model" to ExifInterface.TAG_MODEL,
                                                "Date & Time" to ExifInterface.TAG_DATETIME,
                                                "GPS Latitude" to ExifInterface.TAG_GPS_LATITUDE,
                                                "GPS Longitude" to ExifInterface.TAG_GPS_LONGITUDE,
                                                "Focal Length" to ExifInterface.TAG_FOCAL_LENGTH,
                                                "Exposure Time" to ExifInterface.TAG_EXPOSURE_TIME,
                                                "ISO Speed" to ExifInterface.TAG_ISO_SPEED_RATINGS,
                                                "Software" to ExifInterface.TAG_SOFTWARE
                                            )
                                            for ((lbl, tag) in tags) {
                                                val v = exif.getAttribute(tag)
                                                if (!v.isNullOrBlank()) {
                                                    exifData[lbl] = v
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                                isProcessing = false
                            }
                        },
                        icon = Icons.Default.Shield
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = StorageUtils.getFileNameFromUri(context, selectedUri!!),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(onClick = { selectedUri = null; strippedFile = null }) {
                            Text("Change")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metadata Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Detected Metadata (${exifData.size} tags):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (exifData.isEmpty()) {
                                Text(
                                    text = "No private EXIF tags detected in this file.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                exifData.forEach { (k, v) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(k, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text(v, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (sourceBitmap == null) return@Button
                            scope.launch {
                                isProcessing = true
                                statusMessage = "Removing all EXIF & device tags..."
                                val out = withContext(Dispatchers.IO) {
                                    val f = File(context.cacheDir, "clean_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(f).use { stream ->
                                        sourceBitmap!!.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                                    }
                                    f
                                }
                                strippedFile = out
                                isProcessing = false
                                Toast.makeText(context, "EXIF stripped successfully! Photo is 100% clean.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("strip_exif_button")
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Strip All EXIF Metadata")
                    }

                    if (strippedFile != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Clean Photo Ready!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "All GPS, camera, and device serial numbers removed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
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
                                        statusMessage = "Saving clean photo..."
                                        val uri = withContext(Dispatchers.IO) {
                                            val saved = StorageUtils.saveBitmapToGallery(
                                                context,
                                                sourceBitmap!!,
                                                "clean_photo",
                                                Bitmap.CompressFormat.JPEG,
                                                95
                                            )
                                            if (saved != null) {
                                                historyRepository.addHistoryItem(
                                                    FileHistoryItem(
                                                        id = "clean_${System.currentTimeMillis()}",
                                                        title = "EXIF Cleared Photo",
                                                        filePath = strippedFile!!.absolutePath,
                                                        uriString = saved.toString(),
                                                        mimeType = "image/jpeg",
                                                        sizeBytes = strippedFile!!.length(),
                                                        formattedSize = StorageUtils.formatFileSize(strippedFile!!.length()),
                                                        toolTag = "EXIF Stripper"
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(StringsUtil.get(currentLang, StringsUtil.saveEn, StringsUtil.saveHi))
                            }

                            OutlinedButton(
                                onClick = {
                                    StorageUtils.shareFile(context, strippedFile!!, "image/jpeg", "Clean Photo")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
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

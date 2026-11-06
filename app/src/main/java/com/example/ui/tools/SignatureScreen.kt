package com.example.ui.tools

import android.graphics.Bitmap
import android.graphics.Color
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
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.SignaturePad
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
fun SignatureScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDrawTab by remember { mutableStateOf(true) }
    var capturedSignature by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var savedFile by remember { mutableStateOf<File?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "Digital Signature Maker", "डिजिटल हस्ताक्षर मेकर"),
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
                TabRow(
                    selectedTabIndex = if (isDrawTab) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = isDrawTab,
                        onClick = { isDrawTab = true },
                        text = { Text("Draw Signature") }
                    )
                    Tab(
                        selected = !isDrawTab,
                        onClick = { isDrawTab = false },
                        text = { Text("Upload Existing") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isDrawTab) {
                    SignaturePad(
                        onSignatureCaptured = { bmp ->
                            capturedSignature = bmp
                            Toast.makeText(context, "Signature captured!", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    FilePickerCard(
                        title = "Upload Signature Photo",
                        subtitle = "Select an existing photo of your signature",
                        mimeTypes = arrayOf("image/*"),
                        onFileSelected = { uri ->
                            scope.launch {
                                isProcessing = true
                                statusMessage = "Loading signature..."
                                val bmp = withContext(Dispatchers.IO) {
                                    BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1200, 600)
                                }
                                capturedSignature = bmp
                                isProcessing = false
                            }
                        },
                        icon = Icons.Default.Draw
                    )
                }

                if (capturedSignature != null) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Transparent PNG Output Preview:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFFEEEEEE)), // Checkered/neutral
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = capturedSignature!!.asImageBitmap(),
                            contentDescription = "Signature",
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    statusMessage = "Saving transparent signature..."
                                    val uri = withContext(Dispatchers.IO) {
                                        val dest = File(context.cacheDir, "signature_${System.currentTimeMillis()}.png")
                                        FileOutputStream(dest).use { out ->
                                            capturedSignature!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                        savedFile = dest

                                        val saved = StorageUtils.saveBitmapToGallery(
                                            context,
                                            capturedSignature!!,
                                            "digital_signature",
                                            Bitmap.CompressFormat.PNG,
                                            100
                                        )
                                        if (saved != null) {
                                            historyRepository.addHistoryItem(
                                                FileHistoryItem(
                                                    id = "sig_${System.currentTimeMillis()}",
                                                    title = "Digital Signature (PNG)",
                                                    filePath = dest.absolutePath,
                                                    uriString = saved.toString(),
                                                    mimeType = "image/png",
                                                    sizeBytes = dest.length(),
                                                    formattedSize = StorageUtils.formatFileSize(dest.length()),
                                                    toolTag = "Signature"
                                                )
                                            )
                                        }
                                        saved
                                    }
                                    isProcessing = false
                                    if (uri != null) {
                                        Toast.makeText(context, "Saved transparent signature to Gallery!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("save_signature_button")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save PNG")
                        }

                        if (savedFile != null) {
                            OutlinedButton(
                                onClick = {
                                    StorageUtils.shareFile(context, savedFile!!, "image/png", "Digital Signature")
                                },
                                modifier = Modifier.weight(1f).testTag("share_signature_button")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }

            ProcessingOverlay(isProcessing = isProcessing, message = statusMessage)
        }
    }
}

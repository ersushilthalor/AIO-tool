package com.example.ui.resizer

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.utils.CompressionLevel
import com.example.utils.PageSizeOption
import com.example.utils.PdfUtils
import com.example.utils.StorageUtils
import com.example.utils.StringsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PdfResizerScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var originalSize by remember { mutableStateOf(0L) }
    var pageCount by remember { mutableStateOf(0) }
    var selectedLevel by remember { mutableStateOf(CompressionLevel.MEDIUM) }
    var selectedPageSize by remember { mutableStateOf(PageSizeOption.A4) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var processedFile by remember { mutableStateOf<File?>(null) }
    var processedSize by remember { mutableStateOf(0L) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "PDF Compressor & Resizer", "पीडीएफ कंप्रेसर और रीसाइज़र"),
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
                        title = StringsUtil.get(currentLang, "Select PDF Document", "पीडीएफ दस्तावेज़ चुनें"),
                        subtitle = StringsUtil.get(currentLang, "Select any PDF to compress or resize pages", "पेज आकार बदलने या कंप्रेस करने के लिए चुनें"),
                        mimeTypes = arrayOf("application/pdf"),
                        onFileSelected = { uri ->
                            selectedUri = uri
                            originalSize = StorageUtils.getFileSizeFromUri(context, uri)
                            pageCount = PdfUtils.getPdfPageCount(context, uri)
                            processedFile = null
                            processedSize = 0L
                        },
                        icon = Icons.Default.PictureAsPdf
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = StorageUtils.getFileNameFromUri(context, selectedUri!!),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$pageCount pages • ${StorageUtils.formatFileSize(originalSize)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                selectedUri = null
                                processedFile = null
                            }
                        ) {
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
                            Text(
                                text = "Compression Level:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CompressionLevel.values().forEach { level ->
                                val label = if (currentLang == AppLanguage.HINDI) level.labelHi else level.labelEn
                                FilterChip(
                                    selected = selectedLevel == level,
                                    onClick = { selectedLevel = level },
                                    label = { Text(label) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Output Page Size:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(PageSizeOption.A4, PageSizeOption.LETTER, PageSizeOption.A3).forEach { pSize ->
                                    FilterChip(
                                        selected = selectedPageSize == pSize,
                                        onClick = { selectedPageSize = pSize },
                                        label = { Text(pSize.name) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        statusMessage = "Re-rendering and compressing PDF..."
                                        withContext(Dispatchers.IO) {
                                            val outFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.pdf")
                                            val success = PdfUtils.compressPdf(
                                                context = context,
                                                inputUri = selectedUri!!,
                                                outputFile = outFile,
                                                level = selectedLevel,
                                                targetPageSize = selectedPageSize
                                            )
                                            if (success && outFile.exists()) {
                                                processedFile = outFile
                                                processedSize = outFile.length()
                                            }
                                        }
                                        isProcessing = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("compress_pdf_button")
                            ) {
                                Text(StringsUtil.get(currentLang, "Start Compression", "कंप्रेशन शुरू करें"))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (processedFile != null && processedSize > 0) {
                        val savings = if (originalSize > 0) {
                            (((originalSize - processedSize).toFloat() / originalSize) * 100).roundToInt()
                        } else 0

                        BeforeAfterCard(
                            originalDimensions = "$pageCount Pages",
                            originalSize = StorageUtils.formatFileSize(originalSize),
                            newDimensions = "$pageCount Pages (${selectedPageSize.name})",
                            newSize = StorageUtils.formatFileSize(processedSize),
                            savingsPercentage = savings
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
                                        statusMessage = "Saving PDF to Documents..."
                                        val uri = withContext(Dispatchers.IO) {
                                            val saved = StorageUtils.savePdfToDocuments(context, processedFile!!, "compressed_doc")
                                            if (saved != null) {
                                                historyRepository.addHistoryItem(
                                                    FileHistoryItem(
                                                        id = "pdf_${System.currentTimeMillis()}",
                                                        title = "Compressed PDF ($pageCount pgs)",
                                                        filePath = processedFile!!.absolutePath,
                                                        uriString = saved.toString(),
                                                        mimeType = "application/pdf",
                                                        sizeBytes = processedSize,
                                                        formattedSize = StorageUtils.formatFileSize(processedSize),
                                                        toolTag = "PDF Compressor"
                                                    )
                                                )
                                            }
                                            saved
                                        }
                                        isProcessing = false
                                        if (uri != null) {
                                            Toast.makeText(context, "Saved to Documents folder!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("save_pdf_button")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(StringsUtil.get(currentLang, StringsUtil.saveEn, StringsUtil.saveHi))
                            }

                            OutlinedButton(
                                onClick = {
                                    StorageUtils.shareFile(
                                        context,
                                        processedFile!!,
                                        "application/pdf",
                                        "Compressed PDF"
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("share_pdf_button")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(StringsUtil.get(currentLang, StringsUtil.shareEn, StringsUtil.shareHi))
                            }
                        }
                    }
                }
            }

            ProcessingOverlay(
                isProcessing = isProcessing,
                message = statusMessage
            )
        }
    }
}

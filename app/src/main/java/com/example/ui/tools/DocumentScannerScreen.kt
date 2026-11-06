package com.example.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.utils.PageSizeOption
import com.example.utils.PdfUtils
import com.example.utils.StorageUtils
import com.example.utils.StringsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class DocScanFilter(val label: String) {
    ORIGINAL("Color"),
    BW_HIGH_CONTRAST("B&W Document"),
    GRAYSCALE("Grayscale"),
    MAGIC_COLOR("Enhanced Color")
}

data class ScannedPage(val rawBitmap: Bitmap, var filteredBitmap: Bitmap, var filter: DocScanFilter = DocScanFilter.BW_HIGH_CONTRAST)

@Composable
fun DocumentScannerScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scannedPages = remember { mutableStateListOf<ScannedPage>() }
    var selectedPageIndex by remember { mutableStateOf(0) }
    var selectedFilter by remember { mutableStateOf(DocScanFilter.BW_HIGH_CONTRAST) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var generatedPdf by remember { mutableStateOf<File?>(null) }

    fun applyFilterToBitmap(bitmap: Bitmap, filter: DocScanFilter): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (filter) {
            DocScanFilter.ORIGINAL -> return bitmap
            DocScanFilter.GRAYSCALE -> {
                val cm = ColorMatrix().apply { setSaturation(0f) }
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            DocScanFilter.BW_HIGH_CONTRAST -> {
                // High contrast document B&W filter
                val cm = ColorMatrix(
                    floatArrayOf(
                        1.5f, 1.5f, 1.5f, 0f, -128f,
                        1.5f, 1.5f, 1.5f, 0f, -128f,
                        1.5f, 1.5f, 1.5f, 0f, -128f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            DocScanFilter.MAGIC_COLOR -> {
                // Magic color enhancement (slight contrast + saturation boost)
                val cm = ColorMatrix()
                cm.setSaturation(1.3f)
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 10f,
                        0f, 1.2f, 0f, 0f, 10f,
                        0f, 0f, 1.2f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrastMatrix)
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            val filtered = applyFilterToBitmap(bmp, selectedFilter)
            scannedPages.add(ScannedPage(bmp, filtered, selectedFilter))
            selectedPageIndex = scannedPages.size - 1
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "Document Scanner", "दस्तावेज़ स्कैनर"),
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
                if (scannedPages.isEmpty()) {
                    FilePickerCard(
                        title = StringsUtil.get(currentLang, "Scan Document with Camera", "कैमरे से दस्तावेज स्कैन करें"),
                        subtitle = StringsUtil.get(currentLang, "Auto filters: B&W Document, Grayscale, Color", "ऑटो फ़िल्टर: ब्लैक एंड व्हाइट, कलर"),
                        mimeTypes = arrayOf("image/*"),
                        onFileSelected = { uri ->
                            scope.launch {
                                isProcessing = true
                                statusMessage = "Importing document photo..."
                                val bmp = withContext(Dispatchers.IO) {
                                    BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1800, 1800)
                                }
                                if (bmp != null) {
                                    val filtered = applyFilterToBitmap(bmp, selectedFilter)
                                    scannedPages.add(ScannedPage(bmp, filtered, selectedFilter))
                                }
                                isProcessing = false
                            }
                        },
                        allowCamera = true,
                        onCameraCapture = { cameraLauncher.launch(null) },
                        icon = Icons.Default.DocumentScanner
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${scannedPages.size} Page(s) Scanned",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.testTag("scan_next_page_button")
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Next Page")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Filter selector for the currently viewed page
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Document Filter:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DocScanFilter.values().forEach { filter ->
                                    FilterChip(
                                        selected = selectedFilter == filter,
                                        onClick = {
                                            selectedFilter = filter
                                            if (selectedPageIndex in scannedPages.indices) {
                                                val p = scannedPages[selectedPageIndex]
                                                p.filter = filter
                                                p.filteredBitmap = applyFilterToBitmap(p.rawBitmap, filter)
                                            }
                                        },
                                        label = { Text(filter.label) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active Page Preview
                    if (selectedPageIndex in scannedPages.indices) {
                        val activePage = scannedPages[selectedPageIndex]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .background(androidx.compose.ui.graphics.Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = activePage.filteredBitmap.asImageBitmap(),
                                contentDescription = "Scanned Page",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Page Thumbnails row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(scannedPages) { idx, page ->
                            val isSel = idx == selectedPageIndex
                            Card(
                                modifier = Modifier
                                    .size(70.dp, 90.dp)
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                onClick = { selectedPageIndex = idx }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = page.filteredBitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = {
                                            scannedPages.removeAt(idx)
                                            if (selectedPageIndex >= scannedPages.size) {
                                                selectedPageIndex = (scannedPages.size - 1).coerceAtLeast(0)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp).align(Alignment.TopEnd)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = androidx.compose.ui.graphics.Color.Red)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                statusMessage = "Compiling scanned document to PDF..."
                                val out = withContext(Dispatchers.IO) {
                                    val f = File(context.cacheDir, "scanned_doc_${System.currentTimeMillis()}.pdf")
                                    val ok = PdfUtils.createPdfFromBitmaps(
                                        bitmaps = scannedPages.map { it.filteredBitmap },
                                        outputFile = f,
                                        pageSizeOption = PageSizeOption.A4,
                                        marginPt = 15
                                    )
                                    if (ok && f.exists()) f else null
                                }
                                isProcessing = false
                                if (out != null) {
                                    generatedPdf = out
                                    Toast.makeText(context, "Scanned PDF Ready! (${StorageUtils.formatFileSize(out.length())})", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_scanned_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(StringsUtil.get(currentLang, "Export as Scanned PDF", "स्कैन की गई पीडीएफ बनाएं"))
                    }

                    if (generatedPdf != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        statusMessage = "Saving PDF to Documents..."
                                        val uri = withContext(Dispatchers.IO) {
                                            val saved = StorageUtils.savePdfToDocuments(context, generatedPdf!!, "scanned_document")
                                            if (saved != null) {
                                                historyRepository.addHistoryItem(
                                                    FileHistoryItem(
                                                        id = "scan_${System.currentTimeMillis()}",
                                                        title = "Scanned PDF (${scannedPages.size} pgs)",
                                                        filePath = generatedPdf!!.absolutePath,
                                                        uriString = saved.toString(),
                                                        mimeType = "application/pdf",
                                                        sizeBytes = generatedPdf!!.length(),
                                                        formattedSize = StorageUtils.formatFileSize(generatedPdf!!.length()),
                                                        toolTag = "Scanner"
                                                    )
                                                )
                                            }
                                            saved
                                        }
                                        isProcessing = false
                                        if (uri != null) {
                                            Toast.makeText(context, "Saved to Documents!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(StringsUtil.get(currentLang, StringsUtil.saveEn, StringsUtil.saveHi))
                            }

                            OutlinedButton(
                                onClick = {
                                    StorageUtils.shareFile(context, generatedPdf!!, "application/pdf", "Scanned Document")
                                },
                                modifier = Modifier.weight(1f)
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

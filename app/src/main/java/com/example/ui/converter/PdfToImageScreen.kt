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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.utils.PdfUtils
import com.example.utils.StorageUtils
import com.example.utils.StringsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfToImageScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var previewThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var extractAll by remember { mutableStateOf(true) }
    var pageRangeText by remember { mutableStateOf("1") }
    var isPngFormat by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var extractedBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var savedFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "PDF to Images", "पीडीएफ से फोटो"),
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
                        title = StringsUtil.get(currentLang, "Select PDF to Extract Images", "फोटो निकालने के लिए पीडीएफ चुनें"),
                        subtitle = StringsUtil.get(currentLang, "Extract all pages or selected pages to JPG/PNG", "सभी या चुने हुए पेजों को इमेज बनाएं"),
                        mimeTypes = arrayOf("application/pdf"),
                        onFileSelected = { uri ->
                            selectedUri = uri
                            scope.launch {
                                isProcessing = true
                                statusMessage = "Reading PDF structure..."
                                withContext(Dispatchers.IO) {
                                    pageCount = PdfUtils.getPdfPageCount(context, uri)
                                    previewThumbnail = PdfUtils.renderPdfPageToBitmap(context, uri, 0, 1.0f)
                                }
                                isProcessing = false
                            }
                        },
                        icon = Icons.Default.Collections
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
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$pageCount Total Pages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { selectedUri = null; extractedBitmaps = emptyList() }) {
                            Text("Change")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Page Selection:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = extractAll,
                                    onClick = { extractAll = true },
                                    label = { Text("All Pages ($pageCount)") }
                                )
                                FilterChip(
                                    selected = !extractAll,
                                    onClick = { extractAll = false },
                                    label = { Text("Specific Pages") }
                                )
                            }

                            if (!extractAll) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = pageRangeText,
                                    onValueChange = { pageRangeText = it },
                                    label = { Text("Page numbers (e.g. 1, 2, 4)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Output Format:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !isPngFormat,
                                    onClick = { isPngFormat = false },
                                    label = { Text("JPG (Smaller file)") }
                                )
                                FilterChip(
                                    selected = isPngFormat,
                                    onClick = { isPngFormat = true },
                                    label = { Text("PNG (High resolution)") }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        statusMessage = "Extracting high-res images..."
                                        val bitmaps = withContext(Dispatchers.IO) {
                                            val list = mutableListOf<Bitmap>()
                                            if (extractAll) {
                                                list.addAll(PdfUtils.renderAllPagesToBitmaps(context, selectedUri!!, 200))
                                            } else {
                                                val pages = pageRangeText.split(",").mapNotNull { it.trim().toIntOrNull() }
                                                for (p in pages) {
                                                    val zeroIdx = p - 1
                                                    val bmp = PdfUtils.renderPdfPageToBitmap(context, selectedUri!!, zeroIdx, 2.0f)
                                                    if (bmp != null) list.add(bmp)
                                                }
                                            }
                                            list
                                        }
                                        extractedBitmaps = bitmaps
                                        isProcessing = false
                                        Toast.makeText(context, "${bitmaps.size} pages rendered!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("extract_pdf_pages_button")
                            ) {
                                Text(StringsUtil.get(currentLang, "Extract Images", "फोटो निकालें"))
                            }
                        }
                    }

                    if (extractedBitmaps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Extracted Pages (${extractedBitmaps.size}):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(extractedBitmaps) { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(130.dp, 180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    statusMessage = "Saving all images to Gallery..."
                                    val files = withContext(Dispatchers.IO) {
                                        val fl = mutableListOf<File>()
                                        val fmt = if (isPngFormat) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                                        val ext = if (isPngFormat) "png" else "jpg"
                                        val mime = if (isPngFormat) "image/png" else "image/jpeg"

                                        for ((idx, bmp) in extractedBitmaps.withIndex()) {
                                            val f = File(context.cacheDir, "pdf_page_${idx + 1}.$ext")
                                            FileOutputStream(f).use { out ->
                                                bmp.compress(fmt, 92, out)
                                            }
                                            StorageUtils.saveBitmapToGallery(context, bmp, "pdf_page_${idx + 1}", fmt, 92)
                                            fl.add(f)
                                        }
                                        fl
                                    }
                                    savedFiles = files
                                    isProcessing = false
                                    Toast.makeText(context, "All ${extractedBitmaps.size} images saved to Gallery!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_all_extracted_button")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save All Images to Gallery")
                        }
                    }
                }
            }

            ProcessingOverlay(isProcessing = isProcessing, message = statusMessage)
        }
    }
}

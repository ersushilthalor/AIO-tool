package com.example.ui.converter

import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.runtime.mutableIntStateOf
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

data class ImageItem(val uri: Uri, val bitmap: Bitmap, val name: String)

@Composable
fun ImageToPdfScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val images = remember { mutableStateListOf<ImageItem>() }

    var selectedPageSize by remember { mutableStateOf(PageSizeOption.A4) }
    var marginPt by remember { mutableStateOf(20) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var generatedPdf by remember { mutableStateOf<File?>(null) }

    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessing = true
                statusMessage = "Loading selected images..."
                withContext(Dispatchers.IO) {
                    for (u in uris) {
                        val bmp = BitmapUtils.decodeSampledBitmapFromUri(context, u, 1600, 1600)
                        if (bmp != null) {
                            val name = StorageUtils.getFileNameFromUri(context, u)
                            images.add(ImageItem(u, bmp, name))
                        }
                    }
                }
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "Images to PDF", "फोटो से पीडीएफ"),
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
                    .padding(16.dp)
            ) {
                if (images.isEmpty()) {
                    FilePickerCard(
                        title = StringsUtil.get(currentLang, "Select Images to Convert", "पीडीएफ के लिए फोटो चुनें"),
                        subtitle = StringsUtil.get(currentLang, "Choose one or multiple photos", "एक या अधिक फोटो चुनें और क्रम तय करें"),
                        mimeTypes = arrayOf("image/*"),
                        onFileSelected = { uri ->
                            scope.launch {
                                isProcessing = true
                                statusMessage = "Loading photo..."
                                val bmp = withContext(Dispatchers.IO) {
                                    BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1600, 1600)
                                }
                                if (bmp != null) {
                                    images.add(ImageItem(uri, bmp, StorageUtils.getFileNameFromUri(context, uri)))
                                }
                                isProcessing = false
                            }
                        },
                        icon = Icons.Default.PhotoLibrary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { multiPicker.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth().testTag("add_multiple_images_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(StringsUtil.get(currentLang, "Select Multiple Images", "कई फोटो एक साथ चुनें"))
                    }
                } else {
                    // Header controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${images.size} Photos Added",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row {
                            OutlinedButton(onClick = { multiPicker.launch(arrayOf("image/*")) }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add More")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // PDF Settings Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Page Format:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(PageSizeOption.A4, PageSizeOption.LETTER, PageSizeOption.FIT_IMAGE).forEach { opt ->
                                    FilterChip(
                                        selected = selectedPageSize == opt,
                                        onClick = { selectedPageSize = opt },
                                        label = { Text(opt.name) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Margins:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("None (0pt)" to 0, "Normal (20pt)" to 20, "Wide (40pt)" to 40).forEach { (lbl, pt) ->
                                    FilterChip(
                                        selected = marginPt == pt,
                                        onClick = { marginPt = pt },
                                        label = { Text(lbl) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reorderable list of images
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(images) { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Image(
                                        bitmap = item.bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val moved = images.removeAt(index)
                                                images.add(index - 1, moved)
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < images.size - 1) {
                                                val moved = images.removeAt(index)
                                                images.add(index + 1, moved)
                                            }
                                        },
                                        enabled = index < images.size - 1
                                    ) {
                                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                    }
                                    IconButton(
                                        onClick = { images.removeAt(index) }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                statusMessage = "Creating PDF from ${images.size} photos..."
                                val outFile = withContext(Dispatchers.IO) {
                                    val f = File(context.cacheDir, "images_doc_${System.currentTimeMillis()}.pdf")
                                    val ok = PdfUtils.createPdfFromBitmaps(
                                        bitmaps = images.map { it.bitmap },
                                        outputFile = f,
                                        pageSizeOption = selectedPageSize,
                                        marginPt = marginPt
                                    )
                                    if (ok && f.exists()) f else null
                                }
                                isProcessing = false
                                if (outFile != null) {
                                    generatedPdf = outFile
                                    Toast.makeText(context, "PDF Created (${StorageUtils.formatFileSize(outFile.length())})", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("generate_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(StringsUtil.get(currentLang, "Generate PDF", "पीडीएफ बनाएं"))
                    }

                    if (generatedPdf != null) {
                        Spacer(modifier = Modifier.height(10.dp))
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
                                            val saved = StorageUtils.savePdfToDocuments(context, generatedPdf!!, "photos_pdf")
                                            if (saved != null) {
                                                historyRepository.addHistoryItem(
                                                    FileHistoryItem(
                                                        id = "img_pdf_${System.currentTimeMillis()}",
                                                        title = "Images to PDF (${images.size} pgs)",
                                                        filePath = generatedPdf!!.absolutePath,
                                                        uriString = saved.toString(),
                                                        mimeType = "application/pdf",
                                                        sizeBytes = generatedPdf!!.length(),
                                                        formattedSize = StorageUtils.formatFileSize(generatedPdf!!.length()),
                                                        toolTag = "Images to PDF"
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
                                modifier = Modifier.weight(1f).testTag("save_image_pdf_button")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(StringsUtil.get(currentLang, StringsUtil.saveEn, StringsUtil.saveHi))
                            }

                            OutlinedButton(
                                onClick = {
                                    StorageUtils.shareFile(
                                        context,
                                        generatedPdf!!,
                                        "application/pdf",
                                        "Images PDF"
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("share_image_pdf_button")
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

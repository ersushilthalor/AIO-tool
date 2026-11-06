package com.example.ui.converter

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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

enum class OrganizerTab {
    MERGE,
    SPLIT,
    ROTATE
}

data class PdfFileItem(val uri: Uri, val name: String, val size: Long, val file: File)

@Composable
fun PdfOrganizerScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(OrganizerTab.MERGE) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var outputPdf by remember { mutableStateOf<File?>(null) }

    // Merge State
    val mergeList = remember { mutableStateListOf<PdfFileItem>() }
    val multiPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                for (u in uris) {
                    val name = StorageUtils.getFileNameFromUri(context, u)
                    val size = StorageUtils.getFileSizeFromUri(context, u)
                    val temp = StorageUtils.copyUriToTempFile(context, u, "merge_src", ".pdf")
                    mergeList.add(PdfFileItem(u, name, size, temp))
                }
            }
        }
    }

    // Split State
    var splitUri by remember { mutableStateOf<Uri?>(null) }
    var splitPageCount by remember { mutableStateOf(0) }
    var splitExtractRange by remember { mutableStateOf("1-2") }
    val splitAllResult = remember { mutableStateListOf<File>() }

    // Rotate State
    var rotateUri by remember { mutableStateOf<Uri?>(null) }
    var rotateDegrees by remember { mutableStateOf(90) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "PDF Merge, Split & Rotate", "पीडीएफ जोड़ें, काटें व घुमाएं"),
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
                // Tab Selection
                TabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == OrganizerTab.MERGE,
                        onClick = { activeTab = OrganizerTab.MERGE; outputPdf = null },
                        text = { Text("Merge") }
                    )
                    Tab(
                        selected = activeTab == OrganizerTab.SPLIT,
                        onClick = { activeTab = OrganizerTab.SPLIT; outputPdf = null },
                        text = { Text("Split / Extract") }
                    )
                    Tab(
                        selected = activeTab == OrganizerTab.ROTATE,
                        onClick = { activeTab = OrganizerTab.ROTATE; outputPdf = null },
                        text = { Text("Rotate Pages") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (activeTab) {
                    OrganizerTab.MERGE -> {
                        // Merge View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${mergeList.size} PDFs selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(onClick = { multiPdfPicker.launch(arrayOf("application/pdf")) }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add PDFs")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (mergeList.isEmpty()) {
                            FilePickerCard(
                                title = "Select PDFs to Merge",
                                subtitle = "Pick 2 or more PDF documents to combine into one",
                                mimeTypes = arrayOf("application/pdf"),
                                onFileSelected = { uri ->
                                    scope.launch(Dispatchers.IO) {
                                        val name = StorageUtils.getFileNameFromUri(context, uri)
                                        val size = StorageUtils.getFileSizeFromUri(context, uri)
                                        val temp = StorageUtils.copyUriToTempFile(context, uri, "merge_src", ".pdf")
                                        mergeList.add(PdfFileItem(uri, name, size, temp))
                                    }
                                },
                                icon = Icons.Default.Layers
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(mergeList) { idx, item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${idx + 1}.", fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                                Text(StorageUtils.formatFileSize(item.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (idx > 0) {
                                                        val m = mergeList.removeAt(idx)
                                                        mergeList.add(idx - 1, m)
                                                    }
                                                },
                                                enabled = idx > 0
                                            ) {
                                                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Up")
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (idx < mergeList.size - 1) {
                                                        val m = mergeList.removeAt(idx)
                                                        mergeList.add(idx + 1, m)
                                                    }
                                                },
                                                enabled = idx < mergeList.size - 1
                                            ) {
                                                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Down")
                                            }
                                            IconButton(onClick = { mergeList.removeAt(idx) }) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        statusMessage = "Merging ${mergeList.size} PDFs..."
                                        val out = withContext(Dispatchers.IO) {
                                            val dest = File(context.cacheDir, "merged_${System.currentTimeMillis()}.pdf")
                                            val ok = PdfUtils.mergePdfs(mergeList.map { it.file }, dest)
                                            if (ok && dest.exists()) dest else null
                                        }
                                        isProcessing = false
                                        if (out != null) {
                                            outputPdf = out
                                            Toast.makeText(context, "Merged successfully! Size: ${StorageUtils.formatFileSize(out.length())}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("merge_pdfs_button"),
                                enabled = mergeList.size >= 2
                            ) {
                                Text("Merge PDFs Now")
                            }
                        }
                    }

                    OrganizerTab.SPLIT -> {
                        // Split View
                        if (splitUri == null) {
                            FilePickerCard(
                                title = "Select PDF to Split",
                                subtitle = "Extract pages or separate document",
                                mimeTypes = arrayOf("application/pdf"),
                                onFileSelected = { uri ->
                                    splitUri = uri
                                    splitPageCount = PdfUtils.getPdfPageCount(context, uri)
                                    outputPdf = null
                                },
                                icon = Icons.Default.CallSplit
                            )
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = StorageUtils.getFileNameFromUri(context, splitUri!!),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Total Pages: $splitPageCount", style = MaterialTheme.typography.bodyMedium)

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("Extract Page Range:", fontWeight = FontWeight.SemiBold)
                                    OutlinedTextField(
                                        value = splitExtractRange,
                                        onValueChange = { splitExtractRange = it },
                                        label = { Text("e.g. 1-3 or 2-4") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isProcessing = true
                                                statusMessage = "Extracting pages..."
                                                val out = withContext(Dispatchers.IO) {
                                                    val tempSrc = StorageUtils.copyUriToTempFile(context, splitUri!!, "split_src", ".pdf")
                                                    val parts = splitExtractRange.split("-")
                                                    val start = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 1
                                                    val end = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: start
                                                    val files = PdfUtils.splitPdf(tempSrc, context.cacheDir, listOf(start..end))
                                                    files.firstOrNull()
                                                }
                                                isProcessing = false
                                                if (out != null) {
                                                    outputPdf = out
                                                    Toast.makeText(context, "Pages extracted successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("split_extract_button")
                                    ) {
                                        Text("Extract Specified Pages")
                                    }
                                }
                            }
                        }
                    }

                    OrganizerTab.ROTATE -> {
                        // Rotate View
                        if (rotateUri == null) {
                            FilePickerCard(
                                title = "Select PDF to Rotate",
                                subtitle = "Rotate all pages by 90°, 180°, or 270°",
                                mimeTypes = arrayOf("application/pdf"),
                                onFileSelected = { uri ->
                                    rotateUri = uri
                                    outputPdf = null
                                },
                                icon = Icons.Default.RotateRight
                            )
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = StorageUtils.getFileNameFromUri(context, rotateUri!!),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("Rotate clockwise by:", fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(90, 180, 270).forEach { deg ->
                                            FilterChip(
                                                selected = rotateDegrees == deg,
                                                onClick = { rotateDegrees = deg },
                                                label = { Text("$deg°") }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isProcessing = true
                                                statusMessage = "Rotating PDF pages..."
                                                val out = withContext(Dispatchers.IO) {
                                                    val tempSrc = StorageUtils.copyUriToTempFile(context, rotateUri!!, "rot_src", ".pdf")
                                                    val dest = File(context.cacheDir, "rotated_${System.currentTimeMillis()}.pdf")
                                                    val ok = PdfUtils.rotatePdfPages(tempSrc, dest, rotateDegrees)
                                                    if (ok && dest.exists()) dest else null
                                                }
                                                isProcessing = false
                                                if (out != null) {
                                                    outputPdf = out
                                                    Toast.makeText(context, "Rotated successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("rotate_pdf_button")
                                    ) {
                                        Text("Rotate and Save")
                                    }
                                }
                            }
                        }
                    }
                }

                // Output Actions
                if (outputPdf != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Result Ready: ${StorageUtils.formatFileSize(outputPdf!!.length())}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isProcessing = true
                                            statusMessage = "Saving to Documents..."
                                            val uri = withContext(Dispatchers.IO) {
                                                val saved = StorageUtils.savePdfToDocuments(context, outputPdf!!, "organized_pdf")
                                                if (saved != null) {
                                                    historyRepository.addHistoryItem(
                                                        FileHistoryItem(
                                                            id = "org_pdf_${System.currentTimeMillis()}",
                                                            title = "Organized PDF (${outputPdf!!.name})",
                                                            filePath = outputPdf!!.absolutePath,
                                                            uriString = saved.toString(),
                                                            mimeType = "application/pdf",
                                                            sizeBytes = outputPdf!!.length(),
                                                            formattedSize = StorageUtils.formatFileSize(outputPdf!!.length()),
                                                            toolTag = "PDF Organizer"
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
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save PDF")
                                }
                                OutlinedButton(
                                    onClick = {
                                        StorageUtils.shareFile(context, outputPdf!!, "application/pdf", "Organized PDF")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share")
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

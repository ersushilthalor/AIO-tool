package com.example.ui.tools

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

@Composable
fun PdfSecurityScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLockMode by remember { mutableStateOf(true) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var resultPdf by remember { mutableStateOf<File?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "PDF Lock & Unlock", "पीडीएफ लॉक और अनलॉक"),
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
                    selectedTabIndex = if (isLockMode) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = isLockMode,
                        onClick = { isLockMode = true; resultPdf = null },
                        text = { Text("Lock with Password") },
                        icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) }
                    )
                    Tab(
                        selected = !isLockMode,
                        onClick = { isLockMode = false; resultPdf = null },
                        text = { Text("Unlock / Remove Password") },
                        icon = { Icon(imageVector = Icons.Default.LockOpen, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedUri == null) {
                    FilePickerCard(
                        title = if (isLockMode) "Select PDF to Protect" else "Select Encrypted PDF",
                        subtitle = if (isLockMode) "Encrypt with AES password" else "Remove password protection",
                        mimeTypes = arrayOf("application/pdf"),
                        onFileSelected = { uri ->
                            selectedUri = uri
                            resultPdf = null
                        },
                        icon = if (isLockMode) Icons.Default.Lock else Icons.Default.LockOpen
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
                                text = StorageUtils.formatFileSize(StorageUtils.getFileSizeFromUri(context, selectedUri!!)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { selectedUri = null; resultPdf = null }) {
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
                                text = if (isLockMode) "Set Password to Lock:" else "Enter Current Password to Unlock:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("pdf_password_input"),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Password"
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (password.isBlank()) {
                                        Toast.makeText(context, "Please enter password", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    scope.launch {
                                        isProcessing = true
                                        statusMessage = if (isLockMode) "Encrypting PDF with password..." else "Decrypting PDF..."
                                        val out = withContext(Dispatchers.IO) {
                                            val src = StorageUtils.copyUriToTempFile(context, selectedUri!!, "sec_src", ".pdf")
                                            val dest = File(context.cacheDir, "${if (isLockMode) "locked" else "unlocked"}_${System.currentTimeMillis()}.pdf")
                                            val success = if (isLockMode) {
                                                PdfUtils.lockPdfWithPassword(src, dest, password)
                                            } else {
                                                PdfUtils.unlockPdf(src, dest, password)
                                            }
                                            if (success && dest.exists()) dest else null
                                        }
                                        isProcessing = false
                                        if (out != null) {
                                            resultPdf = out
                                            val msg = if (isLockMode) "PDF Locked successfully!" else "PDF Unlocked successfully!"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        } else {
                                            val err = if (isLockMode) "Encryption failed" else "Failed to unlock. Check password."
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("apply_security_button")
                            ) {
                                Text(if (isLockMode) "Lock PDF" else "Unlock PDF")
                            }
                        }
                    }

                    if (resultPdf != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isLockMode) "Protected PDF Ready!" else "Unlocked PDF Ready!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Size: ${StorageUtils.formatFileSize(resultPdf!!.length())}",
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
                                        statusMessage = "Saving to Documents..."
                                        val uri = withContext(Dispatchers.IO) {
                                            val saved = StorageUtils.savePdfToDocuments(context, resultPdf!!, if (isLockMode) "locked_doc" else "unlocked_doc")
                                            if (saved != null) {
                                                historyRepository.addHistoryItem(
                                                    FileHistoryItem(
                                                        id = "sec_pdf_${System.currentTimeMillis()}",
                                                        title = if (isLockMode) "Locked PDF" else "Unlocked PDF",
                                                        filePath = resultPdf!!.absolutePath,
                                                        uriString = saved.toString(),
                                                        mimeType = "application/pdf",
                                                        sizeBytes = resultPdf!!.length(),
                                                        formattedSize = StorageUtils.formatFileSize(resultPdf!!.length()),
                                                        toolTag = "PDF Security"
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
                                Text(StringsUtil.get(currentLang, StringsUtil.saveEn, StringsUtil.saveHi))
                            }

                            OutlinedButton(
                                onClick = {
                                    StorageUtils.shareFile(context, resultPdf!!, "application/pdf", if (isLockMode) "Locked PDF" else "Unlocked PDF")
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

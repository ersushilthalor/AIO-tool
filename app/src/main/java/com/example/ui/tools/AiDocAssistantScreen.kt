package com.example.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.Card3D
import com.example.ui.components.pulseGlow
import com.example.ui.components.stretchBounceClickable
import com.example.ui.theme.LocalCustomTheme
import com.example.utils.AppLanguage
import com.example.utils.BitmapUtils
import com.example.utils.StorageUtils
import com.example.utils.StringsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatItem(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val isUser: Boolean,
    val attachedBitmap: Bitmap? = null,
    val executionResult: AiExecutionResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun AiDocAssistantScreen(
    currentLang: AppLanguage,
    historyRepository: HistoryRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customTheme = LocalCustomTheme.current

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var selectedAttachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedAttachedSize by remember { mutableStateOf(0L) }

    val messages = remember {
        mutableStateListOf(
            ChatItem(
                text = if (currentLang == AppLanguage.HINDI)
                    "नमस्ते! मैं आपका Gemini AI पावर्ड असिस्टेंट हूँ। आप कोई भी फोटो अटैच करें और बोलें जैसे:\n• 'इसको 50 KB में कंप्रेस कर दो'\n• 'पासपोर्ट फोटो बनाओ white background के साथ'\n• 'Background हटा दो'\n• '90 डिग्री घुमा दो'\n• 'कागज़ को B&W डॉक्यूमेंट बना दो'\nमैं ऐप के टूल्स का उपयोग करके फोटो पर तुरंत बदलाव कर दूंगा!"
                else
                    "Hello! I am your Gemini AI Image & Document Assistant. Attach any photo and tell me what to do:\n• 'Compress this to 50 KB'\n• 'Make India passport photo with white bg'\n• 'Remove background'\n• 'Rotate 90 degrees'\n• 'Apply B&W document filter'\n• 'Add name & date strip'\nI will automatically apply the changes using the app's native tools!",
                isUser = false
            )
        )
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1600, 1600)
                }
                selectedAttachedBitmap = bmp
                selectedAttachedSize = StorageUtils.getFileSizeFromUri(context, uri)
            }
        }
    }

    val quickActionPrompts = listOf(
        "Make 50 KB",
        "India Passport 35x45",
        "US Visa 2x2 in",
        "Remove Background",
        "B&W Doc Filter",
        "Rotate 90°",
        "Add Name & Date Strip",
        "Print Sheet 4x6"
    )

    val geminiService = remember { com.example.data.repository.GeminiAiService() }

    fun sendPrompt(query: String) {
        if (query.isBlank() && selectedAttachedBitmap == null) return

        val attached = selectedAttachedBitmap
        val origBytes = selectedAttachedSize
        selectedAttachedBitmap = null
        selectedAttachedSize = 0L

        messages.add(
            ChatItem(
                text = query.ifBlank { "Apply automatic optimization" },
                isUser = true,
                attachedBitmap = attached
            )
        )

        inputText = ""
        isThinking = true

        scope.launch {
            if (attached != null) {
                // 1. First get real Gemini analysis or insight
                val geminiInsight = withContext(Dispatchers.IO) {
                    geminiService.analyzeImageInstruction(
                        userPrompt = query.ifBlank { "Optimize and enhance this photo" },
                        bitmap = attached
                    )
                }

                // 2. Execute real app tools using AiImageActionProcessor
                val result = withContext(Dispatchers.IO) {
                    AiImageActionProcessor.processUserInstruction(
                        context = context,
                        inputBitmap = attached,
                        userPrompt = query,
                        originalSizeBytes = origBytes
                    )
                }

                val baseExplanation = if (currentLang == AppLanguage.HINDI) result.explanationHi else result.explanationEn
                val finalExplanation = if (geminiInsight.isNotBlank()) {
                    "$geminiInsight\n\n$baseExplanation"
                } else {
                    baseExplanation
                }

                messages.add(
                    ChatItem(
                        text = finalExplanation,
                        isUser = false,
                        executionResult = result
                    )
                )
            } else {
                // REAL live Gemini 3.8 Flash model generation!
                val liveGeminiAnswer = withContext(Dispatchers.IO) {
                    geminiService.chatWithGemini(query)
                }
                messages.add(ChatItem(text = liveGeminiAnswer, isUser = false))
            }
            isThinking = false
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "Gemini AI Image Agent", "जेमिनी एआई इमेज एजेंट"),
                currentLanguage = currentLang,
                onToggleLanguage = {},
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Quick suggestions chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickActionPrompts) { prompt ->
                    FilterChip(
                        selected = false,
                        onClick = { sendPrompt(prompt) },
                        label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.stretchBounceClickable { sendPrompt(prompt) }
                    )
                }
            }

            // Message thread
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("gemini_chat_messages"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages) { msg ->
                    ChatBubbleItem(
                        item = msg,
                        currentLang = currentLang,
                        onSaveResult = { result ->
                            scope.launch {
                                val savedUri = withContext(Dispatchers.IO) {
                                    if (result.resultBitmap != null) {
                                        val uri = StorageUtils.saveBitmapToGallery(
                                            context,
                                            result.resultBitmap,
                                            "ai_modified_${System.currentTimeMillis()}",
                                            Bitmap.CompressFormat.JPEG,
                                            95
                                        )
                                        if (uri != null && result.resultFile != null) {
                                            historyRepository.addHistoryItem(
                                                FileHistoryItem(
                                                    id = "ai_${System.currentTimeMillis()}",
                                                    title = "AI Modified Photo (${result.resultWidth}x${result.resultHeight})",
                                                    filePath = result.resultFile.absolutePath,
                                                    uriString = uri.toString(),
                                                    mimeType = "image/jpeg",
                                                    sizeBytes = result.resultSizeBytes,
                                                    formattedSize = StorageUtils.formatFileSize(result.resultSizeBytes),
                                                    toolTag = "Gemini AI Agent"
                                                )
                                            )
                                        }
                                        uri
                                    } else null
                                }
                                if (savedUri != null) {
                                    Toast.makeText(context, "Saved image to Gallery!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onShareResult = { result ->
                            if (result.resultFile != null) {
                                StorageUtils.shareFile(context, result.resultFile, "image/jpeg", "AI Modified Image")
                            }
                        }
                    )
                }

                if (isThinking) {
                    item {
                        Card3D(modifier = Modifier.widthIn(max = 260.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.HINDI) "एआई बदलाव कर रहा है..." else "AI applying changes...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Attached Photo Preview Bar (if user picked an image)
            AnimatedVisibility(
                visible = selectedAttachedBitmap != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (selectedAttachedBitmap != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = selectedAttachedBitmap!!.asImageBitmap(),
                                contentDescription = "Attached",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Photo Attached (${selectedAttachedBitmap!!.width} x ${selectedAttachedBitmap!!.height} px)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Now type or tap what changes to apply!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { selectedAttachedBitmap = null; selectedAttachedSize = 0L }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }

            // Input Bar with Attachment Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach image button with 3D pulse
                IconButton(
                    onClick = { photoPicker.launch(arrayOf("image/*")) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .stretchBounceClickable { photoPicker.launch(arrayOf("image/*")) }
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach Photo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (selectedAttachedBitmap != null) "Tell AI: e.g. '50 KB karo', 'Passport banao'..." else "Ask or attach a photo...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("ai_agent_input"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { sendPrompt(inputText) },
                    enabled = inputText.isNotBlank() || selectedAttachedBitmap != null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() || selectedAttachedBitmap != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .stretchBounceClickable { sendPrompt(inputText) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() || selectedAttachedBitmap != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    item: ChatItem,
    currentLang: AppLanguage,
    onSaveResult: (AiExecutionResult) -> Unit,
    onShareResult: (AiExecutionResult) -> Unit
) {
    val isUser = item.isUser
    val customTheme = LocalCustomTheme.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(modifier = Modifier.widthIn(max = 320.dp)) {
            // Attached user photo preview
            if (item.attachedBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(160.dp, 120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .align(if (isUser) Alignment.End else Alignment.Start)
                ) {
                    Image(
                        bitmap = item.attachedBitmap.asImageBitmap(),
                        contentDescription = "Attached",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Message Bubble with 3D Depth
            Card3D(
                elevation = if (isUser) 4.dp else 6.dp,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                )
            ) {
                Column {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // If an execution result exists, render the output image, before/after card, and save buttons!
                    if (item.executionResult != null) {
                        val res = item.executionResult

                        Spacer(modifier = Modifier.height(10.dp))

                        // Comparison stats
                        if (res.resultSizeBytes > 0) {
                            BeforeAfterCard(
                                originalDimensions = "${res.originalWidth}x${res.originalHeight} px",
                                originalSize = StorageUtils.formatFileSize(res.originalSizeBytes),
                                newDimensions = "${res.resultWidth}x${res.resultHeight} px",
                                newSize = StorageUtils.formatFileSize(res.resultSizeBytes)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Result Image Preview
                        if (res.resultBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = res.resultBitmap.asImageBitmap(),
                                    contentDescription = "AI Result",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onSaveResult(res) },
                                    modifier = Modifier.weight(1f).stretchBounceClickable { onSaveResult(res) }
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(StringsUtil.get(currentLang, "Save", "सहेजें"), style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = { onShareResult(res) },
                                    modifier = Modifier.weight(1f).stretchBounceClickable { onShareResult(res) }
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(StringsUtil.get(currentLang, "Share", "शेयर"), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        // Extracted OCR text if applicable
                        if (!res.extractedText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = res.extractedText,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 6
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateKnowledgeResponse(query: String): String {
    val q = query.lowercase()
    return when {
        q.contains("compress") || q.contains("size") || q.contains("kb") || q.contains("कम") -> {
            "Please attach or pick an image using the Photo button below, and tell me the target size (e.g. '50 KB me compress kar do' or 'Make under 100 KB'). I will immediately use the app's compressor engine to optimize it!"
        }
        q.contains("passport") || q.contains("पासपोर्ट") -> {
            "Attach a portrait photo and specify your country or requirement (e.g. 'India passport photo with white background' or '2x2 US visa'). I will crop, center the face, remove background and prepare it."
        }
        q.contains("background") || q.contains("bg") || q.contains("हटा") -> {
            "Attach an image and say 'Remove background' or 'Set light blue background'. I will automatically run on-device ML segmentation and apply the change!"
        }
        q.contains("rotate") || q.contains("घुमा") -> {
            "Attach an image and say 'Rotate 90 degrees' or 'Rotate clockwise'. I will immediately rotate it for you."
        }
        q.contains("ocr") || q.contains("text") || q.contains("लिखा") -> {
            "Attach any document photo and say 'Extract text' or 'Read text'. I will extract all text on-device using ML Kit Text Recognition!"
        }
        else -> {
            "Hello! I am your AI Image Agent. Attach any photo using the Gallery button (+) below and tell me what you want to do (e.g., 'Compress to 50 KB', 'Passport photo white bg', 'Remove background', 'Rotate 90°', 'Extract text', 'Add name and date strip'). I will execute the action directly using the app's tools!"
        }
    }
}


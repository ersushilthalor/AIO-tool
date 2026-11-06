package com.example.ui.tools

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppTopBar
import com.example.utils.AppLanguage
import com.example.utils.StringsUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())

@Composable
fun AiDocAssistantScreen(
    currentLang: AppLanguage,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = if (currentLang == AppLanguage.HINDI)
                    "नमस्ते! मैं आपका स्मार्ट डॉक व फोटो असिस्टेंट हूँ। आप पासपोर्ट फोटो विनिर्देश, सरकारी परीक्षा फॉर्म नियम, पीडीएफ कंप्रेशन या दस्तावेज़ सारांश के बारे में कुछ भी पूछ सकते हैं!"
                else
                    "Hello! I am your Smart Document & Photo Assistant. Ask me about passport/visa specs, government exam form requirements (SSC, UPSC, Banking), compression tips, or document conversions!",
                isUser = false
            )
        )
    }

    val quickQuestions = listOf(
        "SSC / UPSC Exam Photo Rules",
        "US Visa (2x2 inch) Specifications",
        "India Passport 35x45 mm rules",
        "How to compress PDF under 100 KB?"
    )

    fun sendQuestion(query: String) {
        if (query.isBlank()) return
        messages.add(ChatMessage(query, isUser = true))
        inputText = ""
        isThinking = true

        scope.launch {
            delay(600) // Fluid natural response timing
            val answer = generateKnowledgeResponse(query)
            messages.add(ChatMessage(answer, isUser = false))
            isThinking = false
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = StringsUtil.get(currentLang, "Smart Doc AI Assistant", "स्मार्ट डॉक एआई सहायक"),
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
            // Quick suggestions
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickQuestions) { q ->
                    FilterChip(
                        selected = false,
                        onClick = { sendQuestion(q) },
                        label = { Text(q, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Chat Messages Thread
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("chat_messages_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }
                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about photo sizes, exam rules...") },
                    modifier = Modifier.weight(1f).testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { sendQuestion(inputText) },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("send_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

fun generateKnowledgeResponse(query: String): String {
    val q = query.lowercase()
    return when {
        q.contains("ssc") || q.contains("upsc") || q.contains("exam") -> {
            "For SSC, UPSC, and State Govt forms:\n" +
            "• Photo Dimensions: 200 x 230 pixels (approx 3.5 x 4.5 cm)\n" +
            "• File Size: Must be strictly between 20 KB and 50 KB\n" +
            "• Background: Plain light grey or white\n" +
            "• Name & Date Strip: Candidate's full name and Date of Photo (D.O.P) must be clearly printed at the bottom strip.\n" +
            "You can use our 'Passport Photo Maker' tool with the Exam Form preset to prepare this in one tap!"
        }
        q.contains("us visa") || q.contains("2x2") -> {
            "For US Visa & Passport:\n" +
            "• Dimensions: 2 x 2 inches (51 x 51 mm, 600 x 600 px at 300 DPI)\n" +
            "• Face Proportion: Head height must be between 1 inch and 1 3/8 inches (50% to 69% of image height)\n" +
            "• Background: Plain white or off-white with zero shadows\n" +
            "• Eyeglasses: Strictly prohibited since Nov 2016."
        }
        q.contains("india passport") || q.contains("35x45") -> {
            "For India Passport:\n" +
            "• Dimensions: 35 mm width x 45 mm height (413 x 531 px at 300 DPI)\n" +
            "• Background: Plain white background is mandatory\n" +
            "• Face Coverage: Head and shoulders must occupy 70% to 80% of photo frame\n" +
            "• Neutral facial expression with mouth closed and eyes looking directly into the camera."
        }
        q.contains("compress") || q.contains("pdf") -> {
            "To compress a PDF under 100KB or 200KB:\n" +
            "1. Open our 'PDF Compressor & Resizer' tool.\n" +
            "2. Select 'Recommended (Balanced)' or 'Maximum Compression'.\n" +
            "3. The tool re-renders raster elements at optimal DPI while preserving text clarity.\n" +
            "4. Check the before/after preview before saving!"
        }
        q.contains("docx") -> {
            "DOCX files are XML-based archives containing paragraphs, text styling, and media. Our 'DOCX to PDF' tool parses the XML structure directly and renders formatted paragraphs, headings, bullet lists, and embedded photos into high quality A4 PDF pages on-device."
        }
        else -> {
            "This toolkit is 100% on-device and offline. You can resize photos by exact pixels, mm, or target KB, make passport photos with AI background cutouts, scan paper documents to PDF, OCR text from images, merge/split PDFs, and stamp watermarks and signatures!"
        }
    }
}

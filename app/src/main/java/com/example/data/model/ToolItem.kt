package com.example.data.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(val enName: String, val hiName: String) {
    ALL("All Tools", "सभी टूल्स"),
    RESIZER("Resizer & Compress", "रीसाइज़र और कंप्रेस"),
    CONVERTER("Converters", "कन्वर्टर"),
    PASSPORT("ID & Passport", "पासपोर्ट और पहचान पत्र"),
    PDF_TOOLS("PDF Tools", "पीडीएफ टूल्स"),
    UTILITIES("AI & Utilities", "एआई और अन्य टूल्स")
}

data class ToolItem(
    val id: String,
    val route: String,
    val titleEn: String,
    val titleHi: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val category: ToolCategory,
    val iconName: String,
    val badge: String? = null
)

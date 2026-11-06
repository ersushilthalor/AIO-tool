package com.example.utils

enum class AppLanguage {
    ENGLISH,
    HINDI
}

object StringsUtil {
    fun get(lang: AppLanguage, en: String, hi: String): String {
        return if (lang == AppLanguage.HINDI) hi else en
    }

    // Common UI Strings
    val appTitleEn = "Doc & Photo Toolkit"
    val appTitleHi = "दस्तावेज़ और फोटो टूलकिट"

    val searchHintEn = "Search tools (e.g. compress, passport, pdf)..."
    val searchHintHi = "टूल्स खोजें (उदा. कंप्रेस, पासपोर्ट, पीडीएफ)..."

    val selectFileEn = "Select File"
    val selectFileHi = "फ़ाइल चुनें"

    val selectMultipleEn = "Select Multiple Files"
    val selectMultipleHi = "एक से अधिक फ़ाइलें चुनें"

    val processingEn = "Processing... Please wait"
    val processingHi = "प्रक्रिया जारी है... कृपया प्रतीक्षा करें"

    val saveEn = "Save to Device"
    val saveHi = "डिवाइस में सहेजें"

    val shareEn = "Share"
    val shareHi = "शेयर करें"

    val savedSuccessEn = "Saved successfully!"
    val savedSuccessHi = "सफलतापूर्वक सहेजा गया!"

    val openEn = "Open"
    val openHi = "खोलें"

    val originalSizeEn = "Original Size"
    val originalSizeHi = "मूल आकार"

    val newSizeEn = "New Size"
    val newSizeHi = "नया आकार"

    val qualityEn = "Quality"
    val qualityHi = "क्वालिटी"

    val widthEn = "Width"
    val widthHi = "चौड़ाई"

    val heightEn = "Height"
    val heightHi = "ऊंचाई"

    val lockAspectRatioEn = "Lock Aspect Ratio"
    val lockAspectRatioHi = "पहलू अनुपात लॉक करें"

    val targetSizeEn = "Target File Size"
    val targetSizeHi = "लक्षित फ़ाइल आकार"

    val targetSizeHintEn = "Enter target size in KB (e.g. 50, 100)"
    val targetSizeHintHi = "KB में लक्षित आकार दर्ज करें (उदा. 50, 100)"

    val presetEn = "Preset"
    val presetHi = "प्रीसेट"

    val customEn = "Custom"
    val customHi = "कस्टम"

    val removeBgEn = "Remove Background (AI)"
    val removeBgHi = "बैकग्राउंड हटाएं (एआई)"

    val bgColorEn = "Background Color"
    val bgColorHi = "बैकग्राउंड का रंग"

    val autoFaceCenterEn = "Auto Face Centering"
    val autoFaceCenterHi = "ऑटो चेहरा केंद्र"

    val nameDateStripEn = "Name & Date Strip"
    val nameDateStripHi = "नाम और तारीख पट्टी"

    val printSheetEn = "Print Sheet (4x6 / A4)"
    val printSheetHi = "प्रिंट शीट (4x6 / A4)"

    val recentFilesEn = "Recent Files"
    val recentFilesHi = "हाल की फ़ाइलें"

    val noRecentFilesEn = "No recent files yet. Process your first photo or document!"
    val noRecentFilesHi = "अभी तक कोई फ़ाइल नहीं है। अपनी पहली फोटो या दस्तावेज़ प्रोसेस करें!"
}

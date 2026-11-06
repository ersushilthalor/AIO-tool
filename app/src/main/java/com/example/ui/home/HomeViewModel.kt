package com.example.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.model.FileHistoryItem
import com.example.data.model.ToolCategory
import com.example.data.model.ToolItem
import com.example.data.repository.HistoryRepository
import com.example.data.repository.LanguageRepository
import com.example.utils.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.example.data.repository.ThemeMode
import com.example.data.repository.ThemeRepository

class HomeViewModel(
    val languageRepository: LanguageRepository,
    val historyRepository: HistoryRepository,
    val themeRepository: ThemeRepository
) : ViewModel() {

    val currentLanguage: StateFlow<AppLanguage> = languageRepository.currentLanguage
    val currentTheme: StateFlow<ThemeMode> = themeRepository.currentTheme
    val recentFiles: StateFlow<List<FileHistoryItem>> = historyRepository.recentFiles

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ToolCategory.ALL)
    val selectedCategory: StateFlow<ToolCategory> = _selectedCategory.asStateFlow()

    val allTools: List<ToolItem> = listOf(
        // Resizer & Compress
        ToolItem(
            id = "photo_resizer",
            route = "photo_resizer",
            titleEn = "Photo Resizer & Target KB",
            titleHi = "फोटो रीसाइज़र और लक्ष्य KB",
            descriptionEn = "Resize by px, cm, mm, inch, DPI or compress to exact file size (e.g. 50 KB)",
            descriptionHi = "पिक्सल, सेमी, मिमी या सटीक KB आकार (उदा. 50 KB) में बदलें",
            category = ToolCategory.RESIZER,
            iconName = "aspect_ratio",
            badge = "Popular"
        ),
        ToolItem(
            id = "pdf_resizer",
            route = "pdf_resizer",
            titleEn = "PDF Compressor & Resizer",
            titleHi = "पीडीएफ कंप्रेसर और रीसाइज़र",
            descriptionEn = "Compress PDF by level or target size, change page size to A4/Letter",
            descriptionHi = "पीडीएफ को कंप्रेस करें और पेज साइज बदलें",
            category = ToolCategory.RESIZER,
            iconName = "picture_as_pdf"
        ),

        // ID & Passport
        ToolItem(
            id = "passport_maker",
            route = "passport_maker",
            titleEn = "Passport & Exam Photo Maker",
            titleHi = "पासपोर्ट और परीक्षा फोटो मेकर",
            descriptionEn = "India, US, UK, Schengen, Exam forms (200x230), AI background remover & 4x6 print sheets",
            descriptionHi = "भारत, यूएस, परीक्षा फॉर्म, एआई बैकग्राउंड रिमूवर और प्रिंट शीट",
            category = ToolCategory.PASSPORT,
            iconName = "badge",
            badge = "AI Powered"
        ),
        ToolItem(
            id = "bg_remover",
            route = "bg_remover",
            titleEn = "AI Background Remover",
            titleHi = "एआई बैकग्राउंड रिमूवर",
            descriptionEn = "100% on-device cutout, replace with white/blue/custom color",
            descriptionHi = "ऑन-डिवाइस बैकग्राउंड हटाएं और नया रंग भरें",
            category = ToolCategory.PASSPORT,
            iconName = "auto_fix_high"
        ),

        // Converters
        ToolItem(
            id = "image_converter",
            route = "image_converter",
            titleEn = "Image Format Converter",
            titleHi = "इमेज फॉर्मेट कन्वर्टर",
            descriptionEn = "Convert between JPG, PNG, WEBP, BMP, GIF (HEIC input)",
            descriptionHi = "JPG, PNG, WEBP, BMP आदि में बदलें",
            category = ToolCategory.CONVERTER,
            iconName = "transform"
        ),
        ToolItem(
            id = "image_to_pdf",
            route = "image_to_pdf",
            titleEn = "Images to PDF",
            titleHi = "फोटो से पीडीएफ",
            descriptionEn = "Combine multiple photos into single PDF with reorder & margins",
            descriptionHi = "कई फोटो को एक पीडीएफ में जोड़ें",
            category = ToolCategory.CONVERTER,
            iconName = "photo_library"
        ),
        ToolItem(
            id = "pdf_to_image",
            route = "pdf_to_image",
            titleEn = "PDF to Images",
            titleHi = "पीडीएफ से फोटो",
            descriptionEn = "Extract all or selected PDF pages into high-res JPG or PNG",
            descriptionHi = "पीडीएफ पेजों को JPG/PNG इमेज में बदलें",
            category = ToolCategory.CONVERTER,
            iconName = "collections"
        ),
        ToolItem(
            id = "doc_to_pdf",
            route = "doc_to_pdf",
            titleEn = "DOCX & TXT to PDF",
            titleHi = "DOCX और TXT से पीडीएफ",
            descriptionEn = "Convert Word documents and text files into formatted PDF",
            descriptionHi = "वर्ड और टेक्स्ट फाइलों को पीडीएफ में बदलें",
            category = ToolCategory.CONVERTER,
            iconName = "article"
        ),

        // PDF Tools
        ToolItem(
            id = "pdf_organizer",
            route = "pdf_organizer",
            titleEn = "PDF Merge, Split & Rotate",
            titleHi = "पीडीएफ जोड़ें, काटें और घुमाएं",
            descriptionEn = "Merge multiple PDFs, split pages, extract ranges or rotate 90°",
            descriptionHi = "पीडीएफ को मर्ज करें, पेज अलग करें या घुमाएं",
            category = ToolCategory.PDF_TOOLS,
            iconName = "layers"
        ),
        ToolItem(
            id = "pdf_security",
            route = "pdf_security",
            titleEn = "PDF Lock & Unlock",
            titleHi = "पीडीएफ लॉक और अनलॉक",
            descriptionEn = "Add or remove password protection securely on device",
            descriptionHi = "पासवर्ड सुरक्षा जोड़ें या हटाएं",
            category = ToolCategory.PDF_TOOLS,
            iconName = "lock"
        ),
        ToolItem(
            id = "pdf_watermark",
            route = "pdf_watermark",
            titleEn = "Watermark & Page Numbers",
            titleHi = "वॉटरमार्क और पेज नंबर",
            descriptionEn = "Stamp custom text watermark and page numbers to all PDF pages",
            descriptionHi = "पीडीएफ में अपना नाम या पेज नंबर जोड़ें",
            category = ToolCategory.PDF_TOOLS,
            iconName = "branding_watermark"
        ),

        // Utilities
        ToolItem(
            id = "doc_scanner",
            route = "doc_scanner",
            titleEn = "Document Scanner",
            titleHi = "दस्तावेज़ स्कैनर",
            descriptionEn = "Scan paper with camera, crop, apply B&W/color filters, export PDF",
            descriptionHi = "कैमरे से स्कैन करें, फ़िल्टर लगाएं और पीडीएफ बनाएं",
            category = ToolCategory.UTILITIES,
            iconName = "document_scanner"
        ),
        ToolItem(
            id = "ocr_text",
            route = "ocr_text",
            titleEn = "Image to Text (OCR)",
            titleHi = "फोटो से टेक्स्ट (ओसीआर)",
            descriptionEn = "Extract text from photos and documents with one-tap copy/share",
            descriptionHi = "फोटो से लिखा हुआ टेक्स्ट निकालें",
            category = ToolCategory.UTILITIES,
            iconName = "text_fields"
        ),
        ToolItem(
            id = "signature",
            route = "signature",
            titleEn = "Digital Signature Maker",
            titleHi = "डिजिटल हस्ताक्षर मेकर",
            descriptionEn = "Draw signature, export transparent PNG or stamp on files",
            descriptionHi = "हस्ताक्षर बनाएं और पारदर्शी PNG में सहेजें",
            category = ToolCategory.UTILITIES,
            iconName = "draw"
        ),
        ToolItem(
            id = "exif_stripper",
            route = "exif_stripper",
            titleEn = "Remove EXIF & Privacy Metadata",
            titleHi = "EXIF और मेटाडेटा हटाएं",
            descriptionEn = "Wipe GPS location, camera info & device tags from photos",
            descriptionHi = "फोटो से जीपीएस और कैमरा विवरण हटाएं",
            category = ToolCategory.UTILITIES,
            iconName = "shield"
        ),
        ToolItem(
            id = "ai_assistant",
            route = "ai_assistant",
            titleEn = "Smart Doc AI Assistant",
            titleHi = "स्मार्ट डॉक एआई सहायक",
            descriptionEn = "Ask requirements, exam photo rules, summarize docs with Gemini",
            descriptionHi = "परीक्षा नियम, फॉर्म साइज व दस्तावेज़ सारांश पूछें",
            category = ToolCategory.UTILITIES,
            iconName = "psychology",
            badge = "AI"
        )
    )

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun onCategorySelected(cat: ToolCategory) {
        _selectedCategory.value = cat
    }

    fun toggleLanguage() {
        languageRepository.toggleLanguage()
    }

    fun toggleTheme() {
        themeRepository.toggleTheme()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                LanguageRepository(context.applicationContext),
                HistoryRepository(context.applicationContext),
                ThemeRepository(context.applicationContext)
            ) as T
        }
    }
}

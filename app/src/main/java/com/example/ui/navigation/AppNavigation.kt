package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.HistoryRepository
import com.example.data.repository.LanguageRepository
import com.example.data.repository.ThemeMode
import com.example.data.repository.ThemeRepository
import com.example.ui.converter.DocToPdfScreen
import com.example.ui.converter.ImageConverterScreen
import com.example.ui.converter.ImageToPdfScreen
import com.example.ui.converter.PdfOrganizerScreen
import com.example.ui.converter.PdfToImageScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.passport.PassportPhotoScreen
import com.example.ui.passport.PassportViewModel
import com.example.ui.resizer.PdfResizerScreen
import com.example.ui.resizer.PhotoResizerScreen
import com.example.ui.resizer.ResizerViewModel
import com.example.ui.tools.AiDocAssistantScreen
import com.example.ui.tools.BgRemoverScreen
import com.example.ui.tools.DocumentScannerScreen
import com.example.ui.tools.ExifStripperScreen
import com.example.ui.tools.HistoryScreen
import com.example.ui.tools.OcrTextScreen
import com.example.ui.tools.PdfSecurityScreen
import com.example.ui.tools.PdfWatermarkScreen
import com.example.ui.tools.SignatureScreen

object AppRoutes {
    const val HOME = "home"
    const val PHOTO_RESIZER = "photo_resizer"
    const val PDF_RESIZER = "pdf_resizer"
    const val IMAGE_CONVERTER = "image_converter"
    const val IMAGE_TO_PDF = "image_to_pdf"
    const val PDF_TO_IMAGE = "pdf_to_image"
    const val DOC_TO_PDF = "doc_to_pdf"
    const val PDF_ORGANIZER = "pdf_organizer"
    const val PASSPORT_MAKER = "passport_maker"
    const val BG_REMOVER = "bg_remover"
    const val DOC_SCANNER = "doc_scanner"
    const val OCR_TEXT = "ocr_text"
    const val PDF_SECURITY = "pdf_security"
    const val PDF_WATERMARK = "pdf_watermark"
    const val SIGNATURE = "signature"
    const val EXIF_STRIPPER = "exif_stripper"
    const val AI_ASSISTANT = "ai_assistant"
    const val HISTORY = "history"
}

@Composable
fun AppNavigation(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(context))
    val resizerViewModel: ResizerViewModel = viewModel(factory = ResizerViewModel.Factory(context))
    val passportViewModel: PassportViewModel = viewModel(factory = PassportViewModel.Factory(context))
    val languageRepository = homeViewModel.languageRepository
    val historyRepository = homeViewModel.historyRepository
    val currentLang by languageRepository.currentLanguage.collectAsState()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME
    ) {
        composable(AppRoutes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                isDarkMode = isDarkMode,
                onToggleTheme = onToggleTheme,
                onNavigateToTool = { route -> navController.navigate(route) },
                onNavigateToHistory = { navController.navigate(AppRoutes.HISTORY) }
            )
        }

        composable(AppRoutes.PHOTO_RESIZER) {
            PhotoResizerScreen(
                viewModel = resizerViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PDF_RESIZER) {
            PdfResizerScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.IMAGE_CONVERTER) {
            ImageConverterScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.IMAGE_TO_PDF) {
            ImageToPdfScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PDF_TO_IMAGE) {
            PdfToImageScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.DOC_TO_PDF) {
            DocToPdfScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PDF_ORGANIZER) {
            PdfOrganizerScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PASSPORT_MAKER) {
            PassportPhotoScreen(
                viewModel = passportViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.BG_REMOVER) {
            BgRemoverScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.DOC_SCANNER) {
            DocumentScannerScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.OCR_TEXT) {
            OcrTextScreen(
                currentLang = currentLang,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PDF_SECURITY) {
            PdfSecurityScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PDF_WATERMARK) {
            PdfWatermarkScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.SIGNATURE) {
            SignatureScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.EXIF_STRIPPER) {
            ExifStripperScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.AI_ASSISTANT) {
            AiDocAssistantScreen(
                currentLang = currentLang,
                historyRepository = historyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.HISTORY) {
            HistoryScreen(
                historyRepository = historyRepository,
                currentLang = currentLang,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

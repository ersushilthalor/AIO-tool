package com.example.ui.passport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.FileHistoryItem
import com.example.data.model.PassportPreset
import com.example.data.repository.HistoryRepository
import com.example.data.repository.LanguageRepository
import com.example.utils.AppLanguage
import com.example.utils.BitmapUtils
import com.example.utils.MlKitUtils
import com.example.utils.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class PassportBgColor(val label: String, val colorInt: Int) {
    WHITE("White", Color.WHITE),
    LIGHT_BLUE("Light Blue", Color.rgb(176, 212, 255)),
    RED("Red", Color.rgb(217, 4, 41)),
    LIGHT_GREY("Light Grey", Color.rgb(235, 235, 235)),
    ORIGINAL("Original", Color.TRANSPARENT)
}

data class PassportState(
    val sourceBitmap: Bitmap? = null,
    val selectedPreset: PassportPreset = PassportPreset.ALL_PRESETS.first(),
    val isAutoFaceCentered: Boolean = true,
    val isBgRemoved: Boolean = false,
    val selectedBgColor: PassportBgColor = PassportBgColor.WHITE,

    // Image Adjustments
    val brightness: Float = 0f, // -50 to 50
    val contrast: Float = 1.0f,  // 0.8 to 1.3

    // Name & Date Strip (Exam forms)
    val enableNameDateStrip: Boolean = false,
    val candidateName: String = "",
    val photoDate: String = "",

    // Target KB
    val enforceMaxKb: Boolean = false,
    val targetKb: Int = 50,

    // Sheet output
    val isPrintSheet: Boolean = false,
    val isSheetA4: Boolean = false, // false = 4x6 inch, true = A4

    // Result
    val previewBitmap: Bitmap? = null,
    val finalFile: File? = null,
    val finalSizeBytes: Long = 0L,
    val isProcessing: Boolean = false,
    val statusMessage: String = ""
)

class PassportViewModel(
    val context: Context,
    val languageRepository: LanguageRepository,
    val historyRepository: HistoryRepository
) : ViewModel() {

    val currentLanguage: StateFlow<AppLanguage> = languageRepository.currentLanguage

    private val _state = MutableStateFlow(PassportState())
    val state: StateFlow<PassportState> = _state.asStateFlow()

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessing = true, statusMessage = "Loading photo safely...")
            withContext(Dispatchers.IO) {
                val bmp = BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1800, 1800)
                if (bmp != null) {
                    _state.value = _state.value.copy(
                        sourceBitmap = bmp,
                        isProcessing = false
                    )
                    generatePassportPreview()
                } else {
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        statusMessage = "Could not decode image"
                    )
                }
            }
        }
    }

    fun onBitmapCaptured(bitmap: Bitmap) {
        _state.value = _state.value.copy(sourceBitmap = bitmap)
        generatePassportPreview()
    }

    fun selectPreset(preset: PassportPreset) {
        _state.value = _state.value.copy(
            selectedPreset = preset,
            targetKb = preset.maxKb ?: _state.value.targetKb,
            enforceMaxKb = preset.maxKb != null
        )
        generatePassportPreview()
    }

    fun toggleBgRemoval(enable: Boolean, color: PassportBgColor) {
        _state.value = _state.value.copy(isBgRemoved = enable, selectedBgColor = color)
        generatePassportPreview()
    }

    fun setAdjustments(brightness: Float, contrast: Float) {
        _state.value = _state.value.copy(brightness = brightness, contrast = contrast)
        generatePassportPreview()
    }

    fun setNameDateStrip(enable: Boolean, name: String, date: String) {
        _state.value = _state.value.copy(enableNameDateStrip = enable, candidateName = name, photoDate = date)
        generatePassportPreview()
    }

    fun setPrintSheet(enable: Boolean, isA4: Boolean) {
        _state.value = _state.value.copy(isPrintSheet = enable, isSheetA4 = isA4)
        generatePassportPreview()
    }

    fun setTargetKb(enable: Boolean, kb: Int) {
        _state.value = _state.value.copy(enforceMaxKb = enable, targetKb = kb)
        generatePassportPreview()
    }

    fun generatePassportPreview() {
        val s = _state.value
        val src = s.sourceBitmap ?: return

        viewModelScope.launch {
            _state.value = s.copy(isProcessing = true, statusMessage = "Preparing passport photo...")
            withContext(Dispatchers.IO) {
                var current = src

                // 1. Face detection auto-cropping
                if (s.isAutoFaceCentered) {
                    val faces = MlKitUtils.detectFaces(current)
                    if (faces.isNotEmpty()) {
                        val targetRatio = s.selectedPreset.widthPx.toFloat() / s.selectedPreset.heightPx.toFloat()
                        val cropRect = MlKitUtils.calculatePassportCropRect(current, faces.first(), targetRatio)
                        current = Bitmap.createBitmap(current, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
                    }
                }

                // 2. Background removal & color
                if (s.isBgRemoved && s.selectedBgColor != PassportBgColor.ORIGINAL) {
                    current = MlKitUtils.removeBackground(current, s.selectedBgColor.colorInt)
                }

                // 3. Brightness & contrast
                if (s.brightness != 0f || s.contrast != 1.0f) {
                    current = BitmapUtils.adjustImage(current, s.brightness, s.contrast)
                }

                // 4. Resize to target preset pixels
                current = Bitmap.createScaledBitmap(current, s.selectedPreset.widthPx, s.selectedPreset.heightPx, true)

                // 5. Name and Date strip if requested
                if (s.enableNameDateStrip && (s.candidateName.isNotBlank() || s.photoDate.isNotBlank())) {
                    current = BitmapUtils.addNameAndDateStrip(current, s.candidateName, s.photoDate)
                }

                // 6. Print sheet if requested
                if (s.isPrintSheet) {
                    current = BitmapUtils.generatePrintSheet(
                        photo = current,
                        isSheetA4 = s.isSheetA4,
                        marginMm = 6f,
                        gapMm = 3f,
                        drawCutLines = true
                    )
                }

                // 7. File size compression
                val targetBytes = if (s.enforceMaxKb) s.targetKb * 1024L else 300 * 1024L
                val comp = BitmapUtils.compressToTargetSize(current, targetBytes)

                val tempFile = File(context.cacheDir, "passport_${System.currentTimeMillis()}.jpg")
                tempFile.outputStream().use { out ->
                    out.write(comp.compressedBytes)
                }

                _state.value = _state.value.copy(
                    previewBitmap = comp.bitmap,
                    finalFile = tempFile,
                    finalSizeBytes = comp.finalSizeBytes,
                    isProcessing = false,
                    statusMessage = ""
                )
            }
        }
    }

    fun savePassport(onSuccess: (String) -> Unit) {
        val s = _state.value
        val bmp = s.previewBitmap ?: return

        viewModelScope.launch {
            _state.value = s.copy(isProcessing = true, statusMessage = "Saving passport photo...")
            val uri = withContext(Dispatchers.IO) {
                val saved = StorageUtils.saveBitmapToGallery(
                    context = context,
                    bitmap = bmp,
                    title = "passport_${s.selectedPreset.id}",
                    format = Bitmap.CompressFormat.JPEG,
                    quality = 95
                )
                if (saved != null && s.finalFile != null) {
                    historyRepository.addHistoryItem(
                        FileHistoryItem(
                            id = "passport_${System.currentTimeMillis()}",
                            title = "Passport Photo (${s.selectedPreset.nameEn})",
                            filePath = s.finalFile.absolutePath,
                            uriString = saved.toString(),
                            mimeType = "image/jpeg",
                            sizeBytes = s.finalSizeBytes,
                            formattedSize = StorageUtils.formatFileSize(s.finalSizeBytes),
                            toolTag = "Passport Maker"
                        )
                    )
                }
                saved
            }
            _state.value = _state.value.copy(isProcessing = false)
            if (uri != null) {
                onSuccess("Saved to Gallery! Size: ${StorageUtils.formatFileSize(s.finalSizeBytes)}")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PassportViewModel(
                context.applicationContext,
                LanguageRepository(context.applicationContext),
                HistoryRepository(context.applicationContext)
            ) as T
        }
    }
}

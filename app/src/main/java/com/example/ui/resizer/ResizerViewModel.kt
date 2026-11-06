package com.example.ui.resizer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.FileHistoryItem
import com.example.data.model.SizeUnit
import com.example.data.repository.HistoryRepository
import com.example.data.repository.LanguageRepository
import com.example.utils.AppLanguage
import com.example.utils.BitmapUtils
import com.example.utils.CompressionLevel
import com.example.utils.PageSizeOption
import com.example.utils.PdfUtils
import com.example.utils.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class ResizeMode {
    PIXELS,
    UNITS,
    PERCENTAGE,
    TARGET_SIZE
}

data class PhotoResizeState(
    val selectedUris: List<Uri> = emptyList(),
    val originalBitmaps: List<Bitmap> = emptyList(),
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalSizeBytes: Long = 0L,
    val resizeMode: ResizeMode = ResizeMode.TARGET_SIZE,

    // Pixels
    val targetWidthPx: String = "",
    val targetHeightPx: String = "",
    val lockAspectRatio: Boolean = true,

    // Units
    val unitValueW: String = "3.5",
    val unitValueH: String = "4.5",
    val selectedUnit: SizeUnit = SizeUnit.CM,
    val selectedDpi: Int = 300,

    // Percentage
    val percentage: Float = 50f,

    // Target KB
    val targetKb: String = "50",

    // Result
    val processedBitmaps: List<Bitmap> = emptyList(),
    val processedSizeBytes: Long = 0L,
    val processedWidth: Int = 0,
    val processedHeight: Int = 0,
    val isProcessing: Boolean = false,
    val statusMessage: String = "",
    val lastSavedUri: Uri? = null,
    val lastSavedFile: File? = null
)

class ResizerViewModel(
    val context: Context,
    val languageRepository: LanguageRepository,
    val historyRepository: HistoryRepository
) : ViewModel() {

    val currentLanguage: StateFlow<AppLanguage> = languageRepository.currentLanguage

    private val _photoState = MutableStateFlow(PhotoResizeState())
    val photoState: StateFlow<PhotoResizeState> = _photoState.asStateFlow()

    fun onPhotosSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _photoState.value = _photoState.value.copy(
                selectedUris = uris,
                isProcessing = true,
                statusMessage = "Loading images safely..."
            )
            withContext(Dispatchers.IO) {
                val bitmaps = mutableListOf<Bitmap>()
                var totalBytes = 0L
                for (uri in uris) {
                    val bmp = BitmapUtils.decodeSampledBitmapFromUri(context, uri)
                    if (bmp != null) {
                        bitmaps.add(bmp)
                    }
                    totalBytes += StorageUtils.getFileSizeFromUri(context, uri)
                }

                if (bitmaps.isNotEmpty()) {
                    val first = bitmaps.first()
                    _photoState.value = _photoState.value.copy(
                        originalBitmaps = bitmaps,
                        originalWidth = first.width,
                        originalHeight = first.height,
                        originalSizeBytes = totalBytes,
                        targetWidthPx = first.width.toString(),
                        targetHeightPx = first.height.toString(),
                        isProcessing = false,
                        statusMessage = ""
                    )
                } else {
                    _photoState.value = _photoState.value.copy(
                        isProcessing = false,
                        statusMessage = "Could not load selected images"
                    )
                }
            }
        }
    }

    fun setResizeMode(mode: ResizeMode) {
        _photoState.value = _photoState.value.copy(resizeMode = mode)
    }

    fun setPixels(w: String, h: String) {
        val s = _photoState.value
        if (s.lockAspectRatio && s.originalWidth > 0 && s.originalHeight > 0) {
            val aspect = s.originalWidth.toFloat() / s.originalHeight.toFloat()
            if (w != s.targetWidthPx && w.isNotBlank()) {
                val newW = w.toIntOrNull() ?: s.originalWidth
                val calculatedH = (newW / aspect).roundToInt()
                _photoState.value = s.copy(targetWidthPx = w, targetHeightPx = calculatedH.toString())
                return
            } else if (h != s.targetHeightPx && h.isNotBlank()) {
                val newH = h.toIntOrNull() ?: s.originalHeight
                val calculatedW = (newH * aspect).roundToInt()
                _photoState.value = s.copy(targetWidthPx = calculatedW.toString(), targetHeightPx = h)
                return
            }
        }
        _photoState.value = s.copy(targetWidthPx = w, targetHeightPx = h)
    }

    fun toggleLockAspectRatio() {
        _photoState.value = _photoState.value.copy(lockAspectRatio = !_photoState.value.lockAspectRatio)
    }

    fun setUnitParams(w: String, h: String, unit: SizeUnit, dpi: Int) {
        _photoState.value = _photoState.value.copy(
            unitValueW = w,
            unitValueH = h,
            selectedUnit = unit,
            selectedDpi = dpi
        )
    }

    fun setPercentage(percent: Float) {
        _photoState.value = _photoState.value.copy(percentage = percent)
    }

    fun setTargetKb(kb: String) {
        _photoState.value = _photoState.value.copy(targetKb = kb)
    }

    fun processPhotoResize() {
        val s = _photoState.value
        if (s.originalBitmaps.isEmpty()) return

        viewModelScope.launch {
            _photoState.value = s.copy(isProcessing = true, statusMessage = "Resizing & compressing...")
            withContext(Dispatchers.IO) {
                val outputBitmaps = mutableListOf<Bitmap>()
                var totalFinalBytes = 0L
                var firstProcessedW = 0
                var firstProcessedH = 0

                for (sourceBmp in s.originalBitmaps) {
                    when (s.resizeMode) {
                        ResizeMode.PIXELS -> {
                            val w = s.targetWidthPx.toIntOrNull() ?: sourceBmp.width
                            val h = s.targetHeightPx.toIntOrNull() ?: sourceBmp.height
                            val resized = BitmapUtils.resizeByPixels(sourceBmp, w, h)
                            outputBitmaps.add(resized)
                            val comp = BitmapUtils.compressToTargetSize(resized, 500 * 1024L)
                            totalFinalBytes += comp.finalSizeBytes
                            firstProcessedW = resized.width
                            firstProcessedH = resized.height
                        }

                        ResizeMode.UNITS -> {
                            val wVal = s.unitValueW.toFloatOrNull() ?: 3.5f
                            val hVal = s.unitValueH.toFloatOrNull() ?: 4.5f
                            val wPx = BitmapUtils.convertUnitToPixels(wVal, s.selectedUnit, s.selectedDpi)
                            val hPx = BitmapUtils.convertUnitToPixels(hVal, s.selectedUnit, s.selectedDpi)
                            val resized = BitmapUtils.resizeByPixels(sourceBmp, wPx, hPx)
                            outputBitmaps.add(resized)
                            val comp = BitmapUtils.compressToTargetSize(resized, 500 * 1024L)
                            totalFinalBytes += comp.finalSizeBytes
                            firstProcessedW = resized.width
                            firstProcessedH = resized.height
                        }

                        ResizeMode.PERCENTAGE -> {
                            val factor = (s.percentage / 100f).coerceIn(0.05f, 5.0f)
                            val w = (sourceBmp.width * factor).roundToInt()
                            val h = (sourceBmp.height * factor).roundToInt()
                            val resized = BitmapUtils.resizeByPixels(sourceBmp, w, h)
                            outputBitmaps.add(resized)
                            val comp = BitmapUtils.compressToTargetSize(resized, 500 * 1024L)
                            totalFinalBytes += comp.finalSizeBytes
                            firstProcessedW = resized.width
                            firstProcessedH = resized.height
                        }

                        ResizeMode.TARGET_SIZE -> {
                            val targetBytes = (s.targetKb.toLongOrNull() ?: 50L) * 1024L
                            val comp = BitmapUtils.compressToTargetSize(sourceBmp, targetBytes)
                            outputBitmaps.add(comp.bitmap)
                            totalFinalBytes += comp.finalSizeBytes
                            firstProcessedW = comp.width
                            firstProcessedH = comp.height
                        }
                    }
                }

                _photoState.value = _photoState.value.copy(
                    processedBitmaps = outputBitmaps,
                    processedSizeBytes = totalFinalBytes,
                    processedWidth = firstProcessedW,
                    processedHeight = firstProcessedH,
                    isProcessing = false,
                    statusMessage = ""
                )
            }
        }
    }

    fun saveResult(onSuccess: (String) -> Unit) {
        val s = _photoState.value
        if (s.processedBitmaps.isEmpty()) return

        viewModelScope.launch {
            _photoState.value = s.copy(isProcessing = true, statusMessage = "Saving image...")
            withContext(Dispatchers.IO) {
                val firstBmp = s.processedBitmaps.first()
                val targetBytes = if (s.resizeMode == ResizeMode.TARGET_SIZE) (s.targetKb.toLongOrNull() ?: 50L) * 1024L else 500 * 1024L
                val comp = BitmapUtils.compressToTargetSize(firstBmp, targetBytes)

                val savedUri = StorageUtils.saveBitmapToGallery(
                    context = context,
                    bitmap = comp.bitmap,
                    title = "resized_photo",
                    format = Bitmap.CompressFormat.JPEG,
                    quality = comp.quality
                )

                // Also write to local cache file for sharing
                val cacheFile = File(context.cacheDir, "resized_${System.currentTimeMillis()}.jpg")
                cacheFile.outputStream().use { out ->
                    comp.bitmap.compress(Bitmap.CompressFormat.JPEG, comp.quality, out)
                }

                if (savedUri != null) {
                    historyRepository.addHistoryItem(
                        FileHistoryItem(
                            id = "photo_${System.currentTimeMillis()}",
                            title = "Resized Photo (${comp.width}x${comp.height})",
                            filePath = cacheFile.absolutePath,
                            uriString = savedUri.toString(),
                            mimeType = "image/jpeg",
                            sizeBytes = comp.finalSizeBytes,
                            formattedSize = StorageUtils.formatFileSize(comp.finalSizeBytes),
                            toolTag = "Photo Resizer"
                        )
                    )

                    _photoState.value = _photoState.value.copy(
                        lastSavedUri = savedUri,
                        lastSavedFile = cacheFile,
                        isProcessing = false,
                        statusMessage = ""
                    )
                    withContext(Dispatchers.Main) {
                        onSuccess("Saved successfully to Gallery!")
                    }
                } else {
                    _photoState.value = _photoState.value.copy(
                        isProcessing = false,
                        statusMessage = "Failed to save to Gallery"
                    )
                }
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ResizerViewModel(
                context.applicationContext,
                LanguageRepository(context.applicationContext),
                HistoryRepository(context.applicationContext)
            ) as T
        }
    }
}

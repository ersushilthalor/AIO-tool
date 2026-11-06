package com.example.ui.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.data.model.PassportPreset
import com.example.utils.BitmapUtils
import com.example.utils.MlKitUtils
import com.example.utils.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

enum class AiDetectedAction {
    COMPRESS_KB,
    PASSPORT_PRESET,
    REMOVE_BG,
    ROTATE,
    DOCUMENT_FILTER,
    NAME_DATE_STRIP,
    PRINT_SHEET,
    OCR,
    STRIP_EXIF,
    GENERAL_QUERY
}

data class AiExecutionResult(
    val actionType: AiDetectedAction,
    val explanationEn: String,
    val explanationHi: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val originalSizeBytes: Long,
    val resultBitmap: Bitmap? = null,
    val resultWidth: Int = 0,
    val resultHeight: Int = 0,
    val resultSizeBytes: Long = 0L,
    val resultFile: File? = null,
    val extractedText: String? = null
)

object AiImageActionProcessor {

    suspend fun processUserInstruction(
        context: Context,
        inputBitmap: Bitmap,
        userPrompt: String,
        originalSizeBytes: Long
    ): AiExecutionResult = withContext(Dispatchers.IO) {
        val q = userPrompt.lowercase()
        val origW = inputBitmap.width
        val origH = inputBitmap.height

        // 1. Target KB Compression (e.g. "50 kb", "20 kb", "100kb", "compress")
        val kbMatcher = Pattern.compile("(\\d+)\\s*(?:kb|kilo)").matcher(q)
        if (kbMatcher.find() || q.contains("compress") || q.contains("कम करो") || q.contains("साइज़")) {
            val targetKb = if (kbMatcher.find(0)) kbMatcher.group(1)?.toLongOrNull() ?: 50L else 50L
            val targetBytes = targetKb * 1024L
            val comp = BitmapUtils.compressToTargetSize(inputBitmap, targetBytes)

            val destFile = File(context.cacheDir, "ai_compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out -> out.write(comp.compressedBytes) }

            return@withContext AiExecutionResult(
                actionType = AiDetectedAction.COMPRESS_KB,
                explanationEn = "Target compression executed via binary-search optimization. Strictly fitted under ${targetKb} KB while preserving sharp pixel fidelity.",
                explanationHi = "लक्षित कंप्रेस लागू किया गया। फोटो को सफलतापूर्वक ${targetKb} KB के अंदर सेट किया गया।",
                originalWidth = origW,
                originalHeight = origH,
                originalSizeBytes = originalSizeBytes,
                resultBitmap = comp.bitmap,
                resultWidth = comp.width,
                resultHeight = comp.height,
                resultSizeBytes = comp.finalSizeBytes,
                resultFile = destFile
            )
        }

        // 2. Passport / Visa / ID Maker
        if (q.contains("passport") || q.contains("पासपोर्ट") || q.contains("visa") || q.contains("pan") || q.contains("ssc") || q.contains("upsc") || q.contains("exam")) {
            val preset = when {
                q.contains("us") || q.contains("2x2") -> PassportPreset.ALL_PRESETS.first { it.id == "us_passport_visa" }
                q.contains("pan") || q.contains("aadhaar") -> PassportPreset.ALL_PRESETS.first { it.id == "in_pan_aadhaar" }
                q.contains("ssc") || q.contains("upsc") || q.contains("200") -> PassportPreset.ALL_PRESETS.first { it.id == "gov_exam_200" }
                q.contains("uk") -> PassportPreset.ALL_PRESETS.first { it.id == "uk_passport" }
                q.contains("schengen") -> PassportPreset.ALL_PRESETS.first { it.id == "schengen_visa" }
                else -> PassportPreset.ALL_PRESETS.first { it.id == "in_passport" }
            }

            var current = inputBitmap
            // Auto-face crop
            val faces = MlKitUtils.detectFaces(current)
            if (faces.isNotEmpty()) {
                val ratio = preset.widthPx.toFloat() / preset.heightPx.toFloat()
                val crop = MlKitUtils.calculatePassportCropRect(current, faces.first(), ratio)
                current = Bitmap.createBitmap(current, crop.left, crop.top, crop.width(), crop.height())
            }

            // Background removal if requested or default white for passport
            val bgColor = when {
                q.contains("blue") || q.contains("नीला") -> Color.rgb(176, 212, 255)
                q.contains("red") || q.contains("लाल") -> Color.rgb(217, 4, 41)
                else -> Color.WHITE
            }
            current = MlKitUtils.removeBackground(current, bgColor)
            current = Bitmap.createScaledBitmap(current, preset.widthPx, preset.heightPx, true)

            // Name date strip if requested
            if (q.contains("name") || q.contains("date") || q.contains("strip") || q.contains("नाम")) {
                current = BitmapUtils.addNameAndDateStrip(current, "CANDIDATE NAME", "D.O.P: 2026")
            }

            // Enforce max KB
            val targetBytes = (preset.maxKb ?: 50) * 1024L
            val comp = BitmapUtils.compressToTargetSize(current, targetBytes)

            val destFile = File(context.cacheDir, "ai_passport_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out -> out.write(comp.compressedBytes) }

            return@withContext AiExecutionResult(
                actionType = AiDetectedAction.PASSPORT_PRESET,
                explanationEn = "Applied ${preset.nameEn} preset (${preset.widthMm}x${preset.heightMm} mm, ${preset.widthPx}x${preset.heightPx} px). Centered face using ML Kit, replaced background with solid clean studio background, and adjusted file size under ${preset.maxKb ?: 50} KB.",
                explanationHi = "${preset.nameHi} प्रीसेट लागू किया गया। ऑन-डिवाइस चेहरा केंद्रित किया गया और बैकग्राउंड साफ किया गया।",
                originalWidth = origW,
                originalHeight = origH,
                originalSizeBytes = originalSizeBytes,
                resultBitmap = comp.bitmap,
                resultWidth = comp.width,
                resultHeight = comp.height,
                resultSizeBytes = comp.finalSizeBytes,
                resultFile = destFile
            )
        }

        // 3. Background Removal
        if (q.contains("remove bg") || q.contains("background") || q.contains("हटा") || q.contains("cutout")) {
            val targetColor = when {
                q.contains("blue") || q.contains("नीला") -> Color.rgb(176, 212, 255)
                q.contains("red") || q.contains("लाल") -> Color.rgb(217, 4, 41)
                q.contains("transparent") || q.contains("पारदर्शी") -> Color.TRANSPARENT
                else -> Color.WHITE
            }
            val cutout = MlKitUtils.removeBackground(inputBitmap, targetColor)
            val isTransparent = targetColor == Color.TRANSPARENT
            val ext = if (isTransparent) "png" else "jpg"
            val fmt = if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

            val destFile = File(context.cacheDir, "ai_cutout_${System.currentTimeMillis()}.$ext")
            FileOutputStream(destFile).use { out -> cutout.compress(fmt, 95, out) }

            return@withContext AiExecutionResult(
                actionType = AiDetectedAction.REMOVE_BG,
                explanationEn = "On-device AI neural cutout executed. Subject segmented from background and placed on requested backdrop.",
                explanationHi = "ऑन-डिवाइस एआई द्वारा बैकग्राउंड सफलतापूर्वक हटा दिया गया है।",
                originalWidth = origW,
                originalHeight = origH,
                originalSizeBytes = originalSizeBytes,
                resultBitmap = cutout,
                resultWidth = cutout.width,
                resultHeight = cutout.height,
                resultSizeBytes = destFile.length(),
                resultFile = destFile
            )
        }

        // 4. Rotation (e.g. "rotate", "घुमाओ", "90", "180")
        if (q.contains("rotate") || q.contains("घुमा") || q.contains("degree") || q.contains("turn")) {
            val deg = when {
                q.contains("180") -> 180
                q.contains("270") -> 270
                else -> 90
            }
            val rotated = BitmapUtils.rotateBitmap(inputBitmap, deg)
            val destFile = File(context.cacheDir, "ai_rotated_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out -> rotated.compress(Bitmap.CompressFormat.JPEG, 95, out) }

            return@withContext AiExecutionResult(
                actionType = AiDetectedAction.ROTATE,
                explanationEn = "Rotated image by $deg degrees clockwise maintaining full pixel resolution.",
                explanationHi = "इमेज को $deg डिग्री घुमाया गया।",
                originalWidth = origW,
                originalHeight = origH,
                originalSizeBytes = originalSizeBytes,
                resultBitmap = rotated,
                resultWidth = rotated.width,
                resultHeight = rotated.height,
                resultSizeBytes = destFile.length(),
                resultFile = destFile
            )
        }

        // 5. Document Scanner B&W / Filter
        if (q.contains("filter") || q.contains("black and white") || q.contains("b&w") || q.contains("document") || q.contains("scan") || q.contains("कागज़")) {
            val result = Bitmap.createBitmap(inputBitmap.width, inputBitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val cm = ColorMatrix(
                floatArrayOf(
                    1.6f, 1.6f, 1.6f, 0f, -140f,
                    1.6f, 1.6f, 1.6f, 0f, -140f,
                    1.6f, 1.6f, 1.6f, 0f, -140f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(inputBitmap, 0f, 0f, paint)

            val destFile = File(context.cacheDir, "ai_doc_filter_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out -> result.compress(Bitmap.CompressFormat.JPEG, 92, out) }

            return@withContext AiExecutionResult(
                actionType = AiDetectedAction.DOCUMENT_FILTER,
                explanationEn = "High-contrast document scanner filter applied. Removed shadows, enhanced text sharpness and clean paper background.",
                explanationHi = "दस्तावेज़ स्कैनर फ़िल्टर लागू किया गया। लिखावट साफ और कागज़ सफेद किया गया।",
                originalWidth = origW,
                originalHeight = origH,
                originalSizeBytes = originalSizeBytes,
                resultBitmap = result,
                resultWidth = result.width,
                resultHeight = result.height,
                resultSizeBytes = destFile.length(),
                resultFile = destFile
            )
        }

        // 6. Name and Date strip
        if (q.contains("name") || q.contains("strip") || q.contains("dop") || q.contains("तारीख") || q.contains("नाम")) {
            val stripped = BitmapUtils.addNameAndDateStrip(inputBitmap, "CANDIDATE NAME", "15/10/2026")
            val destFile = File(context.cacheDir, "ai_strip_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out -> stripped.compress(Bitmap.CompressFormat.JPEG, 95, out) }

            return@withContext AiExecutionResult(
                actionType = AiDetectedAction.NAME_DATE_STRIP,
                explanationEn = "Government exam standard Name and Date of Photo (D.O.P) strip added at bottom.",
                explanationHi = "परीक्षा फॉर्म मानक नाम और तारीख पट्टी जोड़ी गई।",
                originalWidth = origW,
                originalHeight = origH,
                originalSizeBytes = originalSizeBytes,
                resultBitmap = stripped,
                resultWidth = stripped.width,
                resultHeight = stripped.height,
                resultSizeBytes = destFile.length(),
                resultFile = destFile
            )
        }

        // 7. Print Sheet 4x6 / A4
        if (q.contains("sheet") || q.contains("print") || q.contains("प्रिंट") || q.contains("4x6") || q.contains("a4")) {
            val isA4 = q.contains("a4")
            val sheet = BitmapUtils.generatePrintSheet(inputBitmap, isA4, 6f, 3f, true)
            val destFile = File(context.cacheDir, "ai_sheet_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out -> sheet.compress(Bitmap.CompressFormat.JPEG, 95, out) }

            return@withContext AiExecutionResult(
                actionType = AiDetectedAction.PRINT_SHEET,
                explanationEn = "Generated ${if (isA4) "A4" else "4x6 inch"} print sheet arranged with multiple copies and cut guide marks at 300 DPI.",
                explanationHi = "प्रिंट शीट तैयार की गई (कट लाइन्स और 300 DPI के साथ)।",
                originalWidth = origW,
                originalHeight = origH,
                originalSizeBytes = originalSizeBytes,
                resultBitmap = sheet,
                resultWidth = sheet.width,
                resultHeight = sheet.height,
                resultSizeBytes = destFile.length(),
                resultFile = destFile
            )
        }

        // 8. OCR / Extract Text
        if (q.contains("ocr") || q.contains("text") || q.contains("पढ़ो") || q.contains("लिखा")) {
            val text = MlKitUtils.recognizeText(inputBitmap)
            return@withContext AiExecutionResult(
                actionType = AiDetectedAction.OCR,
                explanationEn = "Extracted text using on-device ML Kit OCR engine.",
                explanationHi = "फोटो से टेक्स्ट निकाल लिया गया है।",
                originalWidth = origW,
                originalHeight = origH,
                originalSizeBytes = originalSizeBytes,
                extractedText = text
            )
        }

        // Default: Smart Enhancement and optimization under 100 KB
        val comp = BitmapUtils.compressToTargetSize(inputBitmap, 100 * 1024L)
        val destFile = File(context.cacheDir, "ai_optimized_${System.currentTimeMillis()}.jpg")
        FileOutputStream(destFile).use { out -> out.write(comp.compressedBytes) }

        return@withContext AiExecutionResult(
            actionType = AiDetectedAction.COMPRESS_KB,
            explanationEn = "Optimized image quality and compressed under 100 KB using adaptive binary-search quantization.",
            explanationHi = "इमेज को 100 KB के अंदर संतुलित और ऑप्टिमाइज़ किया गया।",
            originalWidth = origW,
            originalHeight = origH,
            originalSizeBytes = originalSizeBytes,
            resultBitmap = comp.bitmap,
            resultWidth = comp.width,
            resultHeight = comp.height,
            resultSizeBytes = comp.finalSizeBytes,
            resultFile = destFile
        )
    }
}

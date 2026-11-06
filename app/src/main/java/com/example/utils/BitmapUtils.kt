package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.SizeUnit
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BitmapUtils {

    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = 2048,
        reqHeight: Int = 2048
    ): Bitmap? {
        return try {
            var input: InputStream? = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            input = context.contentResolver.openInputStream(uri)
            val decodedBitmap = BitmapFactory.decodeStream(input, null, options)
            input?.close()

            if (decodedBitmap == null) return null

            // Check EXIF orientation and fix rotation if needed
            val orientation = getExifOrientation(context, uri)
            rotateBitmap(decodedBitmap, orientation)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun flipBitmap(bitmap: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            else postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun resizeByPixels(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val w = max(1, targetWidth)
        val h = max(1, targetHeight)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    fun convertUnitToPixels(value: Float, unit: SizeUnit, dpi: Int): Int {
        val safeDpi = max(72, dpi)
        return when (unit) {
            SizeUnit.PX -> value.roundToInt()
            SizeUnit.INCH -> (value * safeDpi).roundToInt()
            SizeUnit.CM -> ((value / 2.54f) * safeDpi).roundToInt()
            SizeUnit.MM -> ((value / 25.4f) * safeDpi).roundToInt()
        }
    }

    data class CompressionResult(
        val bitmap: Bitmap,
        val compressedBytes: ByteArray,
        val finalSizeBytes: Long,
        val quality: Int,
        val width: Int,
        val height: Int
    )

    fun compressToTargetSize(
        sourceBitmap: Bitmap,
        targetSizeBytes: Long,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): CompressionResult {
        var currentBitmap = sourceBitmap
        var quality = 95
        var bestBytes = ByteArray(0)

        // Try binary search on quality first
        var lowQ = 5
        var highQ = 95
        var iterations = 0

        while (lowQ <= highQ && iterations < 8) {
            iterations++
            val midQ = (lowQ + highQ) / 2
            val stream = ByteArrayOutputStream()
            currentBitmap.compress(format, midQ, stream)
            val bytes = stream.toByteArray()

            if (bytes.size <= targetSizeBytes) {
                bestBytes = bytes
                quality = midQ
                lowQ = midQ + 1 // Try higher quality while still under target
            } else {
                highQ = midQ - 1 // Reduce quality
            }
        }

        // If even minimum quality 5 is too big, iteratively downscale dimensions
        var scale = 0.85f
        while ((bestBytes.isEmpty() || bestBytes.size > targetSizeBytes) && currentBitmap.width > 100 && currentBitmap.height > 100) {
            val newW = max(50, (currentBitmap.width * scale).toInt())
            val newH = max(50, (currentBitmap.height * scale).toInt())
            currentBitmap = Bitmap.createScaledBitmap(currentBitmap, newW, newH, true)

            // Re-run binary search on quality for smaller dimension
            lowQ = 5
            highQ = 90
            while (lowQ <= highQ) {
                val midQ = (lowQ + highQ) / 2
                val stream = ByteArrayOutputStream()
                currentBitmap.compress(format, midQ, stream)
                val bytes = stream.toByteArray()

                if (bytes.size <= targetSizeBytes) {
                    bestBytes = bytes
                    quality = midQ
                    lowQ = midQ + 1
                } else {
                    highQ = midQ - 1
                }
            }
        }

        if (bestBytes.isEmpty()) {
            val stream = ByteArrayOutputStream()
            currentBitmap.compress(format, 50, stream)
            bestBytes = stream.toByteArray()
            quality = 50
        }

        val finalBitmap = BitmapFactory.decodeByteArray(bestBytes, 0, bestBytes.size) ?: currentBitmap
        return CompressionResult(
            bitmap = finalBitmap,
            compressedBytes = bestBytes,
            finalSizeBytes = bestBytes.size.toLong(),
            quality = quality,
            width = finalBitmap.width,
            height = finalBitmap.height
        )
    }

    fun adjustImage(
        bitmap: Bitmap,
        brightness: Float, // -100 to 100
        contrast: Float    // 0.5 to 2.0
    ): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // ColorMatrix for brightness and contrast
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    fun addBorder(bitmap: Bitmap, borderColor: Int, borderWidthPx: Int): Bitmap {
        if (borderWidthPx <= 0) return bitmap
        val newW = bitmap.width + (borderWidthPx * 2)
        val newH = bitmap.height + (borderWidthPx * 2)
        val result = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(borderColor)
        canvas.drawBitmap(bitmap, borderWidthPx.toFloat(), borderWidthPx.toFloat(), null)
        return result
    }

    fun addNameAndDateStrip(
        bitmap: Bitmap,
        name: String,
        date: String
    ): Bitmap {
        if (name.isBlank() && date.isBlank()) return bitmap

        val stripHeight = (bitmap.height * 0.16f).roundToInt().coerceAtLeast(60)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height + stripHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw original photo on top
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // Draw white strip background at bottom
        val paintBg = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, bitmap.height.toFloat(), bitmap.width.toFloat(), (bitmap.height + stripHeight).toFloat(), paintBg)

        // Draw subtle separator line
        val linePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 2f
        }
        canvas.drawLine(0f, bitmap.height.toFloat(), bitmap.width.toFloat(), bitmap.height.toFloat(), linePaint)

        // Draw text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = stripHeight * 0.38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val centerX = bitmap.width / 2f
        if (name.isNotBlank() && date.isNotBlank()) {
            val yName = bitmap.height + (stripHeight * 0.45f)
            val yDate = bitmap.height + (stripHeight * 0.85f)
            textPaint.textSize = stripHeight * 0.36f
            canvas.drawText(name.uppercase(), centerX, yName, textPaint)
            textPaint.textSize = stripHeight * 0.32f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText("D.O.P: $date", centerX, yDate, textPaint)
        } else {
            val singleText = if (name.isNotBlank()) name.uppercase() else "D.O.P: $date"
            val yPos = bitmap.height + (stripHeight * 0.62f)
            canvas.drawText(singleText, centerX, yPos, textPaint)
        }

        return result
    }

    fun generatePrintSheet(
        photo: Bitmap,
        isSheetA4: Boolean, // true for A4, false for 4x6 inch
        marginMm: Float = 5f,
        gapMm: Float = 3f,
        drawCutLines: Boolean = true
    ): Bitmap {
        val dpi = 300
        val sheetWidthPx = if (isSheetA4) (8.27f * dpi).roundToInt() else (4f * dpi).roundToInt()
        val sheetHeightPx = if (isSheetA4) (11.69f * dpi).roundToInt() else (6f * dpi).roundToInt()

        val sheetBitmap = Bitmap.createBitmap(sheetWidthPx, sheetHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)
        canvas.drawColor(Color.WHITE)

        val marginPx = ((marginMm / 25.4f) * dpi).roundToInt()
        val gapPx = ((gapMm / 25.4f) * dpi).roundToInt()

        val photoW = photo.width
        val photoH = photo.height

        val usableW = sheetWidthPx - (marginPx * 2)
        val usableH = sheetHeightPx - (marginPx * 2)

        val cols = max(1, (usableW + gapPx) / (photoW + gapPx))
        val rows = max(1, (usableH + gapPx) / (photoH + gapPx))

        // Center the grid on the sheet
        val gridTotalW = (cols * photoW) + ((cols - 1) * gapPx)
        val gridTotalH = (rows * photoH) + ((rows - 1) * gapPx)
        val startX = (sheetWidthPx - gridTotalW) / 2
        val startY = (sheetHeightPx - gridTotalH) / 2

        val cutPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = startX + c * (photoW + gapPx)
                val y = startY + r * (photoH + gapPx)
                canvas.drawBitmap(photo, x.toFloat(), y.toFloat(), null)

                if (drawCutLines) {
                    canvas.drawRect(
                        x.toFloat() - 1,
                        y.toFloat() - 1,
                        (x + photoW).toFloat() + 1,
                        (y + photoH).toFloat() + 1,
                        cutPaint
                    )
                }
            }
        }

        return sheetBitmap
    }
}

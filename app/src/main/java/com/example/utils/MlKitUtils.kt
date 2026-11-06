package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object MlKitUtils {

    suspend fun detectFaces(bitmap: Bitmap): List<Face> = suspendCancellableCoroutine { cont ->
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        val detector = FaceDetection.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (cont.isActive) cont.resume(faces)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(emptyList())
            }
    }

    fun calculatePassportCropRect(bitmap: Bitmap, face: Face, targetAspectRatio: Float): Rect {
        val faceBox = face.boundingBox
        val faceH = faceBox.height()
        val faceCenterX = faceBox.centerX()
        val faceCenterY = faceBox.centerY()

        // Standard passport rules: Face height is ~65-75% of total photo height
        val cropHeight = (faceH / 0.65f).roundToInt()
        val cropWidth = (cropHeight * targetAspectRatio).roundToInt()

        // Passport eye-level / face placement: face center should be ~45% from top
        var top = (faceCenterY - (cropHeight * 0.42f)).roundToInt()
        var left = (faceCenterX - (cropWidth / 2f)).roundToInt()

        // Ensure bounds stay within bitmap
        var finalW = cropWidth
        var finalH = cropHeight

        if (finalW > bitmap.width) {
            finalW = bitmap.width
            finalH = (finalW / targetAspectRatio).roundToInt()
        }
        if (finalH > bitmap.height) {
            finalH = bitmap.height
            finalW = (finalH * targetAspectRatio).roundToInt()
        }

        left = left.coerceIn(0, bitmap.width - finalW)
        top = top.coerceIn(0, bitmap.height - finalH)

        return Rect(left, top, left + finalW, top + finalH)
    }

    suspend fun removeBackground(
        bitmap: Bitmap,
        replacementColor: Int = Color.WHITE
    ): Bitmap = suspendCancellableCoroutine { cont ->
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        val segmenter = Segmentation.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        segmenter.process(image)
            .addOnSuccessListener { mask ->
                val maskBuffer: ByteBuffer = mask.buffer
                val maskWidth = mask.width
                val maskHeight = mask.height

                val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                if (replacementColor != Color.TRANSPARENT) {
                    canvas.drawColor(replacementColor)
                }

                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                val resultPixels = IntArray(bitmap.width * bitmap.height)

                maskBuffer.rewind()
                val scaleX = maskWidth.toFloat() / bitmap.width
                val scaleY = maskHeight.toFloat() / bitmap.height

                for (y in 0 until bitmap.height) {
                    val maskY = min((y * scaleY).toInt(), maskHeight - 1)
                    for (x in 0 until bitmap.width) {
                        val maskX = min((x * scaleX).toInt(), maskWidth - 1)
                        val maskIndex = maskY * maskWidth + maskX
                        val confidence = maskBuffer.getFloat(maskIndex * 4)

                        val index = y * bitmap.width + x
                        val originalColor = pixels[index]

                        if (confidence > 0.45f) {
                            // Subject pixel
                            resultPixels[index] = originalColor
                        } else {
                            // Background pixel
                            resultPixels[index] = replacementColor
                        }
                    }
                }

                output.setPixels(resultPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                if (cont.isActive) cont.resume(output)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(bitmap)
            }
    }

    suspend fun recognizeText(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (cont.isActive) cont.resume(visionText.text)
            }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resume("")
            }
    }
}

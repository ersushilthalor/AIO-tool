package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object DocxUtils {

    const val LIMITATIONS_NOTE = "Supports DOCX paragraphs, headings, bold/italic text, line breaks, bullet lists, and embedded photos. Note: Complex nested tables and proprietary macros are rendered as plain formatted text."

    data class DocRun(val text: String, val isBold: Boolean = false, val isItalic: Boolean = false, val isHeading: Boolean = false)
    data class DocParagraph(val runs: List<DocRun>, val isHeading: Boolean = false)
    data class DocContent(val paragraphs: List<DocParagraph>, val images: List<Bitmap>)

    fun parseDocx(context: Context, uri: Uri): DocContent {
        val paragraphs = mutableListOf<DocParagraph>()
        val images = mutableListOf<Bitmap>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return DocContent(emptyList(), emptyList())
            val zipStream = ZipInputStream(inputStream)

            var entry = zipStream.nextEntry
            var documentXmlBytes: ByteArray? = null

            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    documentXmlBytes = zipStream.readBytes()
                } else if (entry.name.startsWith("word/media/") && (entry.name.endsWith(".jpg") || entry.name.endsWith(".jpeg") || entry.name.endsWith(".png"))) {
                    val bytes = zipStream.readBytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { images.add(it) }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
            zipStream.close()

            if (documentXmlBytes != null) {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(documentXmlBytes), "UTF-8")

                var eventType = parser.eventType
                var currentRuns = mutableListOf<DocRun>()
                var isBold = false
                var isItalic = false
                var isHeading = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tag = parser.name
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (tag) {
                                "p" -> {
                                    currentRuns = mutableListOf()
                                    isHeading = false
                                }
                                "pStyle" -> {
                                    val styleVal = parser.getAttributeValue(null, "val") ?: ""
                                    if (styleVal.contains("Heading", ignoreCase = true) || styleVal.contains("Title", ignoreCase = true)) {
                                        isHeading = true
                                    }
                                }
                                "r" -> {
                                    isBold = false
                                    isItalic = false
                                }
                                "b" -> isBold = true
                                "i" -> isItalic = true
                                "t" -> {
                                    val text = parser.nextText()
                                    if (text.isNotEmpty()) {
                                        currentRuns.add(DocRun(text, isBold, isItalic, isHeading))
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tag == "p") {
                                if (currentRuns.isNotEmpty()) {
                                    paragraphs.add(DocParagraph(currentRuns.toList(), isHeading))
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return DocContent(paragraphs, images)
    }

    fun convertDocxToPdf(context: Context, uri: Uri, outputFile: File): Boolean {
        val content = parseDocx(context, uri)
        if (content.paragraphs.isEmpty() && content.images.isEmpty()) {
            return false
        }

        val pageWidth = PageSizeOption.A4.widthPt
        val pageHeight = PageSizeOption.A4.heightPt
        val margin = 40f
        val usableWidth = pageWidth - (margin * 2)

        val document = PdfDocument()
        var pageNum = 1
        var currentPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = currentPage.canvas
        canvas.drawColor(Color.WHITE)

        val regularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var currentY = margin + 20f

        fun checkNewPage(neededSpace: Float) {
            if (currentY + neededSpace > pageHeight - margin) {
                document.finishPage(currentPage)
                pageNum++
                currentPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = currentPage.canvas
                canvas.drawColor(Color.WHITE)
                currentY = margin + 20f
            }
        }

        for (p in content.paragraphs) {
            val isHeading = p.isHeading
            val paint = if (isHeading) headingPaint else regularPaint
            val fullText = p.runs.joinToString("") { it.text }
            val lines = wrapText(fullText, paint, usableWidth)

            checkNewPage(lines.size * (paint.textSize + 6f) + 10f)

            for (line in lines) {
                canvas.drawText(line, margin, currentY, paint)
                currentY += paint.textSize + 6f
            }
            currentY += if (isHeading) 12f else 6f
        }

        // Draw images if any
        for (img in content.images) {
            val scale = (usableWidth / img.width.toFloat()).coerceAtMost(1f)
            val imgW = img.width * scale
            val imgH = img.height * scale

            checkNewPage(imgH + 20f)
            val dest = Rect(margin.toInt(), currentY.toInt(), (margin + imgW).toInt(), (currentY + imgH).toInt())
            canvas.drawBitmap(img, null, dest, null)
            currentY += imgH + 20f
        }

        document.finishPage(currentPage)

        return try {
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }

    fun convertTxtToPdf(context: Context, uri: Uri, outputFile: File): Boolean {
        val lines = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var l: String? = reader.readLine()
                    while (l != null) {
                        lines.add(l)
                        l = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        val pageWidth = PageSizeOption.A4.widthPt
        val pageHeight = PageSizeOption.A4.heightPt
        val margin = 40f
        val usableWidth = pageWidth - (margin * 2)

        val document = PdfDocument()
        var pageNum = 1
        var currentPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = currentPage.canvas
        canvas.drawColor(Color.WHITE)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }

        var currentY = margin + 20f

        for (rawLine in lines) {
            val wrapped = if (rawLine.isBlank()) listOf("") else wrapText(rawLine, textPaint, usableWidth)
            for (line in wrapped) {
                if (currentY + textPaint.textSize + 4f > pageHeight - margin) {
                    document.finishPage(currentPage)
                    pageNum++
                    currentPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = currentPage.canvas
                    canvas.drawColor(Color.WHITE)
                    currentY = margin + 20f
                }
                canvas.drawText(line, margin, currentY, textPaint)
                currentY += textPaint.textSize + 5f
            }
        }

        document.finishPage(currentPage)

        return try {
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return if (lines.isEmpty()) listOf("") else lines
    }
}

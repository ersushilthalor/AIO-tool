package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

enum class PageSizeOption(val title: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 x 297 mm)", 595, 842),
    A3("A3 (297 x 420 mm)", 842, 1191),
    LETTER("US Letter (8.5 x 11 in)", 612, 792),
    FIT_IMAGE("Fit to Image", 0, 0)
}

enum class CompressionLevel(val labelEn: String, val labelHi: String, val dpi: Int, val quality: Int) {
    LOW("Low Compression (Best Quality)", "कम कंप्रेस (सर्वोत्तम क्वालिटी)", 180, 85),
    MEDIUM("Recommended (Balanced)", "सुझाया गया (संतुलित)", 120, 70),
    HIGH("Maximum Compression (Smallest Size)", "अधिकतम कंप्रेस (न्यूनतम साइज)", 80, 50)
}

object PdfUtils {

    fun getPdfPageCount(context: Context, uri: Uri): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (e: Exception) {
            0
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    fun renderPdfPageToBitmap(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        scale: Float = 1.5f
    ): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
            page = renderer.openPage(pageIndex)

            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Fill white background before rendering
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            page?.close()
            renderer?.close()
            pfd?.close()
        }
    }

    fun renderAllPagesToBitmaps(
        context: Context,
        uri: Uri,
        dpi: Int = 150
    ): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return bitmaps
            renderer = PdfRenderer(pfd)
            val scale = dpi / 72f
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val width = max(1, (page.width * scale).toInt())
                val height = max(1, (page.height * scale).toInt())
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                bitmaps.add(bitmap)
                page.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            renderer?.close()
            pfd?.close()
        }
        return bitmaps
    }

    fun createPdfFromBitmaps(
        bitmaps: List<Bitmap>,
        outputFile: File,
        pageSizeOption: PageSizeOption = PageSizeOption.A4,
        marginPt: Int = 20,
        compressQuality: Int = 85
    ): Boolean {
        val document = PdfDocument()
        try {
            for (i in bitmaps.indices) {
                val bmp = bitmaps[i]
                val pageWidth = if (pageSizeOption == PageSizeOption.FIT_IMAGE) bmp.width else pageSizeOption.widthPt
                val pageHeight = if (pageSizeOption == PageSizeOption.FIT_IMAGE) bmp.height else pageSizeOption.heightPt

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)

                if (pageSizeOption == PageSizeOption.FIT_IMAGE) {
                    canvas.drawBitmap(bmp, 0f, 0f, null)
                } else {
                    val usableW = (pageWidth - (marginPt * 2)).toFloat()
                    val usableH = (pageHeight - (marginPt * 2)).toFloat()

                    val bmpRatio = bmp.width.toFloat() / bmp.height.toFloat()
                    val usableRatio = usableW / usableH

                    var drawW = usableW
                    var drawH = usableH

                    if (bmpRatio > usableRatio) {
                        drawH = drawW / bmpRatio
                    } else {
                        drawW = drawH * bmpRatio
                    }

                    val left = marginPt + (usableW - drawW) / 2f
                    val top = marginPt + (usableH - drawH) / 2f
                    val destRect = Rect(left.toInt(), top.toInt(), (left + drawW).toInt(), (top + drawH).toInt())
                    canvas.drawBitmap(bmp, null, destRect, null)
                }

                document.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            document.close()
        }
    }

    fun compressPdf(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        level: CompressionLevel,
        targetPageSize: PageSizeOption = PageSizeOption.A4
    ): Boolean {
        val bitmaps = renderAllPagesToBitmaps(context, inputUri, level.dpi)
        if (bitmaps.isEmpty()) return false
        return createPdfFromBitmaps(
            bitmaps = bitmaps,
            outputFile = outputFile,
            pageSizeOption = targetPageSize,
            marginPt = 15,
            compressQuality = level.quality
        )
    }

    fun mergePdfs(inputFiles: List<File>, outputFile: File): Boolean {
        return try {
            val merger = PDFMergerUtility()
            merger.destinationFileName = outputFile.absolutePath
            for (f in inputFiles) {
                merger.addSource(f)
            }
            merger.mergeDocuments(null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun splitPdf(
        inputFile: File,
        outputDir: File,
        pageRanges: List<IntRange>? = null // null means split all pages individually
    ): List<File> {
        val resultFiles = mutableListOf<File>()
        var document: PDDocument? = null
        try {
            document = PDDocument.load(inputFile)
            val totalPages = document.numberOfPages

            if (pageRanges == null) {
                for (i in 0 until totalPages) {
                    val singleDoc = PDDocument()
                    singleDoc.addPage(document.getPage(i))
                    val out = File(outputDir, "page_${i + 1}.pdf")
                    singleDoc.save(out)
                    singleDoc.close()
                    resultFiles.add(out)
                }
            } else {
                for ((idx, range) in pageRanges.withIndex()) {
                    val splitDoc = PDDocument()
                    for (p in range) {
                        val zeroIdx = p - 1
                        if (zeroIdx in 0 until totalPages) {
                            splitDoc.addPage(document.getPage(zeroIdx))
                        }
                    }
                    if (splitDoc.numberOfPages > 0) {
                        val out = File(outputDir, "part_${idx + 1}_pages_${range.first}-${range.last}.pdf")
                        splitDoc.save(out)
                        resultFiles.add(out)
                    }
                    splitDoc.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document?.close()
        }
        return resultFiles
    }

    fun rotatePdfPages(inputFile: File, outputFile: File, degrees: Int): Boolean {
        var doc: PDDocument? = null
        return try {
            doc = PDDocument.load(inputFile)
            for (i in 0 until doc.numberOfPages) {
                val page = doc.getPage(i)
                val currentRot = page.rotation
                page.rotation = (currentRot + degrees) % 360
            }
            doc.save(outputFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            doc?.close()
        }
    }

    fun reorderPdfPages(inputFile: File, outputFile: File, newOrderIndices: List<Int>): Boolean {
        var srcDoc: PDDocument? = null
        var destDoc: PDDocument? = null
        return try {
            srcDoc = PDDocument.load(inputFile)
            destDoc = PDDocument()
            val total = srcDoc.numberOfPages

            for (idx in newOrderIndices) {
                if (idx in 0 until total) {
                    destDoc.addPage(srcDoc.getPage(idx))
                }
            }
            destDoc.save(outputFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            srcDoc?.close()
            destDoc?.close()
        }
    }

    fun lockPdfWithPassword(
        inputFile: File,
        outputFile: File,
        userPass: String,
        ownerPass: String = userPass
    ): Boolean {
        var doc: PDDocument? = null
        return try {
            doc = PDDocument.load(inputFile)
            val ap = AccessPermission()
            val spp = StandardProtectionPolicy(ownerPass, userPass, ap).apply {
                encryptionKeyLength = 128
                permissions = ap
            }
            doc.protect(spp)
            doc.save(outputFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            doc?.close()
        }
    }

    fun unlockPdf(
        inputFile: File,
        outputFile: File,
        password: String
    ): Boolean {
        var doc: PDDocument? = null
        return try {
            doc = PDDocument.load(inputFile, password)
            if (doc.isEncrypted) {
                doc.isAllSecurityToBeRemoved = true
            }
            doc.save(outputFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            doc?.close()
        }
    }

    fun addWatermarkAndPageNumbers(
        inputFile: File,
        outputFile: File,
        watermarkText: String?,
        includePageNumbers: Boolean
    ): Boolean {
        var doc: PDDocument? = null
        return try {
            doc = PDDocument.load(inputFile)
            val font = PDType1Font.HELVETICA_BOLD
            val pageCount = doc.numberOfPages

            for (i in 0 until pageCount) {
                val page = doc.getPage(i)
                val mediaBox = page.mediaBox
                val contentStream = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)

                // 1. Watermark
                if (!watermarkText.isNullOrBlank()) {
                    val fontSize = 42f
                    val textWidth = font.getStringWidth(watermarkText) / 1000 * fontSize
                    contentStream.saveGraphicsState()
                    val extGraphics = PDExtendedGraphicsState().apply {
                        nonStrokingAlphaConstant = 0.22f
                    }
                    contentStream.setGraphicsStateParameters(extGraphics)
                    contentStream.setNonStrokingColor(180, 180, 180)
                    contentStream.beginText()
                    contentStream.setFont(font, fontSize)

                    val centerX = mediaBox.width / 2f
                    val centerY = mediaBox.height / 2f
                    contentStream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(45.0), centerX, centerY))
                    contentStream.newLineAtOffset(-textWidth / 2f, 0f)
                    contentStream.showText(watermarkText)
                    contentStream.endText()
                    contentStream.restoreGraphicsState()
                }

                // 2. Page numbers
                if (includePageNumbers) {
                    val pageNumText = "Page ${i + 1} of $pageCount"
                    val numFontSize = 10f
                    val numFont = PDType1Font.HELVETICA
                    val textWidth = numFont.getStringWidth(pageNumText) / 1000 * numFontSize
                    contentStream.beginText()
                    contentStream.setFont(numFont, numFontSize)
                    contentStream.setNonStrokingColor(60, 60, 60)
                    val posX = (mediaBox.width - textWidth) / 2f
                    val posY = 20f
                    contentStream.newLineAtOffset(posX, posY)
                    contentStream.showText(pageNumText)
                    contentStream.endText()
                }

                contentStream.close()
            }
            doc.save(outputFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            doc?.close()
        }
    }
}

package com.example.data.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    enum class PageSize(val widthPt: Int, val heightPt: Int) {
        A4(595, 842),
        LETTER(612, 792),
        A5(420, 595),
        FIT_IMAGE(-1, -1)
    }

    fun exportToPdf(
        context: Context,
        bitmaps: List<Bitmap>,
        pageSize: PageSize = PageSize.A4,
        marginPt: Int = 24,
        backgroundColor: Int = Color.WHITE,
        outputFileName: String = "APOLLO_Export_${System.currentTimeMillis()}.pdf"
    ): File {
        val document = PdfDocument()

        for ((index, bitmap) in bitmaps.withIndex()) {
            val pageW = if (pageSize == PageSize.FIT_IMAGE) bitmap.width else pageSize.widthPt
            val pageH = if (pageSize == PageSize.FIT_IMAGE) bitmap.height else pageSize.heightPt

            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Fill background
            val bgPaint = Paint().apply { color = backgroundColor }
            canvas.drawRect(0f, 0f, pageW.toFloat(), pageH.toFloat(), bgPaint)

            if (pageSize == PageSize.FIT_IMAGE) {
                canvas.drawBitmap(bitmap, 0f, 0f, null)
            } else {
                // Calculate aspect ratio fit within margins
                val availableW = (pageW - 2 * marginPt).toFloat().coerceAtLeast(10f)
                val availableH = (pageH - 2 * marginPt).toFloat().coerceAtLeast(10f)

                val bmpW = bitmap.width.toFloat()
                val bmpH = bitmap.height.toFloat()
                val scale = minOf(availableW / bmpW, availableH / bmpH)

                val drawW = bmpW * scale
                val drawH = bmpH * scale
                val left = (pageW - drawW) / 2f
                val top = (pageH - drawH) / 2f

                val destRect = RectF(left, top, left + drawW, top + drawH)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, null, destRect, paint)
            }

            document.finishPage(page)
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, outputFileName)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return outputFile
    }
}

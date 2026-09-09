package com.realestate.sami.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.realestate.sami.data.local.entity.PropertyEntity
import java.io.File

/**
 * فاز ۵ — خروجی PDF/Excel از لیست ملک‌ها، برای ارسال به همکار یا چاپ.
 * هر دو فایل داخل cache/exports ساخته و سپس با Intent اشتراک‌گذاری (از طریق FileProvider،
 * بدون نیاز به مجوز حافظه) به برنامه‌ی دلخواه کاربر (واتس‌اپ، ایمیل، تلگرام، …) فرستاده می‌شوند.
 */
object PropertyExporter {

    private val HEADERS = listOf("ردیف", "آدرس", "نوع ملک", "نوع معامله", "متراژ", "اتاق", "قیمت (تومان)", "وضعیت")

    private fun rowsFor(properties: List<PropertyEntity>): List<List<Any?>> =
        properties.mapIndexed { index, p ->
            listOf(
                index + 1,
                p.address,
                p.propertyType.toExportLabel(),
                p.dealType.toExportLabel(),
                p.area,
                p.rooms,
                (p.totalPrice ?: p.rentPrice ?: 0L),
                p.status.toExportLabel()
            )
        }

    fun exportToExcel(context: Context, properties: List<PropertyEntity>): Uri? {
        val outFile = exportFile(context, "properties", "xlsx")
        return try {
            XlsxWriter.write(outFile, "املاک", HEADERS, rowsFor(properties))
            shareUri(context, outFile)
        } catch (e: Exception) {
            null
        }
    }

    fun exportToPdf(context: Context, properties: List<PropertyEntity>): Uri? {
        val outFile = exportFile(context, "properties", "pdf")
        return try {
            writePdf(outFile, properties)
            shareUri(context, outFile)
        } catch (e: Exception) {
            null
        }
    }

    fun shareFile(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    private fun exportFile(context: Context, baseName: String, extension: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        return File(dir, "$baseName-${System.currentTimeMillis()}.$extension")
    }

    private fun shareUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun writePdf(outFile: File, properties: List<PropertyEntity>) {
        val pageWidth = 842 // A4 landscape @ 72dpi تا جدول ۸ ستونه جا شود
        val pageHeight = 595
        val marginX = 24f
        val marginTop = 40f
        val rowHeight = 22f
        val rowsPerPage = ((pageHeight - marginTop - 40f) / rowHeight).toInt()

        val document = PdfDocument()
        val headerPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        val cellPaint = Paint().apply { textSize = 11f; textAlign = Paint.Align.RIGHT }
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }

        val colWidths = floatArrayOf(40f, 220f, 90f, 90f, 60f, 50f, 140f, 90f)
        // ستون‌ها راست‌به‌چپ چیده می‌شوند (اولین ستون سمت راست صفحه)
        val colRightEdges = FloatArray(colWidths.size)
        var runningRight = pageWidth - marginX
        for (i in colWidths.indices) {
            colRightEdges[i] = runningRight
            runningRight -= colWidths[i]
        }

        val rows = rowsFor(properties)
        val chunks = rows.chunked(if (rowsPerPage > 0) rowsPerPage else 20)
        val pageCount = if (chunks.isEmpty()) 1 else chunks.size

        for (pageIndex in 0 until pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            canvas.drawText("لیست ملک‌ها — صفحه ${pageIndex + 1}", pageWidth - marginX, 28f, titlePaint)

            var y = marginTop
            HEADERS.forEachIndexed { i, h -> canvas.drawText(h, colRightEdges[i], y, headerPaint) }
            y += rowHeight

            val pageRows = if (chunks.isEmpty()) emptyList() else chunks[pageIndex]
            for (row in pageRows) {
                row.forEachIndexed { i, value ->
                    canvas.drawText(value?.toString() ?: "-", colRightEdges[i], y, cellPaint)
                }
                y += rowHeight
            }

            document.finishPage(page)
        }

        document.writeTo(outFile.outputStream())
        document.close()
    }
}

private fun com.realestate.sami.data.local.entity.PropertyType.toExportLabel(): String = when (this) {
    com.realestate.sami.data.local.entity.PropertyType.APARTMENT -> "آپارتمان"
    com.realestate.sami.data.local.entity.PropertyType.VILLA -> "ویلایی"
    com.realestate.sami.data.local.entity.PropertyType.LAND -> "زمین"
    com.realestate.sami.data.local.entity.PropertyType.COMMERCIAL -> "تجاری"
    com.realestate.sami.data.local.entity.PropertyType.OFFICE -> "اداری"
}

private fun com.realestate.sami.data.local.entity.DealType.toExportLabel(): String = when (this) {
    com.realestate.sami.data.local.entity.DealType.SALE -> "خرید و فروش"
    com.realestate.sami.data.local.entity.DealType.RENT -> "اجاره"
    com.realestate.sami.data.local.entity.DealType.MORTGAGE -> "رهن کامل"
    com.realestate.sami.data.local.entity.DealType.EXCHANGE -> "مبادله"
}

private fun com.realestate.sami.data.local.entity.PropertyStatus.toExportLabel(): String = when (this) {
    com.realestate.sami.data.local.entity.PropertyStatus.AVAILABLE -> "قابل معرفی"
    com.realestate.sami.data.local.entity.PropertyStatus.RESERVED -> "رزرو شده"
    com.realestate.sami.data.local.entity.PropertyStatus.SOLD_OR_RENTED -> "فروخته/اجاره شده"
    com.realestate.sami.data.local.entity.PropertyStatus.ARCHIVED -> "بایگانی"
}

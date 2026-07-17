package com.veggiego.restaurant

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File

object PdfGenerator {

    fun createPdf(

        context: Context,

        fileName: String,

        content: String

    ) {
        val pdfDocument =
            PdfDocument()

        val estimatedLines =

            content.lines().sumOf {

                maxOf(
                    1,
                    (it.length / 24) + 1
                )
            }

        val pageHeight =

            maxOf(
                250,
                (estimatedLines * 20) + 60
            )

        val pageInfo =

            PdfDocument.PageInfo.Builder(

                220,

                pageHeight,

                1

            ).create()

        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas

        val titlePaint = Paint().apply {

            textSize = 22f

            typeface = Typeface.DEFAULT_BOLD

            textAlign = Paint.Align.CENTER
        }

        val centerPaint = Paint().apply {

            textSize = 14f

            textAlign = Paint.Align.CENTER
        }

        val normalPaint = Paint().apply {

            textSize = 12f
        }

        var y = 40

        canvas.drawText(
            "VEGGIEGO",
            110f,
            y.toFloat(),
            titlePaint
        )

        y += 30

        content.lines().forEach { line ->

            when {

                line == "Burger Point" -> {

                    canvas.drawText(
                        line,
                        110f,
                        y.toFloat(),
                        centerPaint
                    )
                }

                line.contains(
                    "Thank You"
                ) ||

                        line.contains(
                            "Visit Again"
                        ) -> {

                    canvas.drawText(
                        line,
                        110f,
                        y.toFloat(),
                        centerPaint
                    )
                    y += 22
                }

                else -> {
                    val maxChars = 28

                    if (
                        line.length > maxChars
                    ) {

                        line.chunked(
                            maxChars
                        ).forEach { part ->

                            canvas.drawText(

                                part,

                                20f,

                                y.toFloat(),

                                normalPaint

                            )

                            y += 22
                        }

                    } else {

                        canvas.drawText(

                            line,

                            20f,

                            y.toFloat(),

                            normalPaint

                        )

                        y += 22
                    }
                }
            }
        }

        pdfDocument.finishPage(page)

        val file = File(
            context.cacheDir,
            fileName
        )

        pdfDocument.writeTo(
            file.outputStream()
        )

        pdfDocument.close()

        val uri = FileProvider.getUriForFile(
            context,
            "com.veggiego.restaurant.provider",
            file
        )

        val intent = Intent(
            Intent.ACTION_VIEW
        )

        intent.setDataAndType(
            uri,
            "application/pdf"
        )

        intent.flags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION

        context.startActivity(
            intent
        )
    }
}
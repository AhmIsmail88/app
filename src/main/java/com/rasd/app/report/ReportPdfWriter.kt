package com.rasd.app.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ReportPdfWriter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f
    private const val ROW_H = 22f

    private fun np(size: Float, bold: Boolean = false, color: Int = Color.BLACK): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = size
        p.color = color
        p.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        return p
    }

    private fun fillPaint(color: Int): Paint = np(1f, color = color).apply { style = Paint.Style.FILL }

    private fun drawHeader(
        c: Canvas,
        h: ReportHeaderInfo,
        title: String,
        subject: String,
        labels: ReportLabels,
        yIn: Float
    ): Float {
        var y = yIn
        val hasLogo = h.logoPath.isNotBlank()
        val logoH = 54f
        var logoW = 0f
        if (hasLogo) {
            val bmp: Bitmap? = BitmapFactory.decodeFile(h.logoPath)
            if (bmp != null) {
                logoW = logoH * bmp.width / bmp.height.toFloat()
                val lx = if (labels.rtl) MARGIN else PAGE_W - MARGIN - logoW
                c.drawBitmap(bmp, lx, MARGIN, np(1f))
            }
        }
        val align = if (labels.rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val anchor = if (labels.rtl) PAGE_W - MARGIN else MARGIN
        val offset = if (hasLogo) 70f else 0f

        val t1 = np(15f, true).apply { textAlign = align }
        val t2 = np(11f).apply { textAlign = align; color = Color.DKGRAY }

        if (h.schoolName.isNotBlank()) c.drawText(h.schoolName, anchor, MARGIN + 16, t1)
        if (h.teacherName.isNotBlank()) c.drawText(labels.teacher + ": " + h.teacherName, anchor, MARGIN + 34, t2)
        if (h.academicYear.isNotBlank()) c.drawText(labels.academicYear + ": " + h.academicYear, anchor, MARGIN + 50, t2)

        y = MARGIN + 74
        c.drawText(title, anchor, y, np(13f, true).apply { textAlign = align })
        y += 18
        c.drawText(
            subject + "   |   " + labels.period + ": " + labels.from + " " + h.fromDate + " " + labels.to + " " + h.toDate,
            if (labels.rtl) anchor - offset else anchor + offset,
            y,
            np(11f).apply { textAlign = align }
        )
        y += 16
        c.drawLine(MARGIN, y, PAGE_W - MARGIN, y, np(1f, color = Color.LTGRAY).apply { strokeWidth = 1f })
        return y + 12
    }

    private fun drawFooter(c: Canvas, labels: ReportLabels) {
        c.drawLine(MARGIN, PAGE_H - MARGIN - 12, PAGE_W - MARGIN, PAGE_H - MARGIN - 12, np(1f, color = Color.LTGRAY).apply { strokeWidth = 0.8f })
        c.drawText(
            labels.generatedBy,
            if (labels.rtl) PAGE_W - MARGIN else MARGIN,
            PAGE_H - MARGIN,
            np(9f, color = Color.GRAY).apply { textAlign = if (labels.rtl) Paint.Align.RIGHT else Paint.Align.LEFT }
        )
    }

    private fun drawTable(
        c: Canvas,
        topIn: Float,
        headers: List<String>,
        widths: List<Float>,
        rows: List<List<String>>,
        boldCol: Int,
        rtl: Boolean
    ): Float {
        var y = topIn
        val left = MARGIN
        val right = PAGE_W - MARGIN
        val hp = np(10f, true)
        c.drawRect(left, y, right, y + ROW_H, fillPaint(Color.rgb(226, 238, 250)))
        if (rtl) {
            var x = right
            headers.forEachIndexed { i, t ->
                hp.textAlign = Paint.Align.CENTER
                c.drawText(t, x - widths[i] / 2f, y + ROW_H - 7f, hp)
                x -= widths[i]
            }
        } else {
            var x = left
            headers.forEachIndexed { i, t ->
                hp.textAlign = Paint.Align.CENTER
                c.drawText(t, x + widths[i] / 2f, y + ROW_H - 7f, hp)
                x += widths[i]
            }
        }
        c.drawRect(left, y, right, y + ROW_H, np(1f, color = Color.GRAY).apply { strokeWidth = 0.7f; style = Paint.Style.STROKE })
        y += ROW_H

        val lp = np(1f, color = Color.LTGRAY).apply { strokeWidth = 0.5f; style = Paint.Style.STROKE }
        rows.forEach { row ->
            if (rtl) {
                var x = right
                row.forEachIndexed { i, v ->
                    val p = if (i == boldCol) np(10f, true) else np(10f)
                    p.textAlign = if (i == boldCol) Paint.Align.RIGHT else Paint.Align.CENTER
                    val tx = if (i == boldCol) x - 6f else x - widths[i] / 2f
                    c.drawText(v, tx, y + ROW_H - 7f, p)
                    x -= widths[i]
                }
            } else {
                var x = left
                row.forEachIndexed { i, v ->
                    val p = if (i == boldCol) np(10f, true) else np(10f)
                    p.textAlign = if (i == boldCol) Paint.Align.LEFT else Paint.Align.CENTER
                    val tx = if (i == boldCol) x + 6f else x + widths[i] / 2f
                    c.drawText(v, tx, y + ROW_H - 7f, p)
                    x += widths[i]
                }
            }
            c.drawRect(left, y, right, y + ROW_H, lp)
            y += ROW_H
        }
        return y
    }

    private fun widths(weights: List<Float>, total: Float): List<Float> {
        val sum = weights.sum()
        return weights.map { it / sum * total }
    }

    private fun termText(v: Int?): String = if (v == null) "-" else "$v%"

    private fun termName(labels: ReportLabels, term: String): String = when (term) {
        TermGrades.MIDYEAR -> labels.termMidyear
        TermGrades.FINAL -> labels.termFinal
        else -> labels.termWork
    }

    fun writeClassReport(report: ClassReport, out: File) {
        val labels = ReportTexts.of(report.header.language)
        val doc = PdfDocument()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var c = page.canvas
        var y = drawHeader(c, report.header, labels.classReport, report.className, labels, MARGIN)
        val headers = listOf(
            labels.colIndex, labels.colName, labels.colPresent, labels.colAbsent,
            labels.colLate, labels.colAttendancePercent, labels.colBehavior, labels.colAverage,
            labels.colTermWork, labels.colMidyear, labels.colFinalTerm
        )
        val w = widths(listOf(0.6f, 2.4f, 0.7f, 0.7f, 0.7f, 0.9f, 1.5f, 1.0f, 0.9f, 0.9f, 0.9f), PAGE_W - 2 * MARGIN)
        val rows = report.rows.map { r ->
            listOf(
                r.index.toString(), r.studentName, r.present.toString(), r.absent.toString(),
                r.late.toString(), r.attendancePercent.toString() + "%", r.behaviorSummary,
                String.format(Locale.US, "%.1f", r.assessmentAverage),
                termText(r.termPercents[TermGrades.TERMLY]),
                termText(r.termPercents[TermGrades.MIDYEAR]),
                termText(r.termPercents[TermGrades.FINAL])
            )
        }
        var i = 0
        while (i < rows.size) {
            val remaining = ((PAGE_H - MARGIN - 30 - y) / ROW_H).toInt() - 1
            val chunk = rows.drop(i).take(if (remaining < 1) 1 else remaining)
            y = drawTable(c, y, headers, w, chunk, 1, labels.rtl)
            i += chunk.size
            if (i < rows.size) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
                c = page.canvas
                y = drawHeader(c, report.header, labels.classReport, report.className, labels, MARGIN)
            }
        }
        drawFooter(c, labels)
        doc.finishPage(page)
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
    }

    fun writeStudentReport(report: StudentReport, out: File) {
        val labels = ReportTexts.of(report.header.language)
        val align = if (labels.rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val anchor = if (labels.rtl) PAGE_W - MARGIN else MARGIN

        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val c = page.canvas
        var y = drawHeader(c, report.header, labels.studentReport, report.className, labels, MARGIN)

        c.drawText(report.studentName + "   (" + report.studentNumber + ")", anchor, y, np(11f, true).apply { textAlign = align })
        y += 18
        c.drawText(
            String.format(Locale.US, labels.attendanceFormat, report.present, report.absent, report.late, report.attendancePercent),
            anchor, y, np(10f).apply { textAlign = align }
        )
        y += 16
        c.drawText(
            String.format(
                Locale.US, labels.pointsFormat,
                String.format(Locale.US, "%.1f", report.behaviorPoints),
                String.format(Locale.US, "%.1f", report.assessmentAverage)
            ),
            anchor, y, np(10f).apply { textAlign = align }
        )
        y += 20
        if (report.termGrades.isNotEmpty()) {
            c.drawText(labels.termGrades, anchor, y, np(12f, true).apply { textAlign = align })
            y += 8
            val wt = widths(listOf(1.8f, 1.1f, 1.0f, 1.0f), PAGE_W - 2 * MARGIN)
            y = drawTable(
                c, y,
                listOf(labels.colTerm, labels.colPercent, labels.colScore, labels.colOutOf),
                wt,
                report.termGrades.map {
                    listOf(
                        termName(labels, it.term),
                        String.format(Locale.US, "%.1f", it.percent) + "%",
                        String.format(Locale.US, "%.1f", it.total),
                        String.format(Locale.US, "%.1f", it.maximum)
                    )
                },
                0,
                labels.rtl
            )
            y += 20
        }
        c.drawText(labels.grades, anchor, y, np(12f, true).apply { textAlign = align })
        y += 8
        val w1 = widths(listOf(1.6f, 2.4f, 1.2f, 0.9f, 0.9f, 1.0f), PAGE_W - 2 * MARGIN)
        y = drawTable(
            c, y,
            listOf(labels.colAssessment, labels.colType, labels.colDate, labels.colScore, labels.colOutOf, labels.colPercent),
            w1,
            report.assessments.map {
                listOf(
                    it.assessmentName, it.type, it.date,
                    String.format(Locale.US, "%.1f", it.score),
                    String.format(Locale.US, "%.1f", it.maximum),
                    String.format(Locale.US, "%.1f", it.percent)
                )
            },
            0,
            labels.rtl
        )
        y += 18
        c.drawText(labels.behavior, anchor, y, np(12f, true).apply { textAlign = align })
        y += 8
        val w2 = widths(listOf(1.2f, 2.0f, 1.2f, 1.0f, 3.0f), PAGE_W - 2 * MARGIN)
        y = drawTable(
            c, y,
            listOf(labels.colDate, labels.colBehavior, labels.colCategory, labels.colPoints, labels.colNote),
            w2,
            report.behaviors.map {
                listOf(it.date, it.typeName, it.category, String.format(Locale.US, "%.1f", it.points), it.note)
            },
            1,
            labels.rtl
        )
        drawFooter(c, labels)
        doc.finishPage(page)
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
    }
}

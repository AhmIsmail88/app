package com.rasd.app.report

import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportXlsxWriter {

    private data class Cell(val text: String, val bold: Boolean = false)
    private data class RowData(val cells: List<Cell>)

    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun isNumeric(s: String): Boolean {
        if (s.isBlank()) return false
        return s.matches(Regex("-?\\d+(\\.\\d+)?"))
    }

    private fun colName(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val r = (n - 1) % 26
            sb.insert(0, ('A' + r))
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun write(
        file: File,
        sheetName: String,
        rows: List<RowData>,
        widths: List<Float>,
        logo: File?,
        rtl: Boolean
    ) {
        val hasLogo = logo != null && logo.exists() && logo.length() > 0

        val contentTypes = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
            append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
            append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
            if (hasLogo) append("<Default Extension=\"png\" ContentType=\"image/png\"/>")
            append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
            append("<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
            append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
            if (hasLogo) append("<Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>")
            append("</Types>")
        }

        val rootRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

        val workbook = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
            "<sheets><sheet name=\"${esc(sheetName)}\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"

        val workbookRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
            "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
            "</Relationships>"

        val styles = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
            "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Arial\"/></font><font><b/><sz val=\"11\"/><name val=\"Arial\"/></font></fonts>" +
            "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>" +
            "<borders count=\"1\"><border/></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
            "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>" +
            "</styleSheet>"

        val colsXml = if (widths.isNotEmpty()) {
            "<cols>" + widths.mapIndexed { i, w ->
                "<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>"
            }.joinToString("") + "</cols>"
        } else ""

        val sb = StringBuilder()
        sb.append("<sheetData>")
        rows.forEachIndexed { ri, row ->
            sb.append("<row r=\"").append(ri + 1).append("\">")
            row.cells.forEachIndexed { ci, cell ->
                if (cell.text.isEmpty()) return@forEachIndexed
                val ref = colName(ci) + (ri + 1)
                val style = if (cell.bold) " s=\"1\"" else ""
                if (isNumeric(cell.text)) {
                    sb.append("<c r=\"").append(ref).append("\"").append(style)
                        .append("><v>").append(cell.text).append("</v></c>")
                } else {
                    sb.append("<c r=\"").append(ref).append("\"").append(style)
                        .append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(esc(cell.text))
                        .append("</t></is></c>")
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData>")

        val drawingRef = if (hasLogo) "<drawing r:id=\"rId1\"/>" else ""
        val viewAttrs = if (rtl) " rightToLeft=\"1\"" else ""

        val sheet = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
            "<sheetViews><sheetView$viewAttrs workbookViewId=\"0\"/></sheetViews>" +
            colsXml + sb + drawingRef + "</worksheet>"

        val sheetRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            (if (hasLogo) "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing\" Target=\"../drawings/drawing1.xml\"/>" else "") +
            "</Relationships>"

        val drawing = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" " +
            "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">" +
            "<xdr:twoCellAnchor>" +
            "<xdr:from><xdr:col>5</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>0</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>" +
            "<xdr:to><xdr:col>7</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>3</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>" +
            "<xdr:pic>" +
            "<xdr:nvPicPr><xdr:cNvPr id=\"1\" name=\"Logo\"/><xdr:cNvPicPr/></xdr:nvPicPr>" +
            "<xdr:blipFill><a:blip xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" r:embed=\"rId1\"/>" +
            "<a:stretch><a:fillRect/></a:stretch></xdr:blipFill>" +
            "<xdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/></a:xfrm>" +
            "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr>" +
            "</xdr:pic><xdr:clientData/></xdr:twoCellAnchor></xdr:wsDr>"

        val drawingRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/image1.png\"/>" +
            "</Relationships>"

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            val parts = mutableListOf<Pair<String, ByteArray>>(
                "[Content_Types].xml" to contentTypes.toByteArray(Charsets.UTF_8),
                "_rels/.rels" to rootRels.toByteArray(Charsets.UTF_8),
                "xl/workbook.xml" to workbook.toByteArray(Charsets.UTF_8),
                "xl/_rels/workbook.xml.rels" to workbookRels.toByteArray(Charsets.UTF_8),
                "xl/styles.xml" to styles.toByteArray(Charsets.UTF_8),
                "xl/worksheets/sheet1.xml" to sheet.toByteArray(Charsets.UTF_8)
            )
            if (hasLogo) {
                parts.add("xl/worksheets/_rels/sheet1.xml.rels" to sheetRels.toByteArray(Charsets.UTF_8))
                parts.add("xl/drawings/drawing1.xml" to drawing.toByteArray(Charsets.UTF_8))
                parts.add("xl/drawings/_rels/drawing1.xml.rels" to drawingRels.toByteArray(Charsets.UTF_8))
                parts.add("xl/media/image1.png" to logo!!.readBytes())
            }
            parts.forEach { (name, content) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content)
                zos.closeEntry()
            }
        }
    }

    private fun num(v: Double): String = String.format(Locale.US, "%.1f", v)

    private fun termText(v: Int?): String = if (v == null) "-" else "$v%"

    private fun termName(labels: ReportLabels, term: String): String = when (term) {
        TermGrades.MIDYEAR -> labels.termMidyear
        TermGrades.FINAL -> labels.termFinal
        else -> labels.termWork
    }

    private fun headerRows(h: ReportHeaderInfo, title: String, subject: String, labels: ReportLabels): List<RowData> {
        val r = mutableListOf<RowData>()
        if (h.schoolName.isNotBlank()) r.add(RowData(listOf(Cell(h.schoolName, true))))
        if (h.teacherName.isNotBlank()) r.add(RowData(listOf(Cell(labels.teacher + ": " + h.teacherName))))
        if (h.academicYear.isNotBlank()) r.add(RowData(listOf(Cell(labels.academicYear + ": " + h.academicYear))))
        r.add(RowData(listOf(Cell(title + " - " + subject, true))))
        r.add(RowData(listOf(Cell(labels.period + ": " + labels.from + " " + h.fromDate + " " + labels.to + " " + h.toDate))))
        r.add(RowData(listOf(Cell(""))))
        return r
    }

    private fun logoFile(h: ReportHeaderInfo): File? =
        if (h.logoPath.isNotBlank()) File(h.logoPath) else null

    fun writeClassReport(report: ClassReport, out: File) {
        val labels = ReportTexts.of(report.header.language)
        val rows = mutableListOf<RowData>()
        rows.addAll(headerRows(report.header, labels.classReport, report.className, labels))
        rows.add(RowData(listOf(
            labels.colIndex, labels.colName, labels.colPresent, labels.colAbsent,
            labels.colLate, labels.colAttendancePercent, labels.colBehavior, labels.colAverage,
            labels.colTermWork, labels.colMidyear, labels.colFinalTerm
        ).map { Cell(it, true) }))
        report.rows.forEach { r ->
            rows.add(RowData(listOf(
                Cell(r.index.toString()),
                Cell(r.studentName, true),
                Cell(r.present.toString()),
                Cell(r.absent.toString()),
                Cell(r.late.toString()),
                Cell(r.attendancePercent.toString() + "%"),
                Cell(r.behaviorSummary),
                Cell(num(r.assessmentAverage)),
                Cell(termText(r.termPercents[TermGrades.TERMLY])),
                Cell(termText(r.termPercents[TermGrades.MIDYEAR])),
                Cell(termText(r.termPercents[TermGrades.FINAL]))
            )))
        }
        write(
            out, labels.classReport, rows,
            listOf(6f, 26f, 9f, 9f, 9f, 12f, 20f, 14f, 12f, 12f, 12f),
            logoFile(report.header), labels.rtl
        )
    }

    fun writeStudentReport(report: StudentReport, out: File) {
        val labels = ReportTexts.of(report.header.language)
        val rows = mutableListOf<RowData>()
        rows.addAll(headerRows(report.header, labels.studentReport, report.className, labels))
        rows.add(RowData(listOf(Cell(report.studentName + "  (" + report.studentNumber + ")", true))))
        rows.add(RowData(listOf(Cell(
            String.format(Locale.US, labels.attendanceFormat, report.present, report.absent, report.late, report.attendancePercent)
        ))))
        rows.add(RowData(listOf(Cell(
            String.format(
                Locale.US, labels.pointsFormat,
                num(report.behaviorPoints), num(report.assessmentAverage)
            )
        ))))
        if (report.termGrades.isNotEmpty()) {
            rows.add(RowData(listOf(Cell(labels.termGrades, true))))
            rows.add(RowData(listOf(
                labels.colTerm, labels.colPercent, labels.colScore, labels.colOutOf
            ).map { Cell(it, true) }))
            report.termGrades.forEach { t ->
                rows.add(RowData(listOf(
                    Cell(termName(labels, t.term), true),
                    Cell(num(t.percent) + "%"),
                    Cell(num(t.total)),
                    Cell(num(t.maximum))
                )))
            }
        }
        rows.add(RowData(listOf(Cell(""))))
        rows.add(RowData(listOf(Cell(labels.grades, true))))
        rows.add(RowData(listOf(
            labels.colAssessment, labels.colType, labels.colDate,
            labels.colScore, labels.colOutOf, labels.colPercent
        ).map { Cell(it, true) }))
        report.assessments.forEach { a ->
            rows.add(RowData(listOf(
                Cell(a.assessmentName, true), Cell(a.type), Cell(a.date),
                Cell(num(a.score)), Cell(num(a.maximum)), Cell(num(a.percent))
            )))
        }
        rows.add(RowData(listOf(Cell(""))))
        rows.add(RowData(listOf(Cell(labels.behavior, true))))
        rows.add(RowData(listOf(
            labels.colDate, labels.colBehavior, labels.colCategory, labels.colPoints, labels.colNote
        ).map { Cell(it, true) }))
        report.behaviors.forEach { b ->
            rows.add(RowData(listOf(
                Cell(b.date), Cell(b.typeName, true), Cell(b.category), Cell(num(b.points)), Cell(b.note)
            )))
        }
        write(out, labels.studentReport, rows, listOf(10f, 24f, 12f, 9f, 9f, 10f), logoFile(report.header), labels.rtl)
    }
}

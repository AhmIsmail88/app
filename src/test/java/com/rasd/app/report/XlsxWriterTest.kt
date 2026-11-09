package com.rasd.app.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/**
 * يتحقق من أن مولّد Excel ينتج حزمة XLSX سليمة البنية (كل الأجزاء موجودة وكل XML صحيح الشكل)
 * ويعمل بالعربية (RTL) والإنجليزية (LTR).
 */
class XlsxWriterTest {

    // PNG 1x1 صالح (شفاف) — مضمَّن لتجنّب الاعتماد على java.awt في اختبارات الوحدة.
    private val pngBytes: ByteArray = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    )

    private fun makeLogo(dir: File): File {
        val f = File(dir, "logo.png")
        f.writeBytes(pngBytes)
        return f
    }

    private fun assertValidPackage(out: File, expectLogo: Boolean) {
        assertTrue("file missing/empty", out.exists() && out.length() > 0)
        ZipFile(out).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toList()
            assertTrue("workbook missing", names.contains("xl/workbook.xml"))
            assertTrue("sheet missing", names.contains("xl/worksheets/sheet1.xml"))
            assertTrue("styles missing", names.contains("xl/styles.xml"))
            assertTrue("content types missing", names.contains("[Content_Types].xml"))
            if (expectLogo) {
                assertTrue("media missing", names.contains("xl/media/image1.png"))
                assertTrue("drawing missing", names.contains("xl/drawings/drawing1.xml"))
                assertTrue("drawing rels missing", names.contains("xl/drawings/_rels/drawing1.xml.rels"))
                assertTrue("sheet rels missing", names.contains("xl/worksheets/_rels/sheet1.xml.rels"))
                assertTrue("logo bytes empty", zip.getEntry("xl/media/image1.png").size > 0)
            } else {
                assertTrue("unexpected drawing", !names.contains("xl/drawings/drawing1.xml"))
            }
            for (n in names.filter { it.endsWith(".xml") || it.endsWith(".rels") }) {
                val text = zip.getInputStream(zip.getEntry(n)).use { it.readBytes().toString(Charsets.UTF_8) }
                assertTrue("$n: not xml", text.startsWith("<?xml"))
                assertTrue("$n: not closed", text.trimEnd().endsWith(">"))
                DocumentBuilderFactory.newInstance()
                    .apply { isNamespaceAware = false }
                    .newDocumentBuilder()
                    .parse(zip.getInputStream(zip.getEntry(n)))
            }
        }
    }

    private fun sheetOf(out: File): String = ZipFile(out).use { zip ->
        zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun tempDir(prefix: String): File =
        File(System.getProperty("java.io.tmpdir"), "$prefix-${System.currentTimeMillis()}").apply { mkdirs() }

    @Test
    fun classReportWithLogoProducesValidXlsx() {
        val dir = tempDir("rasd-xlsx-class")
        val logo = makeLogo(dir)
        val header = ReportHeaderInfo("مدرسة الأمل", "أحمد", "2026", "2026-09-01", "2026-09-22", logo.absolutePath)
        val report = ClassReport(
            header = header,
            className = "الصف العاشر",
            rows = listOf(
                ClassReportRow(1, "طالب أول", "1001", 18, 1, 1, 90, 5.0, "إيجابي: 2 | سلبي: 0", 88.5, 3),
                ClassReportRow(2, "طالب ثاني", "1002", 20, 0, 0, 100, -1.0, "إيجابي: 0 | سلبي: 1", 72.0, 2)
            )
        )
        val out = File(dir, "class.xlsx")
        ReportXlsxWriter.writeClassReport(report, out)
        assertValidPackage(out, expectLogo = true)
        val sheet = sheetOf(out)
        assertTrue("arabic sheet must be RTL", sheet.contains("rightToLeft=\"1\""))
        assertTrue("arabic header expected", sheet.contains("نسبة الحضور"))
    }

    @Test
    fun classReportWithoutLogoHasNoDrawingPart() {
        val dir = tempDir("rasd-xlsx-nologo")
        val header = ReportHeaderInfo("مدرسة", "", "", "2026-09-01", "2026-09-22", "")
        val report = ClassReport(header, "فصل", listOf(ClassReportRow(1, "طالب", "1", 1, 0, 0, 100, 0.0, "-", 50.0, 1)))
        val out = File(dir, "class.xlsx")
        ReportXlsxWriter.writeClassReport(report, out)
        assertValidPackage(out, expectLogo = false)
    }

    @Test
    fun studentReportWithLogoProducesValidXlsx() {
        val dir = tempDir("rasd-xlsx-student")
        val logo = makeLogo(dir)
        val header = ReportHeaderInfo("مدرسة", "مدرس", "2026", "2026-09-01", "2026-09-22", logo.absolutePath)
        val report = StudentReport(
            header = header,
            studentName = "طالب أول",
            studentNumber = "1001",
            className = "الصف العاشر",
            present = 18, absent = 1, late = 1, attendancePercent = 90,
            behaviorPoints = 5.0, assessmentAverage = 88.5,
            assessments = listOf(StudentAssessmentLine("اختبار", "exam", "2026-09-10", 44.0, 50.0, 88.0)),
            behaviors = listOf(StudentBehaviorLine("2026-09-11", "مشاركة ممتازة", "positive", 2.0, "جيد"))
        )
        val out = File(dir, "student.xlsx")
        ReportXlsxWriter.writeStudentReport(report, out)
        assertValidPackage(out, expectLogo = true)
    }

    @Test
    fun englishReportUsesEnglishLabelsAndLtrSheet() {
        val dir = tempDir("rasd-xlsx-en")
        val header = ReportHeaderInfo("Al Amal School", "Ahmed", "2026", "2026-09-01", "2026-09-22", "", "en")
        val report = ClassReport(
            header = header,
            className = "Grade 6A",
            rows = listOf(ClassReportRow(1, "Sara", "1001", 18, 1, 1, 90, 2.0, "Positive: 1 | Negative: 0", 88.0, 2))
        )
        val out = File(dir, "class_en.xlsx")
        ReportXlsxWriter.writeClassReport(report, out)
        assertValidPackage(out, expectLogo = false)
        val sheet = sheetOf(out)
        assertTrue("english header expected", sheet.contains("Class report"))
        assertTrue("english column expected", sheet.contains("Attendance %"))
        assertTrue("english period label expected", sheet.contains("Period"))
        assertTrue("english sheet must be LTR", !sheet.contains("rightToLeft"))
        assertTrue("arabic must not leak", !sheet.contains("نسبة"))
    }

    @Test
    fun reportLabelsSwitchWithLanguage() {
        assertTrue(ReportTexts.of("ar").rtl)
        assertTrue(!ReportTexts.of("en").rtl)
        assertTrue(ReportTexts.of("en-US").classReport == "Class report")
        assertTrue(ReportTexts.of("ar").classReport == "تقرير الفصل")
        assertEquals("Positive: 3 | Negative: 1", ReportTexts.posNeg("en", 3, 1))
        assertEquals("إيجابي: 3 | سلبي: 1", ReportTexts.posNeg("ar", 3, 1))
    }
}

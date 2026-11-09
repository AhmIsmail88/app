package com.rasd.app.report

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

/**
 * يتحقق من ظهور درجات الفصول الدراسية فعلياً في ملف Excel المُصدَّر،
 * وأن الطالب بلا درجات يظهر بشرطة لا بصفر مُختلَق.
 */
class TermGradesReportTest {

    private fun tempDir(prefix: String): File =
        File(System.getProperty("java.io.tmpdir"), "$prefix-${System.currentTimeMillis()}").apply { mkdirs() }

    private fun sheetOf(out: File): String = ZipFile(out).use { zip ->
        zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    @Test
    fun classReportExportsTermColumnsAndKeepsMissingTermsEmpty() {
        val dir = tempDir("rasd-term-class")
        val header = ReportHeaderInfo("Al Amal", "Ahmed", "2026", "2026-09-01", "2026-09-30", "", "en")
        val report = ClassReport(
            header = header,
            className = "Grade 6A",
            rows = listOf(
                ClassReportRow(
                    1, "Sara", "1001", 1, 0, 0, 100, 0.0, "-", 80.0, 1,
                    termPercents = mapOf(TermGrades.TERMLY to 78, TermGrades.MIDYEAR to 65)
                ),
                ClassReportRow(2, "Ali", "1002", 1, 0, 0, 100, 0.0, "-", 50.0, 1)
            )
        )
        val out = File(dir, "class_terms.xlsx")
        ReportXlsxWriter.writeClassReport(report, out)
        val sheet = sheetOf(out)
        assertTrue("term work header missing", sheet.contains("Term work"))
        assertTrue("mid-year header missing", sheet.contains("Mid-year"))
        assertTrue("end-of-year header missing", sheet.contains("End-of-year"))
        assertTrue("termly percent missing", sheet.contains("78%"))
        assertTrue("mid-year percent missing", sheet.contains("65%"))
        assertTrue("student without scores must show a dash", sheet.contains(">-<"))
    }

    @Test
    fun studentReportExportsTermGradesTable() {
        val dir = tempDir("rasd-term-student")
        val header = ReportHeaderInfo("Al Amal", "Ahmed", "2026", "2026-09-01", "2026-09-30", "", "en")
        val report = StudentReport(
            header = header,
            studentName = "Sara",
            studentNumber = "1001",
            className = "Grade 6A",
            present = 10,
            absent = 0,
            late = 0,
            attendancePercent = 100,
            behaviorPoints = 2.0,
            assessmentAverage = 80.0,
            assessments = emptyList(),
            behaviors = emptyList(),
            termGrades = listOf(TermGradeLine(TermGrades.TERMLY, 78.0, 2, 39.0, 50.0))
        )
        val out = File(dir, "student_terms.xlsx")
        ReportXlsxWriter.writeStudentReport(report, out)
        val sheet = sheetOf(out)
        assertTrue("term grades section missing", sheet.contains("Term grades"))
        assertTrue("term percent missing", sheet.contains("78.0%"))
        assertTrue("term total missing", sheet.contains("39.0"))
        assertTrue("term maximum missing", sheet.contains("50.0"))
    }

    @Test
    fun arabicReportUsesArabicTermLabels() {
        val dir = tempDir("rasd-term-ar")
        val header = ReportHeaderInfo("مدرسة", "أحمد", "2026", "2026-09-01", "2026-09-30", "", "ar")
        val report = ClassReport(
            header = header,
            className = "فصل",
            rows = listOf(
                ClassReportRow(
                    1, "سارة", "1001", 1, 0, 0, 100, 0.0, "-", 80.0, 1,
                    termPercents = mapOf(TermGrades.FINAL to 91)
                )
            )
        )
        val out = File(dir, "class_ar.xlsx")
        ReportXlsxWriter.writeClassReport(report, out)
        val sheet = sheetOf(out)
        assertTrue("arabic term work header missing", sheet.contains("أعمال السنة"))
        assertTrue("arabic mid-year header missing", sheet.contains("نصف العام"))
        assertTrue("arabic final header missing", sheet.contains("نهاية العام"))
        assertTrue("arabic final percent missing", sheet.contains("91%"))
        assertTrue("arabic sheet must stay RTL", sheet.contains("rightToLeft=\"1\""))
    }
}

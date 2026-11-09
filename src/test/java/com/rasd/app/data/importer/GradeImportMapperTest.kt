package com.rasd.app.data.importer

import com.rasd.app.data.model.StudentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات مطابقة ملف الدرجات مع طلاب الفصل.
 */
class GradeImportMapperTest {

    private val students = listOf(
        StudentEntity(id = 1, classId = 1, studentNumber = "1001", arabicName = "سارة", englishName = "Sara"),
        StudentEntity(id = 2, classId = 1, studentNumber = "1002", arabicName = "علي", englishName = "Ali"),
        StudentEntity(id = 3, classId = 1, studentNumber = "", arabicName = "منى", englishName = "")
    )

    @Test
    fun matchesByStudentNumberAndSkipsHeader() {
        val rows = listOf(
            listOf("الرقم", "الاسم", "الدرجة"),
            listOf("1001", "سارة", "18"),
            listOf("1002", "علي", "15")
        )
        val r = GradeImportMapper.map(rows, students, numberIdx = 0, arabicIdx = 1, englishIdx = -1, scoreIdx = 2, skipHeader = true, maximumScore = 20f)
        assertEquals(2, r.scores.size)
        assertEquals(1L to 18f, r.scores[0])
        assertEquals(2L to 15f, r.scores[1])
        assertEquals(0, r.unmatched)
        assertEquals(0, r.invalid)
    }

    @Test
    fun matchesByArabicNameWhenNumberIsIgnored() {
        val rows = listOf(listOf("منى", "12"))
        val r = GradeImportMapper.map(rows, students, -1, 0, -1, 1, false, 20f)
        assertEquals(1, r.scores.size)
        assertEquals(3L to 12f, r.scores[0])
    }

    @Test
    fun matchesByEnglishNameCaseInsensitively() {
        val rows = listOf(listOf("sara", "19"))
        val r = GradeImportMapper.map(rows, students, -1, -1, 0, 1, false, 20f)
        assertEquals(1, r.scores.size)
        assertEquals(1L to 19f, r.scores[0])
    }

    @Test
    fun unmatchedRowsAreCounted() {
        val rows = listOf(listOf("9999", "مجهول", "10"))
        val r = GradeImportMapper.map(rows, students, 0, 1, -1, 2, false, 20f)
        assertTrue(r.scores.isEmpty())
        assertEquals(1, r.unmatched)
    }

    @Test
    fun outOfRangeScoresAreRejected() {
        val rows = listOf(listOf("1001", "سارة", "25"), listOf("1002", "علي", "-3"))
        val r = GradeImportMapper.map(rows, students, 0, 1, -1, 2, false, 20f)
        assertTrue(r.scores.isEmpty())
        assertEquals(2, r.invalid)
    }

    @Test
    fun blankScoresAreSkippedSilently() {
        val rows = listOf(listOf("1001", "سارة", ""))
        val r = GradeImportMapper.map(rows, students, 0, 1, -1, 2, false, 20f)
        assertTrue(r.scores.isEmpty())
        assertEquals(0, r.unmatched)
        assertEquals(0, r.invalid)
    }

    @Test
    fun decimalCommaIsAccepted() {
        val rows = listOf(listOf("1001", "سارة", "17,5"))
        val r = GradeImportMapper.map(rows, students, 0, 1, -1, 2, false, 20f)
        assertEquals(1, r.scores.size)
        assertEquals(17.5f, r.scores[0].second, 0.001f)
    }
}

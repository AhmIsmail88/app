package com.rasd.app.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات مطابقة أعمدة الاستيراد.
 */
class StudentImportMapperTest {

    private val rows = listOf(
        listOf("الرقم", "الاسم", "Name"),   // ترويسة
        listOf("1001", "سارة", "Sara"),
        listOf("1002", "علي", "Ali"),
        listOf("1003", "", "Empty")          // بلا اسم عربي ⇒ يُتجاهل
    )

    @Test
    fun defaultMappingSkipsHeaderAndBlankNames() {
        val students = StudentImportMapper.map(rows, classId = 7, numberIdx = 0, arabicIdx = 1, englishIdx = 2, skipHeader = true)
        assertEquals(2, students.size)
        assertEquals("سارة", students[0].arabicName)
        assertEquals("1001", students[0].studentNumber)
        assertEquals("Sara", students[0].englishName)
        assertEquals(7L, students[0].classId)
    }

    @Test
    fun withoutSkipHeaderTheHeaderBecomesAStudent() {
        val students = StudentImportMapper.map(rows, 1, 0, 1, 2, skipHeader = false)
        assertEquals(3, students.size)
        assertEquals("الاسم", students[0].arabicName)
    }

    @Test
    fun ignoringColumnsLeavesThoseFieldsEmpty() {
        val students = StudentImportMapper.map(rows, 1, numberIdx = -1, arabicIdx = 1, englishIdx = -1, skipHeader = true)
        assertEquals(2, students.size)
        assertEquals("", students[0].studentNumber)
        assertEquals("", students[0].englishName)
        assertEquals("سارة", students[0].arabicName)
    }

    @Test
    fun reorderedColumnsAreRespected() {
        val swapped = listOf(
            listOf("Sara", "سارة", "1001"),
            listOf("Ali", "علي", "1002")
        )
        val students = StudentImportMapper.map(swapped, 1, numberIdx = 2, arabicIdx = 1, englishIdx = 0, skipHeader = false)
        assertEquals(2, students.size)
        assertEquals("سارة", students[0].arabicName)
        assertEquals("1001", students[0].studentNumber)
        assertEquals("Sara", students[0].englishName)
    }

    @Test
    fun missingArabicColumnYieldsNothing() {
        val students = StudentImportMapper.map(rows, 1, 0, arabicIdx = 9, englishIdx = 2, skipHeader = true)
        assertTrue(students.isEmpty())
    }

    @Test
    fun valuesAreTrimmed() {
        val padded = listOf(listOf(" 1001 ", "  سارة  ", " Sara "))
        val students = StudentImportMapper.map(padded, 1, 0, 1, 2, skipHeader = false)
        assertEquals("سارة", students[0].arabicName)
        assertEquals("1001", students[0].studentNumber)
        assertEquals("Sara", students[0].englishName)
    }
}

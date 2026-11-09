package com.rasd.app.report

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * يتحقق من تحليل CSV المستخدم في استيراد الطلاب (الفواصل بين علامات التنصيص، الأسطر الفارغة).
 */
class SpreadsheetReaderTest {

    @Test
    fun parsesSimpleCsv() {
        val rows = SpreadsheetReader.parseCsvText("1001,علي,Ali\n1002,سارة,Sara\n")
        assertEquals(2, rows.size)
        assertEquals(listOf("1001", "علي", "Ali"), rows[0])
        assertEquals(listOf("1002", "سارة", "Sara"), rows[1])
    }

    @Test
    fun keepsCommasInsideQuotesAndSkipsBlankLines() {
        val rows = SpreadsheetReader.parseCsvText("1001,\"الاسم, الكامل\",Ali\n\n1002,سارة,Sara")
        assertEquals(2, rows.size)
        assertEquals("الاسم, الكامل", rows[0][1])
        assertEquals("سارة", rows[1][1])
    }

    @Test
    fun trimsCarriageReturnsFromWindowsFiles() {
        val rows = SpreadsheetReader.parseCsvText("1001,علي,Ali\r\n1002,سارة,Sara")
        assertEquals(2, rows.size)
        assertEquals("Ali", rows[0][2])
    }
}

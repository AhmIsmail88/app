package com.rasd.app.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * يتحقق من منطق حساب موعد التنبيه القادم: مستقبَلي، وبالساعة الصحيحة، وبيوم الأسبوع الصحيح.
 */
class PeriodAlarmSchedulerTest {

    @Test
    fun nextTriggerIsInFutureWithRequestedHour() {
        for (key in listOf("1", "2", "3", "4", "5", "6", "7")) {
            for (hour in listOf(0, 7, 12, 23)) {
                val at = PeriodAlarmScheduler.nextTriggerAt(key, hour)
                val dt = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDateTime()
                assertTrue(
                    "should be in the future (key=$key hour=$hour)",
                    dt.isAfter(LocalDateTime.now().minusMinutes(1))
                )
                assertEquals("hour mismatch (key=$key)", hour.toLong(), dt.hour.toLong())
                assertEquals("minutes must be zero", 0L, dt.minute.toLong())
                assertEquals("seconds must be zero", 0L, dt.second.toLong())
            }
        }
    }

    @Test
    fun dayKeyMapsToCorrectWeekday() {
        val expected = mapOf(
            "1" to DayOfWeek.SATURDAY,
            "2" to DayOfWeek.SUNDAY,
            "3" to DayOfWeek.MONDAY,
            "4" to DayOfWeek.TUESDAY,
            "5" to DayOfWeek.WEDNESDAY,
            "6" to DayOfWeek.THURSDAY,
            "7" to DayOfWeek.FRIDAY
        )
        for ((key, day) in expected) {
            val dt = Instant.ofEpochMilli(PeriodAlarmScheduler.nextTriggerAt(key, 10))
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            assertEquals("weekday mismatch for key=$key", day, dt.dayOfWeek)
        }
    }
}

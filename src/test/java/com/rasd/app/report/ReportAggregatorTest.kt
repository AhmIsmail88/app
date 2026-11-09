package com.rasd.app.report

import com.rasd.app.data.model.AssessmentScoreEntity
import com.rasd.app.data.model.AttendanceRecordEntity
import com.rasd.app.data.model.BehaviorEventEntity
import com.rasd.app.data.model.BehaviorTypeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات حساب التقارير: نسب الحضور، نقاط السلوك، ومتوسطات الدرجات.
 */
class ReportAggregatorTest {

    private fun att(status: String) = AttendanceRecordEntity(
        studentId = 1, classId = 1, date = "2026-09-10", status = status
    )

    private fun type(id: Long, category: String, points: Float) = BehaviorTypeEntity(
        id = id, nameArabic = "t$id", nameEnglish = "t$id", category = category, defaultPoints = points
    )

    private fun event(typeId: Long, points: Float?) = BehaviorEventEntity(
        studentId = 1, classId = 1, behaviorTypeId = typeId, date = "2026-09-10", points = points
    )

    private fun score(value: Float, max: Float) = AssessmentScoreEntity(
        assessmentId = 1, studentId = 1, score = value, maximumScore = max, status = "completed"
    )

    @Test
    fun attendancePercentCountsPresentAndLateOverAllMarked() {
        val c = ReportAggregator.attendanceCounts(
            listOf(att("present"), att("present"), att("late"), att("absent"))
        )
        assertEquals(2, c.present)
        assertEquals(1, c.late)
        assertEquals(1, c.absent)
        assertEquals(4, c.marked)
        assertEquals(75, c.percent) // (2 حاضر + 1 متأخر) ÷ 4
    }

    @Test
    fun attendancePercentIsZeroWhenNothingMarked() {
        val c = ReportAggregator.attendanceCounts(emptyList())
        assertEquals(0, c.marked)
        assertEquals(0, c.percent)
    }

    @Test
    fun otherRecordedStatusesCountInTheDenominator() {
        val c = ReportAggregator.attendanceCounts(listOf(att("present"), att("excused")))
        assertEquals(2, c.marked)
        assertEquals(50, c.percent)
    }

    @Test
    fun behaviorPointsUseEventValueThenTypeDefault() {
        val types = mapOf(1L to type(1, "positive", 2f), 2L to type(2, "negative", -1f))
        val events = listOf(
            event(1, 5f),
            event(1, null),
            event(2, null)
        )
        assertEquals(6.0, ReportAggregator.behaviorPoints(events, types), 0.001)
    }

    @Test
    fun behaviorPointsAreZeroWhenTypeIsMissingAndNoExplicitPoints() {
        assertEquals(0.0, ReportAggregator.behaviorPoints(listOf(event(99, null)), emptyMap()), 0.001)
    }

    @Test
    fun behaviorCountsSplitByCategory() {
        val types = mapOf(
            1L to type(1, "positive", 2f),
            2L to type(2, "negative", -1f),
            3L to type(3, "neutral", 0f)
        )
        val events = listOf(event(1, null), event(1, null), event(2, null), event(3, null))
        val (pos, neg) = ReportAggregator.behaviorCounts(events, types)
        assertEquals(2, pos)
        assertEquals(1, neg)
    }

    @Test
    fun scorePercentHandlesZeroMaximum() {
        assertEquals(88.0, ReportAggregator.percentOf(44f, 50f), 0.001)
        assertEquals(0.0, ReportAggregator.percentOf(10f, 0f), 0.001)
    }

    @Test
    fun averagePercentOfScores() {
        val scores = listOf(score(44f, 50f), score(20f, 20f), score(5f, 10f))
        assertEquals(79.333, ReportAggregator.averagePercent(scores), 0.01)
    }

    @Test
    fun averagePercentIsZeroWithoutScores() {
        assertEquals(0.0, ReportAggregator.averagePercent(emptyList()), 0.001)
    }

    @Test
    fun percentIsCappedAtOneHundred() {
        val c = ReportAggregator.attendanceCounts(listOf(att("present"), att("late")))
        assertTrue(c.percent <= 100)
        assertEquals(100, c.percent)
    }
}

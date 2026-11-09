package com.rasd.app.report

import com.rasd.app.data.model.AssessmentEntity
import com.rasd.app.data.model.AssessmentScoreEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات تجميع درجات الفصول الدراسية (أعمال السنة / نصف العام / نهاية العام).
 */
class TermGradesTest {

    private fun assessment(id: Long, term: String, max: Float) = AssessmentEntity(
        id = id,
        classId = 1,
        name = "a$id",
        type = "exam",
        date = "2026-10-01",
        maximumScore = max,
        term = term
    )

    private fun score(aid: Long, sid: Long, value: Float, max: Float) = AssessmentScoreEntity(
        assessmentId = aid,
        studentId = sid,
        score = value,
        maximumScore = max,
        status = "completed"
    )

    private val assessments = mapOf(
        1L to assessment(1, TermGrades.TERMLY, 20f),
        2L to assessment(2, TermGrades.TERMLY, 30f),
        3L to assessment(3, TermGrades.MIDYEAR, 50f),
        4L to assessment(4, TermGrades.FINAL, 100f)
    )

    @Test
    fun percentUsesSumOfMaximumsPerTerm() {
        val scores = listOf(score(1, 1, 15f, 20f), score(2, 1, 24f, 30f)) // 39/50
        assertEquals(78.0, TermGrades.percentOf(scores, assessments), 0.001)
    }

    @Test
    fun noScoresIsZeroPercent() {
        assertEquals(0.0, TermGrades.percentOf(emptyList(), assessments), 0.001)
    }

    @Test
    fun zeroMaximumIsZeroPercent() {
        val scores = listOf(score(9, 1, 10f, 0f))
        assertEquals(0.0, TermGrades.percentOf(scores, emptyMap()), 0.001)
    }

    @Test
    fun percentIsCappedAtOneHundred() {
        val scores = listOf(score(1, 1, 30f, 20f)) // over the maximum, must not exceed 100
        assertEquals(100.0, TermGrades.percentOf(scores, assessments), 0.001)
    }

    @Test
    fun summaryGroupsByTermInOrder() {
        val scores = listOf(score(4, 1, 70f, 100f), score(1, 1, 10f, 20f), score(3, 1, 40f, 50f))
        val lines = TermGrades.summaryOf(scores, assessments)
        assertEquals(3, lines.size)
        assertEquals(TermGrades.TERMLY, lines[0].term)
        assertEquals(50.0, lines[0].percent, 0.001)
        assertEquals(1, lines[0].count)
        assertEquals(TermGrades.MIDYEAR, lines[1].term)
        assertEquals(80.0, lines[1].percent, 0.001)
        assertEquals(TermGrades.FINAL, lines[2].term)
        assertEquals(70.0, lines[2].percent, 0.001)
    }

    @Test
    fun summarySkipsTermsWithoutScores() {
        val scores = listOf(score(1, 1, 10f, 20f), score(3, 1, 25f, 50f))
        val lines = TermGrades.summaryOf(scores, assessments)
        assertEquals(2, lines.size)
        assertTrue(lines.none { it.term == TermGrades.FINAL })
    }

    @Test
    fun totalsAndMaximumsAreSummedPerTerm() {
        val scores = listOf(score(1, 1, 15f, 20f), score(2, 1, 24f, 30f))
        val line = TermGrades.summaryOf(scores, assessments).first()
        assertEquals(39.0, line.total, 0.001)
        assertEquals(50.0, line.maximum, 0.001)
    }

    @Test
    fun unknownOrMissingTermFallsBackToTermWork() {
        assertEquals(TermGrades.TERMLY, TermGrades.termOf(null))
        assertEquals(TermGrades.TERMLY, TermGrades.termOf("weird"))
        assertEquals(TermGrades.MIDYEAR, TermGrades.termOf("midyear"))
        assertEquals(TermGrades.FINAL, TermGrades.termOf("final"))
    }
}

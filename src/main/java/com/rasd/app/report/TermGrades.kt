package com.rasd.app.report

import com.rasd.app.data.model.AssessmentEntity
import com.rasd.app.data.model.AssessmentScoreEntity

/** صف واحد من ملخّص درجات فصل دراسي لطالب. */
data class TermGradeLine(
    val term: String,
    val percent: Double,
    val count: Int,
    val total: Double,
    val maximum: Double
)

/**
 * تجميع درجات الامتحانات حسب الفصل الدراسي:
 * termly = أعمال السنة، midyear = نصف العام، final = نهاية العام.
 *
 * دالة نقية (بلا أندرويد) لذا تُختبر مباشرة.
 */
object TermGrades {

    const val TERMLY = "termly"
    const val MIDYEAR = "midyear"
    const val FINAL = "final"

    val ORDER = listOf(TERMLY, MIDYEAR, FINAL)

    fun termOf(value: String?): String =
        when (value?.trim()?.lowercase()) {
            MIDYEAR, "mid_term", "midterm", "half" -> MIDYEAR
            FINAL, "final_year", "year_end", "annual" -> FINAL
            else -> TERMLY
        }

    private fun maximumOf(score: AssessmentScoreEntity, assessments: Map<Long, AssessmentEntity>): Double {
        val fromAssessment = assessments[score.assessmentId]?.maximumScore ?: 0f
        val m = if (fromAssessment > 0f) fromAssessment else score.maximumScore
        return m.toDouble()
    }

    /** النسبة المئوية = مجموع الدرجات ÷ مجموع العلامات العظمى، مقيّدة بين 0 و 100. */
    fun percentOf(scores: List<AssessmentScoreEntity>, assessments: Map<Long, AssessmentEntity>): Double {
        var total = 0.0
        var maximum = 0.0
        scores.forEach { s ->
            total += s.score.toDouble()
            maximum += maximumOf(s, assessments)
        }
        if (maximum <= 0.0) return 0.0
        return (total / maximum * 100.0).coerceIn(0.0, 100.0)
    }

    /** ملخّص كل فصل دراسي له درجات مسجّلة، مرتّباً: أعمال السنة ثم نصف العام ثم نهاية العام. */
    fun summaryOf(
        scores: List<AssessmentScoreEntity>,
        assessments: Map<Long, AssessmentEntity>
    ): List<TermGradeLine> = ORDER.mapNotNull { term ->
        val termScores = scores.filter { termOf(assessments[it.assessmentId]?.term) == term }
        if (termScores.isEmpty()) {
            null
        } else {
            var maximum = 0.0
            termScores.forEach { maximum += maximumOf(it, assessments) }
            TermGradeLine(
                term = term,
                percent = percentOf(termScores, assessments),
                count = termScores.size,
                total = termScores.sumOf { it.score.toDouble() },
                maximum = maximum
            )
        }
    }
}

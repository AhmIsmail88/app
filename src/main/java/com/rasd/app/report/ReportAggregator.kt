package com.rasd.app.report

import com.rasd.app.data.model.AssessmentScoreEntity
import com.rasd.app.data.model.AttendanceRecordEntity
import com.rasd.app.data.model.BehaviorEventEntity
import com.rasd.app.data.model.BehaviorTypeEntity

/**
 * حسابات التقارير كدوال نقية (بلا سياق أندرويد) — تُختبر مباشرة.
 *
 * اصطلاحات الحساب:
 * - نسبة الحضور = (حاضر + متأخر) ÷ عدد السجلات المُسجَّلة × 100.
 *   أي أن أي حالة أخرى مُسجَّلة (كالإذن) تُحتسب في المقام وتخفض النسبة.
 * - نقاط السلوك = نقاط الحدث إن وُجدت، وإلا النقاط الافتراضية لنوعه، وإلا صفر.
 * - نسبة الدرجة = الدرجة ÷ العلامة العظمى × 100 (وصفر إن كانت العظمى غير موجبة).
 * - المتوسط = متوسط النسب المئوية للدرجات، وصفر إن لم توجد درجات.
 */
object ReportAggregator {

    data class AttendanceCounts(val present: Int, val absent: Int, val late: Int, val marked: Int) {
        val percent: Int
            get() = if (marked > 0) ((present + late) * 100.0 / marked).toInt().coerceIn(0, 100) else 0
    }

    fun attendanceCounts(records: List<AttendanceRecordEntity>): AttendanceCounts {
        val present = records.count { it.status == "present" }
        val absent = records.count { it.status == "absent" }
        val late = records.count { it.status == "late" }
        return AttendanceCounts(present, absent, late, records.size)
    }

    fun behaviorPoints(
        events: List<BehaviorEventEntity>,
        types: Map<Long, BehaviorTypeEntity>
    ): Double = events.sumOf { e ->
        (e.points ?: types[e.behaviorTypeId]?.defaultPoints ?: 0f).toDouble()
    }

    /** يعيد (عدد الإيجابي، عدد السلبي) بحسب تصنيف النوع. */
    fun behaviorCounts(
        events: List<BehaviorEventEntity>,
        types: Map<Long, BehaviorTypeEntity>
    ): Pair<Int, Int> {
        val positive = events.count { types[it.behaviorTypeId]?.category == "positive" }
        val negative = events.count { types[it.behaviorTypeId]?.category == "negative" }
        return positive to negative
    }

    fun percentOf(score: Float, maximum: Float): Double =
        if (maximum > 0f) score * 100.0 / maximum else 0.0

    fun averagePercent(scores: List<AssessmentScoreEntity>): Double =
        if (scores.isEmpty()) 0.0 else scores.map { percentOf(it.score, it.maximumScore) }.average()
}

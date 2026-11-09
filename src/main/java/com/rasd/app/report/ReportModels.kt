package com.rasd.app.report

data class ReportHeaderInfo(
    val schoolName: String,
    val teacherName: String,
    val academicYear: String,
    val fromDate: String,
    val toDate: String,
    val logoPath: String,
    val language: String = "ar"
)

data class ClassReportRow(
    val index: Int,
    val studentName: String,
    val studentNumber: String,
    val present: Int,
    val absent: Int,
    val late: Int,
    val attendancePercent: Int,
    val behaviorPoints: Double,
    val behaviorSummary: String,
    val assessmentAverage: Double,
    val assessmentsCount: Int,
    /** نسبة كل فصل دراسي (termly/midyear/final) محسوبة من كل درجات العام؛ غياب المفتاح = لا درجات. */
    val termPercents: Map<String, Int> = emptyMap()
)

data class ClassReport(
    val header: ReportHeaderInfo,
    val className: String,
    val rows: List<ClassReportRow>
)

data class StudentAssessmentLine(
    val assessmentName: String,
    val type: String,
    val date: String,
    val score: Double,
    val maximum: Double,
    val percent: Double
)

data class StudentBehaviorLine(
    val date: String,
    val typeName: String,
    val category: String,
    val points: Double,
    val note: String
)

data class StudentReport(
    val header: ReportHeaderInfo,
    val studentName: String,
    val studentNumber: String,
    val className: String,
    val present: Int,
    val absent: Int,
    val late: Int,
    val attendancePercent: Int,
    val behaviorPoints: Double,
    val assessmentAverage: Double,
    val assessments: List<StudentAssessmentLine>,
    val behaviors: List<StudentBehaviorLine>,
    /** ملخّص درجات الفصول الدراسية: أعمال السنة / نصف العام / نهاية العام. */
    val termGrades: List<TermGradeLine> = emptyList()
)

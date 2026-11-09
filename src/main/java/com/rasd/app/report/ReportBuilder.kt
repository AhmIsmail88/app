package com.rasd.app.report

import com.rasd.app.data.dao.AssessmentDao
import com.rasd.app.data.dao.AssessmentScoreDao
import com.rasd.app.data.dao.AttendanceRecordDao
import com.rasd.app.data.dao.BehaviorEventDao
import com.rasd.app.data.dao.BehaviorTypeDao
import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.dao.StudentDao
import com.rasd.app.data.model.AppSettingsEntity

class ReportBuilder(
    private val classDao: ClassDao,
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceRecordDao,
    private val assessmentDao: AssessmentDao,
    private val scoreDao: AssessmentScoreDao,
    private val behaviorDao: BehaviorEventDao,
    private val behaviorTypeDao: BehaviorTypeDao
) {

    companion object {
        /** نطاق مفتوح لقراءة كل درجات العام بغضّ النظر عن مدة التقرير المختارة. */
        private const val ALL_FROM = "0000-01-01"
        private const val ALL_TO = "9999-12-31"
    }

    private suspend fun headerInfo(
        from: String,
        to: String,
        settings: AppSettingsEntity,
        language: String
    ): ReportHeaderInfo = ReportHeaderInfo(
        schoolName = settings.schoolName,
        teacherName = settings.teacherName,
        academicYear = settings.academicYear,
        fromDate = from,
        toDate = to,
        logoPath = settings.logoPath,
        language = language
    )

    suspend fun buildClassReport(
        classId: Long,
        from: String,
        to: String,
        settings: AppSettingsEntity,
        language: String
    ): ClassReport {
        val className = classDao.getClassById(classId)?.name ?: ""
        val students = studentDao.getStudentsByClassOnce(classId)
        val attendance = attendanceDao.getAttendanceInRange(classId, from, to)
        val scores = scoreDao.getScoresInRange(classId, from, to)
        val behaviors = behaviorDao.getBehaviorInRange(classId, from, to)
        val types = behaviorTypeDao.getAllOnce().associateBy { it.id }

        // درجات الفصول الدراسية تُحسب من كل درجات العام لا من مدة التقرير فقط.
        val allScores = scoreDao.getScoresInRange(classId, ALL_FROM, ALL_TO)
        val allAssessments = assessmentDao.getAssessmentsInRange(classId, ALL_FROM, ALL_TO).associateBy { it.id }

        val rows = students.mapIndexed { i, st ->
            val counts = ReportAggregator.attendanceCounts(attendance.filter { it.studentId == st.id })
            val beh = behaviors.filter { it.studentId == st.id }
            val (pos, neg) = ReportAggregator.behaviorCounts(beh, types)
            val stScores = scores.filter { it.studentId == st.id }

            ClassReportRow(
                index = i + 1,
                studentName = st.arabicName,
                studentNumber = st.studentNumber,
                present = counts.present,
                absent = counts.absent,
                late = counts.late,
                attendancePercent = counts.percent,
                behaviorPoints = ReportAggregator.behaviorPoints(beh, types),
                behaviorSummary = ReportTexts.posNeg(language, pos, neg),
                assessmentAverage = ReportAggregator.averagePercent(stScores),
                assessmentsCount = stScores.size,
                termPercents = TermGrades.summaryOf(allScores.filter { it.studentId == st.id }, allAssessments)
                    .associate { it.term to it.percent.toInt() }
            )
        }
        return ClassReport(
            header = headerInfo(from, to, settings, language),
            className = className,
            rows = rows
        )
    }

    suspend fun buildStudentReport(
        studentId: Long,
        classId: Long,
        from: String,
        to: String,
        settings: AppSettingsEntity,
        language: String
    ): StudentReport {
        val className = classDao.getClassById(classId)?.name ?: ""
        val st = studentDao.getStudentById(studentId)

        val counts = ReportAggregator.attendanceCounts(
            attendanceDao.getAttendanceInRange(classId, from, to).filter { it.studentId == studentId }
        )

        val types = behaviorTypeDao.getAllOnce().associateBy { it.id }
        val behaviors = behaviorDao.getStudentBehaviorInRange(studentId, from, to)

        val assessmentsMap = assessmentDao.getAssessmentsInRange(classId, from, to).associateBy { it.id }
        val scores = scoreDao.getScoresInRange(classId, from, to).filter { it.studentId == studentId }

        val lines = scores.mapNotNull { s ->
            val a = assessmentsMap[s.assessmentId] ?: return@mapNotNull null
            StudentAssessmentLine(
                assessmentName = a.name,
                type = a.type,
                date = a.date,
                score = s.score.toDouble(),
                maximum = s.maximumScore.toDouble(),
                percent = ReportAggregator.percentOf(s.score, s.maximumScore)
            )
        }.sortedByDescending { it.date }

        val behLines = behaviors.map { e ->
            val t = types[e.behaviorTypeId]
            StudentBehaviorLine(
                date = e.date,
                typeName = t?.nameArabic ?: "-",
                category = t?.category ?: "",
                points = (e.points ?: t?.defaultPoints ?: 0f).toDouble(),
                note = e.note
            )
        }

        val allAssessments = assessmentDao.getAssessmentsInRange(classId, ALL_FROM, ALL_TO).associateBy { it.id }
        val termGrades = TermGrades.summaryOf(scoreDao.getStudentAllScores(studentId, classId), allAssessments)

        return StudentReport(
            header = headerInfo(from, to, settings, language),
            studentName = st?.arabicName ?: "",
            studentNumber = st?.studentNumber ?: "",
            className = className,
            present = counts.present,
            absent = counts.absent,
            late = counts.late,
            attendancePercent = counts.percent,
            behaviorPoints = ReportAggregator.behaviorPoints(behaviors, types),
            assessmentAverage = ReportAggregator.averagePercent(scores),
            assessments = lines,
            behaviors = behLines,
            termGrades = termGrades
        )
    }
}

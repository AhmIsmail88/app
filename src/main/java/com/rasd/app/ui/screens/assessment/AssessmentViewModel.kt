package com.rasd.app.ui.screens.assessment

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasd.app.R
import com.rasd.app.data.dao.AssessmentDao
import com.rasd.app.data.dao.AssessmentScoreDao
import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.dao.StudentDao
import com.rasd.app.data.importer.GradeImportMapper
import com.rasd.app.data.model.AssessmentEntity
import com.rasd.app.data.model.AssessmentScoreEntity
import com.rasd.app.data.model.ClassEntity
import com.rasd.app.data.model.StudentEntity
import com.rasd.app.report.SpreadsheetReader
import com.rasd.app.report.TermGrades
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AssessmentViewModel @Inject constructor(
    private val classDao: ClassDao,
    private val studentDao: StudentDao,
    private val assessmentDao: AssessmentDao,
    private val scoreDao: AssessmentScoreDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val classes: StateFlow<List<ClassEntity>> = classDao.getAllActiveClasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _students = MutableStateFlow<List<StudentEntity>>(emptyList())
    val students: StateFlow<List<StudentEntity>> = _students.asStateFlow()

    private val _assessments = MutableStateFlow<List<AssessmentEntity>>(emptyList())
    val assessments: StateFlow<List<AssessmentEntity>> = _assessments.asStateFlow()

    private val _selectedClassId = MutableStateFlow<Long?>(null)
    val selectedClassId: StateFlow<Long?> = _selectedClassId.asStateFlow()

    private val _selectedAssessmentId = MutableStateFlow<Long?>(null)
    val selectedAssessmentId: StateFlow<Long?> = _selectedAssessmentId.asStateFlow()

    private val _scores = MutableStateFlow<Map<Long, String>>(emptyMap())
    val scores: StateFlow<Map<Long, String>> = _scores.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** فلتر الفصل الدراسي للقوائم: "" = الكل، وإلا termly/midyear/final. */
    private val _termFilter = MutableStateFlow("")
    val termFilter: StateFlow<String> = _termFilter.asStateFlow()

    val filteredAssessments: StateFlow<List<AssessmentEntity>> =
        combine(_assessments, _termFilter) { list, term ->
            if (term.isBlank()) list else list.filter { TermGrades.termOf(it.term) == term }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** صفوف ملف الدرجات بانتظار مطابقة الأعمدة. */
    private val _gradePreview = MutableStateFlow<List<List<String>>>(emptyList())
    val gradePreview: StateFlow<List<List<String>>> = _gradePreview.asStateFlow()

    fun setTermFilter(term: String) { _termFilter.value = term }

    fun selectClass(id: Long) {
        _selectedClassId.value = id
        _selectedAssessmentId.value = null
        _scores.value = emptyMap()
        viewModelScope.launch {
            _students.value = studentDao.getStudentsByClassOnce(id)
            _assessments.value = assessmentDao.getAssessmentsInRange(id, "0000-01-01", "9999-12-31")
        }
    }

    fun createAssessment(name: String, type: String, date: String, maxScore: String, term: String) {
        val cid = _selectedClassId.value
        if (cid == null) {
            _message.value = appContext.getString(R.string.msg_select_class_first)
            return
        }
        if (name.isBlank()) {
            _message.value = appContext.getString(R.string.msg_assessment_name_required)
            return
        }
        val max = maxScore.toFloatOrNull() ?: 100f
        viewModelScope.launch {
            assessmentDao.insertAssessment(
                AssessmentEntity(
                    classId = cid,
                    name = name.trim(),
                    type = if (type.isBlank()) "quiz" else type.trim(),
                    date = if (date.isBlank()) LocalDate.now().toString() else date,
                    maximumScore = max,
                    term = TermGrades.termOf(term)
                )
            )
            _assessments.value = assessmentDao.getAssessmentsInRange(cid, "0000-01-01", "9999-12-31")
            _message.value = appContext.getString(R.string.msg_assessment_created)
        }
    }

    fun selectAssessment(id: Long) {
        _selectedAssessmentId.value = id
        viewModelScope.launch {
            val existing = scoreDao.getScoresForAssessment(id)
            _scores.value = existing.associate { it.studentId to it.score.toString() }
        }
    }

    fun setScore(studentId: Long, value: String) {
        _scores.value = _scores.value + (studentId to value)
    }

    fun saveScores() {
        val aid = _selectedAssessmentId.value
        if (aid == null) {
            _message.value = appContext.getString(R.string.msg_pick_assessment_first)
            return
        }
        val assessment = _assessments.value.firstOrNull { it.id == aid }
        val max = assessment?.maximumScore ?: 100f
        viewModelScope.launch {
            var count = 0
            _scores.value.forEach { (sid, raw) ->
                val value = raw.toFloatOrNull() ?: return@forEach
                val existing = scoreDao.getScoreForStudent(aid, sid)
                if (existing == null) {
                    scoreDao.insertAssessmentScore(
                        AssessmentScoreEntity(
                            assessmentId = aid,
                            studentId = sid,
                            score = value,
                            maximumScore = max,
                            status = "completed"
                        )
                    )
                } else {
                    scoreDao.updateAssessmentScore(
                        existing.copy(score = value, maximumScore = max, updatedAt = System.currentTimeMillis())
                    )
                }
                count++
            }
            _message.value = appContext.getString(R.string.msg_scores_saved_fmt, count)
        }
    }

    /** يقرأ ملف الدرجات فقط ويعرض معاينته لمطابقة الأعمدة. */
    fun parseGradeFile(uri: Uri) {
        if (_selectedAssessmentId.value == null) {
            _message.value = appContext.getString(R.string.msg_pick_assessment_first)
            return
        }
        viewModelScope.launch {
            val rows = withContext(Dispatchers.IO) {
                runCatching { SpreadsheetReader.read(appContext, uri) }.getOrDefault(emptyList())
            }
            if (rows.isEmpty()) {
                _message.value = appContext.getString(R.string.msg_no_rows)
                return@launch
            }
            _gradePreview.value = rows
        }
    }

    fun cancelGradeImport() { _gradePreview.value = emptyList() }

    /**
     * يستورد الدرجات للامتحان المختار بحسب الأعمدة التي اختارها المستخدم.
     * @param numberIdx / arabicIdx / englishIdx أعمدة تعريف الطالب (-1 للتجاهل)
     * @param scoreIdx عمود الدرجة (إلزامي)
     */
    fun importGradesWithMapping(
        numberIdx: Int,
        arabicIdx: Int,
        englishIdx: Int,
        scoreIdx: Int,
        skipHeader: Boolean
    ) {
        val aid = _selectedAssessmentId.value
        if (aid == null) {
            _message.value = appContext.getString(R.string.msg_pick_assessment_first)
            return
        }
        val assessment = _assessments.value.firstOrNull { it.id == aid }
        val max = assessment?.maximumScore ?: 100f
        val result = GradeImportMapper.map(
            rows = _gradePreview.value,
            students = _students.value,
            numberIdx = numberIdx,
            arabicIdx = arabicIdx,
            englishIdx = englishIdx,
            scoreIdx = scoreIdx,
            skipHeader = skipHeader,
            maximumScore = max
        )
        if (result.scores.isEmpty()) {
            _message.value = appContext.getString(R.string.msg_no_valid_rows)
            return
        }
        viewModelScope.launch {
            result.scores.forEach { (sid, value) ->
                val existing = scoreDao.getScoreForStudent(aid, sid)
                if (existing == null) {
                    scoreDao.insertAssessmentScore(
                        AssessmentScoreEntity(
                            assessmentId = aid,
                            studentId = sid,
                            score = value,
                            maximumScore = max,
                            status = "completed"
                        )
                    )
                } else {
                    scoreDao.updateAssessmentScore(
                        existing.copy(score = value, maximumScore = max, updatedAt = System.currentTimeMillis())
                    )
                }
            }
            _scores.value = scoreDao.getScoresForAssessment(aid).associate { it.studentId to it.score.toString() }
            _gradePreview.value = emptyList()
            _message.value = appContext.getString(
                R.string.msg_grades_imported_fmt,
                result.scores.size,
                result.unmatched,
                result.invalid
            )
        }
    }

    fun consumeMessage() { _message.value = null }
}

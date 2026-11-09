package com.rasd.app.ui.screens.students

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasd.app.R
import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.dao.StudentDao
import com.rasd.app.data.model.ClassEntity
import com.rasd.app.data.importer.StudentImportMapper
import com.rasd.app.data.model.StudentEntity
import com.rasd.app.report.SpreadsheetReader
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
import javax.inject.Inject

@HiltViewModel
class StudentListViewModel @Inject constructor(
    private val classDao: ClassDao,
    private val studentDao: StudentDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val classes: StateFlow<List<ClassEntity>> = classDao.getAllActiveClasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _students = MutableStateFlow<List<StudentEntity>>(emptyList())
    val students: StateFlow<List<StudentEntity>> = _students.asStateFlow()

    private val _selectedClassId = MutableStateFlow<Long?>(null)
    val selectedClassId: StateFlow<Long?> = _selectedClassId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** صفوف الملف المُحمَّل بانتظار مطابقة الأعمدة. */
    private val _importPreview = MutableStateFlow<List<List<String>>>(emptyList())
    val importPreview: StateFlow<List<List<String>>> = _importPreview.asStateFlow()

    val filteredStudents: StateFlow<List<StudentEntity>> = combine(_students, _query) { list, q ->
        if (q.isBlank()) list else list.filter {
            it.arabicName.contains(q, ignoreCase = true) ||
                it.englishName.contains(q, ignoreCase = true) ||
                it.studentNumber.contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setQuery(value: String) { _query.value = value }

    fun selectClass(id: Long) {
        _selectedClassId.value = id
        reload()
    }

    private fun reload() {
        val cid = _selectedClassId.value ?: return
        viewModelScope.launch { _students.value = studentDao.getStudentsByClassOnce(cid) }
    }

    fun addStudent(number: String, arabicName: String, englishName: String) {
        val cid = _selectedClassId.value
        if (cid == null) {
            _message.value = appContext.getString(R.string.msg_select_class_first)
            return
        }
        if (arabicName.isBlank()) {
            _message.value = appContext.getString(R.string.msg_student_name_required)
            return
        }
        viewModelScope.launch {
            studentDao.insertStudent(
                StudentEntity(
                    classId = cid,
                    studentNumber = number.trim(),
                    arabicName = arabicName.trim(),
                    englishName = englishName.trim()
                )
            )
            _message.value = appContext.getString(R.string.msg_student_added)
            reload()
        }
    }

    fun updateStudent(entity: StudentEntity) {
        if (entity.arabicName.isBlank()) {
            _message.value = appContext.getString(R.string.msg_arabic_name_required)
            return
        }
        viewModelScope.launch {
            studentDao.updateStudent(entity.copy(updatedAt = System.currentTimeMillis()))
            _message.value = appContext.getString(R.string.msg_student_updated)
            reload()
        }
    }

    fun deleteStudent(entity: StudentEntity) {
        viewModelScope.launch {
            studentDao.deleteStudent(entity)
            _message.value = appContext.getString(R.string.msg_student_deleted)
            reload()
        }
    }

    /** يقرأ الملف فقط ويعرض معاينته لمطابقة الأعمدة. */
    fun parseImportFile(uri: Uri) {
        if (_selectedClassId.value == null) {
            _message.value = appContext.getString(R.string.msg_select_class_first)
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
            _importPreview.value = rows
        }
    }

    fun cancelImport() {
        _importPreview.value = emptyList()
    }

    /**
     * ينفّذ الاستيراد بحسب الأعمدة التي اختارها المستخدم.
     * @param numberIdx عمود رقم الطالب (أو -1 للتجاهل)
     * @param arabicIdx عمود الاسم بالعربية (إلزامي)
     * @param englishIdx عمود الاسم بالإنجليزية (أو -1 للتجاهل)
     */
    fun importWithMapping(numberIdx: Int, arabicIdx: Int, englishIdx: Int, skipHeader: Boolean) {
        val cid = _selectedClassId.value
        if (cid == null) {
            _message.value = appContext.getString(R.string.msg_select_class_first)
            return
        }
        val students = StudentImportMapper.map(_importPreview.value, cid, numberIdx, arabicIdx, englishIdx, skipHeader)
        if (students.isEmpty()) {
            _message.value = appContext.getString(R.string.msg_no_valid_rows)
            return
        }
        viewModelScope.launch {
            studentDao.insertStudents(students)
            _message.value = appContext.getString(R.string.msg_imported_fmt, students.size)
            _importPreview.value = emptyList()
            reload()
        }
    }

    fun consumeMessage() { _message.value = null }
}

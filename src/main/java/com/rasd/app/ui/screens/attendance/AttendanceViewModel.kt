package com.rasd.app.ui.screens.attendance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasd.app.R
import com.rasd.app.data.dao.AttendanceRecordDao
import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.dao.StudentDao
import com.rasd.app.data.model.AttendanceRecordEntity
import com.rasd.app.data.model.ClassEntity
import com.rasd.app.data.model.StudentEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val classDao: ClassDao,
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceRecordDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val classes: StateFlow<List<ClassEntity>> = classDao.getAllActiveClasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _students = MutableStateFlow<List<StudentEntity>>(emptyList())
    val students: StateFlow<List<StudentEntity>> = _students.asStateFlow()

    private val _statuses = MutableStateFlow<Map<Long, String>>(emptyMap())
    val statuses: StateFlow<Map<Long, String>> = _statuses.asStateFlow()

    private val _selectedClassId = MutableStateFlow<Long?>(null)
    val selectedClassId: StateFlow<Long?> = _selectedClassId.asStateFlow()

    private val _date = MutableStateFlow(LocalDate.now().toString())
    val date: StateFlow<String> = _date.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

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
        load()
    }

    fun setDate(value: String) {
        _date.value = value
        load()
    }

    private fun load() {
        val cid = _selectedClassId.value ?: return
        viewModelScope.launch {
            _students.value = studentDao.getStudentsByClassOnce(cid)
            val existing = attendanceDao.getForClassDate(cid, _date.value)
            _statuses.value = existing.associate { it.studentId to it.status }
        }
    }

    fun setStatus(studentId: Long, status: String) {
        _statuses.value = _statuses.value + (studentId to status)
    }

    fun markAll(status: String) {
        _statuses.value = _students.value.associate { it.id to status }
    }

    fun save() {
        val cid = _selectedClassId.value
        if (cid == null) {
            _message.value = appContext.getString(R.string.msg_select_class_first)
            return
        }
        if (_statuses.value.isEmpty()) {
            _message.value = appContext.getString(R.string.msg_nothing_to_save)
            return
        }
        viewModelScope.launch {
            val d = _date.value
            var count = 0
            _statuses.value.forEach { (sid, st) ->
                val existing = attendanceDao.getAttendanceForStudent(sid, cid, d)
                if (existing == null) {
                    attendanceDao.insertAttendance(
                        AttendanceRecordEntity(studentId = sid, classId = cid, date = d, status = st)
                    )
                } else {
                    attendanceDao.updateAttendance(existing.copy(status = st, updatedAt = System.currentTimeMillis()))
                }
                count++
            }
            _message.value = appContext.getString(R.string.msg_attendance_saved, count)
        }
    }

    fun consumeMessage() { _message.value = null }
}

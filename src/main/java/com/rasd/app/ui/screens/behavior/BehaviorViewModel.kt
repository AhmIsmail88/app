package com.rasd.app.ui.screens.behavior

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasd.app.R
import com.rasd.app.data.dao.BehaviorEventDao
import com.rasd.app.data.dao.BehaviorTypeDao
import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.dao.StudentDao
import com.rasd.app.data.model.BehaviorEventEntity
import com.rasd.app.data.model.BehaviorTypeEntity
import com.rasd.app.data.model.ClassEntity
import com.rasd.app.data.model.StudentEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BehaviorViewModel @Inject constructor(
    private val classDao: ClassDao,
    private val studentDao: StudentDao,
    private val behaviorTypeDao: BehaviorTypeDao,
    private val behaviorEventDao: BehaviorEventDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val classes: StateFlow<List<ClassEntity>> = classDao.getAllActiveClasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val types: StateFlow<List<BehaviorTypeEntity>> = behaviorTypeDao.getAllBehaviorTypes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _students = MutableStateFlow<List<StudentEntity>>(emptyList())
    val students: StateFlow<List<StudentEntity>> = _students.asStateFlow()

    private val _selectedClassId = MutableStateFlow<Long?>(null)
    val selectedClassId: StateFlow<Long?> = _selectedClassId.asStateFlow()

    private val _selectedStudentId = MutableStateFlow<Long?>(null)
    val selectedStudentId: StateFlow<Long?> = _selectedStudentId.asStateFlow()

    private val _selectedTypeId = MutableStateFlow<Long?>(null)
    val selectedTypeId: StateFlow<Long?> = _selectedTypeId.asStateFlow()

    private val _eventsForType = MutableStateFlow(0)
    val eventsForType: StateFlow<Int> = _eventsForType.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectClass(id: Long) {
        _selectedClassId.value = id
        _selectedStudentId.value = null
        viewModelScope.launch { _students.value = studentDao.getStudentsByClassOnce(id) }
    }

    fun selectStudent(id: Long) { _selectedStudentId.value = id }

    fun selectType(id: Long) { _selectedTypeId.value = id }

    fun loadEventCount(typeId: Long) {
        viewModelScope.launch { _eventsForType.value = behaviorEventDao.countEventsForType(typeId) }
    }

    private fun defaults(): List<Triple<String, String, Float>> = listOf(
        Triple(appContext.getString(R.string.bt_great_participation), "positive", 2f),
        Triple(appContext.getString(R.string.bt_excellent_homework), "positive", 1f),
        Triple(appContext.getString(R.string.bt_helped_classmate), "positive", 1f),
        Triple(appContext.getString(R.string.bt_disturbing), "negative", -1f),
        Triple(appContext.getString(R.string.bt_no_homework), "negative", -2f),
        Triple(appContext.getString(R.string.bt_late_to_class), "negative", -1f),
        Triple(appContext.getString(R.string.bt_note), "neutral", 0f)
    )

    /**
     * مزامنة الأنواع الجاهزة بأمان:
     * 1) يحذف التكرار **غير المستخدم** (نفس الاسم وبلا سجلات مرتبطة) فلا تتضاعف القائمة.
     * 2) يضيف الأنواع الجاهزة الناقصة فقط.
     */
    fun syncDefaults() {
        viewModelScope.launch {
            var removed = 0
            val seen = mutableSetOf<String>()
            behaviorTypeDao.getAllOnce().sortedBy { it.id }.forEach { t ->
                val key = t.nameArabic.trim()
                if (seen.contains(key)) {
                    if (behaviorEventDao.countEventsForType(t.id) == 0) {
                        behaviorTypeDao.deleteBehaviorType(t)
                        removed++
                    }
                } else {
                    seen.add(key)
                }
            }

            val existing = behaviorTypeDao.getAllOnce().map { it.nameArabic.trim() }.toSet()
            var added = 0
            defaults().forEach { (name, cat, pts) ->
                if (!existing.contains(name)) {
                    behaviorTypeDao.insertBehaviorType(
                        BehaviorTypeEntity(
                            nameArabic = name,
                            nameEnglish = name,
                            category = cat,
                            defaultPoints = pts,
                            isSystemDefault = true,
                            isActive = true
                        )
                    )
                    added++
                }
            }
            _message.value = if (added == 0 && removed == 0) {
                appContext.getString(R.string.msg_defaults_exist)
            } else {
                appContext.getString(R.string.msg_defaults_synced)
            }
        }
    }

    fun saveType(id: Long?, name: String, category: String, points: Float) {
        if (name.isBlank()) {
            _message.value = appContext.getString(R.string.type_name_required)
            return
        }
        viewModelScope.launch {
            if (id == null) {
                behaviorTypeDao.insertBehaviorType(
                    BehaviorTypeEntity(
                        nameArabic = name.trim(),
                        nameEnglish = name.trim(),
                        category = category,
                        defaultPoints = points,
                        isActive = true
                    )
                )
                _message.value = appContext.getString(R.string.msg_type_added)
            } else {
                val t = behaviorTypeDao.getBehaviorTypeById(id)
                if (t != null) {
                    behaviorTypeDao.updateBehaviorType(
                        t.copy(
                            nameArabic = name.trim(),
                            nameEnglish = name.trim(),
                            category = category,
                            defaultPoints = points
                        )
                    )
                    _message.value = appContext.getString(R.string.msg_type_updated)
                }
            }
        }
    }

    fun deleteType(type: BehaviorTypeEntity) {
        viewModelScope.launch {
            behaviorTypeDao.deleteBehaviorType(type)
            if (_selectedTypeId.value == type.id) _selectedTypeId.value = null
            _message.value = appContext.getString(R.string.msg_type_deleted)
        }
    }

    fun record(note: String, date: String) {
        val cid = _selectedClassId.value
        val sid = _selectedStudentId.value
        val tid = _selectedTypeId.value
        if (cid == null || sid == null || tid == null) {
            _message.value = appContext.getString(R.string.msg_behavior_missing)
            return
        }
        viewModelScope.launch {
            val type = types.value.firstOrNull { it.id == tid }
            behaviorEventDao.insertBehaviorEvent(
                BehaviorEventEntity(
                    studentId = sid,
                    classId = cid,
                    behaviorTypeId = tid,
                    date = if (date.isBlank()) LocalDate.now().toString() else date,
                    points = type?.defaultPoints,
                    note = note
                )
            )
            _message.value = appContext.getString(R.string.msg_behavior_saved)
        }
    }

    fun consumeMessage() { _message.value = null }
}

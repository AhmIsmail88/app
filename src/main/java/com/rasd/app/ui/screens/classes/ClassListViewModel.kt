package com.rasd.app.ui.screens.classes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasd.app.R
import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.model.ClassEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassListViewModel @Inject constructor(
    private val classDao: ClassDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val classes: StateFlow<List<ClassEntity>> = classDao.getAllActiveClasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun addClass(name: String, grade: String, section: String, subject: String) {
        if (name.isBlank()) {
            _message.value = appContext.getString(R.string.msg_class_name_required)
            return
        }
        viewModelScope.launch {
            classDao.insertClass(
                ClassEntity(name = name.trim(), grade = grade.trim(), section = section.trim(), subject = subject.trim())
            )
            _message.value = appContext.getString(R.string.msg_class_added)
        }
    }

    fun updateClass(entity: ClassEntity) {
        if (entity.name.isBlank()) {
            _message.value = appContext.getString(R.string.msg_class_name_required)
            return
        }
        viewModelScope.launch {
            classDao.updateClass(entity.copy(updatedAt = System.currentTimeMillis()))
            _message.value = appContext.getString(R.string.msg_class_updated)
        }
    }

    /**
     * الأرشفة تُخفي الفصل من القوائم دون حذف بيانات الطلاب — الخيار الآمن.
     */
    fun archiveClass(entity: ClassEntity) {
        viewModelScope.launch {
            classDao.updateClass(entity.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
            _message.value = appContext.getString(R.string.msg_class_archived)
        }
    }

    /**
     * حذف نهائي: يحذف الفصل وكل طلابه وسجلاتهم (CASCADE) — يُطلب تأكيد صريح في الواجهة.
     */
    fun deleteClass(entity: ClassEntity) {
        viewModelScope.launch {
            classDao.deleteClass(entity)
            _message.value = appContext.getString(R.string.msg_class_deleted)
        }
    }

    fun consumeMessage() { _message.value = null }
}

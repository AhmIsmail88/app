package com.rasd.app.ui.screens.addPeriod

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasd.app.R
import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.dao.ClassPeriodDao
import com.rasd.app.data.model.ClassEntity
import com.rasd.app.data.model.ClassPeriodEntity
import com.rasd.app.notification.PeriodAlarmScheduler
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
class AddPeriodViewModel @Inject constructor(
    private val classDao: ClassDao,
    private val classPeriodDao: ClassPeriodDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val classes: StateFlow<List<ClassEntity>> = classDao.getAllActiveClasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedClassId = MutableStateFlow<Long?>(null)
    val selectedClassId: StateFlow<Long?> = _selectedClassId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectClass(id: Long) { _selectedClassId.value = id }

    fun save(
        periodNumber: String,
        dayKey: String,
        startHour: String,
        endHour: String,
        startDate: String,
        endDate: String,
        teacherName: String
    ) {
        val cid = _selectedClassId.value
        if (cid == null) {
            _message.value = appContext.getString(R.string.msg_select_class_first)
            return
        }
        val number = periodNumber.toIntOrNull()
        val start = startHour.toIntOrNull()
        val end = endHour.toIntOrNull()
        if (number == null || start == null || end == null || start !in 0..23 || end !in 0..23) {
            _message.value = appContext.getString(R.string.msg_hours_invalid)
            return
        }
        viewModelScope.launch {
            val id = classPeriodDao.insertClassPeriod(
                ClassPeriodEntity(
                    classId = cid,
                    periodNumber = number,
                    dayOfWeek = dayKey,
                    startTime = start,
                    endTime = end,
                    startDate = if (startDate.isBlank()) LocalDate.now().toString() else startDate,
                    endDate = endDate,
                    teacherName = teacherName.trim()
                )
            )
            PeriodAlarmScheduler.schedule(appContext, id, cid, number, dayKey, start, teacherName.trim())
            _message.value = appContext.getString(R.string.msg_period_saved)
        }
    }

    fun consumeMessage() { _message.value = null }
}

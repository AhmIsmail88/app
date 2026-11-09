package com.rasd.app.ui.screens.schedule

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
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val classDao: ClassDao,
    private val classPeriodDao: ClassPeriodDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val classes: StateFlow<List<ClassEntity>> = classDao.getAllActiveClasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _periods = MutableStateFlow<List<ClassPeriodEntity>>(emptyList())
    val periods: StateFlow<List<ClassPeriodEntity>> = _periods.asStateFlow()

    private val _selectedClassId = MutableStateFlow<Long?>(null)
    val selectedClassId: StateFlow<Long?> = _selectedClassId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectClass(id: Long) {
        _selectedClassId.value = id
        reload()
    }

    fun reload() {
        val cid = _selectedClassId.value ?: return
        viewModelScope.launch { _periods.value = classPeriodDao.getPeriodsForClass(cid) }
    }

    fun deletePeriod(entity: ClassPeriodEntity) {
        viewModelScope.launch {
            PeriodAlarmScheduler.cancel(
                appContext, entity.id, entity.classId, entity.periodNumber,
                entity.dayOfWeek, entity.startTime, entity.teacherName
            )
            classPeriodDao.deleteClassPeriod(entity)
            _message.value = appContext.getString(R.string.msg_period_deleted)
            reload()
        }
    }

    fun consumeMessage() { _message.value = null }

    /**
     * يعيد جدولة كل حصص الفصل الحالي بعد أن يمنح المدرس إذن «المنبّهات والتذكيرات».
     * لازم لأن الحصص المجدولة قبل الإذن بقيت على الجدولة التقريبية.
     */
    fun rescheduleAll() {
        val items = _periods.value
        if (items.isEmpty()) return
        items.forEach { p ->
            PeriodAlarmScheduler.schedule(
                appContext, p.id, p.classId, p.periodNumber, p.dayOfWeek, p.startTime, p.teacherName
            )
        }
        _message.value = appContext.getString(R.string.exact_alarm_enabled)
    }
}

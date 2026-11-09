package com.rasd.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasd.app.data.dao.AppSettingsDao
import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.dao.StudentDao
import com.rasd.app.data.model.AppSettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val settingsDao: AppSettingsDao,
    classDao: ClassDao,
    studentDao: StudentDao
) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettingsEntity())
    val settings: StateFlow<AppSettingsEntity> = _settings.asStateFlow()

    val classCount: StateFlow<Int> = classDao.getAllActiveClasses()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val studentCount: StateFlow<Int> = studentDao.countStudents()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        viewModelScope.launch {
            settingsDao.getSettingsOnce()?.let { _settings.value = it }
        }
    }
}

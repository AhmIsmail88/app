package com.rasd.app.ui.screens.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasd.app.R
import com.rasd.app.data.dao.AppSettingsDao
import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.dao.StudentDao
import com.rasd.app.data.model.AppSettingsEntity
import com.rasd.app.data.model.ClassEntity
import com.rasd.app.data.model.StudentEntity
import com.rasd.app.report.ClassReport
import com.rasd.app.report.ReportBuilder
import com.rasd.app.report.ReportPdfWriter
import com.rasd.app.report.ReportXlsxWriter
import com.rasd.app.report.StudentReport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

enum class ReportMode { CLASS, STUDENT }

class ExportedFile(val file: File, val mime: String)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val classDao: ClassDao,
    private val studentDao: StudentDao,
    private val settingsDao: AppSettingsDao,
    private val reportBuilder: ReportBuilder,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val classes: StateFlow<List<ClassEntity>> = classDao.getAllActiveClasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _students = MutableStateFlow<List<StudentEntity>>(emptyList())
    val students: StateFlow<List<StudentEntity>> = _students.asStateFlow()

    private val _mode = MutableStateFlow(ReportMode.CLASS)
    val mode: StateFlow<ReportMode> = _mode.asStateFlow()

    private val _selectedClassId = MutableStateFlow<Long?>(null)
    val selectedClassId: StateFlow<Long?> = _selectedClassId.asStateFlow()

    private val _selectedStudentId = MutableStateFlow<Long?>(null)
    val selectedStudentId: StateFlow<Long?> = _selectedStudentId.asStateFlow()

    private val today: LocalDate = LocalDate.now()
    private val _fromDate = MutableStateFlow(today.withDayOfMonth(1).toString())
    val fromDate: StateFlow<String> = _fromDate.asStateFlow()

    private val _toDate = MutableStateFlow(today.toString())
    val toDate: StateFlow<String> = _toDate.asStateFlow()

    private val _classReport = MutableStateFlow<ClassReport?>(null)
    val classReport: StateFlow<ClassReport?> = _classReport.asStateFlow()

    private val _studentReport = MutableStateFlow<StudentReport?>(null)
    val studentReport: StateFlow<StudentReport?> = _studentReport.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _exported = MutableStateFlow<ExportedFile?>(null)
    val exported: StateFlow<ExportedFile?> = _exported.asStateFlow()

    fun setMode(m: ReportMode) {
        _mode.value = m
    }

    fun selectClass(id: Long) {
        _selectedClassId.value = id
        _selectedStudentId.value = null
        viewModelScope.launch { _students.value = studentDao.getStudentsByClassOnce(id) }
    }

    fun selectStudent(id: Long) {
        _selectedStudentId.value = id
    }

    fun setFrom(value: String) { _fromDate.value = value }

    fun setTo(value: String) { _toDate.value = value }

    fun generate() {
        val cid = _selectedClassId.value
        if (cid == null) {
            _message.value = appContext.getString(R.string.msg_select_class_first)
            return
        }
        val studentId: Long? = if (_mode.value == ReportMode.STUDENT) {
            val sid = _selectedStudentId.value
            if (sid == null) {
                _message.value = appContext.getString(R.string.msg_pick_student)
                return
            }
            sid
        } else {
            null
        }
        viewModelScope.launch {
            val settings = settingsDao.getSettingsOnce() ?: AppSettingsEntity()
            val lang = appContext.resources.configuration.locales[0].language

            // أي خطأ غير متوقع في التوليد يُعرض كرسالة بدل أن يُسقط التطبيق.
            runCatching {
                if (studentId == null) {
                    val report = reportBuilder.buildClassReport(cid, _fromDate.value, _toDate.value, settings, lang)
                    _classReport.value = report
                    _studentReport.value = null
                    appContext.getString(R.string.msg_class_report_ready_fmt, report.rows.size)
                } else {
                    val report = reportBuilder.buildStudentReport(studentId, cid, _fromDate.value, _toDate.value, settings, lang)
                    _studentReport.value = report
                    _classReport.value = null
                    appContext.getString(R.string.msg_student_report_ready)
                }
            }.onSuccess { ready ->
                _message.value = ready
            }.onFailure {
                _message.value = appContext.getString(R.string.msg_generate_failed)
            }
        }
    }

    private fun reportsDir(): File =
        File(appContext.getExternalFilesDir(null), "RasdReports").apply { mkdirs() }

    fun exportPdf() {
        val cr = _classReport.value
        val sr = _studentReport.value
        if (cr == null && sr == null) {
            _message.value = appContext.getString(R.string.msg_generate_first)
            return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val out = File(reportsDir(), "report_" + System.currentTimeMillis() + ".pdf")
                    if (cr != null) ReportPdfWriter.writeClassReport(cr, out)
                    else ReportPdfWriter.writeStudentReport(sr!!, out)
                    out
                }
            }.onSuccess { file ->
                _exported.value = ExportedFile(file, "application/pdf")
            }.onFailure {
                _message.value = appContext.getString(R.string.msg_export_failed)
            }
        }
    }

    fun exportExcel() {
        val cr = _classReport.value
        val sr = _studentReport.value
        if (cr == null && sr == null) {
            _message.value = appContext.getString(R.string.msg_generate_first)
            return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val out = File(reportsDir(), "report_" + System.currentTimeMillis() + ".xlsx")
                    if (cr != null) ReportXlsxWriter.writeClassReport(cr, out)
                    else ReportXlsxWriter.writeStudentReport(sr!!, out)
                    out
                }
            }.onSuccess { file ->
                _exported.value = ExportedFile(
                    file,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            }.onFailure {
                _message.value = appContext.getString(R.string.msg_export_failed)
            }
        }
    }

    fun clearExported() { _exported.value = null }

    fun consumeMessage() { _message.value = null }
}

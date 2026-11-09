package com.rasd.app.ui.screens.reports

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.rasd.app.R
import com.rasd.app.report.TermGrades
import com.rasd.app.ui.components.DateField
import java.time.LocalDate
import java.util.Locale

private const val PRESET_CUSTOM = 5

private fun academicStart(today: LocalDate): LocalDate =
    if (today.monthValue >= 9) LocalDate.of(today.year, 9, 1) else LocalDate.of(today.year - 1, 9, 1)

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val classes by viewModel.classes.collectAsState()
    val students by viewModel.students.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val selectedStudentId by viewModel.selectedStudentId.collectAsState()
    val from by viewModel.fromDate.collectAsState()
    val to by viewModel.toDate.collectAsState()
    val classReport by viewModel.classReport.collectAsState()
    val studentReport by viewModel.studentReport.collectAsState()
    val exported by viewModel.exported.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var classMenu by remember { mutableStateOf(false) }
    var studentMenu by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf(0) }

    val presets = listOf(
        stringResource(R.string.preset_this_month),
        stringResource(R.string.preset_last_month),
        stringResource(R.string.preset_last_30),
        stringResource(R.string.preset_half_year),
        stringResource(R.string.preset_year),
        stringResource(R.string.preset_custom)
    )

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }
    LaunchedEffect(exported) {
        exported?.let { e ->
            runCatching {
                val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", e.file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = e.mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share_report)))
            }
            viewModel.clearExported()
        }
    }
    LaunchedEffect(preset) {
        val today = LocalDate.now()
        when (preset) {
            0 -> {
                viewModel.setFrom(today.withDayOfMonth(1).toString())
                viewModel.setTo(today.toString())
            }
            1 -> {
                val m = today.minusMonths(1)
                viewModel.setFrom(m.withDayOfMonth(1).toString())
                viewModel.setTo(m.withDayOfMonth(m.lengthOfMonth()).toString())
            }
            2 -> {
                viewModel.setFrom(today.minusDays(29).toString())
                viewModel.setTo(today.toString())
            }
            3 -> {
                val start = academicStart(today)
                viewModel.setFrom(start.toString())
                viewModel.setTo(LocalDate.of(start.year + 1, 1, 31).toString())
            }
            4 -> {
                val start = academicStart(today)
                viewModel.setFrom(start.toString())
                viewModel.setTo(LocalDate.of(start.year + 1, 6, 30).toString())
            }
        }
    }

    val hasReport = classReport != null || studentReport != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (hasReport) {
                Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportPdf() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(stringResource(R.string.export_pdf)) }
                        OutlinedButton(
                            onClick = { viewModel.exportExcel() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(stringResource(R.string.export_excel)) }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScopeChip(stringResource(R.string.report_class), mode == ReportMode.CLASS, Modifier.weight(1f)) { viewModel.setMode(ReportMode.CLASS) }
                ScopeChip(stringResource(R.string.report_student), mode == ReportMode.STUDENT, Modifier.weight(1f)) { viewModel.setMode(ReportMode.STUDENT) }
            }

            Box {
                SelectorCard(
                    text = classes.firstOrNull { it.id == selectedClassId }?.name ?: stringResource(R.string.select_class),
                    hasValue = selectedClassId != null,
                    modifier = Modifier.fillMaxWidth()
                ) { classMenu = true }
                DropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                    if (classes.isEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.no_classes)) }, onClick = { classMenu = false })
                    }
                    classes.forEach { c ->
                        DropdownMenuItem(text = { Text(c.name) }, onClick = { classMenu = false; viewModel.selectClass(c.id) })
                    }
                }
            }

            if (mode == ReportMode.STUDENT) {
                Box {
                    SelectorCard(
                        text = students.firstOrNull { it.id == selectedStudentId }?.arabicName ?: stringResource(R.string.select_student),
                        hasValue = selectedStudentId != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { studentMenu = true }
                    DropdownMenu(expanded = studentMenu, onDismissRequest = { studentMenu = false }) {
                        students.forEach { s ->
                            DropdownMenuItem(text = { Text(s.arabicName) }, onClick = { studentMenu = false; viewModel.selectStudent(s.id) })
                        }
                    }
                }
            }

            Text(stringResource(R.string.period), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.take(3).forEachIndexed { index, label ->
                    ScopeChip(label, preset == index, Modifier.weight(1f)) { preset = index }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.drop(3).forEachIndexed { i, label ->
                    val index = i + 3
                    ScopeChip(label, preset == index, Modifier.weight(1f)) { preset = index }
                }
            }
            if (preset == PRESET_CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(stringResource(R.string.from_date), from, { viewModel.setFrom(it) }, Modifier.weight(1f))
                    DateField(stringResource(R.string.to_date), to, { viewModel.setTo(it) }, Modifier.weight(1f))
                }
            } else {
                Text(
                    text = stringResource(R.string.rpt_from) + " " + from + " " + stringResource(R.string.rpt_to) + " " + to,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { viewModel.generate() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(R.string.generate_report), style = MaterialTheme.typography.titleMedium) }

            HorizontalDivider()

            classReport?.let { r ->
                Text(r.className, fontWeight = FontWeight.SemiBold)
                ReportTable(
                    headers = listOf(
                        stringResource(R.string.col_index),
                        stringResource(R.string.col_name),
                        stringResource(R.string.col_present),
                        stringResource(R.string.col_absent),
                        stringResource(R.string.col_late),
                        stringResource(R.string.col_percent),
                        stringResource(R.string.col_behavior),
                        stringResource(R.string.col_average)
                    ),
                    rows = r.rows.map {
                        listOf(
                            it.index.toString(), it.studentName, it.present.toString(), it.absent.toString(),
                            it.late.toString(), it.attendancePercent.toString() + "%", it.behaviorSummary,
                            String.format(Locale.US, "%.1f", it.assessmentAverage)
                        )
                    }
                )
                Text(stringResource(R.string.term_grades_title), fontWeight = FontWeight.SemiBold)
                ReportTable(
                    headers = listOf(
                        stringResource(R.string.col_index),
                        stringResource(R.string.col_name),
                        stringResource(R.string.col_term_work),
                        stringResource(R.string.col_midyear),
                        stringResource(R.string.col_final_term)
                    ),
                    rows = r.rows.map { row ->
                        listOf(
                            row.index.toString(),
                            row.studentName,
                            termPercent(row.termPercents[TermGrades.TERMLY]),
                            termPercent(row.termPercents[TermGrades.MIDYEAR]),
                            termPercent(row.termPercents[TermGrades.FINAL])
                        )
                    }
                )
            }

            studentReport?.let { s ->
                Text(s.studentName + "  (" + s.studentNumber + ")", fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.col_present) + ": " + s.present +
                        " | " + stringResource(R.string.col_absent) + ": " + s.absent +
                        " | " + stringResource(R.string.col_late) + ": " + s.late +
                        " | " + stringResource(R.string.col_percent) + ": " + s.attendancePercent + "% | " +
                        stringResource(R.string.col_points) + ": " +
                        String.format(Locale.US, "%.1f", s.behaviorPoints) + " | " +
                        stringResource(R.string.col_average) + ": " +
                        String.format(Locale.US, "%.1f", s.assessmentAverage) + "%",
                    style = MaterialTheme.typography.bodySmall
                )
                if (s.termGrades.isNotEmpty()) {
                    Text(stringResource(R.string.term_grades_title), fontWeight = FontWeight.SemiBold)
                    ReportTable(
                        headers = listOf(
                            stringResource(R.string.col_term),
                            stringResource(R.string.col_percent2),
                            stringResource(R.string.col_score),
                            stringResource(R.string.col_out_of)
                        ),
                        rows = s.termGrades.map {
                            listOf(
                                termName(it.term),
                                String.format(Locale.US, "%.1f", it.percent) + "%",
                                String.format(Locale.US, "%.1f", it.total),
                                String.format(Locale.US, "%.1f", it.maximum)
                            )
                        }
                    )
                }
                Text(stringResource(R.string.grades), fontWeight = FontWeight.SemiBold)
                ReportTable(
                    headers = listOf(
                        stringResource(R.string.col_assessment),
                        stringResource(R.string.col_type),
                        stringResource(R.string.col_date),
                        stringResource(R.string.col_score),
                        stringResource(R.string.col_out_of),
                        stringResource(R.string.col_percent2)
                    ),
                    rows = s.assessments.map {
                        listOf(
                            it.assessmentName, it.type, it.date,
                            String.format(Locale.US, "%.1f", it.score),
                            String.format(Locale.US, "%.1f", it.maximum),
                            String.format(Locale.US, "%.1f", it.percent)
                        )
                    }
                )
                Text(stringResource(R.string.behavior_section), fontWeight = FontWeight.SemiBold)
                ReportTable(
                    headers = listOf(
                        stringResource(R.string.col_date),
                        stringResource(R.string.col_behavior_type),
                        stringResource(R.string.col_category),
                        stringResource(R.string.col_points),
                        stringResource(R.string.col_note)
                    ),
                    rows = s.behaviors.map {
                        listOf(it.date, it.typeName, it.category, String.format(Locale.US, "%.1f", it.points), it.note)
                    }
                )
            }

            Text(
                text = stringResource(R.string.reports_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun termName(term: String): String = when (TermGrades.termOf(term)) {
    TermGrades.MIDYEAR -> stringResource(R.string.col_midyear)
    TermGrades.FINAL -> stringResource(R.string.col_final_term)
    else -> stringResource(R.string.col_term_work)
}

private fun termPercent(value: Int?): String = if (value == null) "-" else "$value%"

@Composable
private fun ScopeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectorCard(text: String, hasValue: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (hasValue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
    }
}

@Composable
private fun ReportTable(headers: List<String>, rows: List<List<String>>) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            headers.forEach { h ->
                Text(
                    text = h,
                    modifier = Modifier.weight(1f).padding(6.dp),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth()) {
                r.forEach { v ->
                    Text(
                        text = v,
                        modifier = Modifier.weight(1f).padding(6.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

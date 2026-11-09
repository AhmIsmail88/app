package com.rasd.app.ui.screens.assessment

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rasd.app.R
import com.rasd.app.report.TermGrades
import com.rasd.app.ui.components.DateField
import java.time.LocalDate

@Composable
fun AssessmentScreen(viewModel: AssessmentViewModel = hiltViewModel()) {
    val classes by viewModel.classes.collectAsState()
    val students by viewModel.students.collectAsState()
    val assessments by viewModel.filteredAssessments.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val selectedAssessmentId by viewModel.selectedAssessmentId.collectAsState()
    val scores by viewModel.scores.collectAsState()
    val msg by viewModel.message.collectAsState()
    val termFilter by viewModel.termFilter.collectAsState()
    val gradePreview by viewModel.gradePreview.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var classMenu by remember { mutableStateOf(false) }
    var assessMenu by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var maxScore by remember { mutableStateOf("100") }
    var term by remember { mutableStateOf(TermGrades.TERMLY) }

    val gradePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.parseGradeFile(uri)
    }

    LaunchedEffect(msg) {
        msg?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val selectedClass = classes.firstOrNull { it.id == selectedClassId }
            Box {
                SelectorCard(selectedClass?.name ?: stringResource(R.string.select_class)) { classMenu = true }
                DropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                    if (classes.isEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.no_classes)) }, onClick = { classMenu = false })
                    }
                    classes.forEach { c ->
                        DropdownMenuItem(text = { Text(c.name) }, onClick = { classMenu = false; viewModel.selectClass(c.id) })
                    }
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.new_assessment), fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.assessment_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text(stringResource(R.string.type)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = maxScore,
                    onValueChange = { maxScore = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.out_of)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            DateField(stringResource(R.string.date), date, { date = it }, Modifier.fillMaxWidth())

            Text(stringResource(R.string.term_label), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TermChips(selected = term, includeAll = false, onSelect = { term = it })

            Button(
                onClick = { viewModel.createAssessment(name, type, date, maxScore, term); name = ""; type = "" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(R.string.create_assessment)) }

            HorizontalDivider()
            Text(stringResource(R.string.grade_entry), fontWeight = FontWeight.SemiBold)

            Text(stringResource(R.string.term_label), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TermChips(selected = termFilter, includeAll = true, onSelect = { viewModel.setTermFilter(it) })

            val selectedAssessment = assessments.firstOrNull { it.id == selectedAssessmentId }
            Box {
                SelectorCard(
                    selectedAssessment?.let { a ->
                        a.name + " · " + termLabel(a.term) + " (" + stringResource(R.string.out_of) + " " + a.maximumScore.toInt() + ")"
                    } ?: stringResource(R.string.select_assessment)
                ) { assessMenu = true }
                DropdownMenu(expanded = assessMenu, onDismissRequest = { assessMenu = false }) {
                    if (assessments.isEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.no_assessments)) }, onClick = { assessMenu = false })
                    }
                    assessments.forEach { a ->
                        DropdownMenuItem(
                            text = { Text(a.name + " · " + termLabel(a.term)) },
                            onClick = { assessMenu = false; viewModel.selectAssessment(a.id) }
                        )
                    }
                }
            }

            if (selectedAssessmentId != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.import_grades),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            gradePicker.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "text/csv",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(14.dp)
                        )
                    ) {
                        Icon(
                            Icons.Outlined.FileDownload,
                            contentDescription = stringResource(R.string.import_grades_cd),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                students.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(s.arabicName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        OutlinedTextField(
                            value = scores[s.id] ?: "",
                            onValueChange = { viewModel.setScore(s.id, it.filter { ch -> ch.isDigit() || ch == '.' }) },
                            label = { Text(stringResource(R.string.score)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                Button(
                    onClick = { viewModel.saveScores() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(stringResource(R.string.save_scores)) }
            }
        }
    }

    if (gradePreview.isNotEmpty()) {
        GradeMappingDialog(
            preview = gradePreview,
            onDismiss = { viewModel.cancelGradeImport() },
            onConfirm = { numberIdx, arabicIdx, englishIdx, scoreIdx, skipHeader ->
                viewModel.importGradesWithMapping(numberIdx, arabicIdx, englishIdx, scoreIdx, skipHeader)
            }
        )
    }
}

@Composable
internal fun termLabel(term: String): String = when (TermGrades.termOf(term)) {
    TermGrades.MIDYEAR -> stringResource(R.string.term_midyear)
    TermGrades.FINAL -> stringResource(R.string.term_final)
    else -> stringResource(R.string.term_termly)
}

@Composable
private fun TermChips(selected: String, includeAll: Boolean, onSelect: (String) -> Unit) {
    val options = buildList {
        if (includeAll) add("")
        add(TermGrades.TERMLY)
        add(TermGrades.MIDYEAR)
        add(TermGrades.FINAL)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { value ->
            val active = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (value.isBlank()) stringResource(R.string.term_all) else termLabel(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GradeMappingDialog(
    preview: List<List<String>>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, Int, Boolean) -> Unit
) {
    val colCount = (preview.maxOfOrNull { it.size } ?: 0).coerceAtMost(12)
    val columns = (0 until colCount).toList()
    val firstRow = preview.firstOrNull() ?: emptyList()
    val headerLikely = firstRow.firstOrNull()?.trim()?.toIntOrNull() == null

    var numberIdx by remember { mutableStateOf(0) }
    var arabicIdx by remember { mutableStateOf(if (colCount > 1) 1 else 0) }
    var englishIdx by remember { mutableStateOf(-1) }
    var scoreIdx by remember { mutableStateOf(if (colCount > 2) 2 else colCount - 1) }
    var skipHeader by remember { mutableStateOf(headerLikely) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.grade_map_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    stringResource(R.string.grade_map_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.import_preview) + " " + firstRow.joinToString(" | ").take(90),
                    style = MaterialTheme.typography.bodySmall
                )
                MappingPicker(stringResource(R.string.field_number), numberIdx, columns, firstRow, true) { numberIdx = it }
                MappingPicker(stringResource(R.string.field_name_ar), arabicIdx, columns, firstRow, true) { arabicIdx = it }
                MappingPicker(stringResource(R.string.field_name_en), englishIdx, columns, firstRow, true) { englishIdx = it }
                MappingPicker(stringResource(R.string.field_score), scoreIdx, columns, firstRow, false) { scoreIdx = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = skipHeader, onCheckedChange = { skipHeader = it })
                    Text(stringResource(R.string.skip_header_row), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(numberIdx, arabicIdx, englishIdx, scoreIdx, skipHeader) }) {
                Text(stringResource(R.string.import_action))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun MappingPicker(
    label: String,
    selected: Int,
    columns: List<Int>,
    firstRow: List<String>,
    allowIgnore: Boolean,
    onSelect: (Int) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(columnLabel(selected, firstRow), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ExpandMore, contentDescription = null)
                }
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                if (allowIgnore) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ignore_column)) },
                        onClick = { open = false; onSelect(-1) }
                    )
                }
                columns.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(columnLabel(c, firstRow)) },
                        onClick = { open = false; onSelect(c) }
                    )
                }
            }
        }
    }
}

@Composable
private fun columnLabel(index: Int, firstRow: List<String>): String {
    if (index < 0) return stringResource(R.string.ignore_column)
    val sample = firstRow.getOrNull(index)?.trim().orEmpty()
    val base = stringResource(R.string.column_n, index + 1)
    return if (sample.isBlank()) base else base + " · " + sample.take(14)
}

@Composable
private fun SelectorCard(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
    }
}

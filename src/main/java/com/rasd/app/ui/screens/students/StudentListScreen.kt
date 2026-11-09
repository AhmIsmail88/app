package com.rasd.app.ui.screens.students

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rasd.app.R
import com.rasd.app.data.model.StudentEntity

@Composable
fun StudentListScreen(viewModel: StudentListViewModel = hiltViewModel()) {
    val classes by viewModel.classes.collectAsState()
    val students by viewModel.students.collectAsState()
    val filtered by viewModel.filteredStudents.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val query by viewModel.query.collectAsState()
    val preview by viewModel.importPreview.collectAsState()
    val msg by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var classMenu by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StudentEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<StudentEntity?>(null) }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.parseImportFile(uri)
    }

    LaunchedEffect(msg) {
        msg?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_student)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val selected = classes.firstOrNull { it.id == selectedClassId }
                    Box(Modifier.weight(1f)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { classMenu = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selected?.name ?: stringResource(R.string.select_class),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Outlined.ExpandMore, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                            if (classes.isEmpty()) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.no_classes)) }, onClick = { classMenu = false })
                            }
                            classes.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = { classMenu = false; viewModel.selectClass(c.id) }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            importPicker.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "text/csv",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            Icons.Outlined.FileDownload,
                            contentDescription = stringResource(R.string.import_cd),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setQuery(it) },
                    label = { Text(stringResource(R.string.search_by_name_or_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp)
                )

                Text(
                    text = stringResource(R.string.students_count_fmt, filtered.size, students.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedClassId == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.pick_class_to_list),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (students.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.no_students), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.no_students_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { s ->
                        StudentRow(entity = s, onEdit = { editing = s }, onDelete = { pendingDelete = s })
                    }
                }
            }
        }
    }

    if (preview.isNotEmpty()) {
        ImportMappingDialog(
            preview = preview,
            onDismiss = { viewModel.cancelImport() },
            onConfirm = { numberIdx, arabicIdx, englishIdx, skipHeader ->
                viewModel.importWithMapping(numberIdx, arabicIdx, englishIdx, skipHeader)
            }
        )
    }

    if (showAdd) {
        StudentDialog(
            title = stringResource(R.string.new_student),
            initial = null,
            onDismiss = { showAdd = false },
            onConfirm = { number, ar, en ->
                viewModel.addStudent(number, ar, en)
                showAdd = false
            }
        )
    }

    editing?.let { victim ->
        StudentDialog(
            title = stringResource(R.string.edit_student),
            initial = victim,
            onDismiss = { editing = null },
            onConfirm = { number, ar, en ->
                viewModel.updateStudent(victim.copy(studentNumber = number, arabicName = ar, englishName = en))
                editing = null
            }
        )
    }

    pendingDelete?.let { victim ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_student_title)) },
            text = { Text(stringResource(R.string.delete_student_msg, victim.arabicName)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteStudent(victim); pendingDelete = null }) {
                    Text(stringResource(R.string.delete_permanently))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun ImportMappingDialog(
    preview: List<List<String>>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, Boolean) -> Unit
) {
    val colCount = (preview.maxOfOrNull { it.size } ?: 0).coerceAtMost(12)
    val columns = (0 until colCount).toList()
    val firstRow = preview.firstOrNull() ?: emptyList()
    val headerLikely = firstRow.firstOrNull()?.trim()?.toIntOrNull() == null

    var numberIdx by remember { mutableStateOf(0) }
    var arabicIdx by remember { mutableStateOf(if (colCount > 1) 1 else 0) }
    var englishIdx by remember { mutableStateOf(if (colCount > 2) 2 else -1) }
    var skipHeader by remember { mutableStateOf(headerLikely) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_map_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    stringResource(R.string.import_map_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.import_preview) + " " + firstRow.joinToString(" | ").take(90),
                    style = MaterialTheme.typography.bodySmall
                )
                MappingPicker(stringResource(R.string.field_number), numberIdx, columns, firstRow, allowIgnore = true) { numberIdx = it }
                MappingPicker(stringResource(R.string.field_name_ar), arabicIdx, columns, firstRow, allowIgnore = false) { arabicIdx = it }
                MappingPicker(stringResource(R.string.field_name_en), englishIdx, columns, firstRow, allowIgnore = true) { englishIdx = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = skipHeader, onCheckedChange = { skipHeader = it })
                    Text(stringResource(R.string.skip_header_row), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(numberIdx, arabicIdx, englishIdx, skipHeader) }) {
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
                    Text(
                        text = columnLabel(selected, firstRow),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
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
private fun StudentRow(
    entity: StudentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entity.arabicName.take(1).ifBlank { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(entity.arabicName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val sub = listOf(entity.studentNumber, entity.englishName).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.options))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun StudentDialog(
    title: String,
    initial: StudentEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var number by remember(initial?.id) { mutableStateOf(initial?.studentNumber ?: "") }
    var ar by remember(initial?.id) { mutableStateOf(initial?.arabicName ?: "") }
    var en by remember(initial?.id) { mutableStateOf(initial?.englishName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text(stringResource(R.string.student_number)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ar, onValueChange = { ar = it }, label = { Text(stringResource(R.string.student_name_ar)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = en, onValueChange = { en = it }, label = { Text(stringResource(R.string.student_name_en)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(number, ar, en) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

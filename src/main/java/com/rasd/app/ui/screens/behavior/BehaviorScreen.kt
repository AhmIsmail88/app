package com.rasd.app.ui.screens.behavior

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.rasd.app.data.model.BehaviorTypeEntity
import com.rasd.app.ui.RasdColors
import com.rasd.app.ui.components.DateField
import java.time.LocalDate

@Composable
fun BehaviorScreen(viewModel: BehaviorViewModel = hiltViewModel()) {
    val classes by viewModel.classes.collectAsState()
    val students by viewModel.students.collectAsState()
    val types by viewModel.types.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val selectedStudentId by viewModel.selectedStudentId.collectAsState()
    val selectedTypeId by viewModel.selectedTypeId.collectAsState()
    val eventsForType by viewModel.eventsForType.collectAsState()
    val msg by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var classMenu by remember { mutableStateOf(false) }
    var studentMenu by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var editing by remember { mutableStateOf<BehaviorTypeEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BehaviorTypeEntity?>(null) }

    LaunchedEffect(msg) {
        msg?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }
    LaunchedEffect(pendingDelete?.id) {
        pendingDelete?.let { viewModel.loadEventCount(it.id) }
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

            val selectedStudent = students.firstOrNull { it.id == selectedStudentId }
            Box {
                SelectorCard(selectedStudent?.arabicName ?: stringResource(R.string.select_student)) { studentMenu = true }
                DropdownMenu(expanded = studentMenu, onDismissRequest = { studentMenu = false }) {
                    students.forEach { s ->
                        DropdownMenuItem(text = { Text(s.arabicName) }, onClick = { studentMenu = false; viewModel.selectStudent(s.id) })
                    }
                }
            }

            HorizontalDivider()

            // إدارة أنواع السلوك
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.behavior_type), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.syncDefaults() }, shape = RoundedCornerShape(12.dp)) {
                        Text(stringResource(R.string.default_types))
                    }
                    Button(onClick = { showAdd = true }, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text(" " + stringResource(R.string.add_type))
                    }
                }
            }

            if (types.isEmpty()) {
                Text(
                    stringResource(R.string.no_behavior_types),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            types.forEach { t ->
                BehaviorTypeRow(
                    type = t,
                    selected = selectedTypeId == t.id,
                    onSelect = { viewModel.selectType(t.id) },
                    onEdit = { editing = t },
                    onDelete = { pendingDelete = t }
                )
            }

            DateField(stringResource(R.string.date), date, { date = it }, Modifier.fillMaxWidth())
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Button(
                onClick = { viewModel.record(note, date); note = "" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(R.string.record_behavior), style = MaterialTheme.typography.titleMedium) }
        }
    }

    if (showAdd) {
        TypeDialog(
            title = stringResource(R.string.add_type),
            initial = null,
            onDismiss = { showAdd = false },
            onConfirm = { name, cat, pts ->
                viewModel.saveType(null, name, cat, pts)
                showAdd = false
            }
        )
    }

    editing?.let { victim ->
        TypeDialog(
            title = stringResource(R.string.edit_type),
            initial = victim,
            onDismiss = { editing = null },
            onConfirm = { name, cat, pts ->
                viewModel.saveType(victim.id, name, cat, pts)
                editing = null
            }
        )
    }

    pendingDelete?.let { victim ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_type_title)) },
            text = { Text(stringResource(R.string.delete_type_msg, victim.nameArabic, eventsForType)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteType(victim); pendingDelete = null }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun BehaviorTypeRow(
    type: BehaviorTypeEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val accent = when (type.category) {
        "positive" -> RasdColors.positive
        "negative" -> RasdColors.negative
        else -> RasdColors.neutral
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = ((type.defaultPoints ?: 0f).toInt()).toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = type.nameArabic,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
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
private fun TypeDialog(
    title: String,
    initial: BehaviorTypeEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Float) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.nameArabic ?: "") }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "positive") }
    var points by remember(initial?.id) { mutableStateOf(((initial?.defaultPoints ?: 1f).toInt()).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.type_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(stringResource(R.string.type_category), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = category == "positive",
                        onClick = { category = "positive" },
                        label = { Text(stringResource(R.string.category_positive)) }
                    )
                    FilterChip(
                        selected = category == "negative",
                        onClick = { category = "negative" },
                        label = { Text(stringResource(R.string.category_negative)) }
                    )
                    FilterChip(
                        selected = category == "neutral",
                        onClick = { category = "neutral" },
                        label = { Text(stringResource(R.string.category_neutral)) }
                    )
                }
                OutlinedTextField(
                    value = points,
                    onValueChange = { v -> points = v.filter { ch -> ch.isDigit() || ch == '-' } },
                    label = { Text(stringResource(R.string.type_points)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = points.toFloatOrNull() ?: 0f
                val signed = when (category) {
                    "negative" -> -kotlin.math.abs(p)
                    "positive" -> kotlin.math.abs(p)
                    else -> 0f
                }
                onConfirm(name, category, signed)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
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

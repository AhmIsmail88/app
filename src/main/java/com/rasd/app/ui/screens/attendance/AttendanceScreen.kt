package com.rasd.app.ui.screens.attendance

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rasd.app.R
import com.rasd.app.ui.RasdColors
import com.rasd.app.ui.components.DateField

private fun statusColor(status: String): Color = when (status) {
    "present" -> RasdColors.present
    "absent" -> RasdColors.absent
    "late" -> RasdColors.late
    else -> RasdColors.neutral
}

@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel = hiltViewModel()) {
    val classes by viewModel.classes.collectAsState()
    val students by viewModel.filteredStudents.collectAsState()
    val statuses by viewModel.statuses.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val date by viewModel.date.collectAsState()
    val query by viewModel.query.collectAsState()
    val msg by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var classMenu by remember { mutableStateOf(false) }

    val statusOptions = listOf(
        "present" to stringResource(R.string.present),
        "absent" to stringResource(R.string.absent),
        "late" to stringResource(R.string.late)
    )
    val presentLabel = stringResource(R.string.present)
    val absentLabel = stringResource(R.string.absent)
    val lateLabel = stringResource(R.string.late)
    val unmarkedLabel = stringResource(R.string.unmarked)
    val selectClassText = stringResource(R.string.select_class)
    val noClassesText = stringResource(R.string.no_classes)
    val saveText = stringResource(R.string.save_attendance)

    LaunchedEffect(msg) {
        msg?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    val present = statuses.count { it.value == "present" }
    val absent = statuses.count { it.value == "absent" }
    val late = statuses.count { it.value == "late" }
    val unmarked = (students.size - statuses.size).coerceAtLeast(0)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 2.dp) {
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(saveText, style = MaterialTheme.typography.titleMedium) }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val selected = classes.firstOrNull { it.id == selectedClassId }
                Box {
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
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selected?.name ?: selectClassText,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                        if (classes.isEmpty()) {
                            DropdownMenuItem(text = { Text(noClassesText) }, onClick = { classMenu = false })
                        }
                        classes.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = { classMenu = false; viewModel.selectClass(c.id) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    DateField(stringResource(R.string.date), date, { viewModel.setDate(it) }, Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountChip(presentLabel, present, RasdColors.present)
                    CountChip(absentLabel, absent, RasdColors.absent)
                    CountChip(lateLabel, late, RasdColors.late)
                    if (unmarked > 0) CountChip(unmarkedLabel, unmarked, RasdColors.neutral)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton(stringResource(R.string.mark_all_present), Modifier.weight(1f)) { viewModel.markAll("present") }
                    SmallButton(stringResource(R.string.mark_all_absent), Modifier.weight(1f)) { viewModel.markAll("absent") }
                }

                if (students.size > 6) {
                    androidx.compose.material3.OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.setQuery(it) },
                        label = { Text(stringResource(R.string.search_student)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            if (students.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_students_hint),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students, key = { it.id }) { s ->
                    StudentAttendanceRow(
                        name = s.arabicName,
                        number = s.studentNumber,
                        current = statuses[s.id],
                        onSelect = { viewModel.setStatus(s.id, it) },
                        options = statusOptions
                    )
                }
            }
        }
    }
}

@Composable
private fun CountChip(label: String, value: Int, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(value.toString(), style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun SmallButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StudentAttendanceRow(
    name: String,
    number: String,
    current: String?,
    onSelect: (String) -> Unit,
    options: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (number.isNotBlank()) {
                    Text(number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (value, label) ->
                    val selected = current == value
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) statusColor(value) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelect(value) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

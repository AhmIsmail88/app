package com.rasd.app.ui.screens.addPeriod

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rasd.app.R
import com.rasd.app.ui.components.DateField
import com.rasd.app.ui.screens.schedule.DAY_KEYS
import com.rasd.app.ui.screens.schedule.dayLabel
import java.time.LocalDate

@Composable
fun AddPeriodScreen(viewModel: AddPeriodViewModel = hiltViewModel()) {
    val classes by viewModel.classes.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val msg by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var periodNumber by remember { mutableStateOf("1") }
    var dayKey by remember { mutableStateOf("1") }
    var startHour by remember { mutableStateOf("8") }
    var endHour by remember { mutableStateOf("9") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }

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
            val selected = classes.firstOrNull { it.id == selectedClassId }
            Box {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { menu = true },
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
                        Text(selected?.name ?: stringResource(R.string.select_class), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.ExpandMore, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (classes.isEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.no_classes)) }, onClick = { menu = false })
                    }
                    classes.forEach { c ->
                        DropdownMenuItem(text = { Text(c.name) }, onClick = { menu = false; viewModel.selectClass(c.id) })
                    }
                }
            }

            Text(stringResource(R.string.day), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DAY_KEYS.take(4).forEach { key ->
                    FilterChip(selected = dayKey == key, onClick = { dayKey = key }, label = { Text(dayLabel(key)) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DAY_KEYS.drop(4).forEach { key ->
                    FilterChip(selected = dayKey == key, onClick = { dayKey = key }, label = { Text(dayLabel(key)) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = periodNumber,
                    onValueChange = { periodNumber = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.period_number)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text(stringResource(R.string.teacher)) },
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startHour,
                    onValueChange = { startHour = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.from_hour)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = endHour,
                    onValueChange = { endHour = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.to_hour)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(stringResource(R.string.from_date), startDate, { startDate = it }, Modifier.weight(1f))
                DateField(stringResource(R.string.to_date), endDate, { endDate = it }, Modifier.weight(1f))
            }

            Button(
                onClick = { viewModel.save(periodNumber, dayKey, startHour, endHour, startDate, endDate, teacherName) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(R.string.save_period), style = MaterialTheme.typography.titleMedium) }
        }
    }
}

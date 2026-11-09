package com.rasd.app.ui.screens.schedule

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rasd.app.R
import com.rasd.app.notification.ExactAlarmPermission

@Composable
fun ScheduleScreen(
    onAddPeriod: () -> Unit = {},
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val classes by viewModel.classes.collectAsState()
    val periods by viewModel.periods.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val msg by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }

    val ctx = LocalContext.current
    var exactAllowed by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // نفحص إذن «المنبّهات والتذكيرات» عند كل عودة للشاشة (قد يعود المدرس من الإعدادات وقد فعّله)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = ExactAlarmPermission.canSchedule(ctx)
                if (now && !exactAllowed) viewModel.rescheduleAll()
                exactAllowed = now
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { exactAllowed = ExactAlarmPermission.canSchedule(ctx) }

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
            Text(stringResource(R.string.weekly_schedule), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            val selected = classes.firstOrNull { it.id == selectedClassId }
            Box {
                SelectorCard(selected?.name ?: stringResource(R.string.select_class)) { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (classes.isEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.no_classes)) }, onClick = { menu = false })
                    }
                    classes.forEach { c ->
                        DropdownMenuItem(text = { Text(c.name) }, onClick = { menu = false; viewModel.selectClass(c.id) })
                    }
                }
            }

            Button(onClick = onAddPeriod, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("  " + stringResource(R.string.add_period))
            }

            if (!exactAllowed) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.exact_alarm_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.exact_alarm_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                        Button(
                            onClick = { ExactAlarmPermission.request(ctx) },
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(stringResource(R.string.exact_alarm_button)) }
                    }
                }
            }

            HorizontalDivider()
            if (selectedClassId == null) {
                Text(stringResource(R.string.pick_class_to_show_schedule), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (periods.isEmpty()) {
                Text(stringResource(R.string.no_periods), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            DAY_KEYS.forEach { key ->
                val dayPeriods = periods.filter { it.dayOfWeek == key }.sortedBy { it.periodNumber }
                if (dayPeriods.isNotEmpty()) {
                    Text(dayLabel(key), fontWeight = FontWeight.SemiBold)
                    dayPeriods.forEach { p ->
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
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.period_label, p.periodNumber), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        stringResource(R.string.period_time, p.startTime, p.endTime) +
                                            (if (p.teacherName.isNotBlank()) "  ·  " + p.teacherName else ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deletePeriod(p) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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

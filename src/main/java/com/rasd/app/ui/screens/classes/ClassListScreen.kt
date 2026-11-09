package com.rasd.app.ui.screens.classes

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.rasd.app.data.model.ClassEntity

@Composable
fun ClassListScreen(viewModel: ClassListViewModel = hiltViewModel()) {
    val classes by viewModel.classes.collectAsState()
    val msg by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ClassEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<ClassEntity?>(null) }

    LaunchedEffect(msg) {
        msg?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_class)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        if (classes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.no_classes), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.no_classes_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(classes, key = { it.id }) { c ->
                    ClassRow(
                        entity = c,
                        onEdit = { editing = c },
                        onArchive = { viewModel.archiveClass(c) },
                        onDelete = { pendingDelete = c }
                    )
                }
            }
        }
    }

    if (showAdd) {
        ClassDialog(
            title = stringResource(R.string.new_class),
            initial = null,
            onDismiss = { showAdd = false },
            onConfirm = { name, grade, subject ->
                viewModel.addClass(name, grade, "", subject)
                showAdd = false
            }
        )
    }

    editing?.let { victim ->
        ClassDialog(
            title = stringResource(R.string.edit_class),
            initial = victim,
            onDismiss = { editing = null },
            onConfirm = { name, grade, subject ->
                viewModel.updateClass(victim.copy(name = name, grade = grade, subject = subject))
                editing = null
            }
        )
    }

    pendingDelete?.let { victim ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_class_title)) },
            text = { Text(stringResource(R.string.delete_class_msg, victim.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteClass(victim)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete_permanently)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun ClassRow(
    entity: ClassEntity,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val sub = listOf(entity.grade, entity.subject).filter { it.isNotBlank() }.joinToString(" · ")
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
                    DropdownMenuItem(text = { Text(stringResource(R.string.archive)) }, onClick = { menu = false; onArchive() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun ClassDialog(
    title: String,
    initial: ClassEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var grade by remember(initial?.id) { mutableStateOf(initial?.grade ?: "") }
    var subject by remember(initial?.id) { mutableStateOf(initial?.subject ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.class_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text(stringResource(R.string.grade)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(stringResource(R.string.subject)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, grade, subject) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

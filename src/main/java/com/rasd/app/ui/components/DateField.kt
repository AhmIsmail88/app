package com.rasd.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(false) }
    val state = rememberDatePickerState()

    Box(modifier) {
        OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (value.isBlank()) label else label + ": " + value)
        }
        if (show) {
            DatePickerDialog(
                onDismissRequest = { show = false },
                confirmButton = {
                    TextButton(onClick = {
                        val ms = state.selectedDateMillis
                        if (ms != null) {
                            val d = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                            onChange(d.toString())
                        }
                        show = false
                    }) { Text("موافق") }
                },
                dismissButton = {
                    TextButton(onClick = { show = false }) { Text("إلغاء") }
                }
            ) {
                DatePicker(state = state)
            }
        }
    }
}

package com.rasd.app.ui.screens.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rasd.app.R

/** مفاتيح أيام الأسبوع (1 = السبت … 7 = الجمعة). */
val DAY_KEYS = listOf("1", "2", "3", "4", "5", "6", "7")

@Composable
fun dayLabel(key: String): String = when (key) {
    "1" -> stringResource(R.string.day_1)
    "2" -> stringResource(R.string.day_2)
    "3" -> stringResource(R.string.day_3)
    "4" -> stringResource(R.string.day_4)
    "5" -> stringResource(R.string.day_5)
    "6" -> stringResource(R.string.day_6)
    "7" -> stringResource(R.string.day_7)
    else -> key
}

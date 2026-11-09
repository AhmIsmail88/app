package com.rasd.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rasd.app.receiver.ScheduleAlarmReceiver
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object PeriodAlarmScheduler {

    private fun dayOfWeek(key: String): DayOfWeek = when (key) {
        "1" -> DayOfWeek.SATURDAY
        "2" -> DayOfWeek.SUNDAY
        "3" -> DayOfWeek.MONDAY
        "4" -> DayOfWeek.TUESDAY
        "5" -> DayOfWeek.WEDNESDAY
        "6" -> DayOfWeek.THURSDAY
        else -> DayOfWeek.FRIDAY
    }

    fun nextTriggerAt(dayKey: String, hour: Int): Long {
        val now = LocalDateTime.now()
        var candidate = LocalDate.now()
            .with(TemporalAdjusters.nextOrSame(dayOfWeek(dayKey)))
            .atTime(hour, 0)
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusWeeks(1)
        }
        return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun pendingIntent(
        context: Context,
        periodId: Long,
        classId: Long,
        periodNumber: Int,
        dayKey: String,
        hour: Int,
        teacherName: String
    ): PendingIntent {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ScheduleAlarmReceiver.ACTION_PERIOD_REMINDER
            putExtra("periodId", periodId)
            putExtra("classId", classId)
            putExtra("periodNumber", periodNumber)
            putExtra("dayKey", dayKey)
            putExtra("hour", hour)
            putExtra("startTime", hour.toString() + ":00")
            putExtra("teacherName", teacherName)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, periodId.toInt(), intent, flags)
    }

    fun schedule(
        context: Context,
        periodId: Long,
        classId: Long,
        periodNumber: Int,
        dayKey: String,
        hour: Int,
        teacherName: String
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, periodId, classId, periodNumber, dayKey, hour, teacherName)
        val at = nextTriggerAt(dayKey, hour)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(
        context: Context,
        periodId: Long,
        classId: Long,
        periodNumber: Int,
        dayKey: String,
        hour: Int,
        teacherName: String
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, periodId, classId, periodNumber, dayKey, hour, teacherName))
    }
}

package com.rasd.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rasd.app.notification.PeriodAlarmScheduler
import com.rasd.app.notification.ScheduleNotificationManager
import java.util.concurrent.TimeUnit

/**
 * BroadcastReceiver لاستقبال منبّهات الجدول (بداية الفترة / تذكير الفترة / إلغاء الكل).
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CLASS_START -> handleClassStart(context, intent)
            ACTION_PERIOD_REMINDER -> handlePeriodReminder(context, intent)
            ACTION_CANCEL_ALL -> cancelAllNotifications(context, intent)
        }
    }

    private fun handleClassStart(context: Context, intent: Intent) {
        val periodNumber = intent.getIntExtra("periodNumber", 0)
        ScheduleNotificationManager.showClassStartedNotification(
            context,
            intent.getLongExtra("classId", 0),
            periodNumber,
            intent.getStringExtra("teacherName") ?: ""
        )
    }

    private fun handlePeriodReminder(context: Context, intent: Intent) {
        val periodNumber = intent.getIntExtra("periodNumber", 0)
        ScheduleNotificationManager.showPeriodReminderNotification(
            context,
            periodNumber,
            intent.getLongExtra("classId", 0),
            intent.getStringExtra("startTime") ?: "",
            intent.getStringExtra("teacherName") ?: ""
        )
        val periodId = intent.getLongExtra("periodId", 0L)
        val dayKey = intent.getStringExtra("dayKey")
        val hour = intent.getIntExtra("hour", -1)
        if (periodId > 0L && dayKey != null && hour >= 0) {
            PeriodAlarmScheduler.schedule(
                context,
                periodId,
                intent.getLongExtra("classId", 0L),
                periodNumber,
                dayKey,
                hour,
                intent.getStringExtra("teacherName") ?: ""
            )
        }
    }

    private fun cancelAllNotifications(context: Context, intent: Intent) {
        ScheduleNotificationManager.cancelClassNotifications(
            context,
            intent.getLongExtra("classId", 0)
        )
    }

    companion object {
        const val ACTION_CLASS_START = "com.rasd.app.SCHEDULE_START"
        const val ACTION_PERIOD_REMINDER = "com.rasd.app.PERIOD_REMINDER"
        const val ACTION_CANCEL_ALL = "com.rasd.app.CANCEL_ALL"
    }
}

/**
 * أدوات جدولة الإشعارات الدورية عبر WorkManager.
 */
object ScheduleAlarmWorker {

    fun schedulePeriodicAlarm(
        context: Context,
        hourOfDay: Int,
        minute: Int,
        periodNumber: Int
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ScheduleReminderWorker>(1L, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay((hourOfDay * 60 + minute).toLong(), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "schedule_$periodNumber",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelPeriodicAlarm(context: Context, periodNumber: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("schedule_$periodNumber")
    }
}

/**
 * Worker يُشغّل منطق تذكير الفترة (هيكل فارغ حالياً).
 */
class ScheduleReminderWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        return Result.success()
    }
}

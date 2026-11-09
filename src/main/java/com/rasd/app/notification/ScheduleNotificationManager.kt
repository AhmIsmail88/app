package com.rasd.app.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rasd.app.R
import com.rasd.app.ui.MainActivity

object ScheduleNotificationManager {

    private const val CHANNEL_ID = "schedule_notifications"
    private const val PERIOD_SLOTS = 12

    /**
     * ينشئ قناة الإشعارات.
     * على Android 8+ (minSdk 26) الإشعار بلا قناة يُهمَل بصمت، لذا النداء إلزامي عند الإقلاع.
     * آمن للتكرار: يحدّث الاسم والوصف حسب لغة التطبيق الحالية.
     */
    fun createNotificationChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    /** هل يسمح النظام بإظهار الإشعارات الآن؟ */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * يعرض الإشعار فقط بعد التأكد من الإذن، ويتحمّل أي رفض من النظام.
     * ملاحظة: الفحص داخل [canNotify] صريح ومقصود (لينت لا يتتبّع الفحص غير المباشر).
     */
    @SuppressLint("MissingPermission")
    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (!canNotify(context)) return
        runCatching { NotificationManagerCompat.from(context).notify(id, builder.build()) }
    }

    private fun builder(context: Context): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

    private fun openSchedule(context: Context, requestCode: Int, periodNumber: Int? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("screen", "schedule")
            if (periodNumber != null) putExtra("periodNumber", periodNumber)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** إشعار بدء الحصة. */
    fun showClassStartedNotification(
        context: Context,
        classId: Long,
        periodNumber: Int,
        teacherName: String
    ) {
        post(
            context,
            periodNumber * 1000,
            builder(context)
                .setContentTitle(context.getString(R.string.notif_started_title, periodNumber))
                .setContentText(context.getString(R.string.notif_started_text, periodNumber, teacherName))
                .setContentIntent(openSchedule(context, 0))
        )
    }

    /** إشعار انتهاء الحصة وتذكير بالحصة القادمة. */
    fun showClassEndedNotification(
        context: Context,
        currentPeriodNumber: Int,
        nextClassTime: String,
        nextClassDay: String?
    ) {
        val text = if (nextClassDay != null) {
            context.getString(R.string.notif_ended_text_next, nextClassTime, nextClassDay)
        } else {
            context.getString(R.string.notif_ended_text)
        }
        post(
            context,
            currentPeriodNumber * 1000 + 1,
            builder(context)
                .setContentTitle(context.getString(R.string.notif_ended_title))
                .setContentText(text)
                .setContentIntent(openSchedule(context, 1))
        )
    }

    /** تذكير قبل بدء الحصة. */
    fun showPeriodReminderNotification(
        context: Context,
        periodNumber: Int,
        classId: Long,
        startTime: String,
        teacherName: String
    ) {
        post(
            context,
            periodNumber * 1000 + 2,
            builder(context)
                .setContentTitle(context.getString(R.string.notif_reminder_title, periodNumber))
                .setSubText(startTime)
                .setContentText(context.getString(R.string.notif_reminder_text, periodNumber, teacherName))
                .setContentIntent(openSchedule(context, 2, periodNumber))
        )
    }

    /** يلغي كل تنبيهات الحصص: بدء + انتهاء + تذكير. */
    fun cancelClassNotifications(context: Context, classId: Long) {
        val nm = NotificationManagerCompat.from(context)
        for (i in 1..PERIOD_SLOTS) {
            nm.cancel(i * 1000)
            nm.cancel(i * 1000 + 1)
            nm.cancel(i * 1000 + 2)
        }
    }
}

package com.rasd.app.notification

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * إذن «المنبّهات والتذكيرات» الخاص (Android 12+).
 *
 * بدونه يجدول النظام تنبيهات الحصة بشكل تقريبي (قد تتأخر دقائق)،
 * و[PeriodAlarmScheduler] يتعامل مع ذلك بأمان عبر جدولة غير دقيقة بدل الانهيار.
 */
object ExactAlarmPermission {

    /** هل يسمح النظام حالياً بجدولة تنبيهات دقيقة؟ */
    fun canSchedule(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    /** يفتح صفحة النظام لتفعيل «المنبّهات والتذكيرات» لهذا التطبيق. */
    fun request(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val appUri = Uri.parse("package:" + context.packageName)
        val direct = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, appUri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(direct) }
            .onFailure { runCatching { context.startActivity(fallback) } }
    }
}

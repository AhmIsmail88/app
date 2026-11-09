package com.rasd.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rasd.app.data.dao.ClassPeriodDao
import com.rasd.app.notification.PeriodAlarmScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootEntryPoint {
    fun classPeriodDao(): ClassPeriodDao
}

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val dao = EntryPointAccessors
            .fromApplication(context.applicationContext, BootEntryPoint::class.java)
            .classPeriodDao()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dao.getAllPeriods().forEach { p ->
                    PeriodAlarmScheduler.schedule(
                        context,
                        p.id,
                        p.classId,
                        p.periodNumber,
                        p.dayOfWeek,
                        p.startTime,
                        p.teacherName
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}

package com.rasd.app

import android.app.Application
import com.rasd.app.notification.ScheduleNotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RasdApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // إنشاء قناة إشعارات الحصة مبكراً: بدونها لا يظهر أي إشعار على Android 8+
        ScheduleNotificationManager.createNotificationChannel(this)
    }
}

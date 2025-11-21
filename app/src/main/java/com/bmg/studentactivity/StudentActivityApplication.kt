package com.bmg.studentactivity

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudentActivityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("StudentActivityApplication", "Application created")
        // Keep the application process alive
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND)
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d("StudentActivityApplication", "Application terminating")
    }
}


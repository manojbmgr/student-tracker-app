package com.bmg.studentactivity

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bmg.studentactivity.services.KeepAliveService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudentActivityApplication : Application(), LifecycleEventObserver {
    companion object {
        private const val TAG = "StudentActivityApp"
    }

    private var serviceStarted = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created")
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // Keep the application process alive
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND)
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Application terminating")
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_START) {
            Log.d(TAG, "App is in FOREGROUND")
            if (!serviceStarted) {
                // Start the KeepAliveService once when the app first comes to the foreground.
                // The service will then manage its own lifecycle and alarm scheduling.
                try {
                    val serviceIntent = Intent(this, KeepAliveService::class.java).apply {
                        action = KeepAliveService.ACTION_CHECK_ALARMS
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    serviceStarted = true
                    Log.d(TAG, "KeepAliveService started from Application class.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting KeepAliveService: ${e.message}", e)
                }
            }
        }
    }
}


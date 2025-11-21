package com.bmg.studentactivity.services

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bmg.studentactivity.R
import com.bmg.studentactivity.ui.activities.ActivitiesActivity

class KeepAliveService : Service() {
    companion object {
        private const val CHANNEL_ID = "KeepAliveServiceChannel"
        private const val NOTIFICATION_ID = 2
        private const val TAG = "KeepAliveService"
        private const val CHECK_INTERVAL = 30000L // Check every 30 seconds
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAndRestartApp()
            handler.postDelayed(this, CHECK_INTERVAL)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.d(TAG, "KeepAliveService started")
        
        // Start periodic check
        handler.postDelayed(checkRunnable, CHECK_INTERVAL)
    }
    
    private fun checkAndRestartApp() {
        if (!isAppRunning()) {
            Log.w(TAG, "App is not running, restarting...")
            val activityIntent = Intent(applicationContext, ActivitiesActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(activityIntent)
        }
    }
    
    private fun isAppRunning(): Boolean {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val packageName = packageName
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For Android 6.0+, check running app processes
                val runningProcesses = activityManager.runningAppProcesses
                runningProcesses?.forEach { processInfo ->
                    if (processInfo.processName == packageName && 
                        processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        return true
                    }
                }
            } else {
                // For older versions, use getRunningTasks (deprecated but works)
                @Suppress("DEPRECATION")
                val runningTasks = activityManager.getRunningTasks(10)
                for (taskInfo in runningTasks) {
                    if (taskInfo.topActivity?.packageName == packageName) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is running: ${e.message}", e)
        }
        return false
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Use START_STICKY to ensure service restarts even if killed without intent
        // START_REDELIVER_INTENT only works if there's an intent to redeliver
        // START_STICKY ensures service restarts even if killed by system
        return START_STICKY
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "=== APP REMOVED FROM TASK MANAGER ===")
        Log.w(TAG, "KeepAliveService: App swiped away, scheduling restart...")
        
        // DO NOT stop alarm service - alarms should continue playing!
        // The alarm service is independent and should keep running
        
        // Use AlarmManager to ensure service restarts even if killed
        // This is more reliable than trying to restart immediately
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val restartIntent = Intent(applicationContext, KeepAliveService::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                0,
                restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Schedule restart in 2 seconds
            val triggerTime = System.currentTimeMillis() + 2000
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.w(TAG, "Scheduled KeepAliveService restart via AlarmManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling service restart: ${e.message}", e)
            // Fallback: try immediate restart
            try {
                val restartIntent = Intent(applicationContext, KeepAliveService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(restartIntent)
                } else {
                    startService(restartIntent)
                }
                Log.w(TAG, "KeepAliveService restarted immediately (fallback)")
            } catch (e2: Exception) {
                Log.e(TAG, "Error in fallback restart: ${e2.message}", e2)
            }
        }
        
        // Also restart the main activity after a short delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val activityIntent = Intent(applicationContext, com.bmg.studentactivity.ui.activities.ActivitiesActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                startActivity(activityIntent)
                Log.w(TAG, "ActivitiesActivity restarted after task removal")
            } catch (e: Exception) {
                Log.e(TAG, "Error restarting activity: ${e.message}", e)
            }
        }, 2000) // 2 second delay to ensure service is ready
        
        Log.w(TAG, "=== KEEPALIVE SERVICE RESTART SCHEDULED (ALARMS CONTINUE) ===")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Keep Alive",
                NotificationManager.IMPORTANCE_HIGH // High importance to prevent removal
            ).apply {
                description = "Keeps the app running in the background"
                setShowBadge(false)
                setBypassDnd(true) // Bypass Do Not Disturb
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, ActivitiesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Student Activity Monitor")
            .setContentText("App is running in background - Do not close")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority to prevent removal
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setAutoCancel(false) // Prevent auto-cancel
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        Log.w(TAG, "=== KeepAliveService destroyed ===")
        
        // Restart service using AlarmManager for more reliable restart
        // This ensures service restarts even if the process is killed
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val restartIntent = Intent(applicationContext, KeepAliveService::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                0,
                restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Schedule restart in 1 second
            val triggerTime = System.currentTimeMillis() + 1000
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.w(TAG, "Scheduled KeepAliveService restart via AlarmManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling service restart: ${e.message}", e)
            // Fallback: try immediate restart
            try {
                val restartIntent = Intent(applicationContext, KeepAliveService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(restartIntent)
                } else {
                    startService(restartIntent)
                }
                Log.w(TAG, "KeepAliveService restarted immediately (fallback)")
            } catch (e2: Exception) {
                Log.e(TAG, "Error in fallback restart: ${e2.message}", e2)
            }
        }
    }
}


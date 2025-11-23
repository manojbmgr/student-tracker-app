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
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bmg.studentactivity.R
import com.bmg.studentactivity.ui.activities.ActivitiesActivity
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KeepAliveService : Service() {
    companion object {
        private const val CHANNEL_ID = "KeepAliveServiceChannel"
        private const val RESTART_CHANNEL_ID = "AppRestartChannel"
        private const val NOTIFICATION_ID = 2
        private const val RESTART_NOTIFICATION_ID = 4
        private const val TAG = "KeepAliveService"
        private const val CHECK_INTERVAL = 10000L // Check every 10 seconds (more frequent to catch kills faster)
        private const val ALARM_CHECK_INTERVAL = 60000L // Check for overdue alarms every 60 seconds

        const val ACTION_CHECK_ALARMS = "com.bmg.studentactivity.CHECK_ALARMS"

        private val isSyncing = AtomicBoolean(false)
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAndRestartApp()
            handler.postDelayed(this, CHECK_INTERVAL)
        }
    }
    private var wakeLock: PowerManager.WakeLock? = null
    
    override fun onCreate() {
        super.onCreate()
        // If the service is recreated, we assume any previous sync failed or was killed.
        // Resetting the flag allows the next alarm trigger to start a new sync.
        isSyncing.set(false)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.d(TAG, "KeepAliveService started")

        // Immediately check if app is running and restart if needed
        handler.post {
            if (!isAppRunning()) {
                Log.w(TAG, "App not running on service start, showing notification to restart.")
                showRestartNotification()
            }
        }
        
        // Start periodic checks
        handler.postDelayed(checkRunnable, CHECK_INTERVAL)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld != true) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "StudentActivity::KeepAliveWakeLock"
            ).apply {
                acquire(ALARM_CHECK_INTERVAL + 10000L /* 70 seconds */)
            }
            Log.d(TAG, "KeepAlive WakeLock acquired.")
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            wakeLock = null
            Log.d(TAG, "KeepAlive WakeLock released.")
        }
    }

    private fun checkAndRestartApp() {
        // This function is now responsible only for ensuring the service stays alive if the app is killed.
        // It will no longer force the UI to open. The UI should only open for a full-screen alarm.
        if (!isAppRunning()) {
            Log.w(TAG, "App process is not running. The service will continue and restart itself if needed.")
            // We don't want to restart the activity here. Let the user open it.
            // The service's onDestroy will handle rescheduling itself via AlarmManager if the process dies.
        }
    }

    private fun restartAppActivity() {
        try {
            Log.w(TAG, "Attempting to start ActivitiesActivity directly.")
            val intent = Intent(this, ActivitiesActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting activity directly. Scheduling with AlarmManager.", e)
            scheduleAppRestart()
        }
    }

    private fun scheduleAppRestart() {
        try {
            Log.w(TAG, "Scheduling app restart with AlarmManager.")
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(this, ActivitiesActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                5, // Unique request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + 2000
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling app restart with AlarmManager.", e)
        }
    }

    private fun showRestartNotification() {
        Log.w(TAG, "Showing notification to restart app.")

        val intent = Intent(this, ActivitiesActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            3, // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, RESTART_CHANNEL_ID)
            .setContentTitle("Student Activity was closed")
            .setContentText("Tap to reopen the app and resume monitoring.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(RESTART_NOTIFICATION_ID, notification)
    }
    
    private fun scheduleNextAlarmCheck() {
        Log.d(TAG, "Scheduling next alarm check.")
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(this, com.bmg.studentactivity.receivers.AlarmCheckReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                7, // Unique request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + ALARM_CHECK_INTERVAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling next alarm check.", e)
        }
    }
    
    private fun isAppRunning(): Boolean {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningProcesses = activityManager.runningAppProcesses ?: return false
            val packageName = packageName
            // A process is considered "running" if it's not cached and has some importance.
            // This is less strict than IMPORTANCE_FOREGROUND and more reliable.
            return runningProcesses.any { it.processName == packageName && it.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is running: ${e.message}", e)
        }
        return false
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received with action: ${intent?.action}")

        if (intent?.action == ACTION_CHECK_ALARMS) {
            if (isSyncing.compareAndSet(false, true)) {
                Log.d(TAG, "Starting a new alarm check.")
                acquireWakeLock()
                checkForOverdueAlarms()
            } else {
                Log.w(TAG, "Alarm check is already in progress. Skipping this trigger.")
            }
        }
        
        // Use START_STICKY to ensure service restarts even if killed without intent
        // START_REDELIVER_INTENT only works if there's an intent to redeliver
        // START_STICKY ensures service restarts even if killed by system
        return START_STICKY
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "=== APP REMOVED FROM TASK MANAGER ===")
        Log.w(TAG, "KeepAliveService: App swiped away. Scheduling service restart to ensure it continues.")

        // Use AlarmManager to ensure the service restarts. Do not restart the activity.
        scheduleServiceRestart()

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

    private fun createRestartNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RESTART_CHANNEL_ID,
                "App Restart",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification to restart the app after it was closed"
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

    private fun checkForOverdueAlarms() {
        Log.d(TAG, "=== Checking for overdue alarms in background ===")

        try {
            val tokenManager = com.bmg.studentactivity.utils.TokenManager(this)
            val apiKey = tokenManager.getApiKey()

            if (apiKey.isNullOrEmpty()) {
                Log.d(TAG, "No API key, skipping alarm check")
                releaseWakeLock() // Release lock if no API key
                return
            }

            // Use coroutine scope for async API call
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Initialize API client
                    val apiClient = com.bmg.studentactivity.data.api.ApiClient
                    apiClient.initialize { tokenManager.getApiKey() }
                    val apiService = apiClient.apiService

                    if (apiService == null) {
                        Log.e(TAG, "API service is null, cannot check for alarms")
                        return@launch
                    }

                    val activityRepository = com.bmg.studentactivity.data.repository.ActivityRepository(apiService, this@KeepAliveService)

                    // Fetch activities from API
                    val result = activityRepository.getActivities()

                    result.onSuccess { response ->
                        if (response.success && response.data != null) {
                            val allActivities = response.data.students?.flatMap { it.activities } ?: emptyList()

                            // Filter for overdue tasks with alarms
                            val overdueWithAlarms = allActivities.filter { activity ->
                                val isOverdue = activity.isOverdue == true
                                val isCompleted = activity.isCompleted == true || activity.isCompletedToday == true
                                val hasAlarmUrl = !activity.alarmAudioUrl.isNullOrEmpty()
                                isOverdue && !isCompleted && hasAlarmUrl
                            }

                            Log.d(TAG, "Found ${overdueWithAlarms.size} overdue tasks with alarms")

                            if (overdueWithAlarms.isNotEmpty()) {
                                // Check if AlarmService is already running
                                val isAlarmServiceRunning = isServiceRunning(AlarmService::class.java)

                                if (!isAlarmServiceRunning) {
                                    Log.w(TAG, "=== Starting AlarmService for ${overdueWithAlarms.size} overdue tasks ===")
                                    // Start alarm service on main thread
                                    Handler(Looper.getMainLooper()).post {
                                        try {
                                            val alarmIntent = Intent(this@KeepAliveService, AlarmService::class.java).apply {
                                                action = AlarmService.ACTION_START_ALARM
                                                val activitiesJson = com.google.gson.Gson().toJson(overdueWithAlarms)
                                                putExtra(AlarmService.EXTRA_OVERDUE_ACTIVITIES, activitiesJson)
                                            }

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                startForegroundService(alarmIntent)
                                            } else {
                                                startService(alarmIntent)
                                            }
                                            Log.w(TAG, "AlarmService started from KeepAliveService")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error starting AlarmService: ${e.message}", e)
                                        }
                                    }
                                } else {
                                    Log.d(TAG, "AlarmService already running, no need to start")
                                }
                            } else {
                                Log.d(TAG, "No overdue tasks with alarms found")
                            }
                        }
                    }.onFailure { exception ->
                        Log.e(TAG, "Failed to check for overdue alarms: ${exception.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in alarm check coroutine: ${e.message}", e)
                } finally {
                    // Always release the wake lock and reset the sync flag from the coroutine
                    Log.d(TAG, "Alarm check finished. Releasing lock and rescheduling.")
                    scheduleNextAlarmCheck()
                    isSyncing.set(false)
                    releaseWakeLock()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for overdue alarms: ${e.message}", e)
            isSyncing.set(false) // Ensure flag is reset on error
            releaseWakeLock() // Ensure lock is released on error
        }
    }
    
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val services = activityManager.getRunningServices(Integer.MAX_VALUE)
            for (service in services) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service: ${e.message}", e)
        }
        return false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        releaseWakeLock()
        Log.w(TAG, "=== KeepAliveService destroyed. Scheduling service restart. ===")
        // When killed, only schedule this service to restart. Do not force the app UI to open.
        scheduleServiceRestart()
    }

    private fun scheduleServiceRestart() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

            val serviceRestartIntent = Intent(applicationContext, KeepAliveService::class.java).apply {
                action = ACTION_CHECK_ALARMS
            }
            val servicePendingIntent = PendingIntent.getService(
                applicationContext,
                0, // request code
                serviceRestartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + 2000
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    servicePendingIntent
                )
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, servicePendingIntent)
            }
            Log.w(TAG, "Scheduled KeepAliveService restart via AlarmManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling service restart: ${e.message}", e)
        }
    }
}


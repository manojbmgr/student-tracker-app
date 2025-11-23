package com.bmg.studentactivity.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bmg.studentactivity.R
import com.bmg.studentactivity.data.models.Activity
import com.bmg.studentactivity.ui.activities.ActivitiesActivity
import com.bmg.studentactivity.ui.activities.AlarmFullScreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val mediaPlayerLock = Any()
    private var currentAlarmIndex = 0
    @Volatile
    private var overdueActivities: List<Activity> = emptyList()
    private val overdueActivitiesLock = Any()
    private var isPlaying = false
    private var alarmJob: Job? = null
    private var checkOverdueJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var wasPlayingBeforeCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isForegroundServiceStarted = false
    
    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    private val telephonyManager: TelephonyManager by lazy {
        getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }
    
    private val powerManager: PowerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    companion object {
        private const val CHANNEL_ID = "AlarmServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_ALARM = "com.bmg.studentactivity.START_ALARM"
        const val ACTION_STOP_ALARM = "com.bmg.studentactivity.STOP_ALARM"
        const val EXTRA_OVERDUE_ACTIVITIES = "extra_overdue_activities"
        
        private const val TAG = "AlarmService"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupPhoneStateListener()
        acquireWakeLock()
        Log.d(TAG, "AlarmService created with wake lock")
    }
    
    private fun acquireWakeLock() {
        try {
            // Use PARTIAL_WAKE_LOCK to keep CPU running even when screen is off
            // This ensures alarms continue playing when device is locked or screen is off
            // 
            // What PARTIAL_WAKE_LOCK does:
            // - Keeps CPU running (allows MediaPlayer to play audio)
            // - Works when screen is OFF (device in sleep mode)
            // - Works when screen is LOCKED (screen on but locked)
            // - Does NOT wake the screen (screen stays off/locked)
            // - Uses minimal battery (only CPU, not screen)
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "StudentActivity::AlarmWakeLock"
            ).apply {
                acquire(10 * 60 * 60 * 1000L /*10 hours*/) // Acquire for up to 10 hours
                Log.d(TAG, "Wake lock acquired - alarms will work with screen locked/off")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock: ${e.message}", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock: ${e.message}", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // We will call startForeground later, once we have an activity to show.
        // The WakeLock will keep the service alive until then.
        Log.d(TAG, "onStartCommand received with action: ${intent?.action}")

        // Ensure wake lock is held
        if (wakeLock?.isHeld != true) {
            acquireWakeLock()
        }
        
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val activitiesJson = intent.getStringExtra(EXTRA_OVERDUE_ACTIVITIES)
                if (activitiesJson != null) {
                    val activities = com.google.gson.Gson().fromJson(
                        activitiesJson,
                        Array<Activity>::class.java
                    ).toList()
                    // If already playing, update the list; otherwise start new
                    if (isPlaying && overdueActivities.isNotEmpty()) {
                        updateOverdueActivities(activities)
                    } else {
                        startAlarms(activities)
                    }
                }
            }
            ACTION_STOP_ALARM -> {
                Log.w(TAG, "=== STOP ALARM COMMAND RECEIVED ===")
                stopAlarms()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                Log.w(TAG, "=== ALARM SERVICE STOPPED ===")
            }
        }
        // Use START_STICKY to ensure service restarts even if killed without intent
        // START_REDELIVER_INTENT only works if there's an intent to redeliver
        // START_STICKY ensures service restarts even if killed by system
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun setupPhoneStateListener() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Use TelephonyCallback for Android 12+
                telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallStateChange(state)
                    }
                }
                telephonyManager.registerTelephonyCallback(
                    mainExecutor,
                    telephonyCallback!!
                )
                Log.d(TAG, "Registered TelephonyCallback for call detection")
            } else {
                // Use PhoneStateListener for older Android versions
                @Suppress("DEPRECATION")
                phoneStateListener = object : PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallStateChange(state)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
                Log.d(TAG, "Registered PhoneStateListener for call detection")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for phone state: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up phone state listener: ${e.message}", e)
        }
    }
    
    private fun handleCallStateChange(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Incoming call or call in progress - pause alarm
                if (isPlaying && mediaPlayer?.isPlaying == true) {
                    wasPlayingBeforeCall = true
                    try {
                        mediaPlayer?.pause()
                        Log.d(TAG, "Alarm paused due to incoming call")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error pausing alarm: ${e.message}", e)
                    }
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended - resume alarm if it was playing before
                if (wasPlayingBeforeCall && isPlaying) {
                    wasPlayingBeforeCall = false
                    try {
                        mediaPlayer?.start()
                        Log.d(TAG, "Alarm resumed after call ended")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming alarm: ${e.message}", e)
                        // If resume fails, try to play next alarm
                        playNextAlarm()
                    }
                }
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Use IMPORTANCE_MAX for alarms to ensure they work with locked screen
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Overdue Task Alarms",
                NotificationManager.IMPORTANCE_MAX // MAX importance for alarms
            ).apply {
                description = "Plays alarms for overdue tasks"
                setSound(null, null) // No sound for notification itself
                enableLights(true) // Enable LED
                enableVibration(false) // No vibration, only audio
                setBypassDnd(true) // Bypass Do Not Disturb
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // Show on lock screen
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created with MAX importance and lock screen visibility")
        }
    }
    
    private fun startAlarms(activities: List<Activity>) {
        val overdueTasks = activities.filter { 
            it.isOverdue == true && 
            (it.isCompleted != true && it.isCompletedToday != true) &&
            !it.alarmAudioUrl.isNullOrEmpty()
        }
        
        if (overdueTasks.isEmpty()) {
            Log.d(TAG, "No overdue tasks with alarm URLs, stopping service")
            stopAlarms()
            stopSelf()
            return
        }
        
        synchronized(overdueActivitiesLock) {
            overdueActivities = overdueTasks
        }
        currentAlarmIndex = 0
        
        // We will call startForeground from playNextAlarm, once we have a specific task.
        
        // Ensure wake lock is held
        if (wakeLock?.isHeld != true) {
            acquireWakeLock()
        }
        
        // Start overlay service
        startOverlayService(overdueTasks, 0)
        
        // Override volume and silent mode
        overrideVolumeAndSilentMode()
        
        // Start periodic check for overdue tasks (every 30 seconds)
        startPeriodicOverdueCheck()
        
        // Start playing alarms
        playNextAlarm()
    }
    
    private fun startPeriodicOverdueCheck() {
        checkOverdueJob?.cancel()
        checkOverdueJob = serviceScope.launch {
            try {
                var shouldContinue = true
                while (shouldContinue) {
                    delay(30_000) // Check every 30 seconds
                    Log.d(TAG, "Periodic check: Fetching fresh data from API...")
                    
                    // Fetch fresh data from API to check if tasks are still overdue
                    // This ensures we get updates even when tasks are completed from another device
                    try {
                        val tokenManager = com.bmg.studentactivity.utils.TokenManager(this@AlarmService)
                        val apiKey = tokenManager.getApiKey()
                        
                        if (!apiKey.isNullOrEmpty()) {
                            // Initialize API client
                            val apiClient = com.bmg.studentactivity.data.api.ApiClient
                            apiClient.initialize { tokenManager.getApiKey() }
                            val apiService = apiClient.apiService
                            
                            if (apiService != null) {
                                val activityRepository = com.bmg.studentactivity.data.repository.ActivityRepository(apiService, this@AlarmService)
                                
                                // Fetch fresh data from API
                                val result = activityRepository.getActivities()
                                
                                result.onSuccess { response ->
                                    if (response.success && response.data != null) {
                                        val allActivities = response.data.students?.flatMap { it.activities } ?: emptyList()
                                        
                                        // Filter for overdue tasks with alarms
                                        val freshOverdueTasks = allActivities.filter { activity ->
                                            activity.isOverdue == true &&
                                            (activity.isCompleted != true && activity.isCompletedToday != true) &&
                                            !activity.alarmAudioUrl.isNullOrEmpty()
                                        }
                                        
                                        Log.d(TAG, "Fresh API data: ${freshOverdueTasks.size} overdue tasks with alarms")
                                        
                                        // Update with fresh data
                                        synchronized(overdueActivitiesLock) {
                                            if (freshOverdueTasks.isEmpty() && overdueActivities.isNotEmpty()) {
                                                Log.w(TAG, "=== No overdue tasks found in fresh API data ===")
                                                Log.w(TAG, "Stopping alarms - all tasks completed or no longer overdue")
                                                stopAlarms()
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    stopForeground(true)
                                                }
                                                stopSelf()
                                                shouldContinue = false // Exit the loop instead of break
                                            } else if (freshOverdueTasks.size != overdueActivities.size) {
                                                Log.d(TAG, "Overdue count changed: ${overdueActivities.size} -> ${freshOverdueTasks.size}")
                                                overdueActivities = freshOverdueTasks
                                                // Update notification
                                                val currentActivity = if (currentAlarmIndex < overdueActivities.size) {
                                                    overdueActivities[currentAlarmIndex]
                                                } else null
                                                startForeground(NOTIFICATION_ID, createNotification(overdueActivities.size, currentActivity))
                                                // Update overlay
                                                updateOverlayService(overdueActivities, currentAlarmIndex)
                                                // Reset index if needed
                                                if (currentAlarmIndex >= overdueActivities.size) {
                                                    currentAlarmIndex = 0
                                                }
                                            } else {
                                                Log.d(TAG, "All ${overdueActivities.size} overdue tasks still active - alarms continue")
                                            }
                                        }
                                    }
                                }.onFailure { exception ->
                                    Log.e(TAG, "Failed to fetch fresh data: ${exception.message}")
                                    // Don't stop alarms if API call fails - continue with existing data
                                }
                            } else {
                                Log.e(TAG, "API service is null, cannot fetch fresh data")
                            }
                        } else {
                            Log.w(TAG, "No API key, cannot fetch fresh data")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching fresh data: ${e.message}", e)
                        // Continue with existing data if fetch fails
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // This is expected when the job is cancelled - don't log as error
                Log.d(TAG, "Periodic overdue check cancelled (expected)")
                throw e // Re-throw to properly handle cancellation
            } catch (e: Exception) {
                // Only log actual errors, not cancellation
                Log.e(TAG, "Error in periodic overdue check: ${e.message}", e)
            }
        }
    }
    
    private fun updateOverdueActivities(newActivities: List<Activity>) {
        val overdueTasks = newActivities.filter { 
            it.isOverdue == true && 
            (it.isCompleted != true && it.isCompletedToday != true) &&
            !it.alarmAudioUrl.isNullOrEmpty()
        }
        
        Log.d(TAG, "Updating overdue activities: ${overdueActivities.size} -> ${overdueTasks.size}")
        
        synchronized(overdueActivitiesLock) {
            overdueActivities = overdueTasks
        }
        
        if (overdueTasks.isEmpty()) {
            // No more overdue tasks, stop alarms
            Log.w(TAG, "=== No more overdue tasks, stopping alarms ===")
            stopAlarms()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        } else {
            // Update notification
            startForeground(NOTIFICATION_ID, createNotification(overdueTasks.size))
            // Reset index if current activity is no longer in list
            if (currentAlarmIndex >= overdueActivities.size) {
                currentAlarmIndex = 0
            }
            // Restart periodic check if not running
            if (checkOverdueJob?.isActive != true) {
                startPeriodicOverdueCheck()
            }
        }
    }
    
    private fun overrideVolumeAndSilentMode() {
        try {
            // Use STREAM_ALARM for better volume control and silent mode override
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            // Set volume to max with flags to override silent mode
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                maxVolume,
                AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
            )
            
            // Also set MUSIC stream to max as fallback
            val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                maxMusicVolume,
                AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
            )
            
            // Request audio focus with ALARM usage
            val result = audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            
            Log.d(TAG, "Volume set to max - Alarm: $maxVolume, Music: $maxMusicVolume, AudioFocus: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume: ${e.message}", e)
        }
    }
    
    private fun checkAndUpdateOverdueTasks() {
        // Filter out only explicitly completed tasks
        // Don't filter by isOverdue flag as it might be stale - rely on fresh data from ActivitiesActivity
        val beforeCount: Int
        synchronized(overdueActivitiesLock) {
            beforeCount = overdueActivities.size
            overdueActivities = overdueActivities.filter { activity ->
                (activity.isCompleted != true && activity.isCompletedToday != true) &&
                !activity.alarmAudioUrl.isNullOrEmpty()
            }
        
            if (overdueActivities.isEmpty() && beforeCount > 0) {
                Log.w(TAG, "All tasks completed, stopping alarms")
                stopAlarms()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                return
            }
        
            if (beforeCount != overdueActivities.size) {
                Log.d(TAG, "Updated overdue count: $beforeCount -> ${overdueActivities.size}")
            }
        }
        
        // Update notification
        val currentActivity = if (currentAlarmIndex < overdueActivities.size) {
            overdueActivities[currentAlarmIndex]
        } else null
        startForeground(NOTIFICATION_ID, createNotification(overdueActivities.size, currentActivity))
        
        // Update overlay
        updateOverlayService(overdueActivities, currentAlarmIndex)
    }
    
    private fun playNextAlarm() {
        synchronized(overdueActivitiesLock) {
            if (overdueActivities.isEmpty()) {
                // If list is empty after an alarm completes, stop the service.
                Log.d(TAG, "No more overdue activities, stopping alarm service.")
                stopAlarms()
                stopSelf()
                return
            }
            
            if (currentAlarmIndex >= overdueActivities.size) {
                // Loop back to start
                currentAlarmIndex = 0
            }
            
            val activity = overdueActivities[currentAlarmIndex]
            val alarmUrl = activity.alarmAudioUrl
            
            if (alarmUrl.isNullOrEmpty() || activity.isCompleted == true || activity.isCompletedToday == true) {
                // Skip to next if no alarm URL or if task was just completed
                currentAlarmIndex++
                playNextAlarm() // Recursively call to find the next valid alarm
                return
            }
        }
        
        // Play the alarm (moved outside the synchronized block)
        val activityToPlay = synchronized(overdueActivitiesLock) {
            if (currentAlarmIndex < overdueActivities.size) {
                overdueActivities[currentAlarmIndex]
            } else {
                null
            }
        }
        activityToPlay?.let { playAlarmForActivity(it) }
    }

    private fun playAlarmForActivity(activity: Activity) {
        // First, ensure any existing player is released.
        releaseMediaPlayer()

        try {
            synchronized(mediaPlayerLock) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED) // Enforce audibility
                            .build()
                    )
                    
                    // Set volume to max
                    setVolume(1.0f, 1.0f)
                    
                    setDataSource(activity.alarmAudioUrl)
                    setOnPreparedListener { mp ->
                        this@AlarmService.isPlaying = true
                        // Ensure volume is max before playing
                        mp.setVolume(1.0f, 1.0f)
                        
                        // CRITICAL: Call startForeground HERE for the first time.
                        // This ensures the notification has the full-screen intent before the system sees it.
                        startForeground(NOTIFICATION_ID, createNotification(overdueActivities.size, activity))
                        isForegroundServiceStarted = true
                        Log.d(TAG, "Service started in foreground with full-screen intent.")
                        
                        mp.start()
                        Log.d(TAG, "Playing alarm for: ${activity.displayTitle} (URL: ${activity.alarmAudioUrl})")
                        Log.d(TAG, "Service is in foreground - should work with locked screen")
                        
                        // Update overlay with current index
                        updateOverlayService(overdueActivities, currentAlarmIndex)
                    }
                    setOnCompletionListener { mp ->
                        this@AlarmService.isPlaying = false
                        Log.d(TAG, "Alarm completed for: ${activity.displayTitle}")
                        
                        // Release the player and move to the next alarm.
                        releaseMediaPlayer()
                        synchronized(overdueActivitiesLock) {
                            currentAlarmIndex++
                        }
                        playNextAlarm()
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        this@AlarmService.isPlaying = false
                        
                        // Release the player on error and move to the next alarm.
                        releaseMediaPlayer()
                        synchronized(overdueActivitiesLock) {
                            this@AlarmService.currentAlarmIndex++
                        }
                        playNextAlarm()
                        true
                    }
                    prepareAsync()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error preparing media player: ${e.message}", e)
            releaseMediaPlayer()
            synchronized(overdueActivitiesLock) {
                currentAlarmIndex++
            }
            playNextAlarm()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm: ${e.message}", e)
            releaseMediaPlayer()
            synchronized(overdueActivitiesLock) {
                currentAlarmIndex++
            }
            playNextAlarm()
        }
    }
    
    private fun stopAlarms() {
        Log.d(TAG, "Stopping alarms...")
        try {
            isPlaying = false
            releaseMediaPlayer()
            alarmJob?.cancel()
            alarmJob = null
            
            // Stop periodic check
            checkOverdueJob?.cancel()
            checkOverdueJob = null
            
            // Clear overdue activities to indicate intentional stop
            synchronized(overdueActivitiesLock) {
                overdueActivities = emptyList()
            }
            currentAlarmIndex = 0
            
            // Hide overlay
            hideOverlayService()
            
            // Restore audio focus
            try {
                audioManager.abandonAudioFocus(null)
            } catch (e: Exception) {
                Log.e(TAG, "Error abandoning audio focus: ${e.message}", e)
            }
            
            Log.d(TAG, "Alarms stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarms: ${e.message}", e)
        }
    }
    
    private fun startOverlayService(activities: List<Activity>, currentIndex: Int) {
        try {
            val intent = Intent(this, AlarmOverlayService::class.java).apply {
                action = AlarmOverlayService.ACTION_SHOW_OVERLAY
                val activitiesJson = com.google.gson.Gson().toJson(activities)
                putExtra(AlarmOverlayService.EXTRA_OVERDUE_ACTIVITIES, activitiesJson)
                putExtra(AlarmOverlayService.EXTRA_CURRENT_INDEX, currentIndex)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting overlay service: ${e.message}", e)
        }
    }
    
    private fun updateOverlayService(activities: List<Activity>, currentIndex: Int) {
        try {
            val intent = Intent(this, AlarmOverlayService::class.java).apply {
                action = AlarmOverlayService.ACTION_UPDATE_OVERLAY
                val activitiesJson = com.google.gson.Gson().toJson(activities)
                putExtra(AlarmOverlayService.EXTRA_OVERDUE_ACTIVITIES, activitiesJson)
                putExtra(AlarmOverlayService.EXTRA_CURRENT_INDEX, currentIndex)
            }
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overlay service: ${e.message}", e)
        }
    }
    
    private fun hideOverlayService() {
        try {
            val intent = Intent(this, AlarmOverlayService::class.java).apply {
                action = AlarmOverlayService.ACTION_HIDE_OVERLAY
            }
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlay service: ${e.message}", e)
        }
    }
    
    private fun releaseMediaPlayer() {
        synchronized(mediaPlayerLock) {
            try {
                mediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                    }
                    // Resets the player to its uninitialized state.
                    reset()
                    // Releases all resources.
                    release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing media player: ${e.message}", e)
            }
            mediaPlayer = null
        }
    }
    
    private fun createNotification(overdueCount: Int, currentActivity: Activity? = null): Notification {
        val intent = Intent(this, ActivitiesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (currentActivity != null && isPlaying) {
            "🔔 Playing: ${currentActivity.displayTitle}"
        } else {
            "Overdue Tasks Alarm"
        }
        
        val text = if (currentActivity != null && isPlaying) {
            "Student: ${currentActivity.studentName ?: currentActivity.studentEmail ?: "Unknown"}\n" +
            "Total overdue: $overdueCount task(s)"
        } else {
            "Playing alarms for $overdueCount overdue task(s)"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Make it non-dismissible
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(null) // No notification sound, only alarm audio
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Immediate foreground service
            .setShowWhen(true)
            .setAutoCancel(false) // Don't auto-cancel

        // ONLY add the full-screen intent if we have a current activity to show
        if (currentActivity != null) {
            val fullScreenIntent = Intent(this, AlarmFullScreenActivity::class.java).apply {
                putExtra(AlarmFullScreenActivity.EXTRA_ACTIVITY, com.google.gson.Gson().toJson(currentActivity))
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                8, // Unique request code
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }
        
        return builder.build()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "=== ALARM SERVICE TASK REMOVED ===")
        Log.w(TAG, "AlarmService: App swiped away, but alarms will continue!")
        // DO NOT stop alarms - they should continue playing even if app is swiped
        // The service will continue running in the foreground
        // Only stop if overdueActivities is empty (intentional stop)
        synchronized(overdueActivitiesLock) {
            if (overdueActivities.isEmpty()) {
                Log.w(TAG, "No overdue activities, stopping service")
                try {
                    stopAlarms()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        stopForeground(Service.STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping service: ${e.message}", e)
                }
            } else {
                Log.w(TAG, "Alarms continue playing (${overdueActivities.size} overdue tasks)")
                // Ensure service stays in foreground even after task removal
                // This is critical for background execution
                try {
                    startForeground(NOTIFICATION_ID, createNotification(overdueActivities.size))
                    Log.w(TAG, "Service kept in foreground after task removal")
                } catch (e: Exception) {
                    Log.e(TAG, "Error keeping service in foreground: ${e.message}", e)
                }
                // Service continues running - alarms keep playing
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlarmService destroyed")

        // Release wake lock
        releaseWakeLock()
        
        // Unregister phone state listener
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                telephonyCallback?.let {
                    telephonyManager.unregisterTelephonyCallback(it)
                }
            } else {
                @Suppress("DEPRECATION")
                phoneStateListener?.let {
                    telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
            Log.d(TAG, "Unregistered phone state listener")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering phone state listener: ${e.message}", e)
        }
        
        try {
            // Save state before stopping - check if we should restart
            val activitiesToRestore = synchronized(overdueActivitiesLock) {
                 overdueActivities.toList()
            }
            val wasPlaying = isPlaying
            val shouldRestart = activitiesToRestore.isNotEmpty() && wasPlaying

            stopAlarms()

            // Only restart if service was unexpectedly killed while playing (not intentionally stopped)
            if (shouldRestart) {
                // Service was killed unexpectedly, restart it
                Log.w(TAG, "Service killed unexpectedly while playing, scheduling restart...")
                scheduleAlarmServiceRestart(activitiesToRestore)
                // Do not restart the app UI from here. The full-screen intent is the only mechanism that should open the UI.
            } else {
                Log.d(TAG, "Service stopped intentionally or no activities to restore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }
    }

    private fun scheduleAlarmServiceRestart(activitiesToRestore: List<Activity>) {
        try {
            val restartIntent = Intent(applicationContext, AlarmService::class.java).apply {
                action = ACTION_START_ALARM
                val activitiesJson = com.google.gson.Gson().toJson(activitiesToRestore)
                putExtra(EXTRA_OVERDUE_ACTIVITIES, activitiesJson)
            }

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                1, // unique request code
                restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = System.currentTimeMillis() + 2000
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.w(TAG, "Scheduled AlarmService restart via AlarmManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling service restart: ${e.message}", e)
        }
    }
}


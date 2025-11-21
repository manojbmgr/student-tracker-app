package com.bmg.studentactivity.receivers

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bmg.studentactivity.services.AlarmService
import com.bmg.studentactivity.ui.activities.ActivitiesActivity
import com.bmg.studentactivity.ui.settings.SettingsActivity
import com.bmg.studentactivity.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        // Use Log.w for better visibility in logcat
        Log.w("BootReceiver", "=== BOOT RECEIVER TRIGGERED ===")
        Log.w("BootReceiver", "Action: $action")
        Log.w("BootReceiver", "Package: ${context.packageName}")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.w("BootReceiver", "Device booted or app updated, scheduling app start")
            
            // Start KeepAliveService first to ensure app stays running
            try {
                val keepAliveIntent = Intent(context, com.bmg.studentactivity.services.KeepAliveService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(keepAliveIntent)
                } else {
                    context.startService(keepAliveIntent)
                }
                Log.w("BootReceiver", "Started KeepAliveService on boot")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error starting KeepAliveService: ${e.message}", e)
            }
            
            // Use Handler to delay start (wait 5 seconds after boot)
            // This ensures the system is fully ready and prevents immediate start issues
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.w("BootReceiver", "=== Attempting to start app after 5 second delay ===")
                    
                    // Check if API key is set
                    val prefs: SharedPreferences = context.getSharedPreferences(
                        Constants.PREF_NAME, 
                        Context.MODE_PRIVATE
                    )
                    val apiKey = prefs.getString(Constants.KEY_API_KEY, null)
                    
                    Log.w("BootReceiver", "API key present: ${!apiKey.isNullOrEmpty()}")
                    Log.w("BootReceiver", "API key length: ${apiKey?.length ?: 0}")
                    
                    if (!apiKey.isNullOrEmpty()) {
                        // Start the activity
                        val startIntent = Intent(context, ActivitiesActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        }
                        context.startActivity(startIntent)
                        Log.w("BootReceiver", "=== Successfully started ActivitiesActivity after boot ===")
                        
                        // Check for overdue tasks and start alarms after a longer delay
                        // This ensures the app has time to initialize and network is ready
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.w("BootReceiver", "=== Starting alarm check after 25 second delay ===")
                            checkAndStartAlarms(context, apiKey)
                        }, 25000) // 25 seconds delay to allow app to fully load data
                    } else {
                        // No API key, just start settings
                        val startIntent = Intent(context, SettingsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        context.startActivity(startIntent)
                        Log.w("BootReceiver", "Started SettingsActivity (no API key)")
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error starting app after boot: ${e.message}", e)
                    e.printStackTrace()
                    // Try again with just MainActivity as fallback
                    try {
                        val fallbackIntent = Intent(context, com.bmg.studentactivity.MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        context.startActivity(fallbackIntent)
                        Log.w("BootReceiver", "Started MainActivity as fallback")
                    } catch (e2: Exception) {
                        Log.e("BootReceiver", "Error starting MainActivity: ${e2.message}", e2)
                        e2.printStackTrace()
                    }
                }
            }, 5000) // 5 second delay to ensure system is ready
        }
    }
    
    private fun checkAndStartAlarms(context: Context, apiKey: String?) {
        if (apiKey.isNullOrEmpty()) {
            Log.w("BootReceiver", "No API key, skipping alarm check")
            return
        }
        
        Log.w("BootReceiver", "=== Starting alarm check after boot ===")
        Log.w("BootReceiver", "Context: ${context.javaClass.simpleName}")
        Log.w("BootReceiver", "API Key length: ${apiKey.length}")
        
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                // Wait a bit more to ensure network is ready
                Log.w("BootReceiver", "Waiting 5 seconds for network to be ready...")
                kotlinx.coroutines.delay(5000)
                
                Log.w("BootReceiver", "Initializing API client...")
                // Create repository instance to check for overdue tasks
                val tokenManager = com.bmg.studentactivity.utils.TokenManager(context)
                val storedApiKey = tokenManager.getApiKey()
                Log.w("BootReceiver", "Stored API key from TokenManager: ${!storedApiKey.isNullOrEmpty()}")
                
                val apiClient = com.bmg.studentactivity.data.api.ApiClient
                apiClient.initialize { tokenManager.getApiKey() }
                val apiService = apiClient.apiService
                
                if (apiService == null) {
                    Log.e("BootReceiver", "API service is null, cannot check for alarms")
                    Log.e("BootReceiver", "This might indicate ApiClient initialization failed")
                    return@launch
                }
                
                Log.w("BootReceiver", "API service initialized successfully")
                val activityRepository = com.bmg.studentactivity.data.repository.ActivityRepository(apiService, context)
                
                Log.w("BootReceiver", "Fetching activities from API...")
                Log.w("BootReceiver", "Base URL: ${com.bmg.studentactivity.utils.Constants.BASE_URL}")
                // Get activities
                val result = activityRepository.getActivities()
                
                result.onSuccess { response ->
                    Log.w("BootReceiver", "=== API CALL SUCCESSFUL ===")
                    Log.w("BootReceiver", "Response success: ${response.success}")
                    Log.w("BootReceiver", "Response data: ${response.data != null}")
                    
                    if (response.success && response.data != null) {
                        val allActivities = response.data.students?.flatMap { it.activities } ?: emptyList()
                        Log.w("BootReceiver", "Found ${allActivities.size} total activities")
                        Log.w("BootReceiver", "Number of students: ${response.data.students?.size ?: 0}")
                        
                        val overdueWithAlarms = allActivities.filter { activity ->
                            activity.isOverdue == true &&
                            (activity.isCompleted != true && activity.isCompletedToday != true) &&
                            !activity.alarmAudioUrl.isNullOrEmpty()
                        }
                        
                        Log.w("BootReceiver", "Found ${overdueWithAlarms.size} overdue tasks with alarms")
                        overdueWithAlarms.forEachIndexed { index, activity ->
                            Log.w("BootReceiver", "  [$index] ${activity.displayTitle} - ${activity.alarmAudioUrl}")
                        }
                        
                        if (overdueWithAlarms.isNotEmpty()) {
                            Log.w("BootReceiver", "=== STARTING ALARM SERVICE ===")
                            Log.w("BootReceiver", "Starting alarm service for ${overdueWithAlarms.size} overdue tasks")
                            
                            // Start alarm service on main thread
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    val alarmIntent = Intent(context, AlarmService::class.java).apply {
                                        action = AlarmService.ACTION_START_ALARM
                                        val activitiesJson = com.google.gson.Gson().toJson(overdueWithAlarms)
                                        putExtra(AlarmService.EXTRA_OVERDUE_ACTIVITIES, activitiesJson)
                                    }
                                    Log.w("BootReceiver", "Creating alarm intent with ${overdueWithAlarms.size} activities")
                                    
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(alarmIntent)
                                        Log.w("BootReceiver", "Called startForegroundService()")
                                    } else {
                                        context.startService(alarmIntent)
                                        Log.w("BootReceiver", "Called startService()")
                                    }
                                    Log.w("BootReceiver", "=== ALARM SERVICE START COMMAND SENT ===")
                                    
                                    // Verify service started after a delay
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        val isServiceRunning = isServiceRunning(context, AlarmService::class.java)
                                        Log.w("BootReceiver", "AlarmService running check: $isServiceRunning")
                                    }, 2000)
                                } catch (e: Exception) {
                                    Log.e("BootReceiver", "Error starting alarm service: ${e.message}", e)
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            Log.w("BootReceiver", "No overdue tasks with alarms found - no alarm service started")
                        }
                    } else {
                        Log.w("BootReceiver", "API response not successful or data is null")
                        Log.w("BootReceiver", "Response message: ${response.message}")
                    }
                }.onFailure { exception ->
                    Log.e("BootReceiver", "=== API CALL FAILED ===")
                    Log.e("BootReceiver", "Error checking for overdue tasks: ${exception.message}", exception)
                    exception.printStackTrace()
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "=== EXCEPTION IN ALARM CHECK ===")
                Log.e("BootReceiver", "Error in alarm check: ${e.message}", e)
                e.printStackTrace()
                // Don't crash, just log the error
            }
        }
    }
    
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val services = activityManager.getRunningServices(Integer.MAX_VALUE)
            for (service in services) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error checking service: ${e.message}", e)
        }
        return false
    }
}


package com.bmg.studentactivity.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bmg.studentactivity.R
import com.bmg.studentactivity.databinding.ActivityActivitiesBinding
import com.bmg.studentactivity.ui.settings.SettingsActivity
import com.bmg.studentactivity.ui.activities.adapters.StudentActivitiesAdapter
import com.bmg.studentactivity.ui.activities.ActivitiesViewModel.ActivityFilter
import com.bmg.studentactivity.services.AlarmService
import com.bmg.studentactivity.services.KeepAliveService
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivitiesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActivitiesBinding
    private val viewModel: ActivitiesViewModel by viewModels()
    private lateinit var adapter: StudentActivitiesAdapter
    private var autoRefreshJob: Job? = null
    
    @Inject
    lateinit var tokenManager: com.bmg.studentactivity.utils.TokenManager
    
    // Permission request launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            android.util.Log.d("ActivitiesActivity", "All permissions granted")
        } else {
            android.util.Log.w("ActivitiesActivity", "Some permissions denied: ${permissions.filter { !it.value }.keys}")
            Toast.makeText(this, "Some permissions are required for full functionality", Toast.LENGTH_LONG).show()
        }
    }
    
    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            android.util.Log.d("ActivitiesActivity", "Overlay permission granted")
        } else {
            android.util.Log.w("ActivitiesActivity", "Overlay permission denied")
        }
    }
    
    private val requestExactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            val hasPermission = alarmManager.canScheduleExactAlarms()
            if (hasPermission) {
                android.util.Log.d("ActivitiesActivity", "Exact alarm permission granted")
            } else {
                android.util.Log.w("ActivitiesActivity", "Exact alarm permission denied")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Activities"
        
        setupTabs()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        
        // Request all required permissions
        requestAllPermissions()
        
        // Request battery optimization exemption
        requestBatteryOptimizationExemption()
        
        // Start keep-alive service to keep app running
        startKeepAliveService()
        
        // Ensure alarm service is running if there are overdue tasks
        // This is important when activity is recreated after being swiped away
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            manageAlarms()
        }, 2000) // Wait 2 seconds for data to load
        
        // Verify API key is set before loading activities
        val apiKey = tokenManager.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            android.util.Log.w("ActivitiesActivity", "No API key found, navigating to settings")
            Toast.makeText(this, "Please set your API key in settings", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        } else {
            android.util.Log.d("ActivitiesActivity", "API key found (length: ${apiKey.length}), loading activities")
            viewModel.loadActivities()
            startAutoRefresh()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Keep app in foreground
        keepAppInForeground()
        // Restart auto-refresh if it's not running
        if (autoRefreshJob?.isActive != true) {
            android.util.Log.d("ActivitiesActivity", "Restarting auto-refresh in onResume")
            startAutoRefresh()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Keep app running even when paused
        keepAppInForeground()
        // Don't stop auto-refresh on pause - let it continue in background
        // This ensures data refreshes even when activity is not visible
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAutoRefresh()
        
        // Only restart service if activity is destroyed (but not finishing normally)
        // Don't restart activity itself to avoid loops - let KeepAliveService handle it
        if (!isFinishing) {
            android.util.Log.d("ActivitiesActivity", "Activity destroyed but not finishing, ensuring services are running")
            startKeepAliveService()
            // Don't restart activity here - let KeepAliveService handle app restart if needed
        }
    }
    
    override fun onStop() {
        super.onStop()
        // When activity goes to background, ensure service is running
        startKeepAliveService()
    }
    
    private fun requestAllPermissions() {
        android.util.Log.d("ActivitiesActivity", "=== Requesting all required permissions ===")
        
        val permissionsToRequest = mutableListOf<String>()
        
        // READ_PHONE_STATE - Required for call detection (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
                android.util.Log.d("ActivitiesActivity", "READ_PHONE_STATE permission needed")
            } else {
                android.util.Log.d("ActivitiesActivity", "READ_PHONE_STATE permission already granted")
            }
        }
        
        // POST_NOTIFICATIONS - Required for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                android.util.Log.d("ActivitiesActivity", "POST_NOTIFICATIONS permission needed")
            } else {
                android.util.Log.d("ActivitiesActivity", "POST_NOTIFICATIONS permission already granted")
            }
        }
        
        // Request runtime permissions
        if (permissionsToRequest.isNotEmpty()) {
            android.util.Log.d("ActivitiesActivity", "Requesting ${permissionsToRequest.size} permissions")
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            android.util.Log.d("ActivitiesActivity", "All runtime permissions already granted")
        }
        
        // SYSTEM_ALERT_WINDOW - Special permission for overlay (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            android.util.Log.d("ActivitiesActivity", "SYSTEM_ALERT_WINDOW permission needed")
            requestDrawOverAppsPermission()
        } else {
            android.util.Log.d("ActivitiesActivity", "SYSTEM_ALERT_WINDOW permission already granted")
        }
        
        // SCHEDULE_EXACT_ALARM - Required for Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                android.util.Log.d("ActivitiesActivity", "SCHEDULE_EXACT_ALARM permission needed")
                requestExactAlarmPermission()
            } else {
                android.util.Log.d("ActivitiesActivity", "SCHEDULE_EXACT_ALARM permission already granted")
            }
        }
    }
    
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                requestExactAlarmPermissionLauncher.launch(intent)
                android.util.Log.d("ActivitiesActivity", "Opened exact alarm permission request")
            } catch (e: Exception) {
                android.util.Log.e("ActivitiesActivity", "Error opening exact alarm settings: ${e.message}", e)
                Toast.makeText(this, "Please enable 'Schedule exact alarms' in app settings", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(PowerManager::class.java)
            val packageName = packageName
            
            // Always check and request if not already exempted
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                android.util.Log.d("ActivitiesActivity", "Battery optimization is enabled, requesting exemption")
                
                // Show a dialog first to inform user
                android.app.AlertDialog.Builder(this)
                    .setTitle("Battery Optimization")
                    .setMessage("To ensure the app runs continuously and alarms work properly, please disable battery optimization for this app.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                            android.util.Log.d("ActivitiesActivity", "Opened battery optimization request")
                        } catch (e: Exception) {
                            android.util.Log.e("ActivitiesActivity", "Error opening battery optimization: ${e.message}", e)
                            // Fallback: Open general battery settings
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                startActivity(intent)
                                Toast.makeText(this, "Please find '${getString(R.string.app_name)}' and disable battery optimization", Toast.LENGTH_LONG).show()
                            } catch (e2: Exception) {
                                android.util.Log.e("ActivitiesActivity", "Error opening battery settings: ${e2.message}", e2)
                                Toast.makeText(this, "Please manually disable battery optimization in Settings > Battery", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton("Later") { _, _ ->
                        android.util.Log.d("ActivitiesActivity", "User chose to skip battery optimization")
                    }
                    .setCancelable(false)
                    .show()
            } else {
                android.util.Log.d("ActivitiesActivity", "Battery optimization already disabled")
            }
        }
    }
    
    private fun requestDrawOverAppsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Display Over Other Apps")
                    .setMessage("To show alarm information over other apps, please grant the 'Display over other apps' permission.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            requestOverlayPermissionLauncher.launch(intent)
                            android.util.Log.d("ActivitiesActivity", "Opened overlay permission request")
                        } catch (e: Exception) {
                            android.util.Log.e("ActivitiesActivity", "Error opening overlay settings: ${e.message}", e)
                            Toast.makeText(this, "Please enable 'Display over other apps' in Settings", Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton("Later") { _, _ -> 
                        android.util.Log.d("ActivitiesActivity", "User chose to skip overlay permission")
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }
    
    private fun startKeepAliveService() {
        val intent = Intent(this, KeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        android.util.Log.d("ActivitiesActivity", "Started KeepAliveService")
    }
    
    private fun keepAppInForeground() {
        // Keep the activity visible and prevent it from being killed
        // The KeepAliveService handles keeping the app running
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                navigateToSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
    
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Completed"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Pending"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Overdue"))
        
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.filterActivities(ActivityFilter.ALL)
                    1 -> viewModel.filterActivities(ActivityFilter.COMPLETED)
                    2 -> viewModel.filterActivities(ActivityFilter.PENDING)
                    3 -> viewModel.filterActivities(ActivityFilter.OVERDUE)
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupRecyclerView() {
        adapter = StudentActivitiesAdapter { student ->
            // Open student activities detail page
            val intent = Intent(this, StudentActivitiesDetailActivity::class.java)
            val studentJson = com.google.gson.Gson().toJson(student)
            intent.putExtra(StudentActivitiesDetailActivity.EXTRA_STUDENT_DATA, studentJson)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }
    
    private fun refreshData() {
        android.util.Log.d("ActivitiesActivity", "Manual refresh triggered")
        viewModel.refreshActivities()
    }
    
    private fun startAutoRefresh() {
        stopAutoRefresh() // Cancel any existing job
        android.util.Log.d("ActivitiesActivity", "Starting auto-refresh (every 60 seconds)")
        autoRefreshJob = lifecycleScope.launch {
            while (true) {
                try {
                    delay(60_000) // 60 seconds = 1 minute
                    if (!isFinishing && !isDestroyed) {
                        android.util.Log.d("ActivitiesActivity", "=== Auto refresh triggered ===")
                        viewModel.refreshActivities()
                    } else {
                        android.util.Log.w("ActivitiesActivity", "Activity finishing/destroyed, stopping auto-refresh")
                        break
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ActivitiesActivity", "Error in auto-refresh loop: ${e.message}", e)
                    // Continue the loop even if there's an error
                    delay(60_000) // Wait before retrying
                }
            }
        }
        android.util.Log.d("ActivitiesActivity", "Auto-refresh job started: ${autoRefreshJob?.isActive}")
    }
    
    private fun stopAutoRefresh() {
        if (autoRefreshJob?.isActive == true) {
            android.util.Log.d("ActivitiesActivity", "Stopping auto-refresh")
            autoRefreshJob?.cancel()
        }
        autoRefreshJob = null
    }
    
    private fun observeViewModel() {
        viewModel.filteredStudentsData.observe(this) { studentsData ->
            adapter.submitList(studentsData)
            if (studentsData.isEmpty()) {
                binding.tvNoData.visibility = android.view.View.VISIBLE
                binding.recyclerView.visibility = android.view.View.GONE
            } else {
                binding.tvNoData.visibility = android.view.View.GONE
                binding.recyclerView.visibility = android.view.View.VISIBLE
            }
            // Check and manage alarms for overdue tasks
            manageAlarms()
        }
        
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            // Stop swipe refresh when loading completes
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
                // Check alarms after loading completes
                manageAlarms()
            }
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun manageAlarms() {
        val overdueActivities = viewModel.getOverdueActivitiesWithAlarms()
        
        if (overdueActivities.isNotEmpty()) {
            // Start alarm service
            val intent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START_ALARM
                val activitiesJson = com.google.gson.Gson().toJson(overdueActivities)
                putExtra(AlarmService.EXTRA_OVERDUE_ACTIVITIES, activitiesJson)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            android.util.Log.d("ActivitiesActivity", "Started alarm service for ${overdueActivities.size} overdue tasks")
        } else {
            // Stop alarm service if no overdue tasks
            val intent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP_ALARM
            }
            stopService(intent)
            android.util.Log.d("ActivitiesActivity", "Stopped alarm service - no overdue tasks")
        }
    }
}


package com.bmg.studentactivity.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bmg.studentactivity.R
import com.bmg.studentactivity.databinding.ActivityActivitiesBinding
import com.bmg.studentactivity.ui.settings.SettingsActivity
import com.bmg.studentactivity.ui.activities.adapters.StudentActivitiesAdapter
import com.bmg.studentactivity.ui.activities.ActivitiesViewModel.ActivityFilter
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
    
    override fun onDestroy() {
        super.onDestroy()
        stopAutoRefresh()
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
        autoRefreshJob = lifecycleScope.launch {
            while (true) {
                delay(60_000) // 60 seconds = 1 minute
                if (!isFinishing) {
                    android.util.Log.d("ActivitiesActivity", "Auto refresh triggered")
                    viewModel.refreshActivities()
                }
            }
        }
    }
    
    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
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
        }
        
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            // Stop swipe refresh when loading completes
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
}


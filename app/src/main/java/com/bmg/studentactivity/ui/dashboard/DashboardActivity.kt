package com.bmg.studentactivity.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bmg.studentactivity.R
import com.bmg.studentactivity.databinding.ActivityDashboardBinding
import com.bmg.studentactivity.ui.activities.ActivitiesActivity
import com.bmg.studentactivity.ui.activities.adapters.ActivitiesAdapter
import com.bmg.studentactivity.ui.auth.LoginActivity
import com.bmg.studentactivity.ui.progress.ProgressActivity
import com.bmg.studentactivity.ui.students.StudentsActivity
import com.bmg.studentactivity.ui.timetable.TimetableActivity
import com.bmg.studentactivity.utils.Constants
import com.bmg.studentactivity.utils.TokenManager
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: ActivitiesAdapter
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is logged in
        if (tokenManager.getToken() == null) {
            navigateToLogin()
            finish()
            return
        }
        
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        setupNavigationDrawer()
        setupRecyclerView()
        observeViewModel()
        loadData()
    }
    
    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        
        binding.navView.setNavigationItemSelectedListener(this)
        binding.navView.setCheckedItem(R.id.nav_dashboard)
        
        // Update header text if needed
        val headerView = binding.navView.getHeaderView(0)
        // val tvName = headerView.findViewById<TextView>(R.id.textView)
        // tvName.text = "Welcome"
    }
    
    private fun setupRecyclerView() {
        adapter = ActivitiesAdapter { activity ->
            viewModel.markActivityComplete(activity)
        }
        binding.recyclerViewActivities.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewActivities.adapter = adapter
    }
    
    private fun observeViewModel() {
        viewModel.activities.observe(this) { activities ->
            if (!isFinishing) {
                try {
                    adapter.submitList(activities) {
                        // Update visibility after list is submitted
                        if (!isFinishing) {
                            if (activities.isEmpty()) {
                                binding.tvNoData.visibility = View.VISIBLE
                                binding.recyclerViewActivities.visibility = View.GONE
                            } else {
                                binding.tvNoData.visibility = View.GONE
                                binding.recyclerViewActivities.visibility = View.VISIBLE
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore if activity is finishing
                }
            }
        }
        
        viewModel.loading.observe(this) { isLoading ->
            if (!isFinishing) {
                try {
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    // Ignore if activity is finishing
                }
            }
        }
        
        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null && !isFinishing) {
                try {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // Ignore if activity is finishing
                }
            }
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                 // Already on dashboard, reload data
                 loadData()
            }
            R.id.nav_timetable -> {
                startActivity(Intent(this, TimetableActivity::class.java))
            }
            R.id.nav_progress -> {
                startActivity(Intent(this, ProgressActivity::class.java))
            }
            R.id.nav_students -> {
                if (tokenManager.getUserType() == Constants.USER_TYPE_PARENT) {
                    startActivity(Intent(this, StudentsActivity::class.java))
                } else {
                    Toast.makeText(this, "Only parents can view students", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.menu_logout -> {
                tokenManager.clearAll()
                navigateToLogin()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clear any pending view updates to prevent buffer queue errors
        viewModel.error.removeObservers(this)
        viewModel.loading.removeObservers(this)
        viewModel.activities.removeObservers(this)
    }
    
    private fun loadData() {
        viewModel.loadTodayActivities()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}

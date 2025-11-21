package com.bmg.studentactivity.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bmg.studentactivity.data.models.ActivitiesResponse
import com.bmg.studentactivity.data.models.StudentActivities
import com.bmg.studentactivity.data.repository.ActivityRepository
import com.bmg.studentactivity.databinding.ActivityStudentActivitiesDetailBinding
import com.bmg.studentactivity.ui.activities.adapters.ActivitiesAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StudentActivitiesDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentActivitiesDetailBinding
    private lateinit var adapter: ActivitiesAdapter
    private var currentStudent: StudentActivities? = null
    
    @Inject
    lateinit var activityRepository: ActivityRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentActivitiesDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Get student data from intent
        val studentJson = intent.getStringExtra(EXTRA_STUDENT_DATA)
        val student = Gson().fromJson(studentJson, StudentActivities::class.java)
        
        if (student != null) {
            currentStudent = student
            setupUI(student)
            setupSwipeRefresh()
        } else {
            finish()
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshActivities()
        }
    }
    
    private fun refreshActivities() {
        val studentEmail = currentStudent?.studentEmail
        if (studentEmail.isNullOrEmpty()) {
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }
        
        lifecycleScope.launch {
            try {
                android.util.Log.d("StudentActivitiesDetailActivity", "Refreshing activities for student: $studentEmail")
                val result = activityRepository.getActivities(studentEmail = studentEmail)
                
                result.onSuccess { response ->
                    if (response.success && response.data != null) {
                        // Find the student in the response
                        val updatedStudent = response.data.students?.find { it.studentEmail == studentEmail }
                        if (updatedStudent != null) {
                            currentStudent = updatedStudent
                            setupUI(updatedStudent)
                        } else {
                            // If not found in students array, check if there's a single activities list
                            if (response.data.activities != null && response.data.activities.isNotEmpty()) {
                                val studentActivities = StudentActivities(
                                    studentEmail = studentEmail,
                                    studentName = currentStudent?.studentName,
                                    profileImg = currentStudent?.profileImg,
                                    profileImgUrl = currentStudent?.profileImgUrl,
                                    activities = response.data.activities,
                                    statistics = response.data.statistics ?: com.bmg.studentactivity.data.models.ActivityStatistics()
                                )
                                currentStudent = studentActivities
                                setupUI(studentActivities)
                            }
                        }
                    } else {
                        Toast.makeText(this@StudentActivitiesDetailActivity, 
                            response.message ?: "Failed to refresh activities", 
                            Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { exception ->
                    android.util.Log.e("StudentActivitiesDetailActivity", "Failed to refresh: ${exception.message}", exception)
                    Toast.makeText(this@StudentActivitiesDetailActivity, 
                        "Failed to refresh: ${exception.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("StudentActivitiesDetailActivity", "Exception during refresh: ${e.message}", e)
                Toast.makeText(this@StudentActivitiesDetailActivity, 
                    "Error: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun setupUI(student: StudentActivities) {
        // Set title
        supportActionBar?.title = student.studentName ?: "Activities"
        
        // Load profile image
        val imageUrl = student.profileImgUrl
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(binding.imgProfile)
        } else {
            binding.imgProfile.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        // Set student info
        binding.tvStudentName.text = student.studentName ?: student.studentEmail
        binding.tvStudentEmail.text = student.studentEmail
        
        // Display statistics
        val stats = student.statistics
        binding.tvTotal.text = "Total: ${stats.total}"
        binding.tvCompleted.text = "Completed: ${stats.completed}"
        binding.tvPending.text = "Pending: ${stats.pending}"
        binding.tvOverdue.text = "Overdue: ${stats.overdue}"
        binding.tvCompletionPercentage.text = "Completion: ${stats.completionPercentage}%"
        
        // Setup RecyclerView
        adapter = ActivitiesAdapter { activity ->
            // Action button click handler - will be used for submission later
            android.widget.Toast.makeText(this, "Action for: ${activity.displayTitle}", android.widget.Toast.LENGTH_SHORT).show()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        // Set activities
        adapter.submitList(student.activities)
        
        // Show empty state if no activities
        if (student.activities.isEmpty()) {
            binding.tvNoData.visibility = android.view.View.VISIBLE
            binding.recyclerView.visibility = android.view.View.GONE
        } else {
            binding.tvNoData.visibility = android.view.View.GONE
            binding.recyclerView.visibility = android.view.View.VISIBLE
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    companion object {
        const val EXTRA_STUDENT_DATA = "extra_student_data"
    }
}


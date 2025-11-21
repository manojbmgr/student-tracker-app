package com.bmg.studentactivity.ui.activities

import android.content.Intent
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
            // Automatically refresh data when activity opens
            refreshActivities()
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
        
        // Show refresh indicator
        binding.swipeRefreshLayout.isRefreshing = true
        
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
        adapter = ActivitiesAdapter(
            onActionClick = { activity ->
                handleActivityAction(activity)
            },
            onImageClick = { imageUrl ->
                val intent = Intent(this, ImageViewerActivity::class.java)
                intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, imageUrl)
                startActivity(intent)
            }
        )
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
    
    private fun handleActivityAction(activity: com.bmg.studentactivity.data.models.Activity) {
        try {
            val status = activity.status
            
            when (status) {
                "Completed" -> {
                    // If has completion image, show image viewer, otherwise show update dialog
                    if (activity.hasCompletionImage && activity.completionImageUrl != null) {
                        val intent = Intent(this, ImageViewerActivity::class.java)
                        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, activity.completionImageUrl)
                        startActivity(intent)
                    } else {
                        showUpdateDialog(activity)
                    }
                }
                "Failed", "Pending", "Overdue" -> {
                    showUpdateDialog(activity)
                }
                else -> {
                    showUpdateDialog(activity)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StudentActivitiesDetailActivity", "Error handling activity action: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showUpdateDialog(activity: com.bmg.studentactivity.data.models.Activity) {
        try {
            if (isFinishing || isDestroyed) {
                android.util.Log.w("StudentActivitiesDetailActivity", "Activity is finishing, cannot show dialog")
                return
            }
            
            val studentEmail = currentStudent?.studentEmail
            if (studentEmail.isNullOrEmpty()) {
                android.util.Log.e("StudentActivitiesDetailActivity", "Student email is null or empty")
                Toast.makeText(this, "Student email not available", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Check if fragment manager is available and not in saved state
            if (supportFragmentManager.isStateSaved) {
                android.util.Log.w("StudentActivitiesDetailActivity", "Fragment manager state is saved, cannot show dialog")
                Toast.makeText(this, "Please try again", Toast.LENGTH_SHORT).show()
                return
            }
            
            android.util.Log.d("StudentActivitiesDetailActivity", "Showing update dialog for activity: ${activity.displayTitle}")
            
            val dialog = com.bmg.studentactivity.ui.activities.dialogs.UpdateTaskDialog.newInstance(
                activity = activity,
                studentEmail = studentEmail,
                onUpdateComplete = { updatedActivity ->
                    // Refresh activities after update
                    android.util.Log.d("StudentActivitiesDetailActivity", "Task updated, refreshing activities")
                    refreshActivities()
                }
            )
            
            // Use commitAllowingStateLoss to prevent crashes if state is saved
            dialog.show(supportFragmentManager, "UpdateTaskDialog")
        } catch (e: IllegalStateException) {
            android.util.Log.e("StudentActivitiesDetailActivity", "IllegalStateException showing dialog: ${e.message}", e)
            // Try to show dialog using commitAllowingStateLoss
            try {
                val studentEmail = currentStudent?.studentEmail ?: return
                val dialog = com.bmg.studentactivity.ui.activities.dialogs.UpdateTaskDialog.newInstance(
                    activity = activity,
                    studentEmail = studentEmail,
                    onUpdateComplete = { updatedActivity ->
                        refreshActivities()
                    }
                )
                dialog.showNow(supportFragmentManager, "UpdateTaskDialog")
            } catch (e2: Exception) {
                android.util.Log.e("StudentActivitiesDetailActivity", "Error showing dialog with showNow: ${e2.message}", e2)
                Toast.makeText(this, "Error showing dialog. Please try again.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("StudentActivitiesDetailActivity", "Error showing update dialog: ${e.message}", e)
            Toast.makeText(this, "Error showing dialog: ${e.message}", Toast.LENGTH_SHORT).show()
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


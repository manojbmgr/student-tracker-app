package com.bmg.studentactivity.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bmg.studentactivity.databinding.ActivityActivitiesBinding
import com.bmg.studentactivity.data.models.Activity
import com.bmg.studentactivity.ui.activities.adapters.StudentActivitiesAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivitiesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActivitiesBinding
    private val viewModel: ActivitiesViewModel by viewModels()
    private lateinit var adapter: StudentActivitiesAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Activities"
        
        setupRecyclerView()
        observeViewModel()
        viewModel.loadActivities()
    }
    
    private fun setupRecyclerView() {
        adapter = StudentActivitiesAdapter { activity ->
            val activityId = activity.activityIdString
            if (activityId.isNotEmpty()) {
                viewModel.markActivityComplete(activityId)
            } else {
                Toast.makeText(this, "Cannot mark activity as complete", Toast.LENGTH_SHORT).show()
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun observeViewModel() {
        viewModel.studentsData.observe(this) { studentsData ->
            adapter.submitList(studentsData)
        }
        
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}


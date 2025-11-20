package com.bmg.studentactivity.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bmg.studentactivity.databinding.ActivityActivitiesBinding
import com.bmg.studentactivity.ui.activities.adapters.ActivitiesAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivitiesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActivitiesBinding
    private val viewModel: ActivitiesViewModel by viewModels()
    private lateinit var adapter: ActivitiesAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        setupRecyclerView()
        observeViewModel()
        viewModel.loadActivities()
    }
    
    private fun setupRecyclerView() {
        adapter = ActivitiesAdapter { activity ->
            viewModel.markActivityComplete(activity.id)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun observeViewModel() {
        viewModel.activities.observe(this) { activities ->
            adapter.submitList(activities)
        }
        
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}


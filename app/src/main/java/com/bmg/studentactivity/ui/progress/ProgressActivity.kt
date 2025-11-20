package com.bmg.studentactivity.ui.progress

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bmg.studentactivity.databinding.ActivityProgressBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProgressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProgressBinding
    private val viewModel: ProgressViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        observeViewModel()
        viewModel.loadProgress()
    }
    
    private fun observeViewModel() {
        viewModel.progressData.observe(this) { data ->
            data?.let {
                binding.tvOverallProgress.text = "Overall Progress: ${(it.overallProgress ?: 0f) * 100}%"
                // Update other progress views
            }
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


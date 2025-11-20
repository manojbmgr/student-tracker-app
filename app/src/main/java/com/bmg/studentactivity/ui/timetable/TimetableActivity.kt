package com.bmg.studentactivity.ui.timetable

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bmg.studentactivity.databinding.ActivityTimetableBinding
import com.bmg.studentactivity.ui.timetable.adapters.TimetableAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimetableActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimetableBinding
    private val viewModel: TimetableViewModel by viewModels()
    private lateinit var adapter: TimetableAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimetableBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        setupRecyclerView()
        observeViewModel()
        viewModel.loadTimetable()
        
        binding.fabAdd.setOnClickListener {
            // Open add/edit dialog
        }
    }
    
    private fun setupRecyclerView() {
        adapter = TimetableAdapter(
            onDeleteClick = { entry ->
                viewModel.deleteEntry(entry.id)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun observeViewModel() {
        viewModel.timetableEntries.observe(this) { entries ->
            adapter.submitList(entries)
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


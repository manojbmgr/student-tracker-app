package com.bmg.studentactivity.ui.students

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bmg.studentactivity.databinding.ActivityStudentsBinding
import com.bmg.studentactivity.ui.students.adapters.StudentsAdapter
import com.bmg.studentactivity.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StudentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentsBinding
    private val viewModel: StudentsViewModel by viewModels()
    private lateinit var adapter: StudentsAdapter
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Students"
        
        setupRecyclerView()
        observeViewModel()
        
        viewModel.loadStudents()
    }
    
    private fun setupRecyclerView() {
        adapter = StudentsAdapter()
        binding.recyclerViewStudents.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewStudents.adapter = adapter
    }
    
    private fun observeViewModel() {
        viewModel.students.observe(this) { students ->
            adapter.submitList(students)
            if (students.isEmpty()) {
                // You can show a "No students found" message if needed
            }
        }
        
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Added error observation
        viewModel.error.observe(this) { errorMessage ->
             if (errorMessage != null) {
                 Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
             }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

package com.bmg.studentactivity.ui.activities.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bmg.studentactivity.data.models.Activity
import com.bmg.studentactivity.data.models.StudentActivities
import com.bmg.studentactivity.databinding.ItemActivityBinding
import com.bmg.studentactivity.databinding.ItemStudentActivitiesBinding

class StudentActivitiesAdapter(
    private val onCompleteClick: (Activity) -> Unit
) : ListAdapter<StudentActivities, StudentActivitiesAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentActivitiesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onCompleteClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemStudentActivitiesBinding,
        private val onCompleteClick: (Activity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val activitiesAdapter = ActivitiesAdapter(onCompleteClick)
        
        init {
            binding.recyclerViewActivities.adapter = activitiesAdapter
        }
        
        fun bind(studentActivities: StudentActivities) {
            binding.tvStudentName.text = studentActivities.studentName ?: studentActivities.studentEmail
            binding.tvStudentEmail.text = studentActivities.studentEmail
            
            // Display statistics
            val stats = studentActivities.statistics
            binding.tvTotal.text = "Total: ${stats.total}"
            binding.tvCompleted.text = "Completed: ${stats.completed}"
            binding.tvPending.text = "Pending: ${stats.pending}"
            binding.tvOverdue.text = "Overdue: ${stats.overdue}"
            binding.tvCompletionPercentage.text = "Completion: ${stats.completionPercentage}%"
            
            // Set activities
            activitiesAdapter.submitList(studentActivities.activities)
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<StudentActivities>() {
        override fun areItemsTheSame(oldItem: StudentActivities, newItem: StudentActivities): Boolean {
            return oldItem.studentEmail == newItem.studentEmail
        }
        
        override fun areContentsTheSame(oldItem: StudentActivities, newItem: StudentActivities): Boolean {
            return oldItem == newItem
        }
    }
}


package com.bmg.studentactivity.ui.activities.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bmg.studentactivity.data.models.StudentActivities
import com.bmg.studentactivity.databinding.ItemStudentActivitiesBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions

class StudentActivitiesAdapter(
    private val onStudentClick: (StudentActivities) -> Unit
) : ListAdapter<StudentActivities, StudentActivitiesAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentActivitiesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onStudentClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemStudentActivitiesBinding,
        private val onStudentClick: (StudentActivities) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            // Make the entire card clickable
            binding.root.setOnClickListener {
                val student = getItem(adapterPosition)
                onStudentClick(student)
            }
        }
        
        fun bind(studentActivities: StudentActivities) {
            binding.tvStudentName.text = studentActivities.studentName ?: studentActivities.studentEmail
            binding.tvStudentEmail.text = studentActivities.studentEmail
            
            // Load profile image
            val imageUrl = studentActivities.profileImgUrl
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.imgProfile)
            } else {
                // Set default placeholder if no image URL
                binding.imgProfile.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            
            // Display statistics
            val stats = studentActivities.statistics
            binding.tvTotal.text = "Total: ${stats.total}"
            binding.tvCompleted.text = "Completed: ${stats.completed}"
            binding.tvPending.text = "Pending: ${stats.pending}"
            binding.tvOverdue.text = "Overdue: ${stats.overdue}"
            binding.tvCompletionPercentage.text = "Completion: ${stats.completionPercentage}%"
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


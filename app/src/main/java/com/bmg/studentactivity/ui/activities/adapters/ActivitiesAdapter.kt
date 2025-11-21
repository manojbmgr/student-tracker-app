package com.bmg.studentactivity.ui.activities.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bmg.studentactivity.data.models.Activity
import com.bmg.studentactivity.databinding.ItemActivityBinding

class ActivitiesAdapter(
    private val onCompleteClick: (Activity) -> Unit
) : ListAdapter<Activity, ActivitiesAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(activity: Activity) {
            binding.tvTitle.text = activity.displayTitle
            binding.tvDescription.text = activity.description ?: activity.notes ?: ""
            binding.tvSubject.text = activity.subject ?: activity.activityType ?: ""
            binding.tvStatus.text = activity.status
            
            // Show time if available
            val timeInfo = if (activity.startTime != null && activity.endTime != null) {
                "${activity.startTime} - ${activity.endTime}"
            } else if (activity.endTime != null) {
                "Due: ${activity.endTime}"
            } else {
                ""
            }
            
            if (timeInfo.isNotEmpty()) {
                binding.tvDescription.text = "${binding.tvDescription.text}\n$timeInfo".trim()
            }
            
            // Update button based on completion status
            if (activity.isCompleted == true || activity.isCompletedToday == true) {
                binding.btnComplete.text = "Completed"
                binding.btnComplete.isEnabled = false
            } else {
                binding.btnComplete.text = "Mark Complete"
                binding.btnComplete.isEnabled = true
                binding.btnComplete.setOnClickListener {
                    onCompleteClick(activity)
                }
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Activity>() {
        override fun areItemsTheSame(oldItem: Activity, newItem: Activity): Boolean {
            return oldItem.activityIdString == newItem.activityIdString
        }
        
        override fun areContentsTheSame(oldItem: Activity, newItem: Activity): Boolean {
            return oldItem == newItem
        }
    }
}


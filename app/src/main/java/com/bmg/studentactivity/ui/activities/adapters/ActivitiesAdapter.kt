package com.bmg.studentactivity.ui.activities.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bmg.studentactivity.data.models.Activity
import com.bmg.studentactivity.databinding.ItemActivityBinding
import com.bumptech.glide.Glide

class ActivitiesAdapter(
    private val onActionClick: (Activity) -> Unit,
    private val onImageClick: ((String) -> Unit)? = null
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
            
            // Set status badge and card background color
            val status = activity.status
            binding.tvStatus.text = status
            
            // Create rounded badge drawable
            val badgeDrawable = GradientDrawable().apply {
                cornerRadius = 12f * binding.root.context.resources.displayMetrics.density
            }
            
            when (status) {
                "Completed" -> {
                    // Light green background for card
                    binding.root.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                    // Green badge
                    badgeDrawable.setColor(android.graphics.Color.parseColor("#4CAF50"))
                    binding.tvStatus.background = badgeDrawable
                    binding.tvStatus.setTextColor(android.graphics.Color.WHITE)
                }
                "Failed" -> {
                    // Light orange/red background for card
                    binding.root.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"))
                    // Orange/red badge
                    badgeDrawable.setColor(android.graphics.Color.parseColor("#FF5722"))
                    binding.tvStatus.background = badgeDrawable
                    binding.tvStatus.setTextColor(android.graphics.Color.WHITE)
                }
                "Overdue" -> {
                    // Light red background for card
                    binding.root.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
                    // Red badge
                    badgeDrawable.setColor(android.graphics.Color.parseColor("#F44336"))
                    binding.tvStatus.background = badgeDrawable
                    binding.tvStatus.setTextColor(android.graphics.Color.WHITE)
                }
                "Pending" -> {
                    // White background for card
                    binding.root.setCardBackgroundColor(android.graphics.Color.WHITE)
                    // Orange badge
                    badgeDrawable.setColor(android.graphics.Color.parseColor("#FF9800"))
                    binding.tvStatus.background = badgeDrawable
                    binding.tvStatus.setTextColor(android.graphics.Color.WHITE)
                }
            }
            
            // Show completion image if available
            if (activity.hasCompletionImage && activity.completionImageUrl != null) {
                binding.imgCompletion.visibility = android.view.View.VISIBLE
                Glide.with(binding.root.context)
                    .load(activity.completionImageUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(binding.imgCompletion)
                
                // Make image clickable to view full screen
                binding.imgCompletion.setOnClickListener {
                    onImageClick?.invoke(activity.completionImageUrl!!)
                }
            } else {
                binding.imgCompletion.visibility = android.view.View.GONE
                binding.imgCompletion.setOnClickListener(null)
            }
            
            // Show remark if available and update position
            if (activity.hasRemark && activity.remark != null) {
                binding.tvRemark.visibility = android.view.View.VISIBLE
                binding.tvRemark.text = "Remark: ${activity.remark}"
                // Update constraint based on whether completion image is visible
                val params = binding.tvRemark.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                if (activity.hasCompletionImage) {
                    params.topToBottom = binding.imgCompletion.id
                    params.topMargin = (4 * binding.root.context.resources.displayMetrics.density).toInt()
                } else {
                    params.topToBottom = binding.tvSubject.id
                    params.topMargin = (8 * binding.root.context.resources.displayMetrics.density).toInt()
                }
                binding.tvRemark.layoutParams = params
            } else {
                binding.tvRemark.visibility = android.view.View.GONE
            }
            
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
            
            // Update action button text based on status
            binding.btnComplete.text = when (status) {
                "Completed" -> "View"
                "Failed" -> "Retry"
                else -> "Action"
            }
            binding.btnComplete.setOnClickListener {
                onActionClick(activity)
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


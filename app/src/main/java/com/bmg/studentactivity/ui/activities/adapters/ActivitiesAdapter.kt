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
            binding.tvTitle.text = activity.title
            binding.tvDescription.text = activity.description ?: ""
            binding.tvSubject.text = activity.subject ?: ""
            binding.tvStatus.text = activity.status
            
            binding.btnComplete.setOnClickListener {
                onCompleteClick(activity)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Activity>() {
        override fun areItemsTheSame(oldItem: Activity, newItem: Activity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Activity, newItem: Activity): Boolean {
            return oldItem == newItem
        }
    }
}


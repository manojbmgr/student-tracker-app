package com.bmg.studentactivity.ui.timetable.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bmg.studentactivity.data.models.TimetableEntry
import com.bmg.studentactivity.databinding.ItemTimetableBinding

class TimetableAdapter(
    private val onDeleteClick: (TimetableEntry) -> Unit
) : ListAdapter<TimetableEntry, TimetableAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTimetableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemTimetableBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: TimetableEntry) {
            binding.tvDay.text = entry.day
            binding.tvTime.text = entry.time
            binding.tvSubject.text = entry.subject
            binding.tvDescription.text = entry.description ?: ""
            
            binding.btnDelete.setOnClickListener {
                onDeleteClick(entry)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<TimetableEntry>() {
        override fun areItemsTheSame(oldItem: TimetableEntry, newItem: TimetableEntry): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TimetableEntry, newItem: TimetableEntry): Boolean {
            return oldItem == newItem
        }
    }
}


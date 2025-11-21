package com.bmg.studentactivity.services

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bmg.studentactivity.R
import com.bmg.studentactivity.data.models.Activity
import com.bmg.studentactivity.ui.activities.ActivitiesActivity

class AlarmOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: AlarmTaskAdapter? = null
    private var overdueActivities: List<Activity> = emptyList()
    private var currentPlayingIndex = -1
    
    companion object {
        private const val TAG = "AlarmOverlayService"
        const val ACTION_SHOW_OVERLAY = "com.bmg.studentactivity.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.bmg.studentactivity.HIDE_OVERLAY"
        const val ACTION_UPDATE_OVERLAY = "com.bmg.studentactivity.UPDATE_OVERLAY"
        const val EXTRA_OVERDUE_ACTIVITIES = "extra_overdue_activities"
        const val EXTRA_CURRENT_INDEX = "extra_current_index"
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "AlarmOverlayService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                val activitiesJson = intent.getStringExtra(EXTRA_OVERDUE_ACTIVITIES)
                val currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, -1)
                if (activitiesJson != null) {
                    val activities = com.google.gson.Gson().fromJson(
                        activitiesJson,
                        Array<Activity>::class.java
                    ).toList()
                    showOverlay(activities, currentIndex)
                }
            }
            ACTION_UPDATE_OVERLAY -> {
                val activitiesJson = intent.getStringExtra(EXTRA_OVERDUE_ACTIVITIES)
                val currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, -1)
                if (activitiesJson != null) {
                    val activities = com.google.gson.Gson().fromJson(
                        activitiesJson,
                        Array<Activity>::class.java
                    ).toList()
                    updateOverlay(activities, currentIndex)
                }
            }
            ACTION_HIDE_OVERLAY -> {
                hideOverlay()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun showOverlay(activities: List<Activity>, currentIndex: Int) {
        if (overlayView != null) {
            updateOverlay(activities, currentIndex)
            return
        }
        
        overdueActivities = activities
        currentPlayingIndex = currentIndex
        
        val layoutInflater = LayoutInflater.from(this)
        overlayView = layoutInflater.inflate(R.layout.overlay_alarm_tasks, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 100
        }
        
        setupOverlayView(overlayView!!)
        
        try {
            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay shown with ${activities.size} tasks")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay: ${e.message}", e)
        }
    }
    
    private fun setupOverlayView(view: View) {
        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewAlarmTasks)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseOverlay)
        
        adapter = AlarmTaskAdapter(overdueActivities, currentPlayingIndex) { activity ->
            // Open activity detail when clicked
            val intent = Intent(this, ActivitiesActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
        
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = adapter
        
        btnClose?.setOnClickListener {
            hideOverlay()
        }
        
        // Make overlay draggable
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = (overlayView?.layoutParams as? WindowManager.LayoutParams)?.x ?: 0
                        initialY = (overlayView?.layoutParams as? WindowManager.LayoutParams)?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val params = overlayView?.layoutParams as? WindowManager.LayoutParams
                        params?.x = initialX + (event.rawX - initialTouchX).toInt()
                        params?.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })
    }
    
    private fun updateOverlay(activities: List<Activity>, currentIndex: Int) {
        overdueActivities = activities
        currentPlayingIndex = currentIndex
        adapter?.updateData(activities, currentIndex)
    }
    
    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
                overlayView = null
                recyclerView = null
                adapter = null
                Log.d(TAG, "Overlay hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding overlay: ${e.message}", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        Log.d(TAG, "AlarmOverlayService destroyed")
    }
    
    private class AlarmTaskAdapter(
        private var activities: List<Activity>,
        private var currentIndex: Int,
        private val onItemClick: (Activity) -> Unit
    ) : RecyclerView.Adapter<AlarmTaskAdapter.ViewHolder>() {
        
        fun updateData(newActivities: List<Activity>, newCurrentIndex: Int) {
            activities = newActivities
            currentIndex = newCurrentIndex
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_alarm_task_overlay, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val activity = activities[position]
            val isPlaying = position == currentIndex
            
            holder.bind(activity, isPlaying)
            holder.itemView.setOnClickListener { onItemClick(activity) }
        }
        
        override fun getItemCount() = activities.size
        
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
            private val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
            private val indicator: View = itemView.findViewById(R.id.indicatorPlaying)
            
            fun bind(activity: Activity, isPlaying: Boolean) {
                tvTaskName.text = activity.displayTitle
                tvStudentName.text = activity.studentName ?: activity.studentEmail ?: "Unknown"
                indicator.visibility = if (isPlaying) View.VISIBLE else View.GONE
                itemView.alpha = if (isPlaying) 1.0f else 0.7f
            }
        }
    }
}


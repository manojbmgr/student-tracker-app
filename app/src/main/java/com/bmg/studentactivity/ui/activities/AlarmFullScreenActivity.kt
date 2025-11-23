package com.bmg.studentactivity.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bmg.studentactivity.data.models.Activity
import com.bmg.studentactivity.databinding.ActivityAlarmFullScreenBinding
import com.bmg.studentactivity.services.AlarmService
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmFullScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmFullScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmFullScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make the activity show over the lock screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val activityJson = intent.getStringExtra(EXTRA_ACTIVITY)
        if (activityJson != null) {
            val activity = Gson().fromJson(activityJson, Activity::class.java)
            binding.tvTaskName.text = activity.displayTitle
            binding.tvStudentName.text = activity.studentName ?: "N/A"
            binding.tvDueTime.text = formatDueDate(activity)
        }

        binding.btnDismiss.setOnClickListener {
            // Stop the alarm service
            val stopIntent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP_ALARM
            }
            startService(stopIntent)
            finish()
        }
    }

    private fun formatDueDate(activity: Activity): String {
        var dueInfo = ""
        if (!activity.dueDate.isNullOrEmpty()) {
            dueInfo += "Due: ${activity.dueDate}"
        }
        if (!activity.startTime.isNullOrEmpty()) {
            dueInfo += " at ${activity.startTime}"
        }
        return if (dueInfo.isNotEmpty()) dueInfo else "No due date specified"
    }

    companion object {
        const val EXTRA_ACTIVITY = "extra_activity"
    }
}

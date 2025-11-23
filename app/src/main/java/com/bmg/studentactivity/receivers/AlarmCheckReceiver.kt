package com.bmg.studentactivity.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bmg.studentactivity.services.KeepAliveService

class AlarmCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.w("AlarmCheckReceiver", "Alarm received, starting KeepAliveService.")

        // Start the service to perform the actual work.
        val serviceIntent = Intent(context, KeepAliveService::class.java).apply {
            action = KeepAliveService.ACTION_CHECK_ALARMS
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}

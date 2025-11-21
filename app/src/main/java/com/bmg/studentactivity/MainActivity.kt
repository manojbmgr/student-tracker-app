package com.bmg.studentactivity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bmg.studentactivity.ui.activities.ActivitiesActivity
import com.bmg.studentactivity.ui.settings.SettingsActivity
import com.bmg.studentactivity.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var tokenManager: TokenManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if API key is set
        if (tokenManager.getApiKey() != null) {
            startActivity(Intent(this, ActivitiesActivity::class.java))
        } else {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        finish()
    }
}
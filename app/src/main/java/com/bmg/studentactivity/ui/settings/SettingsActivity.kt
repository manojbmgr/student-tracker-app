package com.bmg.studentactivity.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bmg.studentactivity.databinding.ActivitySettingsBinding
import com.bmg.studentactivity.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        
        // Load existing API key
        val existingApiKey = tokenManager.getApiKey()
        if (!existingApiKey.isNullOrEmpty()) {
            binding.editTextApiKey.setText(existingApiKey)
        }
        
        binding.buttonSave.setOnClickListener {
            saveApiKey()
        }
        
        binding.buttonClear.setOnClickListener {
            clearApiKey()
        }
    }
    
    private fun saveApiKey() {
        val apiKey = binding.editTextApiKey.text.toString().trim()
        
        if (apiKey.isEmpty()) {
            binding.editTextApiKey.error = "API Key cannot be empty"
            return
        }
        
        tokenManager.saveApiKey(apiKey)
        Toast.makeText(this, "API Key saved successfully", Toast.LENGTH_SHORT).show()
    }
    
    private fun clearApiKey() {
        tokenManager.clearApiKey()
        binding.editTextApiKey.setText("")
        Toast.makeText(this, "API Key cleared", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}


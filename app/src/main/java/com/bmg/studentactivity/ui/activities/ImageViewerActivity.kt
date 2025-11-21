package com.bmg.studentactivity.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bmg.studentactivity.databinding.ActivityImageViewerBinding
import com.bumptech.glide.Glide

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageViewerBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Completion Image"
        
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        if (imageUrl != null && imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .into(binding.imageView)
        } else {
            finish()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    companion object {
        const val EXTRA_IMAGE_URL = "extra_image_url"
    }
}


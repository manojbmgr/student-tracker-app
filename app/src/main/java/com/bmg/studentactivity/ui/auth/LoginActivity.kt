package com.bmg.studentactivity.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.bmg.studentactivity.R
import com.bmg.studentactivity.databinding.ActivityLoginBinding
import com.bmg.studentactivity.ui.dashboard.DashboardActivity
import com.bmg.studentactivity.utils.TokenManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is already logged in
        if (tokenManager.getToken() != null) {
            navigateToDashboard()
            finish()
            return
        }
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewPager()
        observeLoginState()
    }
    
    private fun setupViewPager() {
        val adapter = LoginPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Student" else "Parent"
        }.attach()
    }
    
    private fun observeLoginState() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    // Show loading indicator
                }
                is LoginState.Success -> {
                    navigateToDashboard()
                }
                is LoginState.Error -> {
                    // Show error message
                }
            }
        }
    }
    
    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}


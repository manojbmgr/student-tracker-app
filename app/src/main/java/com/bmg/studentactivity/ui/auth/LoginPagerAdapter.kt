package com.bmg.studentactivity.ui.auth

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bmg.studentactivity.ui.auth.fragments.StudentLoginFragment
import com.bmg.studentactivity.ui.auth.fragments.ParentLoginFragment

class LoginPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2
    
    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            StudentLoginFragment()
        } else {
            ParentLoginFragment()
        }
    }
}

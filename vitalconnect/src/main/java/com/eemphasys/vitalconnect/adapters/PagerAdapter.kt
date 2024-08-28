package com.eemphasys.vitalconnect.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.eemphasys.vitalconnect.ui.fragment.InternalFragment
import com.eemphasys.vitalconnect.ui.fragment.ExternalFragment

class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> InternalFragment()
            1 -> ExternalFragment()
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}
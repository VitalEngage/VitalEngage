package com.eemphasys.vitalconnect.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.eemphasys.vitalconnect.ui.fragment.InternalFragment
import com.eemphasys.vitalconnect.ui.fragment.ExternalFragment

class PagerAdapter(fragment: Fragment,private val showInternal: Boolean,
                   private val showExternal: Boolean) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        var count = 0
        if (showInternal) count++
        if (showExternal) count++
        return count
    }

    override fun createFragment(position: Int): Fragment {
        // Determine which fragment to return based on flags and position
        return when (position) {
            0 -> if (showInternal) InternalFragment() else ExternalFragment()
            1 -> if (showExternal) ExternalFragment() else InternalFragment()
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}
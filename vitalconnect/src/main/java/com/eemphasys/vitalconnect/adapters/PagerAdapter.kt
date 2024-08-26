package com.eemphasys.vitalconnect.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.eemphasys.vitalconnect.ui.fragment.TabOneFragment
import com.eemphasys.vitalconnect.ui.fragment.TabTwoFragment

class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TabOneFragment()
            1 -> TabTwoFragment()
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}
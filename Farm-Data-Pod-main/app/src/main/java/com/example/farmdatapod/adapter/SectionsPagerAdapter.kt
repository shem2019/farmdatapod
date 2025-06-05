package com.example.farmdatapod.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.farmdatapod.hub.hubAggregation.buyingCenter.BuyingCenterFragment
import com.example.farmdatapod.hub.hubAggregation.cig.CIGFragment

class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> BuyingCenterFragment()
            1 -> CIGFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "Buying Center"
            1 -> "CIG"
            else -> null
        }
    }
}
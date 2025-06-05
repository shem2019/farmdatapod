package com.example.farmdatapod.adapter

import androidx.fragment.app.Fragment
import com.example.farmdatapod.produce.indipendent.biodata.BaselineInformationFragment
import com.example.farmdatapod.produce.indipendent.biodata.BasicInformationFragment
import com.example.farmdatapod.produce.indipendent.biodata.InfrastructureInformationFragment
import com.example.farmdatapod.produce.indipendent.biodata.LabourAndChallengesFragment
import com.example.farmdatapod.produce.indipendent.biodata.LivestockInformationFragment
import com.example.farmdatapod.produce.indipendent.biodata.ProduceInformationFragment
import com.example.farmdatapod.produce.indipendent.biodata.ProducerBioDataFragment

class ProducePagerAdapter(fragment: Fragment) : androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 7

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProducerBioDataFragment()
            1 -> BasicInformationFragment()
            2 -> BaselineInformationFragment()
            3 -> LabourAndChallengesFragment()
            4 -> LivestockInformationFragment()
            5 -> InfrastructureInformationFragment()
            6 -> ProduceInformationFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}
package com.example.farmdatapod.season

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.HomepageOption
import com.example.farmdatapod.adapter.HomepageOptionsAdapter
import com.google.android.material.appbar.MaterialToolbar

class SeasonsLandingPageFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seasons_landing_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        toolbar = view.findViewById(R.id.toolbar)

        // Setup toolbar navigation
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Setup RecyclerView
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val options = listOf(
            HomepageOption("Register Season", R.drawable.season),
            HomepageOption("Nursery Planning", R.drawable.training),
            HomepageOption("Land Preparation", R.drawable.buying),
            HomepageOption("Planting Planning", R.drawable.hub),
            HomepageOption("Germination Planning", R.drawable.season),
            HomepageOption("Forecast Yield", R.drawable.producer),
            HomepageOption("Crop Management", R.drawable.price_distribution),
            HomepageOption("Bait Scouting", R.drawable.producer),
            HomepageOption("Crop Nutrition", R.drawable.producer),
            HomepageOption("Crop Protection", R.drawable.producer),
            HomepageOption("Harvest Planning", R.drawable.hub),
        )

        val adapter = HomepageOptionsAdapter(options) { option ->
            when (option.title) {
                "Bait Scouting" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_baitScoutingFragmentFragment
                    )
                }
                "Crop Management" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_cropManagementFragment
                    )
                }
                "Crop Nutrition" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_cropNutritionFragment
                    )
                }
                "Crop Protection" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_cropProtectionFragment
                    )
                }
                "Germination Planning" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_germinationPlanningFragment
                    )
                }
                "Harvest Planning" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_harvestPlanningFragment
                    )
                }
                "Land Preparation" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_landPreparationFragment
                    )
                }
                "Nursery Planning" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_nurseryPlanningFragment
                    )
                }
                "Planting Planning" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_planPlantingFragment
                    )
                }
                "Register Season" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_registerSeasonFragment
                    )
                }
                "Forecast Yield" -> {
                    findNavController().navigate(
                        R.id.action_seasonsLandingPageFragment_to_yieldForecastFragment
                    )
                }
                else -> showTemporaryMessage(option.title)
            }
        }

        recyclerView.adapter = adapter
    }

    private fun showTemporaryMessage(feature: String) {
        Toast.makeText(context, "Feature '$feature' coming soon", Toast.LENGTH_SHORT).show()
    }
}
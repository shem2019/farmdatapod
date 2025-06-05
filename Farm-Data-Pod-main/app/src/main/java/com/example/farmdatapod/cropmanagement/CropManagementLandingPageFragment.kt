package com.example.farmdatapod.cropmanagement

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

class CropManagementLandingPageFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_crop_management_landing_page, container, false)
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
            HomepageOption("Nursery", R.drawable.season),
            HomepageOption("Land Preparation", R.drawable.hub),
            HomepageOption("Planting", R.drawable.buying),
            HomepageOption("Monitor Germination", R.drawable.training),
            HomepageOption("Forecast Yields", R.drawable.producer),
            HomepageOption("Scouting", R.drawable.hub),
            HomepageOption("Activities", R.drawable.hub),
            HomepageOption("Crop Nutrition", R.drawable.hub),
            HomepageOption("Crop Protection", R.drawable.hub),
            HomepageOption("Harvesting", R.drawable.logistics),
            HomepageOption("Order Inputs", R.drawable.price_distribution)
        )

        val adapter = HomepageOptionsAdapter(options) { option ->
            when (option.title) {
                "Nursery" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_nurseryManagementFragment
                    )
                }
                "Land Preparation" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_cropManagementLandPreparationFragment
                    )
                }
                "Planting" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_plantingCropManagementFragment
                    )
                }
                "Monitor Germination" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_germinationCropManagementFragment
                    )
                }
                "Forecast Yields" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_cropManagementForecastYieldFragment
                    )
                }

                "Scouting" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_cropManagementScoutingFragment
                    )
                }
                "Activities" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_cropManagementActivitiesFragment
                    )
                }
                "Crop Nutrition" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_cropManagementNutritionFragment
                    )
                }
                "Crop Protection" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_cropProtectionManagementFragment
                    )
                }
                "Harvesting" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_cropManagementHarvestingFragment
                    )
                }
                "Activities" -> {
                    findNavController().navigate(
                        R.id.action_cropManagementLandingPageFragment_to_cropManagementActivitiesFragment
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
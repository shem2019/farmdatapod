package com.example.farmdatapod.hub.hubAggregation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.HomepageOption
import com.example.farmdatapod.adapter.HomepageOptionsAdapter
import com.example.farmdatapod.databinding.FragmentHubAggregationLandingPageBinding

class HubAggregationLandingPageFragment : Fragment() {

    private var _binding: FragmentHubAggregationLandingPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHubAggregationLandingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Initialize the RecyclerView and Adapter
        val options = listOf(
            HomepageOption("Register Buying Centre", R.drawable.buying),
            HomepageOption("Register Common Interest Groups (CIG)", R.drawable.price_distribution)
        )

        val adapter = HomepageOptionsAdapter(options) { option ->
            when (option.title) {
                "Register Buying Centre" -> {
                    findNavController().navigate(R.id.action_hubAggregationLandingPageFragment_to_buyingCenterFragment)
                }
                "Register Common Interest Groups (CIG)" -> {
                    findNavController().navigate(R.id.action_hubAggregationLandingPageFragment_to_CIGFragment)
                }
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
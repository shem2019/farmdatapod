package com.example.farmdatapod.hub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.HomepageOption
import com.example.farmdatapod.adapter.HomepageOptionsAdapter
import com.example.farmdatapod.databinding.FragmentHubLandingPageBinding

class HubLandingPageFragment : Fragment() {

    private var _binding: FragmentHubLandingPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHubLandingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupBackButton()
    }

    private fun setupRecyclerView() {
        val options = listOf(
            HomepageOption("Hub Registration", R.drawable.hub),
            HomepageOption("Hub Aggregation", R.drawable.buying)
        )

        val adapter = HomepageOptionsAdapter(options) { option ->
            when (option.title) {
                "Hub Registration" -> {
                    Toast.makeText(context, "Navigating to Hub Registration", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_hubLandingPageFragment_to_hubRegistrationFragment)                }
                "Hub Aggregation" -> {
                    Toast.makeText(context, "Navigating to Hub Aggregation", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_hubLandingPageFragment_to_hubAggregationLandingPageFragment)                }
            }
        }

        binding.optionsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            this.adapter = adapter
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.farmdatapod.userregistration.logistics

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.LogisticsAdapter
import com.example.farmdatapod.adapter.LogisticsOption
import com.example.farmdatapod.databinding.FragmentLogisticsLandingPageBinding

class LogisticsLandingPageFragment : Fragment() {

    private var _binding: FragmentLogisticsLandingPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: LogisticsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLogisticsLandingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Create sample data
        val options = listOf(
            LogisticsOption("Individual Logistician", R.drawable.logistics),
            LogisticsOption("Organisation Logistician", R.drawable.logistics),
        )

        // Initialize the adapter with the data
        adapter = LogisticsAdapter(options) { option ->
            if (option.title == "Individual Logistician") {
                findNavController().navigate(R.id.action_logisticsLandingPageFragment_to_individualLogisticianFragment)
            }
            if (option.title =="Organisation Logistician"){
                findNavController().navigate(R.id.action_logisticsLandingPageFragment_to_organisationalLogisticianRegistrationFragment)
            }
        }
        binding.optionsRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
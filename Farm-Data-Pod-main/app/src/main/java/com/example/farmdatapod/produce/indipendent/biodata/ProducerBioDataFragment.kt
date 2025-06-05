package com.example.farmdatapod.produce.indipendent.biodata

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.hub.hubAggregation.buyingCenter.data.BuyingCenterRepository
import com.example.farmdatapod.databinding.FragmentProducerBioDataBinding
import com.example.farmdatapod.hub.hubRegistration.data.Hub
import com.example.farmdatapod.hub.hubRegistration.data.HubRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "ProducerBioDataFragment"

class ProducerBioDataFragment : Fragment() {
    private var _binding: FragmentProducerBioDataBinding? = null
    private val binding get() = _binding!!

    private lateinit var buyingCenterRepository: BuyingCenterRepository
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var spinnersInitialized = false

    // Define selectedHubId as a property of the class
    private var selectedHubId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProducerBioDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buyingCenterRepository = BuyingCenterRepository(requireContext())

        if (!spinnersInitialized) {
            setupSpinners()
            spinnersInitialized = true
        }
        setupButtonListeners()
    }

    private fun setupSpinners() {
        val hubRepository = HubRepository(requireContext())

        lifecycleScope.launch {
            try {
                val hubs = hubRepository.getAllHubs().first()
                Log.d(TAG, "Number of hubs fetched: ${hubs.size}")

                if (hubs.isEmpty()) {
                    binding.hubInputLayout.error = "No hubs available"
                    binding.hubAutoCompleteTextView.setAdapter(null)
                    return@launch
                }

                // Create map of hubName to hubId for easy lookup
                val hubNameToIdMap = hubs.associate { it.hubName to it.id }

                // Create adapter with just the hub names
                val hubAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    hubNameToIdMap.keys.toList()
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                binding.hubAutoCompleteTextView.setAdapter(null)
                binding.hubAutoCompleteTextView.setAdapter(hubAdapter)

                binding.hubAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                    val selectedHubName = hubAdapter.getItem(position)
                    selectedHubId = hubNameToIdMap[selectedHubName] ?: return@setOnItemClickListener
                    updateBuyingCentersForHub(selectedHubId!!)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error setting up hub spinner", e)
                binding.hubInputLayout.error = "Error loading hubs"
            }
        }
    }

    private fun updateBuyingCentersForHub(hubId: Int) {
        lifecycleScope.launch {
            try {
                // Clear previous buying center selection first
                binding.buyingCentersAutoCompleteTextView.setText("")
                binding.buyingCentersAutoCompleteTextView.setAdapter(null)

                val buyingCenters = buyingCenterRepository.getBuyingCentersByHubId(hubId).first()
                Log.d(TAG, "Buying centers for hub $hubId: ${buyingCenters.size}")

                if (buyingCenters.isEmpty()) {
                    binding.buyingCentersInputLayout.error = "No buying centers for selected hub"
                    return@launch
                }

                val buyingCenterAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    buyingCenters.map { it.buyingCenterName }
                )

                binding.buyingCentersAutoCompleteTextView.setAdapter(buyingCenterAdapter)
                binding.buyingCentersInputLayout.error = null

            } catch (e: Exception) {
                Log.e(TAG, "Error loading buying centers for hub $hubId", e)
                binding.buyingCentersInputLayout.error = "Failed to load buying centers"
            }
        }
    }

    private fun setupButtonListeners() {
        binding.backButton.setOnClickListener {
            // Handle back button click
        }
    }

    fun validateForm(): Boolean {
        var isValid = true

        // Validate Hub
        if (binding.hubAutoCompleteTextView.text.isNullOrBlank()) {
            binding.hubInputLayout.error = "Hub is required"
            isValid = false
        } else {
            binding.hubInputLayout.error = null
        }

        // Validate Buying Center
        if (binding.buyingCentersAutoCompleteTextView.text.isNullOrBlank()) {
            binding.buyingCentersInputLayout.error = "Buying Center is required"
            isValid = false
        } else {
            binding.buyingCentersInputLayout.error = null
        }

        return isValid
    }

    fun saveData() {
        if (validateForm()) {
            val selectedHub = binding.hubAutoCompleteTextView.text.toString()
            val selectedBuyingCenter = binding.buyingCentersAutoCompleteTextView.text.toString()

            sharedViewModel.setHub(selectedHub)
            sharedViewModel.setBuyingCenter(selectedBuyingCenter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
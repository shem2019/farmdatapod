package com.example.farmdatapod.logistics.equipments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.farmdatapod.databinding.FragmentEquipmentAllocationBinding
import com.example.farmdatapod.logistics.equipments.data.EquipmentEntity
import com.example.farmdatapod.logistics.equipments.data.EquipmentUiState
import com.example.farmdatapod.logistics.equipments.data.EquipmentViewModel
import com.example.farmdatapod.logistics.planJourney.data.JourneyViewModel
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class EquipmentAllocationFragment : Fragment() {

    private var _binding: FragmentEquipmentAllocationBinding? = null
    private val binding get() = _binding!!

    private val journeyViewModel: JourneyViewModel by viewModels {
        JourneyViewModel.factory(requireActivity().application)
    }

    private val equipmentViewModel: EquipmentViewModel by viewModels {
        EquipmentViewModel.factory(requireActivity().application)
    }

    // Data structures to store journey information
    private val journeyMap = mutableMapOf<String, Long>() // Map of journey display names to journey IDs
    private val stopPointMap = mutableMapOf<String, Long>() // Map of stop point descriptions to stop point IDs

    private var selectedJourneyId: Long? = null
    private var selectedStopPointId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEquipmentAllocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupSubmitButton()
        setupJourneySelectionListener()
        setupStopPointSelectionListener()
    }

    private fun setupObservers() {
        journeyViewModel.journeys.observe(viewLifecycleOwner) { journeysWithStopPoints ->
            // Clear previous data
            journeyMap.clear()

            // Populate journey dropdown
            val journeyNames = journeysWithStopPoints.map { journeyWithStops ->
                // Create a display name (you can customize this)
                val displayName = "${journeyWithStops.journey.driver} - ${journeyWithStops.journey.date_and_time}"

                // Map display name to journey ID
                journeyMap[displayName] = journeyWithStops.journey.id

                displayName
            }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                journeyMap.keys.toList()
            )
            binding.journeyInput.setAdapter(adapter)
        }
    }

    private fun setupJourneySelectionListener() {
        binding.journeyInput.setOnItemClickListener { _, _, position, _ ->
            // Get the selected display name
            val selectedDisplayName = journeyMap.keys.toList()[position]

            // Get the corresponding journey ID
            selectedJourneyId = journeyMap[selectedDisplayName]

            // Find the selected journey
            val selectedJourney = journeyViewModel.journeys.value?.find {
                it.journey.id == selectedJourneyId
            }

            // Clear previous stop point data
            stopPointMap.clear()

            // Populate stop points for the selected journey
            val stopPointDescriptions = selectedJourney?.stopPoints?.map { stopPoint ->
                // Create a display description (you can customize this)
                val description = "${stopPoint.description} - ${stopPoint.time}"

                // Map description to stop point ID
                stopPointMap[description] = stopPoint.id

                description
            } ?: emptyList()

            val stopPointAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                stopPointDescriptions
            )
            binding.stopPointInput.setAdapter(stopPointAdapter)
        }
    }

    private fun setupStopPointSelectionListener() {
        binding.stopPointInput.setOnItemClickListener { _, _, position, _ ->
            // Get the selected stop point description
            val selectedStopPointDescription = stopPointMap.keys.toList()[position]

            // Get the corresponding stop point ID
            selectedStopPointId = stopPointMap[selectedStopPointDescription]
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateInputs()) {
                val equipment = EquipmentEntity(
                    description = binding.descriptionInput.text.toString(),
                    dn_number = binding.dnNumberInput.text.toString(),
                    equipment = binding.equipmentInput.text.toString(),
                    journey_id = selectedJourneyId?.toInt() ?: 0,
                    number_of_units = binding.numberOfUnitsInput.text.toString().toInt(),
                    stop_point_id = selectedStopPointId?.toInt() ?: 0,
                    unit_cost = binding.unitCostInput.text.toString().toInt()
                )

                equipmentViewModel.createEquipment(equipment)

                viewLifecycleOwner.lifecycleScope.launch {
                    equipmentViewModel.uiState.collect { state ->
                        when (state) {
                            is EquipmentUiState.Success -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                clearInputs()
                            }
                            is EquipmentUiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun clearInputs() {
        binding.journeyInput.setText("")
        binding.stopPointInput.setText("")
        binding.descriptionInput.setText("")
        binding.dnNumberInput.setText("")
        binding.equipmentInput.setText("")
        binding.numberOfUnitsInput.setText("")
        binding.unitCostInput.setText("")

        selectedJourneyId = null
        selectedStopPointId = null
    }

    private fun validateInputs(): Boolean {
        return validateTextInput(binding.journeyLayout) &&
                validateTextInput(binding.stopPointLayout) &&
                validateTextInput(binding.descriptionLayout) &&
                validateTextInput(binding.dnNumberLayout) &&
                validateTextInput(binding.equipmentLayout) &&
                validateTextInput(binding.numberOfUnitsLayout) &&
                validateTextInput(binding.unitCostLayout) &&
                validateNumbers()
    }

    // Add this new function to validate number inputs
    private fun validateNumbers(): Boolean {
        try {
            // Only validate if the fields are not empty (empty check is done in validateTextInput)
            if (!binding.numberOfUnitsInput.text.isNullOrEmpty()) {
                binding.numberOfUnitsInput.text.toString().toInt()
            }
            if (!binding.unitCostInput.text.isNullOrEmpty()) {
                binding.unitCostInput.text.toString().toInt()
            }
            return true
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun validateTextInput(inputLayout: TextInputLayout): Boolean {
        val editText = inputLayout.editText
        return if (editText?.text.isNullOrEmpty()) {
            inputLayout.error = "This field is required"
            false
        } else {
            inputLayout.error = null
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
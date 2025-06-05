package com.example.farmdatapod.logistics.inputAllocation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.farmdatapod.databinding.FragmentInputAllocationBinding
import com.example.farmdatapod.logistics.inputAllocation.data.PlanJourneyInputsEntity
import com.example.farmdatapod.logistics.inputAllocation.data.PlanJourneyInputsUiState
import com.example.farmdatapod.logistics.inputAllocation.data.PlanJourneyInputsViewModel
import com.example.farmdatapod.logistics.planJourney.data.JourneyViewModel
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class InputAllocationFragment : Fragment() {

    private var _binding: FragmentInputAllocationBinding? = null
    private val binding get() = _binding!!

    private val journeyViewModel: JourneyViewModel by viewModels {
        JourneyViewModel.factory(requireActivity().application)
    }

    private val planJourneyInputsViewModel: PlanJourneyInputsViewModel by viewModels {
        PlanJourneyInputsViewModel.factory(requireActivity().application)
    }

    private val journeyMap = mutableMapOf<String, Long>()
    private val stopPointMap = mutableMapOf<String, Long>()

    private var selectedJourneyId: Long? = null
    private var selectedStopPointId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInputAllocationBinding.inflate(inflater, container, false)
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
            journeyMap.clear()
            val journeyNames = journeysWithStopPoints.map { journeyWithStops ->
                val displayName = "${journeyWithStops.journey.driver} - ${journeyWithStops.journey.date_and_time}"
                journeyMap[displayName] = journeyWithStops.journey.id
                displayName
            }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                journeyMap.keys.toList()
            )
            binding.journeyIdInput.setAdapter(adapter)
        }
    }

    private fun setupJourneySelectionListener() {
        binding.journeyIdInput.setOnItemClickListener { _, _, position, _ ->
            val selectedDisplayName = journeyMap.keys.toList()[position]
            selectedJourneyId = journeyMap[selectedDisplayName]
            val selectedJourney = journeyViewModel.journeys.value?.find {
                it.journey.id == selectedJourneyId
            }
            stopPointMap.clear()
            val stopPointDescriptions = selectedJourney?.stopPoints?.map { stopPoint ->
                val description = "${stopPoint.description} - ${stopPoint.time}"
                stopPointMap[description] = stopPoint.id
                description
            } ?: emptyList()
            val stopPointAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                stopPointDescriptions
            )
            binding.stopPointIdInput.setAdapter(stopPointAdapter)
        }
    }

    private fun setupStopPointSelectionListener() {
        binding.stopPointIdInput.setOnItemClickListener { _, _, position, _ ->
            val selectedStopPointDescription = stopPointMap.keys.toList()[position]
            selectedStopPointId = stopPointMap[selectedStopPointDescription]
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateInputs()) {
                val inputAllocation = PlanJourneyInputsEntity(
                    journey_id = selectedJourneyId?.toString() ?: "",
                    stop_point_id = selectedStopPointId?.toString() ?: "",
                    input = binding.inputField.text.toString(),
                    number_of_units = binding.numberOfUnitsInput.text.toString().toInt(),
                    unit_cost = binding.unitCostInput.text.toString().toDouble(),
                    description = binding.descriptionInput.text.toString(),
                    dn_number = binding.deliveryNoteNumberInput.text.toString(),
                    server_id = 0L
                )

                planJourneyInputsViewModel.createPlanJourneyInput(inputAllocation)

                viewLifecycleOwner.lifecycleScope.launch {
                    planJourneyInputsViewModel.uiState.collect { state ->
                        when (state) {
                            is PlanJourneyInputsUiState.Success -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                clearInputs()
                            }
                            is PlanJourneyInputsUiState.Error -> {
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
        binding.journeyIdInput.setText("")
        binding.stopPointIdInput.setText("")
        binding.inputField.setText("")
        binding.numberOfUnitsInput.setText("")
        binding.unitCostInput.setText("")
        binding.descriptionInput.setText("")
        binding.deliveryNoteNumberInput.setText("")
        selectedJourneyId = null
        selectedStopPointId = null
    }

    private fun validateInputs(): Boolean {
        return validateTextInput(binding.journeyIdLayout) &&
                validateTextInput(binding.stopPointIdLayout) &&
                validateTextInput(binding.inputLayout) &&
                validateTextInput(binding.numberOfUnitsLayout) &&
                validateTextInput(binding.unitCostLayout) &&
                validateTextInput(binding.descriptionLayout) &&
                validateTextInput(binding.deliveryNoteNumberLayout)
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
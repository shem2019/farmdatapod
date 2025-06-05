package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.databinding.FragmentEquipmentLoadingBinding
import com.example.farmdatapod.logistics.equipments.data.EquipmentEntity
import com.example.farmdatapod.logistics.equipments.data.EquipmentViewModel
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.adapter.EquipmentsAdapter
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data.EquipmentLoadingEntity
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data.EquipmentLoadingState
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data.EquipmentLoadingUiState
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data.EquipmentLoadingViewModel
import com.example.farmdatapod.logistics.planJourney.data.JourneyViewModel
import com.example.farmdatapod.utils.DialogUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class EquipmentLoadingFragment : Fragment() {

    private var _binding: FragmentEquipmentLoadingBinding? = null
    private val binding get() = _binding!!

    private val journeyViewModel: JourneyViewModel by viewModels {
        JourneyViewModel.factory(requireActivity().application)
    }

    private val equipmentViewModel: EquipmentViewModel by viewModels {
        EquipmentViewModel.factory(requireActivity().application)
    }

    private val equipmentLoadingViewModel: EquipmentLoadingViewModel by viewModels {
        EquipmentLoadingViewModel.factory(requireActivity().application)
    }

    private lateinit var equipmentsAdapter: EquipmentsAdapter
    private lateinit var loadingDialog: AlertDialog

    private val journeyMap = mutableMapOf<String, Long>()
    private val stopPointMap = mutableMapOf<String, Long>()

    private var selectedJourneyId: Long? = null
    private var selectedStopPointId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEquipmentLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeLoadingDialog()
        setupRecyclerView()
        setupObservers()
        setupJourneySelectionListener()
        setupStopPointSelectionListener()
        setupBackButton()
        setupSubmitButton()
    }

    private fun initializeLoadingDialog() {
        loadingDialog = DialogUtils(requireContext()).buildLoadingDialog()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        equipmentsAdapter = EquipmentsAdapter()
        equipmentsAdapter.setListener(object : EquipmentsAdapter.EquipmentInteractionListener {
            override fun onEquipmentSelected(equipmentState: EquipmentLoadingState) {
                handleEquipmentSelection(equipmentState)
            }

            override fun onQuantityChanged(equipmentState: EquipmentLoadingState, newQuantity: Int) {
                handleQuantityChange(equipmentState, newQuantity)
            }

            override fun onAuthorizationChanged(equipmentState: EquipmentLoadingState, isAuthorized: Boolean) {
                handleAuthorizationChange(equipmentState, isAuthorized)
            }
        })

        binding.equipmentRecyclerView.apply {
            adapter = equipmentsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun handleEquipmentSelection(equipmentState: EquipmentLoadingState) {
        binding.submitButton.isEnabled = true
    }

    private fun handleQuantityChange(equipmentState: EquipmentLoadingState, newQuantity: Int) {
        // Update the state
        equipmentState.quantityToLoad = newQuantity
    }

    private fun handleAuthorizationChange(equipmentState: EquipmentLoadingState, isAuthorized: Boolean) {
        // Update the state
        equipmentState.isAuthorized = isAuthorized
    }

    private fun setupObservers() {
        // Journey Observer
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
            binding.journeyInput.setAdapter(adapter)
        }

        // Equipment Loading Observer
        // Equipment Loading Observer
        equipmentLoadingViewModel.equipmentLoadings.observe(viewLifecycleOwner) { loadingList ->
            val filteredList = loadingList
                .filter { loading ->
                    loading.journey_id == selectedJourneyId?.toInt() &&
                            loading.stop_point_id == selectedStopPointId?.toInt()
                }
                .map { loading ->
                    EquipmentLoadingState(
                        equipment = loading,
                        quantityToLoad = loading.quantity_loaded,
                        isAuthorized = loading.authorised
                    )
                }

            equipmentsAdapter.submitList(filteredList)
            binding.noEquipmentText.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        }

        // Loading State Observer
        equipmentLoadingViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }

        // Error Observer
        equipmentLoadingViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                equipmentLoadingViewModel.clearError()
            }
        }

        // UI State Observer
        lifecycleScope.launch {
            equipmentLoadingViewModel.uiState.collect { state ->
                when (state) {
                    is EquipmentLoadingUiState.Success -> {
                        hideLoading()
                        showSuccess(state.message)
                        clearSelections()
                    }
                    is EquipmentLoadingUiState.Error -> {
                        hideLoading()
                        showError(state.message)
                    }
                    is EquipmentLoadingUiState.Loading -> showLoading()
                    else -> { /* Initial state, do nothing */ }
                }
            }
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            val selectedState = equipmentsAdapter.getSelectedEquipment()
            selectedState?.let { state ->
                val equipment = state.equipment
                val loadingEntity = EquipmentLoadingEntity(
                    server_id = 0L,
                    authorised = state.isAuthorized,
                    delivery_note_number = equipment.delivery_note_number,
                    dn_number = equipment.delivery_note_number,  // Added this
                    equipment = equipment.equipment,
                    journey_id = selectedJourneyId?.toInt() ?: 0,
                    stop_point_id = selectedStopPointId?.toInt() ?: 0,
                    number_of_units = equipment.quantity_loaded,  // Added this
                    quantity_loaded = state.quantityToLoad,
                    syncStatus = false,
                    lastModified = System.currentTimeMillis(),
                    lastSynced = null
                )
                equipmentLoadingViewModel.createEquipmentLoading(loadingEntity)
            }
        }
        binding.submitButton.isEnabled = false
    }

    private fun setupJourneySelectionListener() {
        binding.journeyInput.setOnItemClickListener { _, _, position, _ ->
            val selectedDisplayName = journeyMap.keys.toList()[position]
            selectedJourneyId = journeyMap[selectedDisplayName]

            val selectedJourney = journeyViewModel.journeys.value?.find {
                it.journey.id == selectedJourneyId
            }

            stopPointMap.clear()
            binding.stopPointInput.text = null

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
            binding.stopPointInput.setAdapter(stopPointAdapter)

            clearSelections()
            loadEquipmentForCurrentSelection()
        }
    }
    private fun loadEquipmentForCurrentSelection() {
        selectedJourneyId?.let { journeyId ->
            equipmentLoadingViewModel.getEquipmentLoadingsByJourneyId(journeyId.toInt())
        }
    }
    private fun setupStopPointSelectionListener() {
        binding.stopPointInput.setOnItemClickListener { _, _, position, _ ->
            val selectedStopPointDescription = stopPointMap.keys.toList()[position]
            selectedStopPointId = stopPointMap[selectedStopPointDescription]

            clearSelections()
            loadEquipmentForCurrentSelection()
        }
    }

    private fun clearSelections() {
        equipmentsAdapter.clearSelection()
        binding.submitButton.isEnabled = false
    }



    private fun showLoading() {
        if (!loadingDialog.isShowing) {
            loadingDialog.show()
        }
    }

    private fun hideLoading() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
        _binding = null
    }
}
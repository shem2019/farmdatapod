package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.databinding.FragmentInputLoadingBinding
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.adapter.InputLoadingAdapter
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data.LoadingInputEntity
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data.LoadingInputUiState
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data.LoadingInputViewModel
import com.example.farmdatapod.logistics.inputAllocation.data.PlanJourneyInputsViewModel
import com.example.farmdatapod.logistics.planJourney.data.JourneyViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class InputLoadingFragment : Fragment() {
    private var _binding: FragmentInputLoadingBinding? = null
    private val binding get() = _binding!!

    private val loadingInputViewModel: LoadingInputViewModel by viewModels {
        LoadingInputViewModel.factory(requireActivity().application)
    }

    private val viewModel: PlanJourneyInputsViewModel by viewModels {
        PlanJourneyInputsViewModel.factory(requireActivity().application)
    }

    private val journeyViewModel: JourneyViewModel by viewModels {
        JourneyViewModel.factory(requireActivity().application)
    }

    private lateinit var adapter: InputLoadingAdapter
    private val journeyMap = mutableMapOf<String, Long>() // Display name to ID
    private val stopPointMap = mutableMapOf<String, Long>() // Display name to ID

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        setupLoadingInputObservers()  // Add this line

        // Load initial data
        journeyViewModel.loadJourneyInfo()
        journeyViewModel.loadJourneyDetails()
        viewModel.loadJourneysAndStopPoints()
    }



    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate Journey selection
        if (binding.journeyInput.text.isNullOrEmpty()) {
            binding.journeyLayout.error = "Please select a journey"
            isValid = false
        }

        // Validate Stop Point selection
        if (binding.stopPointInput.text.isNullOrEmpty()) {
            binding.stopPointLayout.error = "Please select a stop point"
            isValid = false
        }

        // Validate input selection
        if (!adapter.hasSelectedInputs()) {
            showError("Please select at least one input")
            isValid = false
        }

        return isValid
    }

    private fun setupViews() {
        // Initialize RecyclerView and Adapter
        adapter = InputLoadingAdapter()
        binding.inputRecyclerView.apply {
            adapter = this@InputLoadingFragment.adapter
            layoutManager = LinearLayoutManager(context)
            // Initially hide the RecyclerView
            visibility = View.GONE
        }

        // Also initially hide the stop points input
        binding.stopPointLayout.visibility = View.GONE

        // Back button click handler
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Submit button click handler with validation
        binding.submitButton.setOnClickListener {
            if (validateInputs()) {
                handleSubmission()
            }
        }

        // Initially disable submit button
        binding.submitButton.isEnabled = false

        setupDropdowns()
    }

    private fun setupDropdowns() {
        binding.journeyInput.setOnItemClickListener { _, _, position, _ ->
            val selectedName = journeyMap.keys.toList()[position]
            val journeyId = journeyMap[selectedName]
            journeyId?.let { id ->
                // Load journey details for this specific journey
                journeyViewModel.loadJourneyDetailsForJourney(id)

                // Show stop points layout when journey is selected
                binding.stopPointLayout.visibility = View.VISIBLE

                // Filter inputs for this journey
                viewModel.loadPlanJourneyInputs()
                viewModel.filterInputs(journeyId = id.toString())

                // Show the inputs RecyclerView
                binding.inputRecyclerView.visibility = View.VISIBLE

                // Clear previous stop point selection
                binding.stopPointInput.setText("")
                binding.stopPointInput.setAdapter(null)  // Clear the adapter until new stop points load
            }
            binding.journeyLayout.error = null
        }

        // Handle when journey input changes or is cleared
        binding.journeyInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    // Journey selection was cleared
                    binding.stopPointLayout.visibility = View.GONE
                    binding.inputRecyclerView.visibility = View.GONE
                    binding.submitButton.isEnabled = false
                    adapter.setInputs(emptyList())
                }
            }
        })

        binding.stopPointInput.setOnItemClickListener { _, _, position, _ ->
            val selectedName = stopPointMap.keys.toList()[position]
            val stopPointId = stopPointMap[selectedName]
            stopPointId?.let { id ->
                val selectedJourneyName = binding.journeyInput.text.toString()
                val journeyId = journeyMap[selectedJourneyName]
                if (journeyId != null) {
                    viewModel.filterInputs(
                        journeyId = journeyId.toString(),
                        stopPointId = id.toString()
                    )
                }
            }
            binding.stopPointLayout.error = null
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe journey info state
                launch {
                    journeyViewModel.journeyInfoState.collect { state ->
                        when (state) {
                            is JourneyViewModel.JourneyInfoState.Success -> {
                                journeyMap.clear()
                                val journeyNames = state.journeyInfo.map { info ->
                                    val displayName = "${info.journey_name} (ID: ${info.journey_id})"
                                    journeyMap[displayName] = info.journey_id
                                    displayName
                                }
                                val adapter = ArrayAdapter(
                                    requireContext(),
                                    android.R.layout.simple_dropdown_item_1line,
                                    journeyNames
                                )
                                binding.journeyInput.setAdapter(adapter)
                                this@InputLoadingFragment.adapter.updateJourneyNames(state.journeyInfo)
                            }
                            is JourneyViewModel.JourneyInfoState.Error -> {
                                showError(state.message)
                            }
                            is JourneyViewModel.JourneyInfoState.Loading -> {
                                binding.journeyInput.isEnabled = false
                            }
                        }
                    }
                }

                // Observe journey detail state
                launch {
                    journeyViewModel.journeyDetailState.collect { state ->
                        when (state) {
                            is JourneyViewModel.JourneyDetailState.Success -> {
                                stopPointMap.clear()
                                val stopPointNames = state.journeyDetails
                                    .filter { it.stop_point_id != null }
                                    .map { detail ->
                                        val displayName = "${detail.stop_point_name} (ID: ${detail.stop_point_id})"
                                        stopPointMap[displayName] = detail.stop_point_id!!
                                        displayName
                                    }

                                if (stopPointNames.isEmpty()) {
                                    showError("No stop points found for selected journey")
                                    binding.stopPointLayout.visibility = View.GONE
                                } else {
                                    binding.stopPointLayout.visibility = View.VISIBLE
                                    val adapter = ArrayAdapter(
                                        requireContext(),
                                        android.R.layout.simple_dropdown_item_1line,
                                        stopPointNames
                                    )
                                    binding.stopPointInput.setAdapter(adapter)
                                    this@InputLoadingFragment.adapter.updateStopPointNames(state.journeyDetails)
                                }
                            }
                            is JourneyViewModel.JourneyDetailState.Error -> {
                                binding.stopPointLayout.visibility = View.GONE
                                showError(state.message)
                            }
                            is JourneyViewModel.JourneyDetailState.Loading -> {
                                binding.stopPointInput.isEnabled = false
                            }
                        }
                    }
                }
            }
        }

        // Observe filtered inputs
        viewModel.planJourneyInputs.observe(viewLifecycleOwner) { inputs ->
            if (binding.inputRecyclerView.visibility == View.VISIBLE) {
                if (inputs.isEmpty()) {
                    showError("No inputs found for selected journey")
                    binding.inputRecyclerView.visibility = View.GONE
                } else {
                    adapter.setInputs(inputs)
                    binding.submitButton.isEnabled = true
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.submitButton.isEnabled = !isLoading && binding.inputRecyclerView.visibility == View.VISIBLE
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }



    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun handleSubmission() {
        val selectedInputs = adapter.getSelectedInputs()
        val selectedJourneyName = binding.journeyInput.text.toString()
        val selectedStopPointName = binding.stopPointInput.text.toString()

        val journeyId = journeyMap[selectedJourneyName]
        val stopPointId = stopPointMap[selectedStopPointName]

        if (journeyId != null && stopPointId != null) {
            // Disable submit button immediately
            binding.submitButton.isEnabled = false

            selectedInputs.forEach { planJourneyInput ->
                val loadingInput = LoadingInputEntity(
                    server_id = 0L,
                    authorised = false,
                    delivery_note_number = planJourneyInput.dn_number,
                    input = planJourneyInput.id,
                    journey_id = journeyId.toInt(),
                    quantity_loaded = planJourneyInput.number_of_units,
                    stop_point_id = stopPointId.toInt()
                )
                loadingInputViewModel.saveLoadingInput(loadingInput)
            }
        } else {
            showError("Invalid journey or stop point selection")
        }
    }

    private fun setupLoadingInputObservers() {
        // Single collection point for UI state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loadingInputViewModel.uiState.collect { state ->
                    when (state) {
                        is LoadingInputUiState.Success -> {
                            binding.submitButton.isEnabled = true
                            Snackbar.make(
                                binding.root,
                                state.message,
                                Snackbar.LENGTH_SHORT
                            ).show()
                            adapter.clearSelections()
                            // Refresh inputs after successful submission
                            viewModel.loadPlanJourneyInputs()
                        }
                        is LoadingInputUiState.Error -> {
                            binding.submitButton.isEnabled = true
                            showError(state.message)
                        }
                        is LoadingInputUiState.Loading -> {
                            binding.submitButton.isEnabled = false
                        }
                        else -> {
                            binding.submitButton.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        // Clear adapters before clearing binding
        binding.journeyInput.setAdapter(null)
        binding.stopPointInput.setAdapter(null)
        binding.inputRecyclerView.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
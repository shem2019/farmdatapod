package com.example.farmdatapod.logistics.inputTransfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.databinding.FragmentInputTransferBinding
import com.example.farmdatapod.hub.hubRegistration.data.HubRepository
import com.example.farmdatapod.logistics.inputTransfer.data.InputTransferEntity
import com.example.farmdatapod.logistics.inputTransfer.data.InputTransferUiState
import com.example.farmdatapod.logistics.inputTransfer.data.InputTransferViewModel
import kotlinx.coroutines.launch

class InputTransferFragment : Fragment() {
    private var _binding: FragmentInputTransferBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InputTransferViewModel by viewModels {
        InputTransferViewModel.factory(requireActivity().application)
    }

    private lateinit var hubRepository: HubRepository
    private var selectedOriginHubId: Int? = null
    private var selectedDestinationHubId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputTransferBinding.inflate(inflater, container, false)
        hubRepository = HubRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupHubDropdowns()
        setupRequestButton()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupHubDropdowns() {
        viewLifecycleOwner.lifecycleScope.launch {
            hubRepository.getAllHubs().collect { hubList ->
                var filteredHubList = hubList

                // Origin Hub
                val originAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    hubList.map { it.hubName }
                )

                (binding.originHubInput as? AutoCompleteTextView)?.apply {
                    setAdapter(originAdapter)
                    setOnItemClickListener { _, _, position, _ ->
                        selectedOriginHubId = hubList[position].id
                        binding.originHubLayout.error = null

                        // Update destination dropdown by filtering out the selected origin hub
                        val filteredHubs = hubList.filter { it.id != selectedOriginHubId }
                        val destinationAdapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            filteredHubs.map { it.hubName }
                        )

                        // Clear and update destination if it matches the selected origin
                        if (selectedDestinationHubId == selectedOriginHubId) {
                            binding.destinationHubInput.setText("")
                            selectedDestinationHubId = null
                        }

                        binding.destinationHubInput.setAdapter(destinationAdapter)
                    }
                }

                // Destination Hub
                (binding.destinationHubInput as? AutoCompleteTextView)?.apply {
                    // Initially show all hubs except selected origin (if any)
                    val initialFilteredHubs = selectedOriginHubId?.let { originId ->
                        hubList.filter { it.id != originId }
                    } ?: hubList

                    val destinationAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        initialFilteredHubs.map { it.hubName }
                    )

                    setAdapter(destinationAdapter)
                    setOnItemClickListener { _, _, position, _ ->
                        // We need to use the filtered list for getting the correct hub
                        selectedDestinationHubId = initialFilteredHubs[position].id
                        binding.destinationHubLayout.error = null
                    }
                }
            }
        }
    }

    private fun setupRequestButton() {
        binding.requestButton.setOnClickListener {
            if (validateInputs()) {
                createInputTransfer()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate Origin Hub
        if (selectedOriginHubId == null) {
            binding.originHubLayout.error = "Please select origin hub"
            isValid = false
        } else {
            binding.originHubLayout.error = null
        }

        // Validate Destination Hub
        if (selectedDestinationHubId == null) {
            binding.destinationHubLayout.error = "Please select destination hub"
            isValid = false
        } else {
            binding.destinationHubLayout.error = null
        }

        // Validate Product (ensure it's a number)
        if (binding.productInput.text.isNullOrBlank()) {
            binding.productLayout.error = "Please enter a product"
            isValid = false
        } else {
            try {
                binding.productInput.text.toString().toInt()
                binding.productLayout.error = null
            } catch (e: NumberFormatException) {
                binding.productLayout.error = "Please enter a valid product number"
                isValid = false
            }
        }

        // Validate Quantity
        if (binding.quantityInput.text.isNullOrBlank()) {
            binding.quantityLayout.error = "Please enter quantity"
            isValid = false
        } else {
            try {
                binding.quantityInput.text.toString().toInt()
                binding.quantityLayout.error = null
            } catch (e: NumberFormatException) {
                binding.quantityLayout.error = "Please enter a valid number"
                isValid = false
            }
        }

        // Check if origin and destination hubs are different
        if (selectedOriginHubId == selectedDestinationHubId && selectedOriginHubId != null) {
            binding.destinationHubLayout.error = "Origin and destination hubs must be different"
            isValid = false
        }

        return isValid
    }

    private fun createInputTransfer() {
        val inputTransfer = InputTransferEntity(
            server_id = 0,
            origin_hub_id = selectedOriginHubId!!,
            destination_hub_id = selectedDestinationHubId!!,
            input = binding.productInput.text.toString().toInt(),
            quantity = binding.quantityInput.text.toString().toInt(),
            status = "PENDING"
        )

        viewModel.createInputTransfer(inputTransfer)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    InputTransferUiState.Loading -> {
                        binding.requestButton.isEnabled = false
                        binding.requestButton.text = "Processing..."
                    }
                    is InputTransferUiState.Success -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    is InputTransferUiState.Error -> {
                        binding.requestButton.isEnabled = true
                        binding.requestButton.text = "Request"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    InputTransferUiState.Initial -> {
                        // Initial state, no action needed
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
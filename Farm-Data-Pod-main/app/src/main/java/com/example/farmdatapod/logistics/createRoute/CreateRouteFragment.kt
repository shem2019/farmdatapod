package com.example.farmdatapod.logistics.createRoute

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.adapter.StopPointAdapter
import com.example.farmdatapod.databinding.FragmentCreateRouteBinding
import com.example.farmdatapod.logistics.createRoute.data.RouteEntity
import com.example.farmdatapod.logistics.createRoute.data.RouteUiState
import com.example.farmdatapod.logistics.createRoute.data.RouteViewModel
import com.example.farmdatapod.models.RouteStopPoint
import com.example.farmdatapod.utils.NetworkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CreateRouteFragment : Fragment() {
    private var _binding: FragmentCreateRouteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RouteViewModel by viewModels()
    private lateinit var stopPointAdapter: StopPointAdapter

    private var isFormValid = false
    private var isClearing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
        setupValidation()
    }

    private fun setupUI() {
        // Initialize RecyclerView and Adapter
        setupRecyclerView()

        // Add an initial stop point
        stopPointAdapter.addStopPoint(RouteStopPoint(""))

        // Setup Back Button
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Setup Add Stop Point Button
        binding.addStopPointButton.setOnClickListener {
            stopPointAdapter.addStopPoint(RouteStopPoint(""))
        }

        // Setup Submit Button
        binding.submitButton.setOnClickListener {
            if (validateForm()) {
                submitForm()
            }
        }
    }

    private fun setupRecyclerView() {
        stopPointAdapter = StopPointAdapter(mutableListOf())
        binding.stopPointsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stopPointAdapter
        }

        stopPointAdapter.setOnDataChangedListener { stopPoints ->
            validateForm()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is RouteUiState.Loading -> showLoading(true)
                    is RouteUiState.Success -> {
                        showLoading(false)
                        showSuccess(state.message)
                        clearForm()
                    }
                    is RouteUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                    is RouteUiState.Initial -> showLoading(false)
                }
            }
        }
    }


        fun setupValidation() {
            // Route Number Validation
            binding.routeNumberInput.doOnTextChanged { text, _, _, _ ->
                if (!isClearing) {  // Only validate if not clearing
                    binding.routeNumberLayout.error = if (text.isNullOrBlank()) {
                        "Route number is required"
                    } else null
                    validateForm()
                }
            }

            // Starting Point Validation
            binding.startingPointInput.doOnTextChanged { text, _, _, _ ->
                if (!isClearing) {  // Only validate if not clearing
                    binding.startingPointLayout.error = if (text.isNullOrBlank()) {
                        "Starting point is required"
                    } else null
                    validateForm()
                }
            }

            // Final Destination Validation
            binding.finalDestinationInput.doOnTextChanged { text, _, _, _ ->
                if (!isClearing) {  // Only validate if not clearing
                    binding.finalDestinationLayout.error = if (text.isNullOrBlank()) {
                        "Final destination is required"
                    } else null
                    validateForm()
                }
            }
        }


    private fun validateForm(): Boolean {
        val routeNumber = binding.routeNumberInput.text?.toString()
        val startingPoint = binding.startingPointInput.text?.toString()
        val finalDestination = binding.finalDestinationInput.text?.toString()
        val stopPoints = stopPointAdapter.getStopPoints()

        isFormValid = !routeNumber.isNullOrBlank() &&
                !startingPoint.isNullOrBlank() &&
                !finalDestination.isNullOrBlank() &&
                stopPoints.isNotEmpty() &&
                stopPoints.none { it.stop.isBlank() }

        binding.submitButton.isEnabled = isFormValid
        return isFormValid
    }
    private fun submitForm() {
        if (!validateForm()) {
            showError("Please fill all required fields")
            return
        }

        val routeNumber = binding.routeNumberInput.text.toString()
        val startingPoint = binding.startingPointInput.text.toString()
        val finalDestination = binding.finalDestinationInput.text.toString()
        val stopPoints = stopPointAdapter.getStopPoints()

        val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
        if (!isOnline) {
            showOfflineDialog {
                createRoute(routeNumber, startingPoint, finalDestination, stopPoints)
            }
        } else {
            createRoute(routeNumber, startingPoint, finalDestination, stopPoints)
        }
    }

    private fun createRoute(
        routeNumber: String,
        startingPoint: String,
        finalDestination: String,
        stopPoints: List<RouteStopPoint>
    ) {
        val routeEntity = RouteEntity(
            routeNumber = routeNumber,
            startingPoint = startingPoint,
            finalDestination = finalDestination,
            stopPoints = stopPoints
        )
        viewModel.createRoute(routeEntity)
    }

    private fun showOfflineDialog(onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Offline Mode")
            .setMessage("You are currently offline. The route will be saved locally and synced when online.")
            .setPositiveButton("Continue") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearForm() {
        isClearing = true  // Set flag before clearing
        binding.routeNumberInput.text?.clear()
        binding.startingPointInput.text?.clear()
        binding.finalDestinationInput.text?.clear()
        stopPointAdapter.clearStopPoints()

        // Clear any error states
        binding.routeNumberLayout.error = null
        binding.startingPointLayout.error = null
        binding.finalDestinationLayout.error = null

        isClearing = false  // Reset flag after clearing

        // Reset submit button state
        binding.submitButton.isEnabled = false
    }

    private fun showLoading(show: Boolean) {
        binding.submitButton.isEnabled = !show
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
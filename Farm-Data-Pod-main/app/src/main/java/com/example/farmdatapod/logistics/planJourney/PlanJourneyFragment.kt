package com.example.farmdatapod.logistics.planJourney

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.adapter.JourneyAdapter
import com.example.farmdatapod.databinding.FragmentPlanJourneyBinding
import com.example.farmdatapod.logistics.planJourney.data.JourneyEntity
import com.example.farmdatapod.logistics.planJourney.data.JourneyViewModel
import com.example.farmdatapod.logistics.planJourney.data.StopPointEntity
import com.example.farmdatapod.models.StopPoint
import com.example.farmdatapod.utils.DialogUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlanJourneyFragment : Fragment() {
    private var _binding: FragmentPlanJourneyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JourneyViewModel by viewModels {
        JourneyViewModel.factory(requireActivity().application)
    }

    private lateinit var journeyAdapter: JourneyAdapter
    private lateinit var loadingDialog: AlertDialog

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanJourneyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = DialogUtils(requireContext()).buildLoadingDialog()

        setupRecyclerView()
        setupDateTimePicker()
        setupAddStopPointButton()
        setupSubmitButton()
        setupObservers()

        // Initial data load
        viewModel.loadJourneyInfo()
        viewModel.loadJourneyDetails()
        viewModel.loadJourneyNamesMap()

        // Add initial empty stop point
        journeyAdapter.addItem(StopPoint("", "", "", ""))
    }

    private fun setupRecyclerView() {
        journeyAdapter = JourneyAdapter()
        binding.stopPointsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = journeyAdapter
        }
    }

    private fun setupDateTimePicker() {
        binding.dateTimeInput.setOnClickListener {
            showDateTimePicker()
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            selectedDateTime.set(Calendar.YEAR, year)
            selectedDateTime.set(Calendar.MONTH, month)
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val timeListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDateTime.set(Calendar.MINUTE, minute)
                binding.dateTimeInput.setText(displayDateFormat.format(selectedDateTime.time))
            }

            TimePickerDialog(
                context,
                timeListener,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        context?.let {
            DatePickerDialog(
                it,
                dateListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupAddStopPointButton() {
        binding.addStopPointButton.setOnClickListener {
            journeyAdapter.addItem(StopPoint("", "", "", ""))
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateFields()) {
                submitJourney()
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe sync state
                launch {
                    viewModel.syncState.collect { state ->
                        handleSyncState(state)
                    }
                }

                // Observe journey info state
                launch {
                    viewModel.journeyInfoState.collect { state ->
                        handleJourneyInfoState(state)
                    }
                }

                // Observe journey detail state
                launch {
                    viewModel.journeyDetailState.collect { state ->
                        handleJourneyDetailState(state)
                    }
                }
            }
        }

        // Observe journeys LiveData
        viewModel.journeys.observe(viewLifecycleOwner) { journeysList ->
            // Update UI with journeys list if needed
        }
    }

    private fun handleSyncState(state: JourneyViewModel.SyncState) {
        when (state) {
            is JourneyViewModel.SyncState.Syncing -> {
                binding.submitButton.isEnabled = false
                loadingDialog.show()
            }
            is JourneyViewModel.SyncState.Success -> {
                binding.submitButton.isEnabled = true
                loadingDialog.dismiss()
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                clearForm()
            }
            is JourneyViewModel.SyncState.Error -> {
                binding.submitButton.isEnabled = true
                loadingDialog.dismiss()
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is JourneyViewModel.SyncState.Idle -> {
                binding.submitButton.isEnabled = true
                loadingDialog.dismiss()
            }
        }
    }

    private fun handleJourneyInfoState(state: JourneyViewModel.JourneyInfoState) {
        when (state) {
            is JourneyViewModel.JourneyInfoState.Loading -> {
                // Handle loading state if needed
            }
            is JourneyViewModel.JourneyInfoState.Success -> {
                // Update UI with journey info if needed
                // For example, update a spinner with journey names
            }
            is JourneyViewModel.JourneyInfoState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun handleJourneyDetailState(state: JourneyViewModel.JourneyDetailState) {
        when (state) {
            is JourneyViewModel.JourneyDetailState.Loading -> {
                // Handle loading state if needed
            }
            is JourneyViewModel.JourneyDetailState.Success -> {
                // Update UI with journey details if needed
            }
            is JourneyViewModel.JourneyDetailState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun submitJourney() {
        val stopPoints = journeyAdapter.getItems()

        if (stopPoints.isEmpty()) {
            Snackbar.make(binding.root, "Add at least one stop point", Snackbar.LENGTH_SHORT).show()
            return
        }

        val journey = JourneyEntity(
            date_and_time = isoDateFormat.format(selectedDateTime.time),
            driver = binding.driverInput.text.toString().trim(),
            logistician_status = binding.logisticianStatusInput.text.toString().trim(),
            truck = binding.truckInput.text.toString().trim(),
            route_id = 0,
            server_id = 0L,
            user_id = "",
            syncStatus = false,
            lastModified = System.currentTimeMillis()
        )

        val stopPointEntities = stopPoints.map { stopPoint ->
            StopPointEntity(
                journey_id = 0L,
                description = stopPoint.description,
                purpose = stopPoint.purpose,
                stop_point = stopPoint.stop_point,
                time = stopPoint.time,
                syncStatus = false,
                lastModified = System.currentTimeMillis()
            )
        }

        viewModel.saveJourney(journey, stopPointEntities)
    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (binding.dateTimeInput.text.isNullOrEmpty()) {
            binding.dateTimeLayout.error = "Date and Time is required"
            isValid = false
        } else {
            binding.dateTimeLayout.error = null
        }

        if (binding.driverInput.text.isNullOrEmpty()) {
            binding.driverLayout.error = "Driver is required"
            isValid = false
        } else {
            binding.driverLayout.error = null
        }

        if (binding.logisticianStatusInput.text.isNullOrEmpty()) {
            binding.logisticianStatusLayout.error = "Logistician Status is required"
            isValid = false
        } else {
            binding.logisticianStatusLayout.error = null
        }

        if (binding.truckInput.text.isNullOrEmpty()) {
            binding.truckLayout.error = "Truck is required"
            isValid = false
        } else {
            binding.truckLayout.error = null
        }

        if (journeyAdapter.getItems().isEmpty()) {
            Snackbar.make(binding.root, "Add at least one stop point", Snackbar.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun clearForm() {
        binding.dateTimeInput.text = null
        binding.driverInput.text = null
        binding.logisticianStatusInput.text = null
        binding.truckInput.text = null
        journeyAdapter.clearItems()
        selectedDateTime = Calendar.getInstance()
        journeyAdapter.addItem(StopPoint("", "", "", "")) // Add empty stop point after clearing
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
        _binding = null
    }
}
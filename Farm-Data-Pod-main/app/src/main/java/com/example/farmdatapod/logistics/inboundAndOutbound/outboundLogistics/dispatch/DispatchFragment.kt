package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.databinding.FragmentDispatchBinding
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.data.DispatchEntity
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.data.DispatchUiState
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.data.DispatchViewModel
import com.example.farmdatapod.logistics.planJourney.data.JourneyBasicInfo
import com.example.farmdatapod.logistics.planJourney.data.JourneyViewModel
import com.example.farmdatapod.utils.DialogUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DispatchFragment : Fragment() {
    private var _binding: FragmentDispatchBinding? = null
    private val binding get() = _binding!!

    private lateinit var loadingDialog: AlertDialog
    private val journeyMap = mutableMapOf<String, Long>()
    private var selectedJourneyId: Long? = null
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private val journeyViewModel: JourneyViewModel by viewModels {
        JourneyViewModel.factory(requireActivity().application)
    }

    private val dispatchViewModel: DispatchViewModel by viewModels {
        DispatchViewModel.factory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDispatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        loadingDialog = DialogUtils(requireContext()).buildLoadingDialog()

        setupBackButton()
        setupObservers()
        setupSubmitButton()
        setupJourneySelection()
        setupUiStateObserver()
        setupTimeOfDeparturePicker()
        setupDNSFormatter()
        setupLogisticianStatusDropdown()  // Add this line

        // Enable submit button by default
        binding.submitButton.isEnabled = true
    }
    private fun setupLogisticianStatusDropdown() {
        val statuses = arrayOf("Ready", "On route", "Completed")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            statuses
        )
        binding.logisticianStatusInput.setAdapter(adapter)
    }
    private fun setupObservers() {
        journeyViewModel.journeyBasicInfo.observe(viewLifecycleOwner) { journeyInfoList ->
            setupJourneyDropdown(journeyInfoList)
        }
    }

    private fun setupJourneyDropdown(journeyInfoList: List<JourneyBasicInfo>) {
        journeyMap.clear()
        val journeyList = journeyInfoList.map { journeyInfo ->
            journeyMap[journeyInfo.journey_name] = journeyInfo.journey_id
            journeyInfo.journey_name
        }.sorted()

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            journeyList
        )
        binding.journeyInput.setAdapter(adapter)
    }

    private fun setupJourneySelection() {
        binding.journeyInput.setOnItemClickListener { _, _, position, _ ->
            val selectedDisplayName = journeyMap.keys.toList()[position]
            selectedJourneyId = journeyMap[selectedDisplayName]
            binding.journeyLayout.error = null
            clearOtherInputs()
        }
    }

    private fun setupTimeOfDeparturePicker() {
        binding.timeOfDepartureInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            binding.timeOfDepartureInput.setText(dateTimeFormat.format(calendar.time))
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupDNSFormatter() {
        binding.dnsInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val input = binding.dnsInput.text.toString()
                if (input.isNotEmpty() && !input.startsWith("DNS")) {
                    binding.dnsInput.setText("DNS$input")
                }
            }
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            Log.d("DispatchFragment", "Submit button clicked")
            clearErrors()
            if (validateInputs()) {
                createDispatch()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

// Journey validation
        if (selectedJourneyId == null) {
            binding.journeyLayout.error = "Please select a journey"
            isValid = false
            Log.d("DispatchFragment", "Journey validation failed")
        }
        if (binding.logisticianStatusInput.text.isNullOrEmpty()) {
            binding.logisticianStatusLayout.error = "Please select logistician status"
            isValid = false
            Log.d("DispatchFragment", "Logistician status validation failed")
        }
// DNS validation
        val dnsText = binding.dnsInput.text.toString()
        if (!dnsText.matches(Regex("DNS\\d{6}"))) {
            binding.dnsLayout.error = "DNS must be in format DNS123456"
            isValid = false
            Log.d("DispatchFragment", "DNS validation failed")
        }

// Starting mileage validation
        val mileage = binding.startingMileageInput.text.toString().toDoubleOrNull()
        if (mileage == null || mileage <= 0.0) {
            binding.startingMileageLayout.error = "Starting mileage must be greater than 0"
            isValid = false
            Log.d("DispatchFragment", "Mileage validation failed")
        }

// Starting fuel validation
        val fuel = binding.startingFuelInput.text.toString().toDoubleOrNull()
        if (fuel == null || fuel <= 0.0) {
            binding.startingFuelLayout.error = "Starting fuel must be greater than 0"
            isValid = false
            Log.d("DispatchFragment", "Fuel validation failed")
        }

// Time of departure validation
        if (binding.timeOfDepartureInput.text.isNullOrEmpty()) {
            binding.timeOfDepartureLayout.error = "Please select time of departure"
            isValid = false
            Log.d("DispatchFragment", "Time validation failed")
        }

// Seal confirmation
        if (!binding.sealedRadio.isChecked && !binding.notSealedRadio.isChecked) {
            Toast.makeText(requireContext(), "Please confirm seal status", Toast.LENGTH_SHORT).show()
            isValid = false
            Log.d("DispatchFragment", "Seal validation failed")
        }

        Log.d("DispatchFragment", "Final validation result: $isValid")
        return isValid
    }

    private fun createDispatch() {
        Log.d("DispatchFragment", "Creating dispatch with journeyId: $selectedJourneyId")

        val dispatch = DispatchEntity(
            journey_id = selectedJourneyId?.toInt() ?: return,
            server_id = 0L,
            dns = binding.dnsInput.text.toString(),
            documentation = getDocumentationString(),
            confirm_seal = binding.sealedRadio.isChecked,
            logistician_status = binding.logisticianStatusInput.text.toString(), // Use selected status
            starting_fuel = binding.startingFuelInput.text.toString().toDoubleOrNull() ?: 0.0,
            starting_mileage = binding.startingMileageInput.text.toString().toDoubleOrNull() ?: 0.0,
            time_of_departure = binding.timeOfDepartureInput.text.toString(),
            syncStatus = false
        )

        Log.d("DispatchFragment", "Created dispatch entity: $dispatch")
        dispatchViewModel.createDispatch(dispatch)
    }

    private fun setupUiStateObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dispatchViewModel.uiState.collect { state ->
                    when (state) {
                        is DispatchUiState.Initial -> Unit
                        is DispatchUiState.Loading -> {
                            binding.submitButton.isEnabled = false
                            loadingDialog.takeIf { !it.isShowing }?.show()
                        }
                        is DispatchUiState.Success -> {
                            loadingDialog.takeIf { it.isShowing }?.dismiss()
                            binding.submitButton.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            clearInputs()
                        }
                        is DispatchUiState.Error -> {
                            loadingDialog.takeIf { it.isShowing }?.dismiss()
                            binding.submitButton.isEnabled = true
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun getDocumentationString(): String {
        return buildList {
            if (binding.driversLicenseCheckbox.isChecked) add("Driver's License")
            if (binding.insuranceCheckbox.isChecked) add("Insurance")
            if (binding.vehicleRegistrationCheckbox.isChecked) add("Vehicle Registration")
            if (binding.waybillCheckbox.isChecked) add("Waybill")
        }.joinToString(", ")
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Retry") { binding.submitButton.performClick() }
            .show()
    }

    private fun clearErrors() {
        binding.apply {
            journeyLayout.error = null
            dnsLayout.error = null
            startingMileageLayout.error = null
            startingFuelLayout.error = null
            timeOfDepartureLayout.error = null
        }
    }

    private fun clearInputs() {
        binding.apply {
            journeyInput.setText("")
            dnsInput.setText("")
            startingMileageInput.setText("")
            startingFuelInput.setText("")
            timeOfDepartureInput.setText("")
            deliveryNotesInput.setText("")

            driversLicenseCheckbox.isChecked = false
            insuranceCheckbox.isChecked = false
            vehicleRegistrationCheckbox.isChecked = false
            waybillCheckbox.isChecked = false

            sealedRadio.isChecked = false
            notSealedRadio.isChecked = false
        }
        selectedJourneyId = null
        clearErrors()
    }

    private fun clearOtherInputs() {
        binding.apply {
            dnsInput.setText("")
            startingMileageInput.setText("")
            startingFuelInput.setText("")
            timeOfDepartureInput.setText("")
            deliveryNotesInput.setText("")

            driversLicenseCheckbox.isChecked = false
            insuranceCheckbox.isChecked = false
            vehicleRegistrationCheckbox.isChecked = false
            waybillCheckbox.isChecked = false

            sealedRadio.isChecked = false
            notSealedRadio.isChecked = false
        }
        clearErrors()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
        _binding = null
    }
}
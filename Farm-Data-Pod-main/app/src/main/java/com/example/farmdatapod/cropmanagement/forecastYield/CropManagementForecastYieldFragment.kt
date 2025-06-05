package com.example.farmdatapod.cropmanagement.forecastYield

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.farmdatapod.R
import com.example.farmdatapod.cropmanagement.forecastYield.data.YieldForecastManagementViewModel
import com.example.farmdatapod.databinding.FragmentCropManagementForecastYieldBinding
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.season.forecastYields.data.YieldForecast
import com.example.farmdatapod.season.register.registerSeasonData.Season
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.example.farmdatapod.utils.DialogUtils
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class CropManagementForecastYieldFragment : Fragment() {

    private var _binding: FragmentCropManagementForecastYieldBinding? = null
    private val binding get() = _binding!!


    private lateinit var seasonRepository: SeasonRepository
    private lateinit var viewModel: YieldForecastManagementViewModel
    private lateinit var producerRepository: ProducerRepository
    private lateinit var fieldRegistrationRepository: FieldRegistrationRepository

    // Selection tracking variables
    private var selectedProducerCode: String? = null
    private var selectedProducerId: Int? = null
    private var selectedSeasonId: Int? = null
    private var selectedFieldNumber: String? = null

    // Data storage
    private lateinit var producersMap: MutableMap<String, String>
    private lateinit var fieldsMap: MutableMap<String, String>

    // UI elements
    private lateinit var loadingDialog: AlertDialog

    // Coroutine jobs for cancellation
    private var producersJob: Job? = null
    private var fieldsJob: Job? = null

    // Flag for producer type

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize view binding and repositories
        _binding = FragmentCropManagementForecastYieldBinding.inflate(inflater, container, false)
        producerRepository = ProducerRepository(requireContext())
        seasonRepository = SeasonRepository(requireContext())
        fieldRegistrationRepository = FieldRegistrationRepository(requireContext())

        // Initialize the ViewModel
        viewModel = ViewModelProvider(
            this,
            YieldForecastManagementViewModel.Factory(requireContext())
        )[YieldForecastManagementViewModel::class.java]

        // Initialize data storage


        // Initialize loading dialog
        loadingDialog = DialogUtils(requireContext()).buildLoadingDialog()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        setupBackButton()
        setupDatePicker()
        loadProducers() // Start loading producers
        setupForecastQualityDropdown()
        setupSubmitButton()
    }

    private fun setupObservers() {
        viewModel.submitStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is YieldForecastManagementViewModel.SubmitResult.Success -> { // Updated to Management ViewModel
                    Toast.makeText(
                        context,
                        "Yield forecast submitted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearFields()
                }

                is YieldForecastManagementViewModel.SubmitResult.Error -> { // Updated to Management ViewModel
                    Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.submitButton.isEnabled = !isLoading
            if (isLoading) loadingDialog.show() else loadingDialog.dismiss()
        }

        viewModel.syncStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is YieldForecastManagementViewModel.SyncResult.Success -> { // Updated to Management ViewModel
                    Toast.makeText(context, "Synced ${result.count} records", Toast.LENGTH_SHORT)
                        .show()
                }

                is YieldForecastManagementViewModel.SyncResult.Error -> { // Updated to Management ViewModel
                    Toast.makeText(context, "Sync error: ${result.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun loadProducers() {
        producersJob?.cancel()
        producersJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                producerRepository.getAllProducers()
                    .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { producers ->
                        if (isAdded && context != null) {
                            val producerNames =
                                producers.map { "${it.otherName} ${it.lastName} (${it.farmerCode})" }
                            val producerAdapter = ArrayAdapter(
                                requireContext(),
                                R.layout.dropdown_item,
                                producerNames
                            )
                            binding.producerDropdown.setAdapter(producerAdapter)

                            binding.producerDropdown.setOnItemClickListener { _, _, position, _ ->
                                val selectedProducer = producers[position]
                                handleProducerSelection(selectedProducer)
                            }
                        }
                    }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e("FieldRegistration", "Error loading producers", e)
                    Toast.makeText(context, "Error loading producers", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleProducerSelection(selectedProducer: ProducerEntity) {
        selectedProducerId = selectedProducer.id
        selectedProducerCode = selectedProducer.farmerCode
        binding.producerLayout.error = null

        // Load related data
        loadFieldsForProducer(selectedProducer.farmerCode)
        loadSeasonsForProducer()
    }

    private fun loadFieldsForProducer(producerCode: String) {
        fieldsJob?.cancel()
        fieldsJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                fieldRegistrationRepository
                    .getFieldRegistrationsByProducer(producerCode)
                    .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { fieldRegistrations ->
                        // Check if Fragment is still attached and has a valid context
                        if (isAdded && context != null) {
                            fieldsMap = fieldRegistrations.associate {
                                it.fieldRegistration.fieldNumber.toString() to "Field ${it.fieldRegistration.fieldNumber}"
                            }.toMutableMap()
                            updateFieldDropdown()
                        }
                    }
            } catch (e: Exception) {
                if (e !is CancellationException) {  // Ignore normal job cancellation
                    Log.e("FieldDebug", "Error loading fields", e)
                    // Only show Toast if Fragment is attached and has context
                    if (isAdded && context != null) {
                        Toast.makeText(
                            requireContext(),
                            "Error loading fields: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun loadSeasonsForProducer() {
        selectedProducerCode?.let { code ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    seasonRepository.getSeasonsByProducer(code).collect { seasons ->
                        if (isAdded && context != null) {
                            if (seasons.isEmpty()) {
                                binding.seasonLayout.error = "No seasons found for this producer"
                            } else {
                                binding.seasonLayout.error = null
                                updateSeasonDropdown(seasons)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SeasonDebug", "Error loading seasons", e)
                    if (isAdded && context != null) {
                        Toast.makeText(
                            context,
                            "Error loading seasons: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }


    private fun updateFieldDropdown() {
        if (fieldsMap.isEmpty()) {
            binding.fieldLayout.error = "No fields found for this producer"
            binding.fieldDropdown.setText("")
            selectedFieldNumber = null
            return
        }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            fieldsMap.values.toList()
        )
        binding.fieldDropdown.setAdapter(adapter)

        binding.fieldDropdown.setOnItemClickListener { _, _, position, _ ->
            val keys = fieldsMap.keys.toList()
            if (position < keys.size) {
                selectedFieldNumber = keys[position]
                binding.fieldLayout.error = null
            } else {
                selectedFieldNumber = null
                binding.fieldLayout.error = "Error selecting field"
            }
        }
    }

    private fun updateSeasonDropdown(seasons: List<Season>) {
        val seasonNames = seasons.map { it.season_name }
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, seasonNames)
        binding.seasonDropdown.setAdapter(adapter)

        binding.seasonDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedSeason = seasons[position]
            selectedSeasonId = selectedSeason.id
            binding.seasonLayout.error = null
        }
    }

    // UI Setup Methods
    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.US)

                binding.dateInput.tag = apiFormat.format(calendar.time)
                binding.dateInput.setText(displayFormat.format(calendar.time))
            }

        binding.dateInput.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = System.currentTimeMillis()
                show()
            }
        }
    }

    private fun setupForecastQualityDropdown() {
        val forecastQualities = listOf("GOOD", "AVERAGE", "POOR")
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, forecastQualities)
        binding.forecastQualityDropdown.setAdapter(adapter)
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateInputs()) {
                submitYieldForecast()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        with(binding) {
            if (dateInput.text.isNullOrEmpty()) {
                dateLayout.error = "Please select a date"
                isValid = false
            }
            if (selectedProducerCode == null) {
                producerLayout.error = "Please select a producer"
                isValid = false
            }
            if (selectedSeasonId == null) {
                seasonLayout.error = "Please select a season"
                isValid = false
            }
            if (selectedFieldNumber == null) {
                fieldLayout.error = "Please select a field"
                isValid = false
            }
            if (currentCropPopulationInput.text.isNullOrEmpty()) {
                currentCropPopulationLayout.error = "Please enter current crop population"
                isValid = false
            }
            if (targetYieldInput.text.isNullOrEmpty()) {
                targetYieldLayout.error = "Please enter target yield"
                isValid = false
            }
            if (yieldForecastInput.text.isNullOrEmpty()) {
                yieldForecastLayout.error = "Please enter yield forecast"
                isValid = false
            }
            if (forecastQualityDropdown.text.isNullOrEmpty()) {
                forecastQualityLayout.error = "Please select forecast quality"
                isValid = false
            }
        }

        return isValid
    }

    private fun submitYieldForecast() {
        try {
            if (!validateInputs()) return

            val forecast = YieldForecast(
                seasonPlanningId = selectedSeasonId!!,
                producer = formatProducerCode(selectedProducerCode!!),
                season = binding.seasonDropdown.text.toString(),
                field = formatFieldNumber(selectedFieldNumber!!),
                date = binding.dateInput.tag as String,
                currentCropPopulationPc = binding.currentCropPopulationInput.text.toString()
                    .toInt(),
                targetYield = binding.targetYieldInput.text.toString().toInt(),
                yieldForecastPc = binding.yieldForecastInput.text.toString().toInt(),
                forecastQuality = binding.forecastQualityDropdown.text.toString(),
                taComments = binding.taCommentsInput.text.toString()
            )

            val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
            viewModel.submitYieldForecast(forecast, isOnline)

        } catch (e: Exception) {
            Toast.makeText(context, "Error submitting forecast: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Unsynced Data")
            .setMessage("Are you sure you want to clear all unsynced forecasts? This cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                viewModel.clearUnsyncedData()
                Toast.makeText(context, "Unsynced data cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearFields() {
        with(binding) {
            dateInput.text = null
            producerDropdown.text = null
            seasonDropdown.text = null
            fieldDropdown.text = null
            currentCropPopulationInput.text = null
            targetYieldInput.text = null
            yieldForecastInput.text = null
            forecastQualityDropdown.text = null
            taCommentsInput.text = null

            // Clear errors
            dateLayout.error = null
            producerLayout.error = null
            seasonLayout.error = null
            fieldLayout.error = null
            currentCropPopulationLayout.error = null
            targetYieldLayout.error = null
            yieldForecastLayout.error = null
            forecastQualityLayout.error = null
        }

        // Reset selection states
        selectedProducerCode = null
        selectedSeasonId = null
        selectedFieldNumber = null
    }

    // Helper functions
    private fun formatProducerCode(code: String): String {
        return if (!code.startsWith("Producer ")) "Producer $code" else code
    }

    private fun formatFieldNumber(number: String): String {
        return if (!number.startsWith("Field ")) "Field $number" else number
    }


    // Menu related methods
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.forecast_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_unsynced -> {
                showClearDataDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        producersJob?.cancel()
        fieldsJob?.cancel()
    }


}










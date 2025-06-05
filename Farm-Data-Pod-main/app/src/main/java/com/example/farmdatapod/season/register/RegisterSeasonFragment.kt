package com.example.farmdatapod.season.register

import android.app.DatePickerDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentRegisterSeasonBinding
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.season.register.registerSeasonData.Season
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class RegisterSeasonFragment : Fragment() {
    private val TAG = "RegisterSeasonFragment"  // Add this tag
    private var _binding: FragmentRegisterSeasonBinding? = null
    private val binding get() = _binding!!
    private lateinit var seasonRepository: SeasonRepository
    private lateinit var producerRepository: ProducerRepository  // Add this
    private var selectedProducerId: Int? = null
    private var selectedProducerCode: String? = null  // Add this
    private var producersJob: Job? = null  // Add this


    // Date formatters
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Initializing RegisterSeasonFragment")
        _binding = FragmentRegisterSeasonBinding.inflate(inflater, container, false)
        producerRepository = ProducerRepository(requireContext())  // Add this
        seasonRepository = SeasonRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackButton()
        setupProducerDropdown()
        setupDateInputs()
        setupSubmitButton()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupProducerDropdown() {
        producersJob?.cancel()
        producersJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                producerRepository.getAllProducers()
                    .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { producers ->
                        if (isAdded && context != null) {
                            val producerNames = producers.map { "${it.otherName} ${it.lastName} (${it.farmerCode})" }
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
                    Log.e(TAG, "Error loading producers", e)
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Error loading producers: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun handleProducerSelection(selectedProducer: ProducerEntity) {
        selectedProducerId = selectedProducer.id
        selectedProducerCode = selectedProducer.farmerCode
        binding.producerInputLayout.error = null
    }

    private fun setupDateInputs() {
        // Keep track of selected planting date
        var selectedPlantingDate: Calendar? = null

        binding.plantingDateInput.setOnClickListener {
            Log.d(TAG, "Opening planting date picker")
            showPlantingDatePicker { selectedDate ->
                binding.plantingDateInput.setText(selectedDate)
                binding.plantingDateInputLayout.error = null
                Log.d(TAG, "Planting date selected: $selectedDate")
                // Store selected date for harvest date validation
                selectedPlantingDate = displayDateFormat.parse(selectedDate)?.let { date ->
                    Calendar.getInstance().apply { time = date }
                }
                // Clear harvest date when planting date changes
                binding.harvestDateInput.setText("")
            }
        }

        binding.harvestDateInput.setOnClickListener {
            if (selectedPlantingDate == null) {
                Toast.makeText(context, "Please select planting date first", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            Log.d(TAG, "Opening harvest date picker")
            showHarvestDatePicker(selectedPlantingDate!!) { selectedDate ->
                binding.harvestDateInput.setText(selectedDate)
                binding.harvestDateInputLayout.error = null
                Log.d(TAG, "Harvest date selected: $selectedDate")
            }
        }
    }

    private fun showPlantingDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                val displayDate = displayDateFormat.format(calendar.time)
                onDateSelected(displayDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Set minimum date to today
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    private fun showHarvestDatePicker(plantingDate: Calendar, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                val displayDate = displayDateFormat.format(calendar.time)
                onDateSelected(displayDate)
            },
            plantingDate.get(Calendar.YEAR),
            plantingDate.get(Calendar.MONTH),
            plantingDate.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Set minimum date to day after planting
            plantingDate.add(Calendar.DAY_OF_MONTH, 1)
            datePicker.minDate = plantingDate.timeInMillis
        }.show()
    }



    private fun formatDateForApi(displayDate: String): String {
        return try {
            val date = displayDateFormat.parse(displayDate)
            date?.let {
                val calendar = Calendar.getInstance()
                calendar.time = it
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                apiDateFormat.format(calendar.time)
            } ?: displayDate.also {
                Log.e(TAG, "Failed to parse date: $displayDate")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $displayDate", e)
            displayDate
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateInputs()) {
                submitForm()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        Log.d(TAG, "Validating form inputs")

        if (binding.producerDropdown.text.isNullOrEmpty()) {
            binding.producerInputLayout.error = "Please select a producer"
            isValid = false
            Log.d(TAG, "Validation failed: No producer selected")
        }

        if (binding.seasonNameInput.text.isNullOrEmpty()) {
            binding.seasonNameInputLayout.error = "Please enter a season name"
            isValid = false
            Log.d(TAG, "Validation failed: No season name")
        }

        if (binding.plantingDateInput.text.isNullOrEmpty()) {
            binding.plantingDateInputLayout.error = "Please select a planting date"
            isValid = false
            Log.d(TAG, "Validation failed: No planting date")
        }

        if (binding.harvestDateInput.text.isNullOrEmpty()) {
            binding.harvestDateInputLayout.error = "Please select a harvest date"
            isValid = false
            Log.d(TAG, "Validation failed: No harvest date")
        }

        // Only validate date order if both dates are present
        if (!binding.harvestDateInput.text.isNullOrEmpty() &&
            !binding.plantingDateInput.text.isNullOrEmpty()
        ) {
            val plantingDate = displayDateFormat.parse(binding.plantingDateInput.text.toString())
            val harvestDate = displayDateFormat.parse(binding.harvestDateInput.text.toString())

            if (plantingDate != null && harvestDate != null && !harvestDate.after(plantingDate)) {
                binding.harvestDateInputLayout.error = "Harvest date must be after planting date"
                isValid = false
                Log.d(TAG, "Validation failed: Invalid harvest date")
            }
        }

        Log.d(TAG, "Form validation result: $isValid")
        return isValid
    }

    private fun submitForm() {
        Log.d(TAG, "Starting form submission")

        val seasonName = binding.seasonNameInput.text.toString()
        val plantingDateDisplay = binding.plantingDateInput.text.toString()
        val harvestDateDisplay = binding.harvestDateInput.text.toString()
        val producerName = binding.producerDropdown.text.toString()
        val comments = binding.commentsInput.text?.toString() ?: ""

        Log.d(
            TAG, """
            Form Data:
            - Season Name: $seasonName
            - Producer: $producerName
            - Planting Date: $plantingDateDisplay
            - Harvest Date: $harvestDateDisplay
            - Comments: $comments
        """.trimIndent()
        )

        val plantingDateApi = formatDateForApi(plantingDateDisplay)
        val harvestDateApi = formatDateForApi(harvestDateDisplay)

        val season = Season(
            producer = producerName,
            season_name = seasonName,
            planned_date_of_planting = plantingDateApi,
            planned_date_of_harvest = harvestDateApi,
            comments = comments,
            syncStatus = false
        )

        lifecycleScope.launch {
            try {
                val isOnline = isNetworkAvailable()
                Log.d(TAG, "Network status: ${if (isOnline) "Online" else "Offline"}")

                val result = seasonRepository.saveSeason(season, isOnline)

                result.onSuccess {
                    val message = if (isOnline) {
                        "Season registered and synced successfully"
                    } else {
                        "Season saved offline. Will sync when online."
                    }
                    Log.d(TAG, "Save successful: $message")
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    clearForm()
                }.onFailure { exception ->
                    Log.e(TAG, "Save failed", exception)
                    Toast.makeText(
                        context,
                        "Error: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during form submission", e)
                Toast.makeText(
                    context,
                    "Error saving season: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return NetworkUtils.isNetworkAvailable(requireContext())
    }

    private fun clearForm() {
        Log.d(TAG, "Clearing form fields")
        binding.apply {
            producerDropdown.setText("")
            seasonNameInput.setText("")
            plantingDateInput.setText("")
            harvestDateInput.setText("")
            commentsInput.setText("")

            producerInputLayout.error = null
            seasonNameInputLayout.error = null
            plantingDateInputLayout.error = null
            harvestDateInputLayout.error = null
        }

        selectedProducerId = null
        Log.d(TAG, "Form cleared successfully")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        producersJob?.cancel()
        _binding = null
    }
}
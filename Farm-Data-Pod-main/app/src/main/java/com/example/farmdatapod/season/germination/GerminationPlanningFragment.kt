package com.example.farmdatapod.season.germination

import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.season.register.registerSeasonData.Season
import kotlin.coroutines.cancellation.CancellationException
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentGerminationPlanningBinding
import com.example.farmdatapod.season.germination.data.GerminationEntity
import com.example.farmdatapod.season.germination.data.GerminationRepository
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GerminationPlanningFragment : Fragment() {
    private var _binding: FragmentGerminationPlanningBinding? = null
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var germinationRepository: GerminationRepository
    private var selectedProducerId: Int? = null  // Add this line


    private val binding get() = _binding!!

    // Selection tracking variables
    private var selectedProducerCode: String? = null
    private var selectedSeasonId: Long? = null
    private var selectedFieldNumber: String? = null
    private var startDate: String? = null
    private var endDate: String? = null
    private var producersJob: Job? = null
    private var fieldsJob: Job? = null
    private lateinit var producerRepository: ProducerRepository
    private lateinit var fieldRegistrationRepository: FieldRegistrationRepository

    // Data source variables
    private lateinit var seasonRepository: SeasonRepository
    private var fieldsMap: MutableMap<String, String> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGerminationPlanningBinding.inflate(inflater, container, false)
        seasonRepository = SeasonRepository(requireContext())
        producerRepository = ProducerRepository(requireContext())
        fieldRegistrationRepository = FieldRegistrationRepository(requireContext())
        germinationRepository = GerminationRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }


    private fun setupUI() {
        setupToolbar()
        setupDatePicker()
        loadProducers()
        setupSubmitButton()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateInputs()) {
                submitGerminationData()
            }
        }
    }


    // this is the date picker function
    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val displayDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.US)

        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val apiDate = formatDateToISOString(calendar.time)
            val displayDate = displayDateFormat.format(calendar.time)

            when (binding.dateTypeGroup.checkedRadioButtonId) {
                R.id.singleDateRadio -> handleSingleDateSelection(apiDate, displayDate)
                R.id.dateRangeRadio -> {
                    if (startDate == null) {
                        startDate = apiDate
                        showDatePickerDialog(
                            calendar,
                            dateSetListener,
                            "Select End Date",
                            formatISOToMillis(apiDate)
                        )
                        updateDateRangeDisplay(displayDate, null)
                    } else {
                        endDate = apiDate
                        updateDateRangeDisplay(
                            displayDateFormat.format(Date(formatISOToMillis(startDate!!))),
                            displayDate
                        )
                    }
                }
            }
        }


        // Set up radio button listeners
        binding.apply {
            singleDateRadio.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    resetDateSelection()
                    showDatePickerDialog(calendar, dateSetListener, "Select Date")
                }
            }

            dateRangeRadio.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    resetDateSelection()
                    showDatePickerDialog(calendar, dateSetListener, "Select Start Date")
                }
            }
        }
    }


    private fun showDatePickerDialog(
        calendar: Calendar,
        dateSetListener: DatePickerDialog.OnDateSetListener,
        title: String,
        minDate: Long? = null
    ) {
        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle(title)
            datePicker.minDate = minDate ?: System.currentTimeMillis()
            show()
        }
    }

    private fun formatDateToISOString(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd'T'00:00:00", Locale.US).format(date)
    }

    private fun formatISOToMillis(isoDate: String): Long {
        return SimpleDateFormat("yyyy-MM-dd'T'00:00:00", Locale.US).parse(isoDate)?.time
            ?: System.currentTimeMillis()
    }

    private fun handleSingleDateSelection(apiDate: String, displayDate: String) {
        startDate = apiDate
        endDate = null
        binding.dateTypeGroup.tag = apiDate
        updateSingleDateDisplay(displayDate)
    }


    private fun resetDateSelection() {
        startDate = null
        endDate = null
        binding.apply {
            root.findViewWithTag<TextView>("selectedDateText")?.visibility = View.GONE
            root.findViewWithTag<TextView>("dateRangeText")?.visibility = View.GONE
        }
    }

    private fun updateSingleDateDisplay(displayDate: String) {
        binding.apply {
            root.findViewWithTag<TextView>("dateRangeText")?.visibility = View.GONE

            if (root.findViewWithTag<TextView>("selectedDateText") == null) {
                val dateText = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (8 * resources.displayMetrics.density).toInt()
                    }
                    tag = "selectedDateText"
                }
                (dateTypeGroup.parent as ViewGroup).addView(
                    dateText,
                    dateTypeGroup.indexOfChild(dateTypeGroup) + 1
                )
            }
            root.findViewWithTag<TextView>("selectedDateText")?.apply {
                text = "Selected Date: $displayDate"
                visibility = View.VISIBLE
            }
        }
    }

    private fun updateDateRangeDisplay(startDisplay: String, endDisplay: String?) {
        binding.apply {
            root.findViewWithTag<TextView>("selectedDateText")?.visibility = View.GONE

            if (root.findViewWithTag<TextView>("dateRangeText") == null) {
                val dateText = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (8 * resources.displayMetrics.density).toInt()
                    }
                    tag = "dateRangeText"
                }
                (dateTypeGroup.parent as ViewGroup).addView(
                    dateText,
                    dateTypeGroup.indexOfChild(dateTypeGroup) + 1
                )
            }

            root.findViewWithTag<TextView>("dateRangeText")?.apply {
                text = if (endDisplay != null) {
                    "Date Range: $startDisplay to $endDisplay"
                } else {
                    "Start Date: $startDisplay\nPlease select end date"
                }
                visibility = View.VISIBLE
            }
        }
    }

    // Producer Loading Logic
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
                    Log.e(TAG, "Error loading producers", e)
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

    // Field Loading Logic
    private fun loadFieldsForProducer(producerCode: String) {
        fieldsJob?.cancel()
        fieldsJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                fieldRegistrationRepository
                    .getFieldRegistrationsByProducer(producerCode)
                    .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { fieldRegistrations ->
                        if (isAdded && context != null) {
                            fieldsMap = fieldRegistrations.associate {
                                it.fieldRegistration.fieldNumber.toString() to "Field ${it.fieldRegistration.fieldNumber}"
                            }.toMutableMap()
                            updateFieldDropdown()
                        }
                    }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e(TAG, "Error loading fields", e)
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

    // Season Loading Logic
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
                    Log.e(TAG, "Error loading seasons", e)
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

    private fun updateSeasonDropdown(seasons: List<Season>) {
        val seasonNames = seasons.map { it.season_name }
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, seasonNames)
        binding.seasonDropdown.setAdapter(adapter)

        binding.seasonDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedSeason = seasons[position]
            selectedSeasonId = selectedSeason.id.toLong()
            binding.seasonLayout.error = null
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate date selection
        when (binding.dateTypeGroup.checkedRadioButtonId) {
            R.id.singleDateRadio -> {
                if (startDate == null) {
                    Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                    isValid = false
                }
            }

            R.id.dateRangeRadio -> {
                if (startDate == null || endDate == null) {
                    Toast.makeText(
                        context,
                        "Please select both start and end dates",
                        Toast.LENGTH_SHORT
                    ).show()
                    isValid = false
                }
            }

            -1 -> {
                Toast.makeText(context, "Please select a date type", Toast.LENGTH_SHORT).show()
                isValid = false
            }
        }

        // Validate producer selection
        if (selectedProducerCode == null) {
            binding.producerLayout.error = "Please select a producer"
            isValid = false
        }

        // Validate season selection
        if (selectedSeasonId == null) {
            binding.seasonLayout.error = "Please select a season"
            isValid = false
        }

        // Validate field selection
        if (selectedFieldNumber == null) {
            binding.fieldLayout.error = "Please select a field"
            isValid = false
        }

        // Validate crop selection
        if (binding.cropInput.text.isNullOrEmpty()) {
            binding.cropLayout.error = "Please select a crop"
            isValid = false
        }

        // Validate total crop population
        if (binding.totalCropPopulationInput.text.isNullOrEmpty()) {
            binding.totalCropPopulationLayout.error = "Please enter total crop population"
            isValid = false
        }

        // Validate germination percentage
        binding.germinationPercentageInput.text.toString().toFloatOrNull()?.let { percentage ->
            if (percentage < 0 || percentage > 100) {
                binding.germinationPercentageLayout.error = "Percentage must be between 0 and 100"
                isValid = false
            }
        } ?: run {
            binding.germinationPercentageLayout.error =
                "Please enter a valid germination percentage"
            isValid = false
        }

        // Validate status of crop
        if (binding.statusOfCropInput.text.isNullOrEmpty()) {
            binding.statusOfCropLayout.error = "Please enter status of crop"
            isValid = false
        }

        // Validate recommended management
        if (binding.recommendedManagementInput.text.isNullOrEmpty()) {
            binding.recommendedManagementLayout.error = "Please enter recommended management"
            isValid = false
        }

        // Validate labor man days
        if (binding.laborManDaysInput.text.isNullOrEmpty()) {
            binding.laborManDaysLayout.error = "Please enter labor man days"
            isValid = false
        }

        // Validate unit cost
        if (binding.unitCostInput.text.isNullOrEmpty()) {
            binding.unitCostLayout.error = "Please enter unit cost"
            isValid = false
        }

        return isValid
    }




    private fun submitGerminationData() {
        Log.d(TAG, "Starting germination data submission")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.submitButton.isEnabled = false
                val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
                Log.d(TAG, "Network status: ${if (isOnline) "Online" else "Offline"}")

                val germinationData = createGerminationEntity()
                val result = germinationRepository.saveGermination(germinationData, isOnline)

                result.onSuccess {
                    val message = if (isOnline) {
                        "Germination data registered and synced successfully"
                    } else {
                        "Germination data saved offline. Will sync when online."
                    }
                    Log.d(TAG, "Save successful: $message")
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    clearFields()
                    findNavController().navigateUp()
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
                    "Error saving germination data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.submitButton.isEnabled = true
            }
        }
    }

    private fun createGerminationEntity(): GerminationEntity {
        val dateToUse = when (binding.dateTypeGroup.checkedRadioButtonId) {
            R.id.singleDateRadio -> startDate
            R.id.dateRangeRadio -> "$startDate to $endDate"
            else -> formatDateToISOString(Calendar.getInstance().time)
        }

        return GerminationEntity(
            crop = binding.cropInput.text.toString(),
            dateOfGermination = dateToUse ?: "",
            field = selectedFieldNumber ?: "",
            germinationPercentage = binding.germinationPercentageInput.text.toString().toIntOrNull()
                ?: 0,
            laborManDays = binding.laborManDaysInput.text.toString().toIntOrNull() ?: 0,
            producer = selectedProducerCode ?: "",
            recommendedManagement = binding.recommendedManagementInput.text.toString(),
            season = selectedSeasonId?.toString() ?: "",
            seasonPlanningId = selectedSeasonId?.toInt() ?: 0,
            statusOfCrop = binding.statusOfCropInput.text.toString(),
            totalCropPopulation = binding.totalCropPopulationInput.text.toString(),
            unitCostOfLabor = binding.unitCostInput.text.toString().toDoubleOrNull() ?: 0.0,
            isSynced = false
        )
    }

    private fun clearFields() {
        binding.apply {
            // Clear all form fields
            cropInput.setText("")
            producerDropdown.setText("")
            seasonDropdown.setText("")
            fieldDropdown.setText("")
            totalCropPopulationInput.setText("")
            germinationPercentageInput.setText("")
            statusOfCropInput.setText("")
            recommendedManagementInput.setText("")
            laborManDaysInput.setText("")
            unitCostInput.setText("")
            dateTypeGroup.clearCheck()

            // Reset date selections
            resetDateSelection()

            // Reset all variables
            selectedProducerCode = null
            selectedSeasonId = null
            selectedFieldNumber = null
            startDate = null
            endDate = null

            // Clear all errors
            cropLayout.error = null
            producerLayout.error = null
            seasonLayout.error = null
            fieldLayout.error = null
            totalCropPopulationLayout.error = null
            germinationPercentageLayout.error = null
            statusOfCropLayout.error = null
            recommendedManagementLayout.error = null
            laborManDaysLayout.error = null
            unitCostLayout.error = null

            // Disable dependent dropdowns
            seasonDropdown.isEnabled = false
            fieldDropdown.isEnabled = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        producersJob?.cancel()
        fieldsJob?.cancel()
        _binding = null
    }
}
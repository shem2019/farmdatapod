package com.example.farmdatapod.cropmanagement.cropProtection

import android.R
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.adapter.ApplicantsAdapter
import com.example.farmdatapod.cropmanagement.cropProtection.data.CropProtectionManagementViewModel
import com.example.farmdatapod.databinding.FragmentCropProtectionBinding

import com.example.farmdatapod.databinding.FragmentCropProtectionManagementBinding
import com.example.farmdatapod.models.NameOfApplicants
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.season.cropProtection.data.CropProtectionEntity
import com.example.farmdatapod.season.cropProtection.data.CropProtectionViewModel
import com.example.farmdatapod.season.register.registerSeasonData.Season
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.example.farmdatapod.utils.DialogUtils
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class CropProtectionManagementFragment : Fragment() {

    private var _binding: FragmentCropProtectionManagementBinding? = null
    private val binding get() = _binding!!

        private lateinit var applicantsAdapter: ApplicantsAdapter
        private val applicantsList = mutableListOf<NameOfApplicants>()
        private val viewModel: CropProtectionManagementViewModel by viewModels()

        private var selectedProducerCode: String? = null
        private var selectedSeasonId: Long? = null
        private var selectedFieldNumber: String? = null
        private var startDate: String? = null
        private var endDate: String? = null
        private var producersJob: Job? = null
        private var fieldsJob: Job? = null
        private lateinit var producerRepository: ProducerRepository
        private lateinit var fieldRegistrationRepository: FieldRegistrationRepository
        private lateinit var loadingDialog: AlertDialog


        // Data source variables
        private lateinit var seasonRepository: SeasonRepository
        private var fieldsMap: MutableMap<String, String> = mutableMapOf()
        private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
        private var selectedProducerId: Int? = null  // Add this line

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentCropProtectionManagementBinding.inflate(inflater, container, false)
            seasonRepository = SeasonRepository(requireContext())
            producerRepository = ProducerRepository(requireContext())
            loadingDialog = DialogUtils(requireContext()).buildLoadingDialog()
            fieldRegistrationRepository = FieldRegistrationRepository(requireContext())
            return binding.root
        }
        private val weatherConditions = listOf(
            "Sunny", "Cloudy", "Rainy", "Windy", "Snowy",
            "Humid", "Dry", "Foggy", "Stormy", "Hazy"
        )

        private val formulations = listOf(
            "Granules (G)",
            "Wettable Powders (WP)",
            "Emulsifiable Concentrates (EC)",
            "Soluble Powders (SP)",
            "Liquid Concentrates (LC)",
            "Dusts (D)",
            "Suspension Concentrates (SC)",
            "Water Dispersible Granules (WDG)",
            "Ultra-Low Volume (ULV)",
            "Tablets (TB)"
        )

        private val units = listOf(
            "Kilograms (kg)",
            "Grams (g)",
            "Liters (L)",
            "Milliliters (mL)",
            "Tons (t)",
            "Pounds (lbs)",
            "Ounces (oz)",
            "Cubic meters (m³)",
            "Teaspoons (tsp)",
            "Tablespoons (tbsp)"
        )

        private fun setupDropdowns() {
            // Weather Conditions Dropdown
            val weatherAdapter = ArrayAdapter(
                requireContext(),
                R.layout.simple_dropdown_item_1line,
                weatherConditions
            )
            binding.weatherDropdown.setAdapter(weatherAdapter)

            // Formulation Dropdown
            val formulationAdapter = ArrayAdapter(
                requireContext(),
                R.layout.simple_dropdown_item_1line,
                formulations
            )
            binding.formulationDropdown.setAdapter(formulationAdapter)

            // Units Dropdown
            val unitsAdapter = ArrayAdapter(
                requireContext(),
                R.layout.simple_dropdown_item_1line,
                units
            )
            binding.unitDropdown.setAdapter(unitsAdapter)
        }
        private fun setupTimePicker() {
            binding.timeInput.setOnClickListener {
                val calendar = Calendar.getInstance()
                val timePickerDialog = TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        binding.timeInput.setText(timeFormat.format(calendar.time))
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true // 24-hour format
                )
                timePickerDialog.show()
            }
        }
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            setupToolbar()
            setupDatePicker()
            loadProducers()
            setupRecyclerView()
            setupDropdowns()
            setupTimePicker()
            setupClickListeners()
            observeViewModel()
        }
        // Keep only this version of observeViewModel()
        private fun observeViewModel() {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.savingState.collect { state ->
                    when (state) {
                        is CropProtectionManagementViewModel.SavingState.Idle -> {
                            loadingDialog.dismiss()
                        }
                        is CropProtectionManagementViewModel.SavingState.Saving -> {
                            loadingDialog.show()
                        }
                        is CropProtectionManagementViewModel.SavingState.Success -> {
                            loadingDialog.dismiss()
                            Toast.makeText(context, "Successfully saved", Toast.LENGTH_SHORT).show()
                            clearForm()
                        }
                        is CropProtectionManagementViewModel.SavingState.Error -> {
                            loadingDialog.dismiss()
                        }
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.errorState.collect { error ->
                    error?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }




        private fun setupToolbar() {
            binding.backButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }


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
                    com.example.farmdatapod.R.id.singleDateRadio -> handleSingleDateSelection(apiDate, displayDate)
                    com.example.farmdatapod.R.id.dateRangeRadio -> {
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
                                    com.example.farmdatapod.R.layout.dropdown_item,
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
                        Log.e(ContentValues.TAG, "Error loading producers", e)
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
                        Log.e(ContentValues.TAG, "Error loading fields", e)
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
                com.example.farmdatapod.R.layout.dropdown_item,
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
                        Log.e(ContentValues.TAG, "Error loading seasons", e)
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
            val adapter = ArrayAdapter(requireContext(), com.example.farmdatapod.R.layout.dropdown_item, seasonNames)
            binding.seasonDropdown.setAdapter(adapter)

            binding.seasonDropdown.setOnItemClickListener { _, _, position, _ ->
                val selectedSeason = seasons[position]
                selectedSeasonId = selectedSeason.id.toLong()
                binding.seasonLayout.error = null
            }
        }

        private fun setupRecyclerView() {
            applicantsAdapter = ApplicantsAdapter()
            binding.applicantsRecyclerView.apply {
                adapter = applicantsAdapter
                layoutManager = LinearLayoutManager(context)
            }
            addInitialForm()
        }

        // Add these missing functions
        private fun addInitialForm() {
            val initialApplicant = NameOfApplicants(
                name = "",
                ppes_used = "",
                equipment_used = ""
            )
            applicantsAdapter.addItem(initialApplicant)
        }

        private fun addNewApplicant() {
            val newApplicant = NameOfApplicants(
                name = "",
                ppes_used = "",
                equipment_used = ""
            )
            applicantsAdapter.addItem(newApplicant)
        }

        private fun setupClickListeners() {
            binding.addApplicantButton.setOnClickListener {
                addNewApplicant()
            }

            // Fixed double click listener
            binding.submitButton.setOnClickListener {
                if (validateInputs()) {
                    submitForm()
                }
            }

            binding.backButton.setOnClickListener {
                activity?.onBackPressed()
            }
        }


        private fun validateInputs(): Boolean {
            var isValid = true

            // Validate date selection
            if (binding.dateTypeGroup.checkedRadioButtonId == -1) {
                Toast.makeText(context, "Please select a date type", Toast.LENGTH_SHORT).show()
                isValid = false
            } else if (startDate == null) {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                isValid = false
            } else if (binding.dateRangeRadio.isChecked && endDate == null) {
                Toast.makeText(context, "Please select an end date", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            // Validate time
            if (binding.timeInput.text.toString().isEmpty()) {
                binding.timeLayout.error = "Please select time of application"
                isValid = false
            }

            // Validate dropdowns
            if (binding.weatherDropdown.text.toString().isEmpty()) {
                binding.weatherLayout.error = "Please select weather condition"
                isValid = false
            }

            if (binding.producerDropdown.text.toString().isEmpty()) {
                binding.producerLayout.error = "Please select a producer"
                isValid = false
            }

            if (binding.seasonDropdown.text.toString().isEmpty()) {
                binding.seasonLayout.error = "Please select a season"
                isValid = false
            }

            if (binding.fieldDropdown.text.toString().isEmpty()) {
                binding.fieldLayout.error = "Please select a field"
                isValid = false
            }

            if (binding.formulationDropdown.text.toString().isEmpty()) {
                binding.formulationLayout.error = "Please select formulation"
                isValid = false
            }

            if (binding.unitDropdown.text.toString().isEmpty()) {
                binding.unitLayout.error = "Please select unit"
                isValid = false
            }

            // Validate text inputs with specific requirements
            isValid =
                validateField(binding.productLayout, binding.productInput.text.toString()) && isValid

            isValid = validateField(
                binding.whoClassificationLayout,
                binding.whoClassificationInput.text.toString()
            ) && isValid

            // Validate numeric inputs
            isValid = validateNumericField(
                binding.numberOfUnitsLayout,
                binding.numberOfUnitsInput.text.toString(),
                "Please enter number of units",
                "Number of units must be greater than 0"
            ) && isValid

            isValid = validateNumericField(
                binding.costPerUnitLayout,
                binding.costPerUnitInput.text.toString(),
                "Please enter cost per unit",
                "Cost per unit must be greater than 0"
            ) && isValid

            isValid =
                validateField(binding.dosageLayout, binding.dosageInput.text.toString()) && isValid

            isValid = validateField(
                binding.mixingRatioLayout,
                binding.mixingRatioInput.text.toString()
            ) && isValid

            isValid = validateNumericField(
                binding.totalWaterLayout,
                binding.totalWaterInput.text.toString(),
                "Please enter total amount of water",
                "Total water must be greater than 0"
            ) && isValid

            isValid = validateNumericField(
                binding.laborManDaysLayout,
                binding.laborManDaysInput.text.toString(),
                "Please enter labor man days",
                "Labor man days must be greater than 0"
            ) && isValid

            isValid = validateNumericField(
                binding.unitCostOfLaborLayout,
                binding.unitCostOfLaborInput.text.toString(),
                "Please enter unit cost of labor",
                "Unit cost must be greater than 0"
            ) && isValid

            // Validate applicants


            return isValid
        }

        private fun validateField(layout: TextInputLayout, text: String): Boolean {
            return if (text.isEmpty()) {
                layout.error = "This field is required"
                false
            } else {
                layout.error = null
                true
            }
        }

        private fun validateNumericField(
            layout: TextInputLayout,
            text: String,
            emptyError: String,
            invalidError: String
        ): Boolean {
            return when {
                text.isEmpty() -> {
                    layout.error = emptyError
                    false
                }

                text.toDoubleOrNull() ?: 0.0 <= 0 -> {
                    layout.error = invalidError
                    false
                }

                else -> {
                    layout.error = null
                    true
                }
            }
        }



        private fun submitForm() {
            if (!validateInputs()) return

            try {
                val cropProtection = CropProtectionEntity(
                    timeOfApplication = binding.timeInput.text.toString(),
                    weatherCondition = binding.weatherDropdown.text.toString(),
                    producer = binding.producerDropdown.text.toString(),
                    season = binding.seasonDropdown.text.toString(),
                    field = binding.fieldDropdown.text.toString(),
                    product = binding.productInput.text.toString(),
                    whoClassification = binding.whoClassificationInput.text.toString(),
                    formulation = binding.formulationDropdown.text.toString(),
                    unit = binding.unitDropdown.text.toString(),
                    numberOfUnits = binding.numberOfUnitsInput.text.toString().toLong(),
                    costPerUnit = binding.costPerUnitInput.text.toString().toDouble(),
                    dosage = binding.dosageInput.text.toString(),
                    mixingRatio = binding.mixingRatioInput.text.toString(),
                    totalWater = binding.totalWaterInput.text.toString().toDouble(),
                    laborManDays = binding.laborManDaysInput.text.toString().toDouble(),
                    unitCostOfLabor = binding.unitCostOfLaborInput.text.toString().toDouble(),
                    comments = binding.commentsInput.text.toString(),
                    date = startDate ?: "",
                    season_planning_id = selectedSeasonId ?: 0L
                )

                // Get the applicants directly from the adapter
                val applicants = applicantsAdapter.getItems()

                // Validate applicants
                if (applicants.isEmpty()) {
                    Toast.makeText(context, "Please add at least one applicant", Toast.LENGTH_SHORT).show()
                    return
                }

                if (applicants.any { it.name.isBlank() || it.ppes_used.isBlank() || it.equipment_used.isBlank() }) {
                    Toast.makeText(context, "Please fill in all applicant details", Toast.LENGTH_SHORT).show()
                    return
                }

                viewModel.saveCropProtection(cropProtection, applicants)
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }



        // Rest of your existing code remains the same




        private fun clearForm() {
            binding.apply {
                timeInput.setText("")
                weatherDropdown.setText("")
                producerDropdown.setText("")
                seasonDropdown.setText("")
                fieldDropdown.setText("")
                productInput.setText("")
                whoClassificationInput.setText("")
                formulationDropdown.setText("")
                unitDropdown.setText("")
                numberOfUnitsInput.setText("")
                costPerUnitInput.setText("")
                dosageInput.setText("")
                mixingRatioInput.setText("")
                totalWaterInput.setText("")
                laborManDaysInput.setText("")
//            binding.unitCostInput.setText("")
                commentsInput.setText("")

                // Clear errors
                timeLayout.error = null
                weatherLayout.error = null
                producerLayout.error = null
                seasonLayout.error = null
                fieldLayout.error = null
                productLayout.error = null
                whoClassificationLayout.error = null
                formulationLayout.error = null
                unitLayout.error = null
                numberOfUnitsLayout.error = null
                costPerUnitLayout.error = null
                dosageLayout.error = null
                mixingRatioLayout.error = null
                totalWaterLayout.error = null
                laborManDaysLayout.error = null
                unitCostOfLaborLayout.error = null

                // Reset date selection
                resetDateSelection()

                // Reset applicants
                applicantsAdapter.clearItems()
                addInitialForm()

                // Reset selection variables
                selectedProducerCode = null
                selectedSeasonId = null
                selectedFieldNumber = null
                selectedProducerId = null
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            viewModel.resetSavingState()
            fieldsJob?.cancel()
            producersJob?.cancel()
            _binding = null
        }
    }









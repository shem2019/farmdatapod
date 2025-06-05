package com.example.farmdatapod.cropmanagement.nutrition

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.ApplicantsAdapter
import com.example.farmdatapod.cropmanagement.nutrition.data.CropNutritionManagementViewModel
import com.example.farmdatapod.databinding.FragmentCropManagementNutritionBinding
import com.example.farmdatapod.models.NameOfApplicants
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.season.nutrition.data.ApplicantEntity
import com.example.farmdatapod.season.nutrition.data.CropNutritionEntity
import com.example.farmdatapod.season.register.registerSeasonData.Season
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.example.farmdatapod.utils.DialogUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class CropManagementNutritionFragment : Fragment() {

    private var _binding: FragmentCropManagementNutritionBinding? = null
    private val binding get() = _binding!!


        private lateinit var applicantsAdapter: ApplicantsAdapter
        private val applicantsList = mutableListOf<NameOfApplicants>()
        private var selectedProducerCode: String? = null
        private var selectedSeasonId: Long? = null
        private var selectedFieldNumber: String? = null
        private var startDate: String? = null
        private var endDate: String? = null
        private var producersJob: Job? = null
        private var fieldsJob: Job? = null
        private lateinit var producerRepository: ProducerRepository
        private lateinit var fieldRegistrationRepository: FieldRegistrationRepository
        private val viewModel: CropNutritionManagementViewModel by viewModels()
        private lateinit var loadingDialog: AlertDialog

        // Data source variables
        private lateinit var seasonRepository: SeasonRepository
        private var fieldsMap: MutableMap<String, String> = mutableMapOf()
        private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
        private var selectedProducerId: Int? = null  // Add this line

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentCropManagementNutritionBinding.inflate(inflater, container, false)
            seasonRepository = SeasonRepository(requireContext())
            producerRepository = ProducerRepository(requireContext())
            loadingDialog = DialogUtils(requireContext()).buildLoadingDialog()
            fieldRegistrationRepository = FieldRegistrationRepository(requireContext())
            return binding.root
        }
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            setupDatePicker()
            loadProducers()
            setupRecyclerView()
            setupDropdowns()
            setupTimePicker()
            setupClickListeners()
            observeViewModel()
        }
        private fun observeViewModel() {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.uiState.flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.STARTED
                ).collectLatest { state ->
                    when (state) {
                        is CropNutritionManagementViewModel.UiState.Loading -> {
                            loadingDialog.show()
                            binding.submitButton.isEnabled = false
                        }
                        is CropNutritionManagementViewModel.UiState.Success -> {
                            loadingDialog.dismiss()
                            binding.submitButton.isEnabled = true
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                            if (state.message.contains("saved successfully")) {
                                clearForm()
                            }
                        }
                        is CropNutritionManagementViewModel.UiState.Error -> {
                            loadingDialog.dismiss()
                            binding.submitButton.isEnabled = true
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                        CropNutritionManagementViewModel.UiState.Initial -> {
                            loadingDialog.dismiss()
                            binding.submitButton.isEnabled = true
                        }
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.cropNutritionList.flowWithLifecycle(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.STARTED
                ).collectLatest { nutritionList ->
                    // Handle the list if needed
                }
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
            val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, seasonNames)
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
                android.R.layout.simple_dropdown_item_1line,
                weatherConditions
            )
            binding.weatherDropdown.setAdapter(weatherAdapter)

            // Formulation Dropdown
            val formulationAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                formulations
            )
            binding.formulationDropdown.setAdapter(formulationAdapter)

            // Units Dropdown
            val unitsAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
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
        private fun validateInputs(): Boolean {
            var isValid = true

            if (binding.timeInput.text.isNullOrEmpty()) {
                binding.timeInput.error = "Time is required"
                isValid = false
            }

            if (binding.weatherDropdown.text.isNullOrEmpty()) {
                binding.weatherDropdown.error = "Weather condition is required"
                isValid = false
            }

            if (binding.producerDropdown.text.isNullOrEmpty()) {
                binding.producerDropdown.error = "Producer is required"
                isValid = false
            }

            if (binding.seasonDropdown.text.isNullOrEmpty()) {
                binding.seasonDropdown.error = "Season is required"
                isValid = false
            }

            if (binding.fieldDropdown.text.isNullOrEmpty()) {
                binding.fieldDropdown.error = "Field is required"
                isValid = false
            }

            if (binding.productInput.text.isNullOrEmpty()) {
                binding.productInput.error = "Product is required"
                isValid = false
            }
            if (startDate == null) {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (binding.categoryInput.text.isNullOrEmpty()) {
                binding.categoryInput.error = "Category is required"
                isValid = false
            }

            if (binding.formulationDropdown.text.isNullOrEmpty()) {
                binding.formulationDropdown.error = "Formulation is required"
                isValid = false
            }

            if (binding.unitDropdown.text.isNullOrEmpty()) {
                binding.unitDropdown.error = "Unit is required"
                isValid = false
            }

            if (binding.numberOfUnitsInput.text.isNullOrEmpty()) {
                binding.numberOfUnitsInput.error = "Number of units is required"
                isValid = false
            }

            if (binding.costPerUnitInput.text.isNullOrEmpty()) {
                binding.costPerUnitInput.error = "Cost per unit is required"
                isValid = false
            }

            if (binding.dosageInput.text.isNullOrEmpty()) {
                binding.dosageInput.error = "Dosage is required"
                isValid = false
            }

            if (binding.mixingRatioInput.text.isNullOrEmpty()) {
                binding.mixingRatioInput.error = "Mixing ratio is required"
                isValid = false
            }

            if (binding.totalWaterInput.text.isNullOrEmpty()) {
                binding.totalWaterInput.error = "Total amount of water is required"
                isValid = false
            }

            if (binding.laborManDaysInput.text.isNullOrEmpty()) {
                binding.laborManDaysInput.error = "Labor man days is required"
                isValid = false
            }

            if (binding.unitCostInput.text.isNullOrEmpty()) {
                binding.unitCostInput.error = "Unit cost of labor is required"
                isValid = false
            }

            return isValid
        }

        private fun submitForm() {
            try {
                // Verify we have a valid season ID
                if (selectedSeasonId == null) {
                    binding.seasonLayout.error = "Please select a valid season"
                    return
                }

                // Get all form values
                val timeOfApplication = binding.timeInput.text.toString().trim()
                val weatherCondition = binding.weatherDropdown.text.toString().trim()
                val producer = binding.producerDropdown.text.toString().trim()
                val season = binding.seasonDropdown.text.toString().trim()
                val field = binding.fieldDropdown.text.toString().trim()
                val product = binding.productInput.text.toString().trim()
                val category = binding.categoryInput.text.toString().trim()
                val formulation = binding.formulationDropdown.text.toString().trim()
                val unit = binding.unitDropdown.text.toString().trim()
                val numberOfUnits = binding.numberOfUnitsInput.text.toString().trim().toLong()
                val costPerUnit = binding.costPerUnitInput.text.toString().trim().toDouble()
                val dosage = binding.dosageInput.text.toString().trim()
                val mixingRatio = binding.mixingRatioInput.text.toString().trim()
                val totalWater = binding.totalWaterInput.text.toString().trim().toDouble()
                val laborManDays = binding.laborManDaysInput.text.toString().trim().toDouble()
                val unitCostOfLabor = binding.unitCostInput.text.toString().trim().toDouble()
                val comments = binding.commentsInput.text.toString().trim()

                val date = when (binding.dateTypeGroup.checkedRadioButtonId) {
                    R.id.singleDateRadio -> startDate
                    R.id.dateRangeRadio -> if (endDate != null) "$startDate to $endDate" else startDate
                    else -> null
                } ?: throw IllegalStateException("Date must be selected")

                // Get first applicant's data
                val firstApplicant = applicantsAdapter.getItems().firstOrNull()
                if (firstApplicant == null ||
                    (firstApplicant.name.isEmpty() &&
                            firstApplicant.ppes_used.isEmpty() &&
                            firstApplicant.equipment_used.isEmpty())) {
                    Toast.makeText(context, "Please add applicant details", Toast.LENGTH_SHORT).show()
                    return
                }

                // Create CropNutritionEntity
                val cropNutritionEntity = CropNutritionEntity(
                    timeOfApplication = timeOfApplication,
                    weatherCondition = weatherCondition,
                    producer = producer,
                    season = season,
                    field = field,
                    product = product,
                    category = category,
                    formulation = formulation,
                    unit = unit,
                    numberOfUnits = numberOfUnits.toLong(),
                    costPerUnit = costPerUnit,
                    dosage = dosage,
                    mixingRatio = mixingRatio,
                    totalWater = totalWater,
                    laborManDays = laborManDays,
                    unitCostOfLabor = unitCostOfLabor,
                    comments = comments,
                    date = date,
                    season_planning_id = selectedSeasonId!!
                )

                // Create ApplicantEntity (will get cropNutritionId after insertion)
                val applicantEntity = ApplicantEntity(
                    cropNutritionId = 0L, // This will be set after nutrition is saved
                    name = firstApplicant.name,
                    ppesUsed = firstApplicant.ppes_used,
                    equipmentUsed = firstApplicant.equipment_used
                )

                // Save using the new ViewModel method
                viewModel.saveCropNutrition(cropNutritionEntity, listOf(applicantEntity))

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }





        private fun clearForm() {
            binding.apply {
                timeInput.setText("")
                weatherDropdown.setText("")
                producerDropdown.setText("")
                seasonDropdown.setText("")
                fieldDropdown.setText("")
                productInput.setText("")
                categoryInput.setText("")
                formulationDropdown.setText("")
                unitDropdown.setText("")
                numberOfUnitsInput.setText("")
                costPerUnitInput.setText("")
                dosageInput.setText("")
                mixingRatioInput.setText("")
                totalWaterInput.setText("")
                laborManDaysInput.setText("")
                unitCostInput.setText("")
                commentsInput.setText("")

                // Clear errors if any
                timeInput.error = null
                weatherDropdown.error = null
                producerDropdown.error = null
                seasonDropdown.error = null
                fieldDropdown.error = null
                productInput.error = null
                categoryInput.error = null
                formulationDropdown.error = null
                unitDropdown.error = null
                numberOfUnitsInput.error = null
                costPerUnitInput.error = null
                dosageInput.error = null
                mixingRatioInput.error = null
                totalWaterInput.error = null
                laborManDaysInput.error = null
                unitCostInput.error = null

                // Reset date selection
                resetDateSelection()

                // Clear applicants and add initial form
                applicantsAdapter.clearItems() // Use adapter's method instead of applicantsList
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
            if (loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }
            _binding = null
        }
    }




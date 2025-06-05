package com.example.farmdatapod.cropmanagement.harvesting

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.example.farmdatapod.R

import com.example.farmdatapod.adapter.BuyerAdapter
import com.example.farmdatapod.cropmanagement.harvesting.data.HarvestManagementRepository
import com.example.farmdatapod.cropmanagement.harvesting.data.HarvestManagementViewModel
import com.example.farmdatapod.databinding.FragmentCropManagementHarvestingBinding
import com.example.farmdatapod.databinding.FragmentHarvestPlanningBinding
import com.example.farmdatapod.models.Buyer
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.season.harvest.data.HarvestPlanning
import com.example.farmdatapod.season.harvest.data.HarvestPlanningBuyer
import com.example.farmdatapod.season.harvest.data.HarvestPlanningRepository
import com.example.farmdatapod.season.harvest.data.HarvestPlanningViewModel
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

class CropManagementHarvestingFragment : Fragment() {

    private var _binding: FragmentCropManagementHarvestingBinding? = null
    private val binding get() = _binding!!

        private lateinit var buyersAdapter: BuyerAdapter
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val TAG = "HarvestPlanningFragment"
        private var selectedStartTime: Calendar? = null


    private val viewModel: HarvestManagementViewModel by viewModels {
        HarvestManagementViewModel.provideFactory(requireActivity().application, HarvestManagementRepository(requireContext()))
    }

        // State variables
        private var selectedProducerCode: String? = null
        private var selectedSeasonId: Long? = null
        private var selectedFieldNumber: String? = null
        private var startDate: String? = null
        private var endDate: String? = null
        private var producersJob: Job? = null
        private var fieldsJob: Job? = null
        private var selectedProducerId: Int? = null
        private var fieldsMap: MutableMap<String, String> = mutableMapOf()
        private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener

        // Repository instances
        private lateinit var producerRepository: ProducerRepository
        private lateinit var fieldRegistrationRepository: FieldRegistrationRepository
        private lateinit var seasonRepository: SeasonRepository
        private lateinit var loadingDialog: AlertDialog


        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentCropManagementHarvestingBinding.inflate(inflater, container, false)
            initializeRepositories()
            return binding.root
        }

        private fun initializeRepositories() {
            seasonRepository = SeasonRepository(requireContext())
            producerRepository = ProducerRepository(requireContext())
            loadingDialog = DialogUtils(requireContext()).buildLoadingDialog()
            fieldRegistrationRepository = FieldRegistrationRepository(requireContext())
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            setupViews()
            loadProducers()
        }

        private fun setupViews() {
            setupToolbar()
            setupDatePicker()
            setupTimeSelectors()
            setupDropdowns()
            setupBuyersSection()
            setupClickListeners()
            setupObservers()
        }

        private fun setupToolbar() {
            binding.backButton.setOnClickListener {
                findNavController().navigateUp()
            }
            binding.toolbar.title = "Harvest Planning"
        }

        private fun setupClickListeners() {
            binding.addBuyerButton.setOnClickListener {
                addNewBuyerForm()
                binding.buyersRecyclerView.post {
                    buyersAdapter.itemCount.let { count ->
                        if (count > 0) {
                            binding.buyersRecyclerView.smoothScrollToPosition(count - 1)
                        }
                    }
                }
            }

            binding.submitButton.setOnClickListener {
                if (validateInputs()) {
                    submitForm()
                }
            }
        }
        private fun setupTimeSelectors() {
            binding.startTimeInput.setOnClickListener {
                showStartTimePicker()
            }
            binding.endTimeInput.setOnClickListener {
                if (selectedStartTime == null) {
                    Toast.makeText(context, "Please select start time first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                showEndTimePicker()
            }
        }

        private fun showStartTimePicker() {
            val calendar = Calendar.getInstance()

            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)

                    selectedStartTime = calendar.clone() as Calendar
                    val timeStr = timeFormatter.format(calendar.time)
                    binding.startTimeInput.setText(timeStr)
                    viewModel.updateFormField("startTime", timeStr)

                    // Clear end time when start time changes
                    binding.endTimeInput.setText("")
                    viewModel.updateFormField("endTime", "")
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true // 24 hour format
            ).show()
        }

        private fun showEndTimePicker() {
            val calendar = selectedStartTime?.clone() as Calendar

            // Add 2 hours to start time for minimum end time
            calendar.add(Calendar.HOUR_OF_DAY, 2)

            val endTimePicker = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val selectedTime = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }

                    // Validate that selected time is after minimum end time
                    if (selectedTime.before(calendar)) {
                        Toast.makeText(
                            context,
                            "End time must be at least 2 hours after start time",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@TimePickerDialog
                    }

                    val timeStr = timeFormatter.format(selectedTime.time)
                    binding.endTimeInput.setText(timeStr)
                    viewModel.updateFormField("endTime", timeStr)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true // 24 hour format
            )

            // Set minimum time to 2 hours after start time
            endTimePicker.show()
        }

        private fun validateTimeRange(): Boolean {
            val startTime = binding.startTimeInput.text.toString()
            val endTime = binding.endTimeInput.text.toString()

            if (startTime.isEmpty()) {
                binding.startTimeLayout.error = "Start time is required"
                return false
            }
            if (endTime.isEmpty()) {
                binding.endTimeLayout.error = "End time is required"
                return false
            }

            val start = timeFormatter.parse(startTime)
            val end = timeFormatter.parse(endTime)

            if (start == null || end == null) return false

            // Create calendars for proper time comparison
            val startCalendar = Calendar.getInstance().apply {
                time = start
            }
            val endCalendar = Calendar.getInstance().apply {
                time = end
            }

            // Add 2 hours to start time for minimum end time
            val minEndCalendar = startCalendar.clone() as Calendar
            minEndCalendar.add(Calendar.HOUR_OF_DAY, 2)

            return if (endCalendar.before(minEndCalendar)) {
                binding.endTimeLayout.error = "End time must be at least 2 hours after start time"
                false
            } else {
                binding.startTimeLayout.error = null
                binding.endTimeLayout.error = null
                true
            }
        }


        private fun setupObservers() {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.savingState.collect { state ->
                    when (state) {
                        is HarvestManagementViewModel.SavingState.Idle -> {
                            loadingDialog.dismiss()
                        }
                        is HarvestManagementViewModel.SavingState.Saving -> {
                            loadingDialog.show()
                        }
                        is HarvestManagementViewModel.SavingState.Success -> {
                            loadingDialog.dismiss()
                            Toast.makeText(context, "Successfully saved", Toast.LENGTH_SHORT).show()
                            clearForm()
                            findNavController().navigateUp()
                        }
                        is HarvestManagementViewModel.SavingState.Error -> {
                            loadingDialog.dismiss()
                        }
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.errorState.collect { error ->
                    error?.let {
                        showError(it)
                        viewModel.clearError()
                    }
                }
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

            // Add these validations in validateInputs()
            isValid = validateRequiredSelection(
                selectedProducerId != null,
                binding.producerLayout,
                "Please select a producer"
            ) && isValid

            isValid = validateRequiredSelection(
                selectedFieldNumber != null,
                binding.fieldLayout,
                "Please select a field"
            ) && isValid

            isValid = validateRequiredSelection(
                selectedSeasonId != null,
                binding.seasonLayout,
                "Please select a season"
            ) && isValid

            // Required field validations
            isValid = validateField(
                binding.startTimeLayout,
                binding.startTimeInput.text.toString()
            ) && isValid
            isValid = validateField(
                binding.endTimeLayout,
                binding.endTimeInput.text.toString()
            ) && isValid

            // Time range validation
            if (binding.startTimeInput.text?.isNotEmpty() == true &&
                binding.endTimeInput.text?.isNotEmpty() == true
            ) {
                isValid = validateTimeRange() && isValid
            }

            // Dropdown validations
            isValid = validateField(
                binding.producerLayout,
                binding.producerDropdown.text.toString()
            ) && isValid
            isValid = validateField(
                binding.seasonLayout,
                binding.seasonDropdown.text.toString()
            ) && isValid
            isValid = validateField(
                binding.fieldLayout,
                binding.fieldDropdown.text.toString()
            ) && isValid
            isValid = validateField(
                binding.harvestedUnitsLayout,
                binding.harvestedUnitsDropdown.text.toString()
            ) && isValid
            isValid = validateField(
                binding.harvestedQualityLayout,
                binding.harvestedQualityDropdown.text.toString()
            ) && isValid

            // Numeric field validations
            isValid = validateNumericField(
                binding.numberOfUnitsLayout,
                binding.numberOfUnitsInput.text.toString()
            ) && isValid
            isValid = validateNumericField(
                binding.weightPerUnitLayout,
                binding.weightPerUnitInput.text.toString()
            ) && isValid
            isValid = validateNumericField(
                binding.pricePerUnitLayout,
                binding.pricePerUnitInput.text.toString()
            ) && isValid
            isValid = validateNumericField(
                binding.laborManDaysLayout,
                binding.laborManDaysInput.text.toString()
            ) && isValid
            isValid = validateNumericField(
                binding.unitCostOfLaborLayout,
                binding.unitCostOfLaborInput.text.toString()
            ) && isValid

            return isValid
        }

        // Rest of your existing helper functions remain the same
        // (setupDatePicker, setupTimeSelectors, loadProducers, etc.)



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



        private fun setupDropdowns() {
            // Harvested units dropdown
            val harvestedUnits = arrayOf("Kg", "Tons", "Bags")
            setupDropdown(binding.harvestedUnitsDropdown, harvestedUnits)
            binding.harvestedUnitsDropdown.setOnItemClickListener { _, _, position, _ ->
                val selectedUnit = harvestedUnits[position]
                viewModel.updateFormField("harvestedUnits", selectedUnit)
            }

            // Harvested quality dropdown
            val qualities = arrayOf("Good", "Average", "Poor")
            setupDropdown(binding.harvestedQualityDropdown, qualities)
            binding.harvestedQualityDropdown.setOnItemClickListener { _, _, position, _ ->
                val selectedQuality = qualities[position]
                viewModel.updateFormField("harvestedQuality", selectedQuality)
            }
        }

        private fun setupDropdown(autoCompleteTextView: AutoCompleteTextView, items: Array<String>) {
            val adapter = ArrayAdapter(
                requireContext(),
                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                items
            )
            autoCompleteTextView.setAdapter(adapter)
        }


        private fun handleSubmissionError(error: Exception) {
            Log.e(TAG, "Submission error", error)
            val errorMessage = when (error) {
                is IllegalStateException -> error.message ?: "Invalid form data"
                else -> "Error submitting form: ${error.message}"
            }
            showError(errorMessage)
        }

        private fun showError(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }



        private fun setupBuyersSection() {
            buyersAdapter =
                BuyerAdapter()  // Assuming you'll update BuyerAdapter to match ApplicantsAdapter pattern
            binding.buyersRecyclerView.apply {
                adapter = buyersAdapter
                layoutManager = LinearLayoutManager(context)
            }
            addInitialBuyerForm()

            binding.addBuyerButton.setOnClickListener {
                addNewBuyerForm()
                // Smooth scroll to the new item
                binding.buyersRecyclerView.post {
                    buyersAdapter.itemCount.let { count ->
                        if (count > 0) {
                            binding.buyersRecyclerView.smoothScrollToPosition(count - 1)
                        }
                    }
                }
            }
        }

        private fun addInitialBuyerForm() {
            val initialBuyer = Buyer(
                name = "",
                contact_info = "",
                quantity = 0
            )
            buyersAdapter.addItem(initialBuyer)
        }

        private fun addNewBuyerForm() {
            val newBuyer = Buyer(
                name = "",
                contact_info = "",
                quantity = 0
            )
            buyersAdapter.addItem(newBuyer)
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

        private fun validateNumericField(layout: TextInputLayout, text: String): Boolean {
            return if (text.isEmpty()) {
                layout.error = "This field is required"
                false
            } else if (text.toDoubleOrNull() == null) {
                layout.error = "Please enter a valid number"
                false
            } else if (text.toDouble() <= 0) {
                layout.error = "Value must be greater than 0"
                false
            } else {
                layout.error = null
                true
            }
        }







        private fun validateRequiredSelection(
            condition: Boolean,
            layout: TextInputLayout,
            errorMessage: String
        ): Boolean {
            return if (!condition) {
                layout.error = errorMessage
                false
            } else {
                layout.error = null
                true
            }
        }

        private fun submitForm() {
            try {
                val harvestPlanning = HarvestPlanning(
                    field = selectedFieldNumber ?: throw IllegalStateException("Field must be selected"),
                    season = binding.seasonDropdown.text.toString(),
                    producer = binding.producerDropdown.text.toString(),
                    date = startDate ?: throw IllegalStateException("Date must be selected"),
                    laborManDays = binding.laborManDaysInput.text.toString().toIntOrNull()
                        ?: throw IllegalStateException("Invalid labor man days"),
                    unitCostOfLabor = binding.unitCostOfLaborInput.text.toString().toDoubleOrNull()
                        ?: throw IllegalStateException("Invalid unit cost of labor"),
                    startTime = binding.startTimeInput.text.toString().takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException("Start time must be set"),
                    endTime = binding.endTimeInput.text.toString().takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException("End time must be set"),
                    weightPerUnit = binding.weightPerUnitInput.text.toString().toDoubleOrNull()
                        ?: throw IllegalStateException("Invalid weight per unit"),
                    pricePerUnit = binding.pricePerUnitInput.text.toString().toDoubleOrNull()
                        ?: throw IllegalStateException("Invalid price per unit"),
                    harvestedUnits = binding.harvestedUnitsDropdown.text.toString().takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException("Harvested units must be selected"), // Changed to use string directly
                    numberOfUnits = binding.numberOfUnitsInput.text.toString().toIntOrNull()
                        ?: throw IllegalStateException("Invalid number of units"),
                    harvestedQuality = binding.harvestedQualityDropdown.text.toString().takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException("Harvested quality must be selected"),
                    comments = binding.commentsInput.text.toString(),
                    seasonPlanningId = selectedSeasonId?.toInt() ?: throw IllegalStateException("Season must be selected")
                )

                // Rest of the submitForm() implementation remains the same
                val buyers = buyersAdapter.getItems().map { buyer ->
                    HarvestPlanningBuyer(
                        name = buyer.name.takeIf { it.isNotBlank() }
                            ?: throw IllegalStateException("Buyer name cannot be empty"),
                        contactInfo = buyer.contact_info.takeIf { it.isNotBlank() }
                            ?: throw IllegalStateException("Buyer contact info cannot be empty"),
                        quantity = buyer.quantity,
                        harvestPlanningId = 0
                    )
                }

                if (buyers.isEmpty()) {
                    throw IllegalStateException("Please add at least one buyer")
                }

                viewModel.saveHarvestPlan(harvestPlanning, buyers)

            } catch (e: Exception) {
                handleSubmissionError(e)
            }
        }



        private fun clearForm() {
            binding.apply {
                startTimeInput.setText("")
                endTimeInput.setText("")
                numberOfUnitsInput.setText("")
                weightPerUnitInput.setText("")
                pricePerUnitInput.setText("")
                laborManDaysInput.setText("")
                unitCostOfLaborInput.setText("")
                commentsInput.setText("")
                harvestedUnitsDropdown.setText("")
                harvestedQualityDropdown.setText("")
                producerDropdown.setText("")
                seasonDropdown.setText("")
                fieldDropdown.setText("")

                // Clear errors
                startTimeLayout.error = null
                endTimeLayout.error = null
                numberOfUnitsLayout.error = null
                weightPerUnitLayout.error = null
                pricePerUnitLayout.error = null
                laborManDaysLayout.error = null
                unitCostOfLaborLayout.error = null
                harvestedUnitsLayout.error = null
                harvestedQualityLayout.error = null
                producerLayout.error = null
                seasonLayout.error = null
                fieldLayout.error = null

                // Reset date selection
                resetDateSelection()

                // Reset buyers
                buyersAdapter.clearItems()
                addInitialBuyerForm()

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
            if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }
            _binding = null
        }

    }







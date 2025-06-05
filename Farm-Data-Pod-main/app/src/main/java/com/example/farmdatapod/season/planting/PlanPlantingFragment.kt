package com.example.farmdatapod.season.planting

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.season.register.registerSeasonData.Season
import kotlin.coroutines.cancellation.CancellationException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentPlanPlantingBinding
import com.example.farmdatapod.models.MethodOfPlanting
import com.example.farmdatapod.models.PlanPlantingModel
import com.example.farmdatapod.models.PlantingMaterial
import com.example.farmdatapod.season.planting.data.PlanPlantingRepository
import com.example.farmdatapod.season.planting.data.PlantingMethodType
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.example.farmdatapod.utils.NetworkUtils
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PlanPlantingFragment : Fragment() {
    private var _binding: FragmentPlanPlantingBinding? = null
    private val binding get() = _binding!!

    private lateinit var planPlantingRepository: PlanPlantingRepository
    private var producersJob: Job? = null
    private var fieldsJob: Job? = null
    private lateinit var producerRepository: ProducerRepository
    private lateinit var fieldRegistrationRepository: FieldRegistrationRepository
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Selection tracking variables
    private var selectedProducerCode: String? = null
    private var selectedProducerId: Int? = null  // Add this line
    private var selectedSeasonId: Long? = null
    private var selectedFieldNumber: String? = null
    private var startDate: String? = null
    private var endDate: String? = null

    // Data source variables
    private lateinit var seasonRepository: SeasonRepository
    private var producersMap: Map<String, String> = emptyMap()
    private var fieldsMap: MutableMap<String, String> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanPlantingBinding.inflate(inflater, container, false)
        planPlantingRepository = PlanPlantingRepository(requireContext())
        producerRepository = ProducerRepository(requireContext())
        fieldRegistrationRepository = FieldRegistrationRepository(requireContext())
        seasonRepository = SeasonRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProducers() // Add this call to start loading producers
        setupToolbar()
//        setupProducerDropdown()  // Initialize producer dropdown first
        setupDatePickerListeners() // Handle date selection if needed
        setupDropdowns() // Setup other dropdowns (planting materials, etc.)
        setupPlantingMaterialListener()
        setupMethodOfPlantingListener()
        setupSubmitButton()
        loadSavedPlantingPlans()
    }


    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
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

    private fun setupDatePickerListeners() {
        binding.dateTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            resetDateSelection()
            when (checkedId) {
                R.id.singleDateRadio -> {
                    // Setup single date picker dialog
                    val calendar = Calendar.getInstance()
                    showDatePickerDialog(
                        calendar,
                        { _, year, month, day ->
                            calendar.set(year, month, day)
                            val apiDate = formatDateToISOString(calendar.time)
                            val displayDate = dateFormatter.format(calendar.time)
                            handleSingleDateSelection(apiDate, displayDate)
                        },
                        "Select Date"
                    )
                }

                R.id.dateRangeRadio -> {
                    // Setup start date picker
                    val startCalendar = Calendar.getInstance()
                    showDatePickerDialog(
                        startCalendar,
                        { _, year, month, day ->
                            startCalendar.set(year, month, day)
                            val startApiDate = formatDateToISOString(startCalendar.time)
                            val startDisplayDate = dateFormatter.format(startCalendar.time)
                            startDate = startApiDate
                            updateDateRangeDisplay(startDisplayDate, null)

                            // Show end date picker after selecting start date
                            val endCalendar = Calendar.getInstance()
                            showDatePickerDialog(
                                endCalendar,
                                { _, endYear, endMonth, endDay ->
                                    endCalendar.set(endYear, endMonth, endDay)
                                    val endApiDate = formatDateToISOString(endCalendar.time)
                                    val endDisplayDate = dateFormatter.format(endCalendar.time)
                                    endDate = endApiDate
                                    updateDateRangeDisplay(startDisplayDate, endDisplayDate)
                                },
                                "Select End Date",
                                startCalendar.timeInMillis
                            )
                        },
                        "Select Start Date"
                    )
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


    private fun setupDropdowns() {
        // Basic dropdowns


        // Planting materials
        val plantingMaterials = arrayOf(PLANTING_MATERIAL_SEED, PLANTING_MATERIAL_SEEDLING)
        val seedUnits = arrayOf("kg", "g", "oz")
        val seedlingUnits = arrayOf("pieces", "trays", "boxes")

        // Method of planting options
        val plantingMethods = arrayOf(
            "Manual",
            "Tractor",
            "Spreader",
            "Cart(hand/animal)",
            "Drill",
            "Broadcasting",
            "Hydroponics",
            "Aeroponics",
            "No-till(without soil disturbance)"
        )


        setupDropdown(binding.plantingMaterialDropdown, plantingMaterials)
        setupDropdown(binding.methodOfPlantingDropdown, plantingMethods)
        setupDropdown(binding.seedUnitDropdown, seedUnits)
        setupDropdown(binding.seedlingUnitDropdown, seedlingUnits)
    }

    private fun setupDropdown(autoCompleteTextView: AutoCompleteTextView, items: Array<String>) {
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, items)
        autoCompleteTextView.setAdapter(adapter)
    }

    private fun setupPlantingMaterialListener() {
        binding.plantingMaterialDropdown.setOnItemClickListener { _, _, _, _ ->
            updatePlantingMaterialFields()
        }
    }

    private fun setupMethodOfPlantingListener() {
        binding.methodOfPlantingDropdown.setOnItemClickListener { _, _, _, _ ->
            updateMethodOfPlantingFields()
        }
    }

    private fun updatePlantingMaterialFields() {
        when (binding.plantingMaterialDropdown.text.toString()) {
            PLANTING_MATERIAL_SEED -> {
                binding.seedFieldsContainer.visibility = View.VISIBLE
                binding.seedlingFieldsContainer.visibility = View.GONE
            }

            PLANTING_MATERIAL_SEEDLING -> {
                binding.seedFieldsContainer.visibility = View.GONE
                binding.seedlingFieldsContainer.visibility = View.VISIBLE
            }

            else -> {
                binding.seedFieldsContainer.visibility = View.GONE
                binding.seedlingFieldsContainer.visibility = View.GONE
            }
        }
    }

    private fun updateMethodOfPlantingFields() {
        val selectedMethod = try {
            PlantingMethodType.fromString(binding.methodOfPlantingDropdown.text.toString())
        } catch (e: IllegalArgumentException) {
            return
        }

        // Show/hide appropriate containers
        binding.manualPlantingContainer.visibility =
            if (selectedMethod.requiresLabor) View.VISIBLE else View.GONE

        binding.methodUnitsContainer.visibility =
            if (selectedMethod.requiresUnits) View.VISIBLE else View.GONE

        // Clear inputs when hiding
        if (!selectedMethod.requiresLabor) {
            binding.laborManDaysInput.text?.clear()
        }
        if (!selectedMethod.requiresUnits) {
            binding.methodUnitsInput.text?.clear()
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

        // Validate common fields
        val commonFields = listOf(
            binding.producerLayout to binding.producerDropdown.text.toString(),
            binding.seasonLayout to binding.seasonDropdown.text.toString(),
            binding.fieldLayout to binding.fieldDropdown.text.toString(),
            binding.cropLayout to binding.cropInput.text.toString(),
            binding.cropCycleLayout to binding.cropCycleInput.text.toString(),
            binding.cropPopulationLayout to binding.cropPopulationInput.text.toString(),
            binding.targetPopulationLayout to binding.targetPopulationInput.text.toString(),
            binding.plantingMaterialLayout to binding.plantingMaterialDropdown.text.toString(),
            binding.methodOfPlantingLayout to binding.methodOfPlantingDropdown.text.toString(),
            binding.unitCostLayout to binding.unitCostInput.text.toString()
        )

        commonFields.forEach { (layout, value) ->
            if (!validateField(layout, value)) {
                isValid = false
            }
        }

        // Validate planting material fields
        when (binding.plantingMaterialDropdown.text.toString()) {
            PLANTING_MATERIAL_SEED -> {
                val seedFields = listOf(
                    binding.seedBatchNumberLayout to binding.seedBatchNumberInput.text.toString(),
                    binding.seedUnitLayout to binding.seedUnitDropdown.text.toString(),
                    binding.seedUnitCostLayout to binding.seedUnitCostInput.text.toString()
                )
                seedFields.forEach { (layout, value) ->
                    if (!validateField(layout, value)) {
                        isValid = false
                    }
                }
            }

            PLANTING_MATERIAL_SEEDLING -> {
                val seedlingFields = listOf(
                    binding.seedlingSourceLayout to binding.seedlingSourceInput.text.toString(),
                    binding.seedlingUnitLayout to binding.seedlingUnitDropdown.text.toString(),
                    binding.seedlingUnitCostLayout to binding.seedlingUnitCostInput.text.toString()
                )
                seedlingFields.forEach { (layout, value) ->
                    if (!validateField(layout, value)) {
                        isValid = false
                    }
                }
            }
        }

        // Validate method of planting fields
        val selectedMethod = try {
            PlantingMethodType.fromString(binding.methodOfPlantingDropdown.text.toString())
        } catch (e: IllegalArgumentException) {
            null
        }

        selectedMethod?.let { method ->
            if (method.requiresLabor) {
                if (!validateField(
                        binding.laborManDaysLayout,
                        binding.laborManDaysInput.text.toString()
                    )
                ) {
                    isValid = false
                }
            }
            if (method.requiresUnits) {
                if (!validateField(
                        binding.methodUnitsLayout,
                        binding.methodUnitsInput.text.toString()
                    )
                ) {
                    isValid = false
                }
            }
        }

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

    private fun submitForm() {
        try {
            // Show loading state
            showLoading(true)

            // 1. Create planting material
            val plantingMaterial = createPlantingMaterial()

            // 2. Create method of planting
            val selectedMethod = PlantingMethodType.fromString(
                binding.methodOfPlantingDropdown.text.toString()
            )
            val methodOfPlanting = createMethodOfPlanting(selectedMethod)

            // 3. Get labor days based on method type
            val laborDays = if (selectedMethod.requiresLabor) {
                binding.laborManDaysInput.text.toString().toIntOrNull()
                    ?: throw IllegalStateException("Please enter valid labor man days")
            } else 0

            // 4. Get date of planting
            val dateOfPlanting = when (binding.dateTypeGroup.checkedRadioButtonId) {
                R.id.singleDateRadio -> startDate
                R.id.dateRangeRadio -> "$startDate to $endDate"
                else -> throw IllegalStateException("Please select a date type")
            } ?: throw IllegalStateException("Please select a valid date")

            // 5. Create planting model
            val planPlantingModel = PlanPlantingModel(
                producer = selectedProducerCode
                    ?: throw IllegalStateException("Please select a producer"),
                field = selectedFieldNumber ?: throw IllegalStateException("Please select a field"),
                season = binding.seasonDropdown.text.toString(),
                date_of_planting = dateOfPlanting,
                crop = binding.cropInput.text.toString(),
                crop_population = binding.cropPopulationInput.text.toString().toIntOrNull()
                    ?: throw IllegalStateException("Please enter a valid crop population"),
                target_population = binding.targetPopulationInput.text.toString().toIntOrNull()
                    ?: throw IllegalStateException("Please enter a valid target population"),
                crop_cycle_in_weeks = binding.cropCycleInput.text.toString().toIntOrNull()
                    ?: throw IllegalStateException("Please enter a valid crop cycle"),
                planting_material = plantingMaterial,
                method_of_planting = methodOfPlanting,
                labor_man_days = laborDays,
                unit_cost_of_labor = binding.unitCostInput.text.toString().toDoubleOrNull()
                    ?: throw IllegalStateException("Please enter a valid unit cost of labor"),
                season_planning_id = selectedSeasonId?.toInt()
                    ?: throw IllegalStateException("Please select a valid season")
            )

            // 6. Save using repository
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
                    Log.d(TAG, "Network status: ${if (isOnline) "Online" else "Offline"}")

                    planPlantingRepository.savePlanPlanting(planPlantingModel, isOnline)
                        .onSuccess { completePlan ->
                            Log.d(
                                TAG,
                                "Successfully saved planting plan with ID: ${completePlan.plan.id}"
                            )

                            val successMessage = if (isOnline) {
                                if (completePlan.plan.sync_status) {
                                    "Planting plan saved and synced successfully"
                                } else {
                                    "Planting plan saved locally, but sync failed. Will retry later."
                                }
                            } else {
                                "Planting plan saved locally. Will sync when online."
                            }

                            showSuccess(successMessage)
                            clearFields()
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Failed to save planting plan", error)
                            showError("Failed to save planting plan: ${error.localizedMessage ?: "Unknown error"}")
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in saving planting plan", e)
                    showError("An unexpected error occurred: ${e.localizedMessage ?: "Unknown error"}")
                } finally {
                    showLoading(false)
                }
            }
        } catch (e: IllegalStateException) {
            showError(e.message ?: "Please check your inputs")
            showLoading(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error in form submission", e)
            showError("An unexpected error occurred: ${e.localizedMessage ?: "Unknown error"}")
            showLoading(false)
        }
    }

    private fun loadSavedPlantingPlans() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                planPlantingRepository.getPlantingPlansFlow().collect { plans ->
                    // Handle the plans if needed, e.g., show in a list
                    Log.d(TAG, "Loaded ${plans.size} planting plans")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading planting plans", e)
            }
        }
    }


    private fun createPlantingMaterial(): PlantingMaterial {
        return when (binding.plantingMaterialDropdown.text.toString()) {
            PLANTING_MATERIAL_SEED -> {
                PlantingMaterial(
                    type = PLANTING_MATERIAL_SEED,
                    seed_batch_number = binding.seedBatchNumberInput.text.toString(),
                    source = null,
                    unit = binding.seedUnitDropdown.text.toString(),
                    unit_cost = binding.seedUnitCostInput.text.toString().toIntOrNull()
                        ?: throw IllegalStateException("Invalid unit cost for seed")
                )
            }

            PLANTING_MATERIAL_SEEDLING -> {
                PlantingMaterial(
                    type = PLANTING_MATERIAL_SEEDLING,
                    seed_batch_number = null,
                    source = binding.seedlingSourceInput.text.toString(),
                    unit = binding.seedlingUnitDropdown.text.toString(),
                    unit_cost = binding.seedlingUnitCostInput.text.toString().toIntOrNull()
                        ?: throw IllegalStateException("Invalid unit cost for seedling")
                )
            }

            else -> throw IllegalStateException("Please select a planting material type")
        }
    }

    private fun createMethodOfPlanting(selectedMethod: PlantingMethodType): MethodOfPlanting {
        return MethodOfPlanting(
            method = binding.methodOfPlantingDropdown.text.toString(),
            unit = if (selectedMethod.requiresUnits) {
                binding.methodUnitsInput.text.toString().toIntOrNull()
                    ?: throw IllegalStateException("Please enter valid units for the planting method")
            } else null,
            labor_man_days = if (selectedMethod.requiresLabor) {
                binding.laborManDaysInput.text.toString().toIntOrNull()
                    ?: throw IllegalStateException("Please enter valid labor man days")
            } else null
        )
    }

    private var isClearing = false

    private fun clearFields() {
        if (isClearing) return
        isClearing = true

        try {
            binding.apply {
                // Temporarily remove listeners
                dateTypeGroup.setOnCheckedChangeListener(null)
                plantingMaterialDropdown.onItemClickListener = null
                methodOfPlantingDropdown.onItemClickListener = null

                // Clear input fields
                val inputFields = listOf(
                    producerDropdown,
                    seasonDropdown,
                    fieldDropdown,
                    cropInput,
                    cropCycleInput,
                    cropPopulationInput,
                    targetPopulationInput,
                    plantingMaterialDropdown,
                    methodOfPlantingDropdown,
                    unitCostInput
                )
                inputFields.forEach { it.setText("") }

                // Clear planting material fields
                val materialFields = listOf(
                    seedBatchNumberInput,
                    seedUnitDropdown,
                    seedUnitCostInput,
                    seedlingSourceInput,
                    seedlingUnitDropdown,
                    seedlingUnitCostInput
                )
                materialFields.forEach { it.setText("") }

                // Clear method fields
                laborManDaysInput.setText("")
                methodUnitsInput.setText("")

                // Reset visibility states
                val containersToHide = listOf(
                    seedFieldsContainer,
                    seedlingFieldsContainer,
                    manualPlantingContainer,
                    methodUnitsContainer
                )
                containersToHide.forEach { it.visibility = View.GONE }

                // Reset date selection
                dateTypeGroup.clearCheck()
                resetDateSelection()

                // Clear any error states
                val inputLayouts = listOf(
                    producerLayout,
                    seasonLayout,
                    fieldLayout,
                    cropLayout,
                    cropCycleLayout,
                    cropPopulationLayout,
                    targetPopulationLayout,
                    plantingMaterialLayout,
                    methodOfPlantingLayout,
                    unitCostLayout
                )
                inputLayouts.forEach { it.error = null }

                // Reset selection tracking variables
                selectedProducerCode = null
                selectedSeasonId = null
                selectedFieldNumber = null
                startDate = null
                endDate = null

            }
        } finally {
            // Restore listeners
            setupDatePickerListeners()
            setupPlantingMaterialListener()
            setupMethodOfPlantingListener()
            isClearing = false
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        showLoading(false)
    }

    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        clearFields()
        findNavController().navigateUp()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.submitButton.apply {
            isEnabled = !isLoading
            text = if (isLoading) "Submitting..." else "Submit"
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        producersJob?.cancel()
        fieldsJob?.cancel()
        _binding = null
    }

    companion object {
        const val TAG = "PlanPlantingFragment"
        private const val PLANTING_MATERIAL_SEED = "Seed"
        private const val PLANTING_MATERIAL_SEEDLING = "Seedling"
    }
}
package com.example.farmdatapod.season.cropManagement

import android.app.DatePickerDialog
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
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.*
import com.example.farmdatapod.databinding.FragmentCropManagementBinding
import com.example.farmdatapod.models.*
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.season.cropManagement.data.CropManagementViewModel
import com.example.farmdatapod.season.register.registerSeasonData.Season
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.example.farmdatapod.utils.DialogUtils
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class CropManagementFragment : Fragment() {
    private var _binding: FragmentCropManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CropManagementViewModel by viewModels()
    private lateinit var loadingDialog: AlertDialog

    // Existing adapter declarations
    private lateinit var gappingAdapter: GappingActivityAdapter
    private lateinit var stakingAdapter: StakingActivityAdapter
    private lateinit var pruningAdapter: PruningActivityAdapter
    private lateinit var thinningAdapter: ThinningActivityAdapter
    private lateinit var weedingAdapter: WeedingActivityAdapter
    private lateinit var wateringAdapter: WateringActivityAdapter

    // Lists to hold activities
    private val gappingActivities = mutableListOf<GappingActivity>()
    private val stakingActivities = mutableListOf<StakingActivity>()
    private val pruningActivities = mutableListOf<PruningActivity>()
    private val thinningActivities = mutableListOf<ThinningActivity>()
    private val weedingActivities = mutableListOf<WeedingActivity>()
    private val wateringActivities = mutableListOf<WateringActivity>()

    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var selectedProducerId: Int? = null
    private var selectedProducerCode: String? = null
    private var selectedSeasonId: Long? = null
    private var selectedFieldNumber: String? = null
    private var startDate: String? = null
    private var endDate: String? = null

    private var producersJob: Job? = null
    private var fieldsJob: Job? = null

    private lateinit var producerRepository: ProducerRepository
    private lateinit var fieldRegistrationRepository: FieldRegistrationRepository
    private lateinit var seasonRepository: SeasonRepository
    private var fieldsMap: MutableMap<String, String> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCropManagementBinding.inflate(inflater, container, false)
        setupRepositories()
        return binding.root
    }

    private fun setupRepositories() {
        seasonRepository = SeasonRepository(requireContext())
        producerRepository = ProducerRepository(requireContext())
        fieldRegistrationRepository = FieldRegistrationRepository(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLoadingDialog()
        setupViews()
        observeViewModel()
    }

    private fun setupLoadingDialog() {
        loadingDialog = DialogUtils(requireContext()).buildLoadingDialog()
    }

    private fun setupViews() {
        setupToolbar()
        setupRecyclerViews()
        setupSubmitButton()
        setupDatePicker()
        loadProducers()
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

    private fun setupRecyclerViews() {
        // Initialize Gapping Activities
        gappingAdapter = GappingActivityAdapter(gappingActivities) { position ->
            gappingActivities.removeAt(position)
            gappingAdapter.notifyItemRemoved(position)
        }
        binding.gappingRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = gappingAdapter
        }
        addInitialGappingActivity()

        // Initialize Staking Activities
        stakingAdapter = StakingActivityAdapter(stakingActivities) { position ->
            stakingActivities.removeAt(position)
            stakingAdapter.notifyItemRemoved(position)
        }
        binding.stakingRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stakingAdapter
        }
        addInitialStakingActivity()

        // Initialize Pruning Activities
        pruningAdapter = PruningActivityAdapter(pruningActivities) { position ->
            pruningActivities.removeAt(position)
            pruningAdapter.notifyItemRemoved(position)
        }
        binding.pruningRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pruningAdapter
        }
        addInitialPruningActivity()

        // Initialize Thinning Activities
        thinningAdapter = ThinningActivityAdapter(thinningActivities) { position ->
            thinningActivities.removeAt(position)
            thinningAdapter.notifyItemRemoved(position)
        }
        binding.thinningRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = thinningAdapter
        }
        addInitialThinningActivity()

        // Initialize Weeding Activities
        weedingAdapter = WeedingActivityAdapter(weedingActivities) { position ->
            weedingActivities.removeAt(position)
            weedingAdapter.notifyItemRemoved(position)
        }
        binding.weedingRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = weedingAdapter
        }
        addInitialWeedingActivity()

        // Initialize Watering Activities
        wateringAdapter = WateringActivityAdapter(wateringActivities) { position ->
            wateringActivities.removeAt(position)
            wateringAdapter.notifyItemRemoved(position)
        }
        binding.wateringRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = wateringAdapter
        }
        addInitialWateringActivity()
    }

    private fun addInitialGappingActivity() {
        val initialGapping = GappingActivity(
            activity = "Gapping",
            crop_population = 0,
            man_days = 0,
            planting_material = "",
            target_population = 0,
            unit_cost_of_labor = 0.0
        )
        gappingActivities.add(initialGapping)
        gappingAdapter.notifyItemInserted(gappingActivities.size - 1)
    }

    private fun addInitialStakingActivity() {
        val initialStaking = StakingActivity(
            activity = "Staking",
            cost_per_unit = 0.0,
            man_days = 0,
            unit_cost_of_labor = 0.0,
            unit_stakes = 0
        )
        stakingActivities.add(initialStaking)
        stakingAdapter.notifyItemInserted(stakingActivities.size - 1)
    }

    private fun addInitialPruningActivity() {
        val initialPruning = PruningActivity(
            activity = "Pruning",
            equipment_used = "",
            man_days = 0,
            unit_cost_of_labor = 0.0
        )
        pruningActivities.add(initialPruning)
        pruningAdapter.notifyItemInserted(pruningActivities.size - 1)
    }

    private fun addInitialThinningActivity() {
        val initialThinning = ThinningActivity(
            activity = "Thinning",
            equipment_used = "",
            man_days = 0,
            unit_cost_of_labor = 0.0
        )
        thinningActivities.add(initialThinning)
        thinningAdapter.notifyItemInserted(thinningActivities.size - 1)
    }

    private fun addInitialWeedingActivity() {
        val initialWeeding = WeedingActivity(
            activity = "Weeding",
            input = "",
            man_days = 0,
            method_of_weeding = "",
            unit_cost_of_labor = 0.0
        )
        weedingActivities.add(initialWeeding)
        weedingAdapter.notifyItemInserted(weedingActivities.size - 1)
    }

    private fun addInitialWateringActivity() {
        val initialWatering = WateringActivity(
            activity = "Watering",
            cost_of_fuel = 0.0,
            discharge_hours = 0,
            end_time = "",
            frequency_of_watering = "",
            man_days = 0,
            start_time = "",
            type_of_irrigation = "",
            unit_cost = 0.0,
            unit_cost_of_labor = 0.0
        )
        wateringActivities.add(initialWatering)
        wateringAdapter.notifyItemInserted(wateringActivities.size - 1)
    }






    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
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

        // Validate date selection
        if (binding.dateTypeGroup.checkedRadioButtonId == -1) {
            // No date type selected
            isValid = false
            // You might want to show an error message here
        }

        // Validate date fields based on selection
        when (binding.dateTypeGroup.checkedRadioButtonId) {
            R.id.singleDateRadio -> {
                // Validate single date
                // Add your date validation logic here
            }
            R.id.dateRangeRadio -> {
                // Validate date range
                // Add your date range validation logic here
            }
        }

        // Validate required dropdown selections
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

        // Validate comments
        isValid = validateField(
            binding.commentsLayout,
            binding.commentsInput.text.toString()
        ) && isValid

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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                if (isLoading) {
                    loadingDialog.show()
                    binding.submitButton.isEnabled = false
                } else {
                    loadingDialog.dismiss()
                    binding.submitButton.isEnabled = true
                }
            }

            viewModel.savingStatus.observe(viewLifecycleOwner) { result ->
                result.fold(
                    onSuccess = {
                        Toast.makeText(context, "Successfully saved crop management", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    },
                    onFailure = { error ->
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            viewModel.syncStatus.observe(viewLifecycleOwner) { result ->
                result.fold(
                    onSuccess = { stats ->
                        Toast.makeText(
                            context,
                            "Sync completed: ${stats.uploadedCount} uploaded, ${stats.downloadedCount} downloaded",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(context, "Sync error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun submitForm() {
        if (!validateInputs()) {
            return
        }

        val cropManagement = CropManagementModel(
            producer = selectedProducerId?.toString() ?: "",
            season = selectedSeasonId?.toString() ?: "",
            field = selectedFieldNumber ?: "",
            date = startDate ?: "",
            season_planning_id = selectedSeasonId?.toInt() ?: 0,
            comments = binding.commentsInput.text.toString(),
            gappingActivity = gappingActivities.firstOrNull() ?: createDefaultGappingActivity(),
            weedingActivity = weedingActivities.firstOrNull() ?: createDefaultWeedingActivity(),
            pruningActivity = pruningActivities.firstOrNull() ?: createDefaultPruningActivity(),
            stakingActivity = stakingActivities.firstOrNull() ?: createDefaultStakingActivity(),
            thinningActivity = thinningActivities.firstOrNull() ?: createDefaultThinningActivity(),
            wateringActivity = wateringActivities.firstOrNull() ?: createDefaultWateringActivity()
        )

        viewModel.saveCropManagement(cropManagement)
    }

    private fun createDefaultGappingActivity() = GappingActivity(
        activity = "Gapping",
        crop_population = 0,
        man_days = 0,
        planting_material = "",
        target_population = 0,
        unit_cost_of_labor = 0.0
    )

    private fun createDefaultWeedingActivity() = WeedingActivity(
        activity = "Weeding",
        input = "",
        man_days = 0,
        method_of_weeding = "",
        unit_cost_of_labor = 0.0
    )

    private fun createDefaultPruningActivity() = PruningActivity(
        activity = "Pruning",
        equipment_used = "",
        man_days = 0,
        unit_cost_of_labor = 0.0
    )

    private fun createDefaultStakingActivity() = StakingActivity(
        activity = "Staking",
        cost_per_unit = 0.0,
        man_days = 0,
        unit_cost_of_labor = 0.0,
        unit_stakes = 0
    )

    private fun createDefaultThinningActivity() = ThinningActivity(
        activity = "Thinning",
        equipment_used = "",
        man_days = 0,
        unit_cost_of_labor = 0.0
    )

    private fun createDefaultWateringActivity() = WateringActivity(
        activity = "Watering",
        cost_of_fuel = 0.0,
        discharge_hours = 0,
        end_time = "",
        frequency_of_watering = "",
        man_days = 0,
        start_time = "",
        type_of_irrigation = "",
        unit_cost = 0.0,
        unit_cost_of_labor = 0.0
    )


    override fun onDestroyView() {
        super.onDestroyView()
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
        producersJob?.cancel()
        fieldsJob?.cancel()
        _binding = null
    }
}
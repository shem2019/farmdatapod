package com.example.farmdatapod.cropmanagement.nursery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.farmdatapod.R



import android.app.DatePickerDialog

import android.util.Log

import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.adapter.ManagementActivityAdapter
import com.example.farmdatapod.cropmanagement.nursery.data.NurseryManagementViewModel
import com.example.farmdatapod.databinding.FragmentNurseryManagementBinding
import com.example.farmdatapod.models.Input
import com.example.farmdatapod.models.NurseryManagementActivity
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.season.nursery.data.NurseryPlanningViewModel
import com.example.farmdatapod.season.planting.PlanPlantingFragment
import com.example.farmdatapod.season.register.registerSeasonData.Season
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class NurseryManagementFragment : Fragment() {
    private var _binding: FragmentNurseryManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityAdapter: ManagementActivityAdapter
    private val viewModel: NurseryManagementViewModel by viewModels()
    private lateinit var producerRepository: ProducerRepository
    private lateinit var seasonRepository: SeasonRepository
    private var producersJob: Job? = null
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedProducerCode: String? = null
    private var selectedProducerId: Int? = null
    private var selectedSeasonId: Long? = null
    private var startDate: String? = null
    private var endDate: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNurseryManagementBinding.inflate(inflater, container, false)
        producerRepository = ProducerRepository(requireContext())
        seasonRepository = SeasonRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        loadProducers()
        setupDatePickerListeners()
        setupManagementActivities()
        setupButtons()
        setupTrayTypeDropdown()
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


    private fun getFormattedDateRange(): String {
        if (startDate == null) {
            return "" // Or handle error
        }
        return if (endDate != null) {
            "$startDate to $endDate"
        } else {
            "$startDate to $startDate" // If single date, use same date for range
        }
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

    private fun setupTrayTypeDropdown() {
        val trayTypes = listOf(
            "Seedling Tray",
            "Germination Tray",
            "Propagation Tray",
            "Plug Tray"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            trayTypes
        )

        binding.trayTypeDropdown.setAdapter(adapter)
        binding.trayTypeDropdown.setText("Select", false) // Default text
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
                    Log.e(PlanPlantingFragment.TAG, "Error loading producers", e)
                    Toast.makeText(context, "Error loading producers", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun getFormattedDateForSubmission(): String {
        return try {
            when {
                binding.singleDateRadio.isChecked -> {
                    startDate ?: throw IllegalStateException("No date selected")
                }
                binding.dateRangeRadio.isChecked -> {
                    if (startDate == null || endDate == null) {
                        throw IllegalStateException("Date range not completely selected")
                    }
                    "$startDate to $endDate"
                }
                else -> throw IllegalStateException("Please select either single date or date range")
            }
        } catch (e: IllegalStateException) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            throw e
        }
    }
    private fun handleProducerSelection(selectedProducer: ProducerEntity) {
        selectedProducerId = selectedProducer.id
        selectedProducerCode = selectedProducer.farmerCode
        binding.producerLayout.error = null
        loadSeasonsForProducer() // Make sure you have this method too
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
                    Log.e(PlanPlantingFragment.TAG, "Error loading seasons", e)
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


    private fun setupButtons() {
        // Add Activity Button
        binding.addCropButton.setOnClickListener {
            Log.d("NurseryPlanning", "Add Activity button clicked")
            val newActivity = NurseryManagementActivity(
                management_activity = "",
                input = mutableListOf(
                    Input("", 0.0)  // Single empty input
                ),
                man_days = "",
                unit_cost_of_labor = 0.0,
                frequency = ""
            )
            activityAdapter.addActivity(newActivity)

            // Auto-scroll to the newly added activity
            binding.managementActivitiesRecyclerView.post {
                binding.managementActivitiesRecyclerView.smoothScrollToPosition(activityAdapter.itemCount - 1)
            }
        }

        // Submit Button remains the same
        binding.submitButton.setOnClickListener {
            if (validateInputs()) {
                submitForm()
            }
        }
    }
    private fun setupToolbar() {
        binding.toolbar.findViewById<View>(R.id.backButton).setOnClickListener {
            requireActivity().onBackPressed()
        }
    }



    private fun setupManagementActivities() {
        Log.d("NurseryPlanning", "Setting up RecyclerView")

        activityAdapter = ManagementActivityAdapter(
            activities = mutableListOf(),
            onDeleteActivity = { position ->
                Log.d("NurseryPlanning", "Delete callback for position $position")
                activityAdapter.getActivities().toMutableList().let { activities ->
                    activities.removeAt(position)
                    activityAdapter.updateActivities(activities)
                }
            },
            onActivityUpdated = { position, activity ->
                Log.d("NurseryPlanning", "Update callback for position $position")
                activityAdapter.getActivities().toMutableList().let { activities ->
                    activities[position] = activity
                    activityAdapter.updateActivities(activities)
                }
            }
        )

        binding.managementActivitiesRecyclerView.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activityAdapter
        }

        addInitialActivity()  // Add this line
    }
    private fun addInitialActivity() {
        val initialActivity = NurseryManagementActivity(
            management_activity = "",
            input = mutableListOf(
                Input("", 0.0)  // Single empty input
            ),
            man_days = "",
            unit_cost_of_labor = 0.0,
            frequency = ""
        )
        activityAdapter.addActivity(initialActivity)
        Log.d("NurseryPlanning", "Initial activity added with one empty input")
    }


    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate Producer
        if (binding.producerDropdown.text.isNullOrEmpty()) {
            binding.producerLayout.error = "Please select a producer"
            isValid = false
        } else {
            binding.producerLayout.error = null
        }

        // Validate Season
        if (binding.seasonDropdown.text.isNullOrEmpty()) {
            binding.seasonLayout.error = "Please select a season"
            isValid = false
        } else {
            binding.seasonLayout.error = null
        }
        if (startDate == null) {
            // You might want to add a TextView for error display
            Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // If date range is selected, validate end date
        if (binding.dateRangeRadio.isChecked && endDate == null) {
            Toast.makeText(requireContext(), "Please select end date", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        // Validate Crop Cycle
        if (binding.cropCycleInput.text.isNullOrEmpty()) {
            binding.cropCycleLayout.error = "Please enter crop cycle in weeks"
            isValid = false
        } else {
            binding.cropCycleLayout.error = null
        }

        // Validate Crop
        if (binding.cropInput.text.isNullOrEmpty()) {
            binding.cropLayout.error = "Please enter crop"
            isValid = false
        } else {
            binding.cropLayout.error = null
        }

        // Validate Variety
        if (binding.varietyInput.text.isNullOrEmpty()) {
            binding.varietyLayout.error = "Please enter variety"
            isValid = false
        } else {
            binding.varietyLayout.error = null
        }

        // Validate Seed Batch
        if (binding.seedBatchInput.text.isNullOrEmpty()) {
            binding.seedBatchLayout.error = "Please enter seed batch number"
            isValid = false
        } else {
            binding.seedBatchLayout.error = null
        }

        // Validate Tray Type
        if (binding.trayTypeDropdown.text.isNullOrEmpty()) {
            binding.trayTypeLayout.error = "Please select type of trays"
            isValid = false
        } else {
            binding.trayTypeLayout.error = null
        }

        // Validate Number of Trays
        if (binding.numberOfTraysInput.text.isNullOrEmpty()) {
            binding.numberOfTraysLayout.error = "Please enter number of trays"
            isValid = false
        } else {
            binding.numberOfTraysLayout.error = null
        }

        return isValid
    }

    private fun submitForm() {
        if (!validateInputs()) return

        try {
            val isOnline = NetworkUtils.isNetworkAvailable(requireContext())

            // Get the properly formatted date string based on selection
            val dateString = getFormattedDateForSubmission()

            viewModel.saveNurseryPlan(
                producer = binding.producerDropdown.text.toString(),
                season = binding.seasonDropdown.text.toString(),
                dateString = dateString,  // Send single date or date range string
                cropCycle = binding.cropCycleInput.text.toString().toInt(),
                crop = binding.cropInput.text.toString(),
                variety = binding.varietyInput.text.toString(),
                seedBatch = binding.seedBatchInput.text.toString(),
                trayType = binding.trayTypeDropdown.text.toString(),
                numberOfTrays = binding.numberOfTraysInput.text.toString().toInt(),
                comments = binding.commentsInput.text?.toString(),
                managementActivities = activityAdapter.getActivities(),
                seasonPlanningId = selectedSeasonId ?: 0,  // Add this line
                isOnline = isOnline,
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        if (isOnline) "Nursery plan saved and synced"
                        else "Nursery plan saved (offline)",
                        Toast.LENGTH_SHORT
                    ).show()
                    requireActivity().onBackPressed()
                },
                onError = { error ->
                    Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_LONG).show()
                }
            )
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

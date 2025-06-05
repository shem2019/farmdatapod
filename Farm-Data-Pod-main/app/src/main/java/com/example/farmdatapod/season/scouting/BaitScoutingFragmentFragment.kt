package com.example.farmdatapod.season.scouting

import android.app.DatePickerDialog
import android.content.ContentValues.TAG
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentBaitScoutingFragmentBinding
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.season.register.registerSeasonData.Season
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.example.farmdatapod.season.scouting.data.BaitRepository
import com.example.farmdatapod.season.scouting.data.BaitScoutingEntity
import com.example.farmdatapod.utils.NetworkUtils
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class BaitScoutingFragmentFragment : Fragment() {
    private var _binding: FragmentBaitScoutingFragmentBinding? = null
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var baitRepository: BaitRepository
    private var producersJob: Job? = null
    private var fieldsJob: Job? = null
    private lateinit var seasonRepository: SeasonRepository
    private lateinit var producerRepository: ProducerRepository
    private lateinit var fieldRegistrationRepository: FieldRegistrationRepository
    private val binding get() = _binding!!
    private var selectedProducerCode: String? = null
    private var selectedSeasonId: Long? = null
    private var selectedFieldNumber: String? = null
    private var startDate: String? = null
    private var endDate: String? = null
    private lateinit var producersMap: MutableMap<String, String>
    private lateinit var fieldsMap: MutableMap<String, String>
    private var selectedProducerId: Int? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBaitScoutingFragmentBinding.inflate(inflater, container, false)
        producersMap = mutableMapOf()  // Add this
        fieldsMap = mutableMapOf()
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        baitRepository = BaitRepository(requireContext())
        producerRepository = ProducerRepository(requireContext())
        seasonRepository = SeasonRepository(requireContext())
        fieldRegistrationRepository = FieldRegistrationRepository(requireContext())
        loadProducers()
        setupToolbar()
        setupDropdowns()
        setupSubmitButton()
    }

    private fun setupDropdowns() {
        setupDatePicker()
        setupProblemIdentifiedDropdown()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private var selectedProblem: String? = null

    private fun setupProblemIdentifiedDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            problems
        )
        binding.problemIdentifiedDropdown.setAdapter(adapter)

        binding.problemIdentifiedDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedProblem = problems[position]
            binding.problemIdentifiedLayout.error = null
            Log.d("ProblemDebug", "Selected problem: $selectedProblem")
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
                    Log.e("FieldDebug", "Error loading fields", e)
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


    val problems = arrayOf("Pest", "Disease", "Nutrition Deficiency", "Crop Stress", "Other")




    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateInputs()) {
                submitForm()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate dropdown selections
        isValid = validateField(
            binding.producerLayout,
            binding.producerDropdown.text.toString()
        ) && isValid
        isValid =
            validateField(binding.seasonLayout, binding.seasonDropdown.text.toString()) && isValid
        isValid =
            validateField(binding.fieldLayout, binding.fieldDropdown.text.toString()) && isValid
        isValid = validateField(
            binding.problemIdentifiedLayout,
            binding.problemIdentifiedDropdown.text.toString()
        ) && isValid

        // Validate text inputs
        isValid = validateField(
            binding.baitStationLayout,
            binding.baitStationInput.text.toString()
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

    private fun submitForm() {
        Log.d(TAG, "Starting bait scouting data submission")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.submitButton.isEnabled = false
                val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
                Log.d(TAG, "Network status: ${if (isOnline) "Online" else "Offline"}")

                // Create entity
                val baitData = createBaitEntity()
                val result = baitRepository.saveBaitScouting(baitData, isOnline)

                result.onSuccess {
                    val message = if (isOnline) {
                        "Bait scouting data registered and synced successfully"
                    } else {
                        "Bait scouting data saved offline. Will sync when online."
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
                    "Error saving bait scouting data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.submitButton.isEnabled = true
            }
        }
    }

    private fun createBaitEntity(): BaitScoutingEntity {
        // First ensure we have all required data
        if (selectedSeasonId == null || selectedProducerCode == null || selectedFieldNumber == null ||
            selectedProblem == null || binding.baitStationInput.text.toString().isEmpty()
        ) {
            throw IllegalStateException("Please fill in all required fields")
        }

        return BaitScoutingEntity(
            bait_station = binding.baitStationInput.text.toString(),
            date = startDate ?: formatDateToISOString(Calendar.getInstance().time),
            field = selectedFieldNumber!!,
            problem_identified = selectedProblem!!,
            producer = selectedProducerCode!!,
            season = binding.seasonDropdown.text.toString(),
            season_planning_id = selectedSeasonId!!.toInt(),
            is_synced = false
        )
    }


    private fun clearFields() {
        binding.apply {
            baitStationInput.text?.clear()
            producerDropdown.setText("")
            seasonDropdown.setText("")
            fieldDropdown.setText("")
            problemIdentifiedDropdown.setText("")
            resetDateSelection()
        }
        // Reset selection variables
        selectedProducerCode = null
        selectedSeasonId = null
        selectedFieldNumber = null
        selectedProblem = null
        startDate = null
        endDate = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.farmdatapod.season.landPreparation

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.CoverCropAdapter
import com.example.farmdatapod.adapter.MulchingAdapter
import com.example.farmdatapod.adapter.SoilAnalysisAdapter
import com.example.farmdatapod.databinding.FragmentLandPreparationBinding
import com.example.farmdatapod.models.CoverCrop
import com.example.farmdatapod.models.LandPreparationMulching
import com.example.farmdatapod.models.LandPreparationSoilAnalysis
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.season.register.registerSeasonData.Season
import com.example.farmdatapod.season.register.registerSeasonData.SeasonRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import androidx.fragment.app.viewModels
import androidx.appcompat.app.AlertDialog
import com.example.farmdatapod.season.landPreparation.data.CoverCropEntity
import com.example.farmdatapod.season.landPreparation.data.LandPreparationEntity
import com.example.farmdatapod.season.landPreparation.data.LandPreparationViewModel
import com.example.farmdatapod.season.landPreparation.data.MulchingEntity
import com.example.farmdatapod.season.landPreparation.data.SoilAnalysisEntity
import com.example.farmdatapod.utils.DialogUtils
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class LandPreparationFragment : Fragment() {
    private var _binding: FragmentLandPreparationBinding? = null
    private val binding get() = _binding!!

    private lateinit var coverCropAdapter: CoverCropAdapter
    private lateinit var mulchingAdapter: MulchingAdapter
    private lateinit var soilAnalysisAdapter: SoilAnalysisAdapter
    private var selectedProducerId: Int? = null  // Add this line
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener


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

    private val viewModel: LandPreparationViewModel by viewModels()
    private var seasonPlanningId: Int = 0
    private lateinit var loadingDialog: AlertDialog

    // Data source variables
    private lateinit var seasonRepository: SeasonRepository
    private var fieldsMap: MutableMap<String, String> = mutableMapOf()

    private val landPrepMethods = listOf(
        "Conventional Tillage",
        "Conservation Tillage",
        "Zero Tillage (No-Till)",
        "Minimum Tillage",
        "Strip Tillage",
        "Ridge Tillage",
        "Plowing",
        "Harrowing",
        "Rotavation",
        "Levelling"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLandPreparationBinding.inflate(inflater, container, false)
        seasonRepository = SeasonRepository(requireContext())
        producerRepository = ProducerRepository(requireContext())
        fieldRegistrationRepository = FieldRegistrationRepository(requireContext())
        loadingDialog = DialogUtils(requireContext()).buildLoadingDialog()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupToolbar()
        setupRecyclerViews()
        setupSubmitButton()
        setupDatePicker()
        loadProducers()
        setupDropdowns()
    }
    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) loadingDialog.show() else loadingDialog.dismiss()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                loadingDialog.dismiss()
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { message ->
            message?.let {
                loadingDialog.dismiss()
                showSuccess(it)
                viewModel.clearSuccess()
                findNavController().navigateUp()
            }
        }
    }

    private fun setupDropdowns() {
        // Setup land preparation methods dropdown
        val landPrepAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            landPrepMethods
        )
        binding.landPrepMethodDropdown.setAdapter(landPrepAdapter)
        binding.landPrepMethodDropdown.setOnItemClickListener { _, _, _, _ ->
            binding.landPrepMethodLayout.error = null
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
        // Setup Cover Crop RecyclerView
        coverCropAdapter = CoverCropAdapter()
        binding.coverCropRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = coverCropAdapter
        }
        // Add initial cover crop form
        coverCropAdapter.addItem(
            CoverCrop(
                coverCrop = "",
                dateOfEstablishment = "",
                unit = "",
                unitCost = 0.0,
                typeOfInoculant = "",
                dateOfIncorporation = "",
                manDays = 0,
                unitCostOfLabor = 0.0
            )
        )

        // Setup Mulching RecyclerView
        mulchingAdapter = MulchingAdapter()
        binding.mulchingRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mulchingAdapter
        }
        // Add initial mulching form
        mulchingAdapter.addItem(
            LandPreparationMulching(
                typeOfMulch = "",
                costOfMulch = 0.0,
                lifeCycleOfMulchInSeasons = 0,
                manDays = 0,
                unitCostOfLabor = 0.0
            )
        )

        // Setup Soil Analysis RecyclerView
        soilAnalysisAdapter = SoilAnalysisAdapter()
        binding.soilAnalysisRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = soilAnalysisAdapter
        }
        // Add initial soil analysis form
        soilAnalysisAdapter.addItem(
            LandPreparationSoilAnalysis(
                typeOfAnalysis = "",
                costOfAnalysis = 0.0,
                lab = ""
            )
        )
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateForm()) {
                submitForm()
            }
        }
    }

    private fun validateForm(): Boolean {
        // Validate only required fields
        with(binding) {
            if (producerDropdown.text.isNullOrBlank()) {
                showError("Please select a producer")
                return false
            }

            if (seasonDropdown.text.isNullOrBlank()) {
                showError("Please select a season")
                return false
            }

            if (fieldDropdown.text.isNullOrBlank()) {
                showError("Please select a field")
                return false
            }

            if (landPrepMethodDropdown.text.isNullOrBlank()) {
                showError("Please select a land preparation method")
                return false
            }
        }

        return true
    }

    private fun submitForm() {
        if (!validateForm()) return

        loadingDialog.show()

        val landPrep = LandPreparationEntity(
            producerId = selectedProducerId!!,  // Safe because of validateForm
            seasonId = selectedSeasonId!!,      // Safe because of validateForm
            fieldNumber = selectedFieldNumber!!,  // Safe because of validateForm
            dateOfLandPreparation = startDate!!,  // Safe because of validateForm
            methodOfLandPreparation = binding.landPrepMethodDropdown.text.toString(),
            season_planning_id = selectedSeasonId?.toInt() ?: 0,  // Use snake_case to match the entity
            syncStatus = false
        )

        // Optional: Create cover crop entity if data exists
        val coverCrop = coverCropAdapter.getItems().firstOrNull()?.let { crop ->
            if (crop.coverCrop.isNotBlank()) {  // Only create if there's actual data
                CoverCropEntity(
                    landPrepId = 0,  // Will be set in repository
                    coverCrop = crop.coverCrop,
                    dateOfEstablishment = crop.dateOfEstablishment,
                    unit = crop.unit,
                    unitCost = crop.unitCost,
                    typeOfInoculant = crop.typeOfInoculant,
                    dateOfIncorporation = crop.dateOfIncorporation,
                    manDays = crop.manDays,
                    unitCostOfLabor = crop.unitCostOfLabor
                )
            } else null
        }

        // Optional: Create mulching entity if data exists
        val mulching = mulchingAdapter.getItems().firstOrNull()?.let { mulch ->
            if (mulch.typeOfMulch.isNotBlank()) {  // Only create if there's actual data
                MulchingEntity(
                    landPrepId = 0,  // Will be set in repository
                    typeOfMulch = mulch.typeOfMulch,
                    costOfMulch = mulch.costOfMulch,
                    lifeCycleOfMulchInSeasons = mulch.lifeCycleOfMulchInSeasons,
                    manDays = mulch.manDays,
                    unitCostOfLabor = mulch.unitCostOfLabor
                )
            } else null
        }

        // Optional: Create soil analysis entity if data exists
        val soilAnalysis = soilAnalysisAdapter.getItems().firstOrNull()?.let { analysis ->
            if (analysis.typeOfAnalysis.isNotBlank()) {  // Only create if there's actual data
                SoilAnalysisEntity(
                    landPrepId = 0,  // Will be set in repository
                    typeOfAnalysis = analysis.typeOfAnalysis,
                    costOfAnalysis = analysis.costOfAnalysis,
                    lab = analysis.lab
                )
            } else null
        }

        // Submit through ViewModel
        lifecycleScope.launch {
            try {
                viewModel.saveLandPreparation(
                    landPrep = landPrep,
                    coverCrop = coverCrop,
                    mulching = mulching,
                    soilAnalysis = soilAnalysis
                )
            } catch (e: Exception) {
                loadingDialog.dismiss()
                showError("Error saving land preparation: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark))
            .show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_green_dark))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

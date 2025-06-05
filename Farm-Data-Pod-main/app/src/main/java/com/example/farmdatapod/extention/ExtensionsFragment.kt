package com.example.farmdatapod.extention

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.ExtensionService
import com.example.farmdatapod.adapter.*
import com.example.farmdatapod.databinding.FragmentExtensionsBinding
import com.example.farmdatapod.models.*
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.utils.DateRangePicker
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.utils.calculateDateRange
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ExtensionsFragment : Fragment() {
    private var _binding: FragmentExtensionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkViewModel: NetworkViewModel
    private lateinit var plannedPlantingDate: String
    private var cropCycleWeeks: Int = 0

    private lateinit var dbHandler: DBHandler
    private lateinit var dateRangePicker: DateRangePicker
    private var selectedProducerId: Int? = null
    private lateinit var selectedFieldNumber: String

    // Adapters
    private lateinit var pesticideApplicationAdapter: PesticideApplicationAdapter
    private lateinit var addExtScoutingAdapter: AddExtScoutingAdapter
    private lateinit var fertiliserAdapter: FertiliserAdapter
    private lateinit var forecastAdapter: ForecastAdapter
    private lateinit var marketProduceAdapter: MarketProduceAdapter

    // Crop Management
    private lateinit var checkboxes: List<CheckBox>
    private lateinit var layouts: List<LinearLayout>

    // Crop Management Data Objects
    private val gapping = Gapping()
    private val harvesting = Harvesting()
    private val liming = Liming()
    private val soilAnalysis = SoilAnalysis()
    private val transplanting = Transplanting()
    private val prunningThinningDesuckering = PrunningThinningDesuckering()
    private val mulching = Mulching()
    private val nursery = Nursery()
    private val weeding = Weeding()

    // Crop Planning
    private var plantingDate: String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
    private var selectedProducer: String = ""
    private var selectedField: String = ""
    private var cropCycle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHandler = DBHandler(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExtensionsBinding.inflate(inflater, container, false)
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dateRangePicker = DateRangePicker(requireContext())

        setupRecyclerViews()
        setupCropManagement()
        setupCropPlanning()
        setupMarketProduceRecyclerView()
        setupAddCropButton()
        setupSubmitButton()
        setupDateCalculations()
    }
    private fun setupDateCalculations() {
        binding.plantingDateEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                plannedPlantingDate = s.toString()
                updateAllDateRanges()
            }
        })

        binding.cropCycleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                cropCycleWeeks = s.toString().toIntOrNull() ?: 0
                updateAllDateRanges()
            }
        })

        setupWeekNumberListeners()
    }

    private fun setupWeekNumberListeners() {
        val weekNumberInputs = listOf(
            binding.nurseryWeekNumber,
            binding.limingWeekNumber,
            binding.soilAnalysisWeekNumber,
            binding.transplantingWeekNumber,
            binding.mulchingWeekNumber,
            binding.pruningWeekNumber,
            binding.harvestingWeekNumber
        )

        weekNumberInputs.forEach { input ->
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateDateRange(input)
                }
            })
        }
    }

    private fun updateDateRange(weekNumberInput: EditText) {
        val weekNumber = weekNumberInput.text.toString().toIntOrNull()
        if (weekNumber != null && weekNumber > 0 && weekNumber <= cropCycleWeeks) {
            val dateRange = calculateDateRange(plannedPlantingDate, weekNumber)
            when (weekNumberInput) {
                binding.nurseryWeekNumber -> binding.nurseryDateRange.setText(dateRange)
                binding.limingWeekNumber -> binding.limingDateRange.setText(dateRange)
                binding.soilAnalysisWeekNumber -> binding.soilAnalysisDateRange.setText(dateRange)
                binding.transplantingWeekNumber -> binding.transplantingDateRange.setText(dateRange)
                binding.mulchingWeekNumber -> binding.mulchingDateRange.setText(dateRange)
                binding.pruningWeekNumber -> binding.pruningDateRange.setText(dateRange)
                binding.harvestingWeekNumber -> binding.harvestingDateRange.setText(dateRange)
            }
        }
    }

    private fun updateAllDateRanges() {
        if (plannedPlantingDate.isNotEmpty() && cropCycleWeeks > 0) {
            updateDateRange(binding.nurseryWeekNumber)
            updateDateRange(binding.limingWeekNumber)
            updateDateRange(binding.soilAnalysisWeekNumber)
            updateDateRange(binding.transplantingWeekNumber)
            updateDateRange(binding.mulchingWeekNumber)
            updateDateRange(binding.pruningWeekNumber)
            updateDateRange(binding.harvestingWeekNumber)
        }
    }


    private fun setupRecyclerViews() {
        setupPesticideApplicationRecyclerView()
        setupScoutingRecyclerView()
        setupFertiliserRecyclerView()
        setupForecastRecyclerView()
    }

    private fun setupPesticideApplicationRecyclerView() {
        pesticideApplicationAdapter = PesticideApplicationAdapter(binding.pesticideApplicationFormRecyclerView)
        binding.pesticideApplicationFormRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pesticideApplicationAdapter
        }
        pesticideApplicationAdapter.addPesticideUsage() // Add initial form
        binding.addPesticideApplicationFormButton.setOnClickListener {
            pesticideApplicationAdapter.addPesticideUsage()
        }
    }

    private fun setupMarketProduceRecyclerView() {
        marketProduceAdapter = MarketProduceAdapter(binding.cropRecyclerView)
        binding.cropRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = marketProduceAdapter
        }
        marketProduceAdapter.addMarketProduce() // Add initial form
    }

    private fun setupAddCropButton() {
        binding.addCropButton.setOnClickListener {
            marketProduceAdapter.addMarketProduce()
        }
    }

    private fun setupScoutingRecyclerView() {
        addExtScoutingAdapter = AddExtScoutingAdapter()
        binding.scoutingFormRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = addExtScoutingAdapter
        }
        addExtScoutingAdapter.addForm() // Add initial form
        binding.addScoutingButton.setOnClickListener {
            addExtScoutingAdapter.addForm()
        }
    }

    private fun setupFertiliserRecyclerView() {
        fertiliserAdapter = FertiliserAdapter(binding.fertiliserUsedFormRecyclerView)
        binding.fertiliserUsedFormRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = fertiliserAdapter
        }
        fertiliserAdapter.addFertiliserUsage() // Add initial form
        binding.addFertiliserFormButton.setOnClickListener {
            fertiliserAdapter.addFertiliserUsage()
        }
    }

    private fun setupForecastRecyclerView() {
        forecastAdapter = ForecastAdapter(binding.forecastFormRecyclerView)
        binding.forecastFormRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = forecastAdapter
        }
        forecastAdapter.addForecastYield() // Add initial form
        binding.addForecastFormButton.setOnClickListener {
            forecastAdapter.addForecastYield()
        }
    }

    private fun setupCropManagement() {
        setupDateRangePickers()
        initializeCheckboxesAndLayouts()
        setupCropManagementListeners()
        setupDataCollectors()
    }

    private fun setupDateRangePickers() {
        val dateRangeFields = listOf(
            binding.nurseryDateRange,
            binding.soilAnalysisDateRange,
            binding.limingDateRange,
            binding.transplantingDateRange,
            binding.mulchingDateRange,
            binding.pruningDateRange,
            binding.harvestingDateRange
        )

        dateRangeFields.forEach { editText ->
            editText.setOnClickListener {
                dateRangePicker.show(editText)
            }
        }
    }

    private fun initializeCheckboxesAndLayouts() {
        checkboxes = listOf(
            binding.checkboxNursery,
            binding.checkboxSoilAnalysis,
            binding.checkboxLiming,
            binding.checkboxTransplanting,
            binding.checkboxMulching,
            binding.checkboxPruning,
            binding.checkboxHarvesting
        )

        layouts = listOf(
            binding.layoutNursery,
            binding.layoutSoilAnalysis,
            binding.layoutLiming,
            binding.layoutTransplanting,
            binding.layoutMulching,
            binding.layoutPruning,
            binding.layoutHarvesting
        )
    }

    private fun setupCropManagementListeners() {
        checkboxes.forEachIndexed { index, checkbox ->
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                layouts[index].visibility = if (isChecked) View.VISIBLE else View.GONE
                validateAtLeastOneChecked()
            }
        }
    }

    private fun setupDataCollectors() {
        // Nursery
        binding.nurseryWeekNumber.addTextChangedListener { nursery.week_number = it.toString() }
        binding.nurseryDateRange.addTextChangedListener { nursery.date_range = it.toString() }
        binding.mansDaysInput.addTextChangedListener {
            nursery.man_days = it.toString().toIntOrNull() ?: 0
        }
        binding.unitCostInput.addTextChangedListener { nursery.unit_cost = it.toString() }

        // Liming
        binding.limingWeekNumber.addTextChangedListener { liming.week_number = it.toString() }
        binding.limingDateRange.addTextChangedListener { liming.date_range = it.toString() }
        binding.mansDaysInputLiming.addTextChangedListener { liming.man_days = it.toString() }
        binding.unitCostInputLiming.addTextChangedListener { liming.unit_cost = it.toString() }
        binding.numberOfUnitsInput.addTextChangedListener { liming.number_of_units = it.toString() }
        binding.quantityPerUnitInput.addTextChangedListener { liming.start_date = it.toString() }
        binding.costPerUnitInput.addTextChangedListener { liming.cost_per_unit = it.toString() }

        // Soil Analysis
        binding.soilAnalysisWeekNumber.addTextChangedListener {
            soilAnalysis.week_number = it.toString()
        }
        binding.soilAnalysisDateRange.addTextChangedListener {
            soilAnalysis.date_range = it.toString()
        }
        binding.mansDaysInputSoilAnalysis.addTextChangedListener {
            soilAnalysis.man_days = it.toString()
        }
        binding.unitCostInputSoilAnalysis.addTextChangedListener {
            soilAnalysis.unit_cost = it.toString()
        }
        binding.typeOfAnalysisInput.addTextChangedListener {
            soilAnalysis.type_of_analysis = it.toString()
        }

        // Transplanting
        binding.transplantingWeekNumber.addTextChangedListener {
            transplanting.week_number = it.toString()
        }
        binding.transplantingDateRange.addTextChangedListener {
            transplanting.date_range = it.toString()
        }
        binding.mansDaysInputTransplanting.addTextChangedListener {
            transplanting.man_days = it.toString().toIntOrNull() ?: 0
        }
        binding.unitCostInputTransplanting.addTextChangedListener {
            transplanting.unit_cost = it.toString()
        }
        binding.plantPopulationInput.addTextChangedListener {
            transplanting.plant_population = it.toString()
        }
        binding.projectedYieldInput.addTextChangedListener {
            transplanting.projected_yield = it.toString()
        }

        // Mulching
        binding.mulchingWeekNumber.addTextChangedListener { mulching.week_number = it.toString() }
        binding.mulchingDateRange.addTextChangedListener { mulching.date_range = it.toString() }
        binding.mansDaysInputMulching.addTextChangedListener {
            mulching.man_days = it.toString().toIntOrNull() ?: 0
        }
        binding.unitCostInputMulching.addTextChangedListener { mulching.unit_cost = it.toString() }

        // Pruning/Thinning/Desuckering
        binding.pruningWeekNumber.addTextChangedListener {
            prunningThinningDesuckering.week_number = it.toString()
        }
        binding.pruningDateRange.addTextChangedListener {
            prunningThinningDesuckering.date_range = it.toString()
        }
        binding.mansDaysInputPruning.addTextChangedListener {
            prunningThinningDesuckering.man_days = it.toString().toIntOrNull() ?: 0
        }
        binding.unitCostInputPruning.addTextChangedListener {
            prunningThinningDesuckering.unit_cost = it.toString()
        }

        // Harvesting
        binding.harvestingWeekNumber.addTextChangedListener {
            harvesting.week_number = it.toString()
        }
        binding.harvestingDateRange.addTextChangedListener { harvesting.date_range = it.toString() }
        binding.mansDaysInputHarvesting.addTextChangedListener {
            harvesting.man_days = it.toString().toIntOrNull() ?: 0
        }
        binding.unitCostInputHarvesting.addTextChangedListener {
            harvesting.unit_cost = it.toString()
        }
        binding.quantityHarvestedInput.addTextChangedListener {
            harvesting.plant_population = it.toString()
        }
        binding.quantitySoldInput.addTextChangedListener {
            harvesting.projected_yield = it.toString()
        }
    }

    private fun setupCropPlanning() {
        setupSpinners()
        setupDatePicker()
        setupTextWatchers()
    }

    private fun setupSpinners() {
        // Producer Spinner
        val dbHandler = DBHandler(requireContext())
        val producerNamesMap = dbHandler.getProducerNames()

        val producerAdapter = ProducerBiodataRegAdapter(requireContext(), producerNamesMap)
        binding.producerAutoComplete.setAdapter(producerAdapter)

        binding.producerAutoComplete.setOnItemClickListener { parent, view, position, id ->
            selectedProducerId = producerAdapter.getProducerId(position)
            Log.d("SelectedProducer", "Selected Producer ID: $selectedProducerId")
        }

        // Field Number spinner
//        val fieldNumbersMap = dbHandler.getFieldNumbers()

//        val fieldAdapter = FieldNumberAdapter(requireContext(), fieldNumbersMap)
//        binding.fieldAutoComplete.setAdapter(fieldAdapter)

//        binding.fieldAutoComplete.setOnItemClickListener { parent, view, position, id ->
//            // Capture the selected field number using the adapter
//            selectedFieldNumber = fieldAdapter.getFieldNumber(position).toString()
//            Log.d("SelectedField", "Selected Field Number: $selectedFieldNumber")
//        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        binding.plantingDateEditText.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    plantingDate = isoDateFormat.format(calendar.time)
                    val displayDate = displayDateFormat.format(calendar.time)
                    binding.plantingDateEditText.setText(displayDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    private fun setupTextWatchers() {
        binding.producerAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                selectedProducer = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.fieldAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                selectedField = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.cropCycleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cropCycle = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun validateAtLeastOneChecked() {
        if (!isAnyCheckboxChecked()) {
            Toast.makeText(context, "Please select at least one activity", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun isAnyCheckboxChecked(): Boolean {
        return checkboxes.any { it.isChecked }
    }

    private fun validateAllForms(): Boolean {
        val isFertilerValid = fertiliserAdapter.validateForms()
        val isScoutingValid = addExtScoutingAdapter.validateForms()
        val isForecastValid = forecastAdapter.validateForms()
        val isPestApplicationValid = pesticideApplicationAdapter.validateForms()
        val isCropManagementValid = isAnyCheckboxChecked()
        val isCropPlanningValid = selectedProducer.isNotEmpty() &&
                selectedField.isNotEmpty() &&
                plantingDate.isNotEmpty() &&
                cropCycle.isNotEmpty()

        return isFertilerValid && isScoutingValid && isForecastValid &&
                isPestApplicationValid && isCropManagementValid && isCropPlanningValid
    }

    private fun setupSubmitButton() {
        // Observe network connectivity changes
        networkViewModel.networkLiveData.observe(viewLifecycleOwner) { isNetworkAvailable ->
            Log.d("ExtensionServiceFragment", "Network status changed: $isNetworkAvailable")

            binding.submitButton.setOnClickListener {
                Log.d("ExtensionServiceFragment", "Submit button clicked")
                if (validateAllForms()) {
                    Log.d("ExtensionServiceFragment", "All forms validated. Checking network availability...")
                    if (isNetworkAvailable) {
                        Log.d("ExtensionServiceFragment", "Network available. Proceeding with online submission.")
//                        submitForms()
                    } else {
                        Log.d("ExtensionServiceFragment", "No network available. Handling offline submission.")
                        handleOfflineSubmission()
                    }
                } else {
                    Log.d("ExtensionServiceFragment", "Form validation failed")
                    Toast.makeText(context, "Please fill all required fields correctly", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleOfflineSubmission() {
        Log.d("ExtensionServiceFragment", "Handling offline submission.")

        // Debugging each adapter's data
        Log.d("ExtensionServiceFragment", "ext_scouting_stations: ${addExtScoutingAdapter.getForms()}")
        Log.d("ExtensionServiceFragment", "fertlizers_used: ${fertiliserAdapter.getForms()}")
        Log.d("ExtensionServiceFragment", "marketProduces: ${marketProduceAdapter.getMarketProduces()}")
        Log.d("ExtensionServiceFragment", "forecast_yields: ${forecastAdapter.getForms()}")
        Log.d("ExtensionServiceFragment", "pesticides_used: ${pesticideApplicationAdapter.getForms()}")

        val extensionServiceRequestModel = ExtensionServiceRequestModel(
            field = selectedFieldNumber,
            gapping = gapping,
            harvesting = harvesting,
            liming = liming,
            mulching = mulching,
            nursery = nursery,
            planned_date_of_planting = plantingDate,
            producer = selectedProducerId.toString(),
            prunning_thinning_desuckering = prunningThinningDesuckering,
            soil_analysis = soilAnalysis,
            transplanting = transplanting,
            weeding = weeding,
            week_number = cropCycle.toIntOrNull() ?: 0,
            ext_scouting_stations = addExtScoutingAdapter.getForms(),
            fertlizers_used = fertiliserAdapter.getForms(),
            marketProduces = marketProduceAdapter.getMarketProduces(),
            forecast_yields = forecastAdapter.getForms(),
            pesticides_used = pesticideApplicationAdapter.getForms()
        )

        // Log the details of the ExtensionServiceRequestModel
        Log.d("ExtensionServiceFragment", "ExtensionServiceRequestModel: ${Gson().toJson(extensionServiceRequestModel)}")

        // Create ExtensionService object from the request model
        val extensionService = ExtensionService(
            id = 0,
            producer = extensionServiceRequestModel.producer,
            fieldName = extensionServiceRequestModel.field,
            planned_date_of_planting = extensionServiceRequestModel.planned_date_of_planting,
            week_number = extensionServiceRequestModel.week_number,
            nursery = Gson().toJsonTree(extensionServiceRequestModel.nursery).asJsonObject,
            gapping = Gson().toJsonTree(extensionServiceRequestModel.gapping).asJsonObject,
            soil_analysis = Gson().toJsonTree(extensionServiceRequestModel.soil_analysis).asJsonObject,
            liming = Gson().toJsonTree(extensionServiceRequestModel.liming).asJsonObject,
            transplanting = Gson().toJsonTree(extensionServiceRequestModel.transplanting).asJsonObject,
            weeding = Gson().toJsonTree(extensionServiceRequestModel.weeding).asJsonObject,
            prunning_thinning_desuckering = Gson().toJsonTree(extensionServiceRequestModel.prunning_thinning_desuckering).asJsonObject,
            mulching = Gson().toJsonTree(extensionServiceRequestModel.mulching).asJsonObject,
            harvesting = Gson().toJsonTree(extensionServiceRequestModel.harvesting).asJsonObject,
            ext_scouting_stations = extensionServiceRequestModel.ext_scouting_stations,
            pesticides_used = extensionServiceRequestModel.pesticides_used,
            fertlizers_used = extensionServiceRequestModel.fertlizers_used,
            forecast_yields = extensionServiceRequestModel.forecast_yields,
            marketProduces = extensionServiceRequestModel.marketProduces
        )

        Log.d("ExtensionServiceFragment", "ExtensionService Offline Submit: ${Gson().toJson(extensionService)}")

        // Use DBHandler to insert the ExtensionService object
        val dbHandler = DBHandler(requireContext())
        val isInserted = try {
            dbHandler.insertExtensionService(extensionService)
        } catch (e: Exception) {
            Log.e("ExtensionServiceFragment", "Error inserting data: ${e.message}", e)
            false
        }

        // Provide feedback based on the insertion result
        if (isInserted) {
            Log.d("ExtensionServiceFragment", "Data saved offline successfully.")
            Toast.makeText(context, "Data saved offline successfully.", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("ExtensionServiceFragment", "Failed to save data offline.")
            Toast.makeText(context, "Failed to save data offline.", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun submitForms() {
//        val extensionServiceRequestModel = ExtensionServiceRequestModel(
//            ext_scouting_stations = addExtScoutingAdapter.getForms(),
//            fertlizers_used = fertiliserAdapter.getForms(),
//            field = selectedFieldNumber,
//            forecast_yields = forecastAdapter.getForms(),
//            gapping = gapping,
//            harvesting = harvesting,
//            liming = liming,
//            marketProduces = marketProduceAdapter.getMarketProduces(),
//            mulching = mulching,
//            nursery = nursery,
//            pesticides_used = pesticideApplicationAdapter.getForms(),
//            planned_date_of_planting = plantingDate,
//            producer = selectedProducerId.toString(),
//            prunning_thinning_desuckering = prunningThinningDesuckering,
//            soil_analysis = soilAnalysis,
//            transplanting = transplanting,
//            weeding = weeding,
//            week_number = cropCycle.toIntOrNull() ?: 0
//        )
//
//        context?.let { ctx ->
//            RestClient.getApiService(ctx).registerExtensionService(extensionServiceRequestModel)
//                .enqueue(object : Callback<ExtensionServiceRequestModel> {
//                    override fun onResponse(
//                        call: Call<ExtensionServiceRequestModel>,
//                        response: Response<ExtensionServiceRequestModel>
//                    ) {
//                        if (response.isSuccessful) {
//                            // Handle successful response
//                            Toast.makeText(context, "Extension service registered successfully", Toast.LENGTH_LONG).show()
//                            // You might want to clear forms or navigate to another screen here
//                        } else {
//                            // Handle error response
//                            Toast.makeText(context, "Failed to register extension service", Toast.LENGTH_LONG).show()
//                        }
//                    }
//
//                    override fun onFailure(call: Call<ExtensionServiceRequestModel>, t: Throwable) {
//                        // Handle network failure
//                        Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
//                    }
//                })
//        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
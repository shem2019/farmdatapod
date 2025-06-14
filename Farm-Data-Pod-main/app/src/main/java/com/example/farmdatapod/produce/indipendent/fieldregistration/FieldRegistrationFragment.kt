package com.example.farmdatapod.produce.indipendent.fieldregistration

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.adapter.FieldCrop
import com.example.farmdatapod.adapter.FieldCropAdapter
import com.example.farmdatapod.databinding.FragmentFieldRegistrationBinding
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.CropEntity
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationEntity
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationRepository
import com.example.farmdatapod.utils.NetworkUtils
import com.example.farmdatapod.utils.SharedPrefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FieldRegistrationFragment : Fragment() {

    private var _binding: FragmentFieldRegistrationBinding? = null
    private val binding get() = _binding!!
    private lateinit var cropAdapter: FieldCropAdapter
    private lateinit var producerRepository: ProducerRepository
    private lateinit var fieldRegistrationRepository: FieldRegistrationRepository
    private var selectedProducerCode: String? = null
    private var selectedProducerId: Int? = null
    private var producersJob: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldRegistrationBinding.inflate(inflater, container, false)
        producerRepository = ProducerRepository(requireContext())
        fieldRegistrationRepository = FieldRegistrationRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        loadProducers()
    }

    private fun loadProducers() {
        producersJob?.cancel()
        producersJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                producerRepository.getAllProducers()
                    .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                    .collect { producers ->
                        if (isAdded && context != null) {
                            Log.d("FieldRegistration", "Loading producers: ${producers.size}")
                            val producerNames = producers.map { "${it.otherName} ${it.lastName} (${it.farmerCode})" }

                            val producerAdapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                producerNames
                            )
                            binding.producerAutoCompleteTextView.setAdapter(producerAdapter)

                            binding.producerAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                                val selectedProducer = producers[position]
                                selectedProducerId = selectedProducer.id
                                selectedProducerCode = selectedProducer.farmerCode
                                Log.d("FieldRegistration", "Selected producer - ID: $selectedProducerId, Code: $selectedProducerCode")
                                binding.producerLayout.error = null
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

    private fun setupUI() {
        setupBackButton()
        setupSubmitButton()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showPlantingDatePicker(position: Int) {
        val currentCrop = cropAdapter.getCrops()[position]
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 8) // Set a default time
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }

                // FIX: Correct date format
                val apiPlantingDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(selectedCalendar.time)
                val displayPlantingDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(selectedCalendar.time)

                val updatedCrop = currentCrop.copy(
                    plantingDate = apiPlantingDate,
                    displayPlantingDate = displayPlantingDate
                )
                cropAdapter.updateCrop(position, updatedCrop)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showHarvestDatePicker(position: Int) {
        val currentCrop = cropAdapter.getCrops()[position]
        val calendar = Calendar.getInstance()

        if (currentCrop.plantingDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please select planting date first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // FIX: Correct date format for parsing
            val plantingDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .parse(currentCrop.plantingDate)

            if (plantingDate != null) {
                calendar.time = plantingDate
            }

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, 8) // Set a default time
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }

                    if (selectedCalendar.time.before(plantingDate) || selectedCalendar.time == plantingDate) {
                        Toast.makeText(requireContext(), "Harvest date must be after planting date", Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }

                    // FIX: Correct date format
                    val apiHarvestDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(selectedCalendar.time)
                    val displayHarvestDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(selectedCalendar.time)

                    val updatedCrop = currentCrop.copy(
                        harvestDate = apiHarvestDate,
                        displayHarvestDate = displayHarvestDate
                    )
                    cropAdapter.updateCrop(position, updatedCrop)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = (plantingDate.time + 86400000) // Add one day
            }.show()
        } catch (e: Exception) {
            Log.e("DatePicker", "Error setting harvest date", e)
            Toast.makeText(requireContext(), "Error setting harvest date. Please select planting date again.", Toast.LENGTH_LONG).show()
        }
    }


    private fun setupRecyclerView() {
        cropAdapter = FieldCropAdapter(
            onEditClick = { crop, position ->
                showEditDialog(crop, position)
            },
            onDeleteClick = { _, position ->
                showDeleteConfirmation(position)
            },
            onPlantingDateClick = { position ->
                showPlantingDatePicker(position)
            },
            onHarvestDateClick = { position ->
                showHarvestDatePicker(position)
            }
        )

        binding.cropsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cropAdapter
            setHasFixedSize(true)
        }

        addInitialForm()
        setupAddCropButton()
    }

    private fun addInitialForm() {
        val initialCrop = FieldCrop(
            cropName = "",
            variety = "",
            plantingDate = "",
            harvestDate = "",
            plantPopulation = "",
            baselineYield = 0.0,
            baselineIncome = "",
            baselineCost = ""
        )
        cropAdapter.addCrop(initialCrop)
    }

    private fun setupAddCropButton() {
        binding.addCropButton.setOnClickListener {
            val newCrop = FieldCrop(
                cropName = "",
                variety = "",
                plantingDate = "",
                harvestDate = "",
                plantPopulation = "",
                baselineYield = 0.0,
                baselineIncome = "",
                baselineCost = ""
            )
            cropAdapter.addCrop(newCrop)
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateMainForm()) {
                submitFieldRegistration()
            }
        }
    }

    private fun validateMainForm(): Boolean {
        var isValid = true
        binding.apply {
            if (producerAutoCompleteTextView.text.isNullOrBlank() || selectedProducerCode == null) {
                producerLayout.error = "Please select a producer from the list"
                isValid = false
            } else {
                producerLayout.error = null
            }

            if (fieldSizeEditText.text.isNullOrBlank()) {
                fieldSizeLayout.error = "Please enter field size"
                isValid = false
            } else {
                val fieldSize = fieldSizeEditText.text.toString().toFloatOrNull()
                if (fieldSize == null || fieldSize <= 0) {
                    fieldSizeLayout.error = "Field size must be a number greater than 0"
                    isValid = false
                } else {
                    fieldSizeLayout.error = null
                }
            }

            if (fieldNumberEditText.text.isNullOrBlank()) {
                fieldNumberLayout.error = "Please enter field number"
                isValid = false
            } else {
                val fieldNumber = fieldNumberEditText.text.toString().toIntOrNull()
                if (fieldNumber == null || fieldNumber <= 0) {
                    fieldNumberLayout.error = "Field number must be a number greater than 0"
                    isValid = false
                } else {
                    fieldNumberLayout.error = null
                }
            }
        }

        if (cropAdapter.getItemCount() == 0) {
            Toast.makeText(context, "Please add at least one crop", Toast.LENGTH_SHORT).show()
            isValid = false
        } else {
            val crops = cropAdapter.getCrops()
            for ((index, crop) in crops.withIndex()) {
                if (crop.cropName.trim().isEmpty() ||
                    crop.variety.trim().isEmpty() ||
                    crop.plantingDate.isEmpty() ||
                    crop.harvestDate.isEmpty() ||
                    crop.plantPopulation.trim().isEmpty() ||
                    crop.plantPopulation.trim().toIntOrNull() == null ||
                    crop.baselineIncome.trim().isEmpty() ||
                    crop.baselineIncome.trim().toDoubleOrNull() == null ||
                    crop.baselineCost.trim().isEmpty() ||
                    crop.baselineCost.trim().toDoubleOrNull() == null) {
                    Toast.makeText(
                        context,
                        "Please fill all fields with valid numbers for crop ${index + 1}",
                        Toast.LENGTH_LONG
                    ).show()
                    isValid = false
                    break
                }
            }
        }

        return isValid
    }

    private fun submitFieldRegistration() {
        val producerCode = selectedProducerCode ?: return
        val sharedPrefs = SharedPrefs(requireContext())
        val userId = sharedPrefs.getUserId() ?: "default_user"

        // FIX: Convert to correct types
        val fieldRegistration = FieldRegistrationEntity(
            producerId = producerCode,
            fieldNumber = binding.fieldNumberEditText.text.toString().toInt(),
            fieldSize = binding.fieldSizeEditText.text.toString().toFloat(),
            userId = userId,
            syncStatus = false
        )

        val crops = cropAdapter.getCrops()
        // FIX: Convert to correct types
        val cropEntities = crops.mapNotNull { fieldCrop ->
            try {
                CropEntity(
                    fieldRegistrationId = 0,
                    cropName = fieldCrop.cropName.trim(),
                    cropVariety = fieldCrop.variety.trim(),
                    datePlanted = fieldCrop.plantingDate,
                    dateOfHarvest = fieldCrop.harvestDate,
                    population = fieldCrop.plantPopulation.trim().toInt(),
                    baselineYield = fieldCrop.baselineYield,
                    baselineIncome = fieldCrop.baselineIncome.trim().toDouble(),
                    baselineCost = fieldCrop.baselineCost.trim().toDouble(),
                    sold = false, // Default value
                    syncStatus = false
                )
            } catch (e: NumberFormatException) {
                Log.e("FieldRegistration", "Invalid number format in crop data", e)
                null
            }
        }

        if (crops.size != cropEntities.size) {
            Toast.makeText(requireContext(), "Please check that all numbers are entered correctly.", Toast.LENGTH_LONG).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
            val result = fieldRegistrationRepository.saveFieldRegistration(
                fieldRegistration,
                cropEntities,
                isOnline
            )
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(
                        requireContext(),
                        if (isOnline) "Field registration saved and sync initiated."
                        else "Field registration saved locally (offline).",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigateUp()
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("FieldRegistration", "Submission failed", error)
                    Toast.makeText(
                        requireContext(),
                        "Error: ${error?.message ?: "An unknown error occurred."}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun showEditDialog(crop: FieldCrop, position: Int) {
        cropAdapter.notifyItemChanged(position)
    }

    private fun showDeleteConfirmation(position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Crop")
            .setMessage("Are you sure you want to delete this crop?")
            .setPositiveButton("Delete") { _, _ ->
                cropAdapter.deleteCrop(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.cropsRecyclerView?.adapter = null
        producersJob?.cancel()
        _binding = null
    }

    private fun resetForm() {
        selectedProducerId = null
        selectedProducerCode = null
        binding.producerAutoCompleteTextView.setText("", false)
        binding.producerLayout.error = null

        binding.fieldSizeEditText.setText("")
        binding.fieldSizeLayout.error = null
        binding.fieldNumberEditText.setText("")
        binding.fieldNumberLayout.error = null

        cropAdapter.clearCrops()
        addInitialForm()

        Toast.makeText(requireContext(), "Form has been reset", Toast.LENGTH_SHORT).show()
    }
}
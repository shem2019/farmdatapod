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
        fieldRegistrationRepository = FieldRegistrationRepository(requireContext())  // Add this line
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
                            // Create a list of producer display names
                            val producerNames = producers.map { "${it.otherName} ${it.lastName} (${it.farmerCode})" }

                            val producerAdapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                producerNames
                            )
                            binding.producerAutoCompleteTextView.setAdapter(producerAdapter)

                            // Set up item click listener
                            binding.producerAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                                val selectedProducer = producers[position]
                                selectedProducerId = selectedProducer.id
                                selectedProducerCode = selectedProducer.farmerCode
                                Log.d("FieldRegistration", "Selected producer - ID: $selectedProducerId, Code: $selectedProducerCode")

                                // Clear any previous error
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
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }

                // Format for API
                val apiPlantingDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .format(selectedCalendar.time)
                // Format for display
                val displayPlantingDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(selectedCalendar.time)

                // Update the crop with both formats
                val updatedCrop = currentCrop.copy(
                    plantingDate = apiPlantingDate,
                    displayPlantingDate = displayPlantingDate
                )
                cropAdapter.updateCrop(position, updatedCrop)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() // Set minimum date to today
        }.show()
    }

    private fun showHarvestDatePicker(position: Int) {
        val currentCrop = cropAdapter.getCrops()[position]
        val calendar = Calendar.getInstance()

        if (currentCrop.plantingDate.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please select planting date first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val plantingDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(currentCrop.plantingDate)

            if (plantingDate != null) {
                calendar.time = plantingDate
            }

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }

                    if (selectedCalendar.time <= plantingDate) {
                        Toast.makeText(
                            requireContext(),
                            "Harvest date must be after planting date",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@DatePickerDialog
                    }

                    // Format for API
                    val apiHarvestDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .format(selectedCalendar.time)
                    // Format for display
                    val displayHarvestDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(selectedCalendar.time)

                    // Update the crop with both formats
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
                datePicker.minDate = (plantingDate.time + 86400000) // Add one day in milliseconds
            }.show()
        } catch (e: Exception) {
            Log.e("DatePicker", "Error setting harvest date", e)
            Toast.makeText(
                requireContext(),
                "Error setting harvest date",
                Toast.LENGTH_SHORT
            ).show()
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
            if (producerAutoCompleteTextView.text.isNullOrBlank()) {
                producerLayout.error = "Please select a producer"
                isValid = false
            } else if (selectedProducerId == null || selectedProducerCode == null) {
                producerLayout.error = "Please select a valid producer from the list"
                isValid = false
            } else {
                producerLayout.error = null
            }

            if (fieldSizeEditText.text.isNullOrBlank()) {
                fieldSizeLayout.error = "Please enter field size"
                isValid = false
            } else {
                try {
                    val fieldSize = fieldSizeEditText.text.toString().toFloat()
                    if (fieldSize <= 0) {
                        fieldSizeLayout.error = "Field size must be greater than 0"
                        isValid = false
                    } else {
                        fieldSizeLayout.error = null
                    }
                } catch (e: NumberFormatException) {
                    fieldSizeLayout.error = "Please enter a valid number"
                    isValid = false
                }
            }

            if (fieldNumberEditText.text.isNullOrBlank()) {
                fieldNumberLayout.error = "Please enter field number"
                isValid = false
            } else {
                try {
                    val fieldNumber = fieldNumberEditText.text.toString().toInt()
                    if (fieldNumber <= 0) {
                        fieldNumberLayout.error = "Field number must be greater than 0"
                        isValid = false
                    } else {
                        fieldNumberLayout.error = null
                    }
                } catch (e: NumberFormatException) {
                    fieldNumberLayout.error = "Please enter a valid number"
                    isValid = false
                }
            }
        }

        if (cropAdapter.getItemCount() == 0) {
            Toast.makeText(context, "Please add at least one crop", Toast.LENGTH_SHORT).show()
            isValid = false
        } else {
            // Validate crop data
            val crops = cropAdapter.getCrops()
            crops.forEachIndexed { index, crop ->
                if (crop.cropName.trim().isEmpty() ||
                    crop.variety.trim().isEmpty() ||
                    crop.plantingDate.isEmpty() ||
                    crop.harvestDate.isEmpty() ||
                    crop.plantPopulation.trim().isEmpty() ||
                    crop.baselineIncome.trim().isEmpty() ||
                    crop.baselineCost.trim().isEmpty()
                ) {
                    Toast.makeText(
                        context,
                        "Please fill all fields for crop ${index + 1}",
                        Toast.LENGTH_SHORT
                    ).show()
                    isValid = false
                    return@forEachIndexed
                }
            }
        }

        return isValid
    }


    private fun submitFieldRegistration() {
        if (selectedProducerCode == null) {
            Toast.makeText(requireContext(), "Please select a valid producer", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPrefs = SharedPrefs(requireContext())
        val userId = sharedPrefs.getUserId()

        val fieldRegistration = FieldRegistrationEntity(
            producerId = selectedProducerCode!!,  // Using the farmer code
            fieldNumber = binding.fieldNumberEditText.text.toString().toInt(),
            fieldSize = binding.fieldSizeEditText.text.toString(),
            userId = userId, // Get from SharedPreferences
            syncStatus = false
        )

        // Validate that all crops have both dates before submitting
        val crops = cropAdapter.getCrops()
        val invalidCrops = crops.filter { it.plantingDate.isEmpty() || it.harvestDate.isEmpty() }
        if (invalidCrops.isNotEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please set both planting and harvest dates for all crops",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val cropEntities = crops.map { fieldCrop ->
            CropEntity(
                fieldRegistrationId = 0, // Will be updated after registration is saved
                cropName = fieldCrop.cropName.trim(),
                cropVariety = fieldCrop.variety.trim(),
                datePlanted = fieldCrop.plantingDate,
                dateOfHarvest = fieldCrop.harvestDate,
                population = fieldCrop.plantPopulation.trim(),
                baselineYield = fieldCrop.baselineYield,
                baselineIncome = fieldCrop.baselineIncome.trim(),
                baselineCost = fieldCrop.baselineCost.trim(),
                syncStatus = false
            )
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val isOnline = NetworkUtils.isNetworkAvailable(requireContext())

                val result = fieldRegistrationRepository.saveFieldRegistration(
                    fieldRegistration,
                    cropEntities,
                    isOnline
                )

                withContext(Dispatchers.Main) {
                    when {
                        result.isSuccess -> {
                            Toast.makeText(
                                requireContext(),
                                if (isOnline) "Field registration saved and synced"
                                else "Field registration saved (offline)",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        }
                        result.isFailure -> {
                            val errorMessage = when (val error = result.exceptionOrNull()) {
                                is retrofit2.HttpException -> {
                                    when (error.code()) {
                                        404 -> "Producer not found"
                                        401 -> "Authentication error"
                                        else -> "Server error: ${error.message()}"
                                    }
                                }
                                else -> error?.message ?: "Unknown error occurred"
                            }
                            Toast.makeText(
                                requireContext(),
                                "Error: $errorMessage",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        else -> {
                            Toast.makeText(
                                requireContext(),
                                "Unknown error occurred",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = when (e) {
                        is IllegalStateException -> "Please try again"
                        else -> e.message ?: "Unknown error occurred"
                    }
                    Toast.makeText(
                        requireContext(),
                        "Error: $errorMessage",
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
        _binding?.cropsRecyclerView?.adapter = null  // Add null check
        producersJob?.cancel()
        selectedProducerId = null
        selectedProducerCode = null
        _binding?.producerAutoCompleteTextView?.setAdapter(null)
        producersJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
    private fun resetForm() {
        // Reset producer fields
        selectedProducerId = null
        selectedProducerCode = null
        binding.producerAutoCompleteTextView.setText("")
        binding.producerLayout.error = null

        // Reset field size and number
        binding.fieldSizeEditText.setText("")
        binding.fieldSizeLayout.error = null
        binding.fieldNumberEditText.setText("")
        binding.fieldNumberLayout.error = null

        // Reset crops
        cropAdapter.clearCrops() // This clears all crops

        // Add a fresh initial crop form
        addInitialForm()

        // Reset any error states
        binding.apply {
            producerLayout.error = null
            fieldSizeLayout.error = null
            fieldNumberLayout.error = null
        }

        // Notify the user
        Toast.makeText(requireContext(), "Form has been reset", Toast.LENGTH_SHORT).show()
    }
}
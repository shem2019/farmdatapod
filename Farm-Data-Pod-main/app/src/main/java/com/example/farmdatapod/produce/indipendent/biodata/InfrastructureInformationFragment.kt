package com.example.farmdatapod.produce.indipendent.biodata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentInfrastructureInformationBinding
import com.google.android.material.snackbar.Snackbar

class InfrastructureInformationFragment : Fragment() {

    private var _binding: FragmentInfrastructureInformationBinding? = null
    private val binding get() = _binding!!
    private lateinit var ppesRadioGroup: RadioGroup
    private lateinit var irrigationPumpRadioGroup: RadioGroup

    // Use SharedViewModel to share data across fragments
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfrastructureInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ppesRadioGroup = binding.ppeRadioGroup
        irrigationPumpRadioGroup = binding.irrigationPumpRadioGroup

        ppesRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.ppeYesRadioButton -> {
                    binding.ppesCheckboxes.visibility = View.VISIBLE
                }
                else -> {
                    binding.ppesCheckboxes.visibility = View.GONE
                }
            }
        }

        irrigationPumpRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.irrigationPumpYesRadioButton -> {
                    binding.irrigationPumpCheckboxes.visibility = View.VISIBLE
                }
                else -> {
                    binding.irrigationPumpCheckboxes.visibility = View.GONE
                }
            }
        }

        setupDropdowns()
        setupButtonListeners()
    }

    private fun setupDropdowns() {
        val dropdowns = listOf(
            binding.housingTypeAutoCompleteTextView to R.array.housing_types,
            binding.housingFloorAutoCompleteTextView to R.array.housing_floor_types,
            binding.housingRoofAutoCompleteTextView to R.array.housing_roof_types,
            binding.lightingFuelAutoCompleteTextView to R.array.lighting_fuel_types,
            binding.cookingFuelAutoCompleteTextView to R.array.cooking_fuel_types,
            binding.harvestingEquipmentAutoCompleteTextView to R.array.harvesting_equipment_types,
            binding.transportationTypeAutoCompleteTextView to R.array.transportation_types,
            binding.toiletFloorAutoCompleteTextView to R.array.toilet_floor_types
        )

        dropdowns.forEach { (autoCompleteTextView, arrayResId) ->
            val adapter = ArrayAdapter.createFromResource(
                requireContext(),
                arrayResId,
                android.R.layout.simple_dropdown_item_1line
            )
            autoCompleteTextView.setAdapter(adapter)
        }
    }

    private fun setupButtonListeners() {
        binding.backButton.setOnClickListener {
            // Handle back navigation
            // For example: findNavController().navigateUp()
        }

    }

    fun validateForm(): Boolean {
        var isValid = true

        // Validate dropdowns
        val dropdowns = listOf(
            binding.housingTypeInputLayout,
            binding.housingFloorInputLayout,
            binding.housingRoofInputLayout,
            binding.lightingFuelInputLayout,
            binding.cookingFuelInputLayout,
            binding.harvestingEquipmentInputLayout,
            binding.transportationTypeInputLayout,
            binding.toiletFloorInputLayout
        )

        dropdowns.forEach { inputLayout ->
            if (inputLayout.editText?.text.isNullOrBlank()) {
                inputLayout.error = "This field is required"
                isValid = false
            } else {
                inputLayout.error = null
            }
        }

        // Validate radio buttons
        val radioGroups = listOf(
            binding.waterFilterRadioGroup to "Water Filter",
            binding.waterTankRadioGroup to "Water Tank",
            binding.handWashingRadioGroup to "Hand Washing Facilities",
            binding.ppeRadioGroup to "PPE's",
            binding.waterWellsRadioGroup to "Water Wells or Weirs",
            binding.irrigationPumpRadioGroup to "Irrigation Pump"
        )

        radioGroups.forEach { (radioGroup, name) ->
            if (radioGroup.checkedRadioButtonId == -1) {
                Snackbar.make(binding.root, "$name selection is required", Snackbar.LENGTH_LONG).show()
                isValid = false
            }
        }

        return isValid
    }

    fun saveData() {
        // Collect data from form
        val ppesSelected = if (binding.ppeYesRadioButton.isChecked) {
            val ppesCheckboxes = listOf(
                binding.maskCheckBox,
                binding.glovesCheckBox,
                binding.overallsCheckBox,
                binding.gogglesCheckBox,
                binding.gumbootsCheckBox
            )
            ppesCheckboxes.filter { it.isChecked }.joinToString(", ") { it.text.toString() }
        } else {
            "No"
        }

        val irrigationPumpSelected = if (binding.irrigationPumpYesRadioButton.isChecked) {
            val irrigationPumpCheckboxes = listOf(
                binding.electricCheckBox,
                binding.solarCheckBox,
                binding.dieselOrPetrolCheckBox,
                binding.kineticCheckBox
            )
            irrigationPumpCheckboxes.filter { it.isChecked }.joinToString(", ") { it.text.toString() }
        } else {
            "No"
        }

        val infrastructureData = mapOf(
            "Housing Type" to binding.housingTypeAutoCompleteTextView.text.toString(),
            "Housing Floor" to binding.housingFloorAutoCompleteTextView.text.toString(),
            "Housing Roof" to binding.housingRoofAutoCompleteTextView.text.toString(),
            "Lighting Fuel" to binding.lightingFuelAutoCompleteTextView.text.toString(),
            "Cooking Fuel" to binding.cookingFuelAutoCompleteTextView.text.toString(),
            "Harvesting Equipment" to binding.harvestingEquipmentAutoCompleteTextView.text.toString(),
            "Transportation Type" to binding.transportationTypeAutoCompleteTextView.text.toString(),
            "Toilet Floor" to binding.toiletFloorAutoCompleteTextView.text.toString(),
            "Water Filter" to if (binding.waterFilterYesRadioButton.isChecked) "Yes" else "No",
            "Water Tank > 5000L" to if (binding.waterTankYesRadioButton.isChecked) "Yes" else "No",
            "Hand Washing Facilities" to if (binding.handWashingYesRadioButton.isChecked) "Yes" else "No",
            "PPE's" to ppesSelected,
            "Water Wells or Weirs" to if (binding.waterWellsYesRadioButton.isChecked) "Yes" else "No",
            "Irrigation Pump" to irrigationPumpSelected
        )

        // Save this data to the ViewModel
        sharedViewModel.setInfrastructureData(infrastructureData)

        Snackbar.make(binding.root, "Data saved successfully", Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
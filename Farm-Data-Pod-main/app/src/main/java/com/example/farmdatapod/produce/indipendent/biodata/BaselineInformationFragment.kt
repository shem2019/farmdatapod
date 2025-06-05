package com.example.farmdatapod.produce.indipendent.biodata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentBaselineInformationBinding
import com.google.android.material.snackbar.Snackbar

class BaselineInformationFragment : Fragment() {

    private var _binding: FragmentBaselineInformationBinding? = null
    private val binding get() = _binding!!
    private lateinit var extensionServiceRadioGroup: RadioGroup
    private lateinit var accessToIrrigationRadioGroup: RadioGroup

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBaselineInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        extensionServiceRadioGroup = binding.extensionServicesRadioGroup
        accessToIrrigationRadioGroup = binding.accessToIrrigationRadioGroup

        setupAutoCompleteTextViews()
        setupButtonListeners()

        extensionServiceRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.extensionPartialRadioButton -> {
                    // Show all fields
                    binding.specifyExtensionServiceInputLayout.visibility = View.VISIBLE
                }
                else -> {
                    // Hide all fields
                    binding.specifyExtensionServiceInputLayout.visibility = View.GONE
                }
            }
        }

        accessToIrrigationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.irrigationOtherRadioButton -> {
                    // Show all fields
                    binding.specifyOtherTypeOfIrrigationInputLayout.visibility = View.VISIBLE
                    binding.accessToIrrigationCheckboxes.visibility = View.GONE
                }
                R.id.irrigationYesRadioButton -> {
                    // Show all fields
                    binding.accessToIrrigationCheckboxes.visibility = View.VISIBLE
                    binding.specifyOtherTypeOfIrrigationInputLayout.visibility = View.GONE
                }
                else -> {
                    // Hide all fields
                    binding.accessToIrrigationCheckboxes.visibility = View.GONE
                    binding.specifyOtherTypeOfIrrigationInputLayout.visibility = View.GONE
                }
            }
        }
    }

    private fun setupAutoCompleteTextViews() {
        val farmAccessibilityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.farm_accessibility_options)
        )
        binding.farmAccessibilityAutoCompleteTextView.setAdapter(farmAccessibilityAdapter)

        val cropListAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.crop_list_options)
        )
        binding.cropListAutoCompleteTextView.setAdapter(cropListAdapter)
    }

    private fun setupButtonListeners() {
        binding.backButton.setOnClickListener {
            // Handle back button click
        }
    }

    fun validateForm(): Boolean {
        var isValid = true

        // Validate Total Land Size
        if (binding.totalLandSizeEditText.text.isNullOrBlank()) {
            binding.totalLandSizeInputLayout.error = "Total Land Size is required"
            isValid = false
        } else {
            binding.totalLandSizeInputLayout.error = null
        }

        // Validate Uncultivated Land Size
        if (binding.uncultivatedLandSizeEditText.text.isNullOrBlank()) {
            binding.uncultivatedLandSizeInputLayout.error = "Uncultivated Land Size is required"
            isValid = false
        } else {
            binding.uncultivatedLandSizeInputLayout.error = null
        }

        // Validate Cultivated Land Size
        if (binding.cultivatedLandSizeEditText.text.isNullOrBlank()) {
            binding.cultivatedLandSizeInputLayout.error = "Cultivated Land Size is required"
            isValid = false
        } else {
            binding.cultivatedLandSizeInputLayout.error = null
        }

        // Validate Homestead Size
        if (binding.homesteadSizeEditText.text.isNullOrBlank()) {
            binding.homesteadSizeInputLayout.error = "Homestead Size is required"
            isValid = false
        } else {
            binding.homesteadSizeInputLayout.error = null
        }

        // Validate Farm Accessibility
        if (binding.farmAccessibilityAutoCompleteTextView.text.isNullOrBlank()) {
            binding.farmAccessibilityInputLayout.error = "Farm Accessibility is required"
            isValid = false
        } else {
            binding.farmAccessibilityInputLayout.error = null
        }

        // Validate Crop List
        if (binding.cropListAutoCompleteTextView.text.isNullOrBlank()) {
            binding.cropListInputLayout.error = "Crop List is required"
            isValid = false
        } else {
            binding.cropListInputLayout.error = null
        }

        // Validate Access to Irrigation
        if (binding.accessToIrrigationRadioGroup.checkedRadioButtonId == -1) {
            Snackbar.make(binding.root, "Please select Access to Irrigation", Snackbar.LENGTH_LONG).show()
            isValid = false
        }

        // Validate Extension Services
        if (binding.extensionServicesRadioGroup.checkedRadioButtonId == -1) {
            Snackbar.make(binding.root, "Please select Extension Services option", Snackbar.LENGTH_LONG).show()
            isValid = false
        }

        // Validate land sizes
        val totalLandSize = binding.totalLandSizeEditText.text.toString().toFloatOrNull() ?: 0f
        val uncultivatedLandSize = binding.uncultivatedLandSizeEditText.text.toString().toFloatOrNull() ?: 0f
        val cultivatedLandSize = binding.cultivatedLandSizeEditText.text.toString().toFloatOrNull() ?: 0f
        val homesteadSize = binding.homesteadSizeEditText.text.toString().toFloatOrNull() ?: 0f

        if (uncultivatedLandSize + cultivatedLandSize + homesteadSize > totalLandSize) {
            Snackbar.make(binding.root, "Sum of land sizes cannot exceed total land size", Snackbar.LENGTH_LONG).show()
            isValid = false
        }

        return isValid
    }

    fun saveData() {
        if (validateForm()) {
            // Retrieve and convert form data
            val totalLandSize = binding.totalLandSizeEditText.text.toString().toFloat()
            val uncultivatedLandSize = binding.uncultivatedLandSizeEditText.text.toString().toFloat()
            val cultivatedLandSize = binding.cultivatedLandSizeEditText.text.toString().toFloat()
            val homesteadSize = binding.homesteadSizeEditText.text.toString().toFloat()
            val farmAccessibility = binding.farmAccessibilityAutoCompleteTextView.text.toString()
            val cropList = binding.cropListAutoCompleteTextView.text.toString()

            // Handle access to irrigation
            val selectedIrrigationId = binding.accessToIrrigationRadioGroup.checkedRadioButtonId
            val irrigationAccess = when (selectedIrrigationId) {
                R.id.irrigationYesRadioButton -> {
                    val irrigationTypes = mutableListOf<String>()
                    if (binding.canalCheckBox.isChecked) irrigationTypes.add("Canal")
                    if (binding.dripCheckBox.isChecked) irrigationTypes.add("Drip")
                    if (binding.overheadCheckBox.isChecked) irrigationTypes.add("Overhead")
                    if (binding.floodCheckBox.isChecked) irrigationTypes.add("Flood")
                    val otherIrrigation = binding.specifyOtherTypeOfIrrigationEditText.text.toString()
                    if (otherIrrigation.isNotEmpty()) irrigationTypes.add("Other, $otherIrrigation")
                    if (irrigationTypes.isEmpty()) "Yes" else "Yes, ${irrigationTypes.joinToString(", ")}"
                }
                R.id.irrigationNoRadioButton -> "No"
                R.id.irrigationOtherRadioButton -> {
                    val otherIrrigation = binding.specifyOtherTypeOfIrrigationEditText.text.toString()
                    if (otherIrrigation.isNotEmpty()) "Other, $otherIrrigation" else "Other"
                }
                else -> ""
            }

            // Handle extension services
            val selectedExtensionId = binding.extensionServicesRadioGroup.checkedRadioButtonId
            val extensionServices = when (selectedExtensionId) {
                R.id.extensionYesRadioButton -> "Yes"
                R.id.extensionPartialRadioButton -> {
                    val specifyExtensionService = binding.specifyExtensionServiceEditText.text.toString()
                    if (specifyExtensionService.isNotEmpty()) "Partial, $specifyExtensionService" else "Partial"
                }
                R.id.extensionNoRadioButton -> "No"
                else -> null
            }

            // Save data to ViewModel
            sharedViewModel.setTotalLandSize(totalLandSize)
            sharedViewModel.setUncultivatedLandSize(uncultivatedLandSize)
            sharedViewModel.setCultivatedLandSize(cultivatedLandSize)
            sharedViewModel.setHomesteadSize(homesteadSize)
            sharedViewModel.setFarmAccessibility(farmAccessibility)
            sharedViewModel.setCropList(cropList)
            sharedViewModel.setAccessToIrrigation(irrigationAccess)
            sharedViewModel.setExtensionServices(extensionServices ?: "")

            // Notify user of successful data save
            Snackbar.make(binding.root, "Data saved successfully", Snackbar.LENGTH_LONG).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

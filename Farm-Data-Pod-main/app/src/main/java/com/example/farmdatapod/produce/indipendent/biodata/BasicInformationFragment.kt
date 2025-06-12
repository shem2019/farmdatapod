package com.example.farmdatapod.produce.indipendent.biodata

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentBasicInformationBinding
import com.example.farmdatapod.produce.indipendent.biodata.SharedViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class BasicInformationFragment : Fragment() {

    private var _binding: FragmentBasicInformationBinding? = null
    private val binding get() = _binding!!
    private lateinit var primaryProducerRadioGroup: RadioGroup
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBasicInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        primaryProducerRadioGroup = binding.primaryProducerRadioGroup

        setupSpinners()
        setupDatePicker()
        setupButtonListeners()
        setupFinancialDropdowns()

        primaryProducerRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.noRadioButton -> {
                    // Show all fields for primary producer
                    binding.firstNamePPInputLayout.visibility = View.VISIBLE
                    binding.lastNamePPInputLayout.visibility = View.VISIBLE
                    binding.idNumberPPInputLayout.visibility = View.VISIBLE
                    binding.phoneNumberPPInputLayout.visibility = View.VISIBLE
                    binding.genderPPRadioGroup.visibility = View.VISIBLE
                    binding.emailPPInputLayout.visibility = View.VISIBLE
                    binding.dateOfBirthPPInputLayout.visibility = View.VISIBLE
                    binding.primaryProducerDetailsTextView.visibility = View.VISIBLE
                }
                R.id.yesRadioButton -> {
                    // Hide all fields if not primary producer
                    binding.firstNamePPInputLayout.visibility = View.GONE
                    binding.lastNamePPInputLayout.visibility = View.GONE
                    binding.idNumberPPInputLayout.visibility = View.GONE
                    binding.phoneNumberPPInputLayout.visibility = View.GONE
                    binding.genderPPRadioGroup.visibility = View.GONE
                    binding.emailPPInputLayout.visibility = View.GONE
                    binding.dateOfBirthPPInputLayout.visibility = View.GONE
                    binding.primaryProducerDetailsTextView.visibility = View.GONE
                }
            }
        }
    }

    private fun setupSpinners() {
        val educationAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.education_levels)
        )
        binding.educationAutoCompleteTextView.setAdapter(educationAdapter)

        val countyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.region_array)
        )
        binding.countyAutoCompleteTextView.setAdapter(countyAdapter)
    }

    private fun setupFinancialDropdowns() {
        val providers = resources.getStringArray(R.array.mobile_money_providers)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, providers)
        binding.mobileMoneyProviderAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        binding.dateOfBirthEditText.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date of birth")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.dateOfBirthEditText.setText(dateFormat.format(Date(selection)))
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupButtonListeners() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.getLocationButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the missing permissions
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                return@setOnClickListener
            }

            // Permissions are granted, retrieve the location
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val locationDetails = "Latitude: $latitude, Longitude: $longitude"

                    // Update the locationEditText field with the location details
                    binding.locationEditText.setText(locationDetails)

                    // Make the TextInputEditText read-only
                    binding.locationEditText.isFocusable = false
                    binding.locationEditText.isFocusableInTouchMode = false

                    Snackbar.make(
                        binding.root,
                        "Location: $locationDetails",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    Snackbar.make(binding.root, "Unable to retrieve location", Snackbar.LENGTH_LONG).show()
                }
            }.addOnFailureListener {
                Snackbar.make(binding.root, "Failed to retrieve location", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    fun validateForm(): Boolean {
        var isValid = true

        // Validate all fields
        isValid = validateField(binding.lastNameEditText, binding.lastNameInputLayout, "Last Name is required") && isValid
        isValid = validateField(binding.otherNameEditText, binding.otherNameInputLayout, "Other Name is required") && isValid
        isValid = validateField(binding.idNumberEditText, binding.idNumberInputLayout, "ID Number is required") && isValid
        isValid = validateEmail(binding.emailEditText, binding.emailInputLayout) && isValid
        isValid = validatePhone(binding.phoneEditText, binding.phoneInputLayout) && isValid
        isValid = validateField(binding.locationEditText, binding.locationInputLayout, "Location is required") && isValid
        isValid = validateField(binding.educationAutoCompleteTextView, binding.educationInputLayout, "Education Level is required") && isValid
        isValid = validateField(binding.dateOfBirthEditText, binding.dateOfBirthInputLayout, "Date of Birth is required") && isValid
        isValid = validateField(binding.countyAutoCompleteTextView, binding.countyInputLayout, "County is required") && isValid
        isValid = validateField(binding.subCountyEditText, binding.subCountyInputLayout, "Sub County is required") && isValid
        isValid = validateField(binding.wardEditText, binding.wardInputLayout, "Ward is required") && isValid
        isValid = validateField(binding.villageEditText, binding.villageInputLayout, "Village is required") && isValid

        // Validate gender
        if (binding.genderRadioGroup.checkedRadioButtonId == -1) {
            Snackbar.make(binding.root, "Please select a gender", Snackbar.LENGTH_LONG).show()
            isValid = false
        }

        // Validate primary producer
        if (binding.primaryProducerRadioGroup.checkedRadioButtonId == -1) {
            Snackbar.make(binding.root, "Please indicate if you're the primary producer", Snackbar.LENGTH_LONG).show()
            isValid = false
        }

        return isValid
    }

    private fun validateField(view: View, inputLayout: TextInputLayout, errorMessage: String): Boolean {
        return when (view) {
            is TextInputEditText -> {
                if (view.text.isNullOrBlank()) {
                    inputLayout.error = errorMessage
                    false
                } else {
                    inputLayout.error = null
                    true
                }
            }
            is AutoCompleteTextView -> {
                if (view.text.isNullOrBlank()) {
                    inputLayout.error = errorMessage
                    false
                } else {
                    inputLayout.error = null
                    true
                }
            }
            else -> {
                inputLayout.error = "Unsupported view type"
                false
            }
        }
    }


    private fun validateEmail(editText: TextInputEditText, inputLayout: TextInputLayout): Boolean {
        return if (editText.text.isNullOrBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(editText.text.toString()).matches()) {
            inputLayout.error = "Valid Email is required"
            false
        } else {
            inputLayout.error = null
            true
        }
    }

    private fun validatePhone(editText: TextInputEditText, inputLayout: TextInputLayout): Boolean {
        return if (editText.text.isNullOrBlank() || editText.text.toString().length < 10) {
            inputLayout.error = "Valid Phone Number is required"
            false
        } else {
            inputLayout.error = null
            true
        }
    }

    fun saveData() {
        if (validateForm()) {
            // Retrieve values from form fields
            val otherName = binding.otherNameEditText.text.toString()
            val lastName = binding.lastNameEditText.text.toString()
            val idNumber = binding.idNumberEditText.text.toString().toLongOrNull() ?: 0L
            val email = binding.emailEditText.text.toString()
            val phone = binding.phoneEditText.text.toString()
            val location = binding.locationEditText.text.toString()
            val educationLevel = binding.educationAutoCompleteTextView.text.toString()
            val dateOfBirth = binding.dateOfBirthEditText.text.toString()
            val county = binding.countyAutoCompleteTextView.text.toString()
            val subCounty = binding.subCountyEditText.text.toString()
            val ward = binding.wardEditText.text.toString()
            val village = binding.villageEditText.text.toString()

            val selectedGenderId = binding.genderRadioGroup.checkedRadioButtonId
            val selectedGender = when (selectedGenderId) {
                R.id.maleRadioButton -> "Male"
                R.id.femaleRadioButton -> "Female"
                else -> "Not Selected"
            }

            val selectedPrimaryProducerId = binding.primaryProducerRadioGroup.checkedRadioButtonId
            val isPrimaryProducer = when (selectedPrimaryProducerId) {
                R.id.yesRadioButton -> "Yes"
                R.id.noRadioButton -> "No"
                else -> ""
            }

            fun formatServerDate(dateString: String): String {
                return try {
                    val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val date = inputFormat.parse(dateString)
                    date?.let { outputFormat.format(it) } ?: dateString
                } catch (e: Exception) {
                    dateString
                }
            }

            val primaryProducerData = if (isPrimaryProducer == "No") {
                val firstNamePP = binding.firstNamePPEditText.text.toString()
                val lastNamePP = binding.lastNamePPEditText.text.toString()
                val idNumberPP = binding.idNumberPPEditText.text.toString()
                val phoneNumberPP = binding.phoneNumberPPEditText.text.toString()
                val genderPP = if (binding.genderPPRadioGroup.checkedRadioButtonId == R.id.malePPRadioButton) "Male" else "Female"
                val emailPP = binding.emailPPEditText.text.toString()
                val dobPP = binding.dateOfBirthPPEditText.text.toString()

                listOf(
                    mapOf(
                        "response" to "No",
                        "firstname" to firstNamePP,
                        "other_name" to lastNamePP,
                        "id_number" to (idNumberPP.toLongOrNull() ?: 0L),
                        "phone_number" to phoneNumberPP,
                        "gender" to genderPP,
                        "email" to emailPP,
                        "date_of_birth" to formatServerDate(dobPP)
                    )
                )
            } else {
                listOf(mapOf("response" to "Yes"))
            }

            // NEW: COLLECT FINANCIAL DATA
            val bankName = binding.bankNameEditText.text.toString()
            val bankAccountNumber = binding.bankAccountNumberEditText.text.toString()
            val bankAccountHolder = binding.bankAccountHolderEditText.text.toString()
            val mobileMoneyProvider = binding.mobileMoneyProviderAutoCompleteTextView.text.toString()
            val mobileMoneyNumber = binding.mobileMoneyNumberEditText.text.toString()

            sharedViewModel.apply {
                setFirstName(otherName)
                setLastName(lastName)
                setIdNumber(idNumber)
                setEmail(email)
                setPhone(phone)
                setLocation(location)
                setEducationLevel(educationLevel)
                setDateOfBirth(dateOfBirth)
                setCounty(county)
                setSubCounty(subCounty)
                setWard(ward)
                setVillage(village)
                setGender(selectedGender)
                setPrimaryProducer(primaryProducerData)

                // NEW: SAVE FINANCIAL DATA TO VIEWMODEL
                setBankName(bankName.ifEmpty { null })
                setBankAccountNumber(bankAccountNumber.ifEmpty { null })
                setBankAccountHolder(bankAccountHolder.ifEmpty { null })
                setMobileMoneyProvider(mobileMoneyProvider.ifEmpty { null })
                setMobileMoneyNumber(mobileMoneyNumber.ifEmpty { null })
            }

            Snackbar.make(binding.root, "Data saved", Snackbar.LENGTH_LONG).show()
        } else {
            Snackbar.make(binding.root, "Please correct the errors in the form", Snackbar.LENGTH_LONG).show()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

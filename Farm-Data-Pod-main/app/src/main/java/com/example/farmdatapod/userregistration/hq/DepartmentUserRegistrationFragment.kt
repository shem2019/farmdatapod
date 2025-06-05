package com.example.farmdatapod.userregistration.hq

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.HQUser
import com.example.farmdatapod.databinding.FragmentDepartmentUserRegistrationBinding
import com.example.farmdatapod.models.HQUsersRequest
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class DepartmentUserRegistrationFragment : Fragment() {

    private var _binding: FragmentDepartmentUserRegistrationBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkViewModel: NetworkViewModel
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDepartmentUserRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)

        setupSpinner()
        setupSubmitButton()
        setupDatePicker()
        setupBackButton()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupSpinner() {
        val roles = arrayOf("Role A", "Role B", "Role C", "Role D", "Role E")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.relatedRolesSpinner.adapter = adapter
    }

//    private fun setupSubmitButton() {
//        binding.submitButton.setOnClickListener {
//            if (validateInputs()) {
//                registerHQUser()
//            }
//        }
//    }

    private fun setupSubmitButton() {
        // Observe network connectivity changes
        networkViewModel.networkLiveData.observe(viewLifecycleOwner) { isNetworkAvailable ->
            Log.d("HQRegistrationFragment", "Network status changed: $isNetworkAvailable")

            // Set up the submit button click listener
            binding.submitButton.setOnClickListener {
                Log.d("HQRegistrationFragment", "Submit button clicked")
                if (validateInputs()) {
                    Log.d("HQRegistrationFragment", "Inputs validated. Checking network availability...")
                    if (isNetworkAvailable) {
                        Log.d("HQRegistrationFragment", "Network available. Proceeding with HQ user registration.")
                        registerHQUser()
                    } else {
                        Log.d("HQRegistrationFragment", "No network available. Showing error message.")
                        offlineRegisterHQUser()
                        // Toast.makeText(context, "Submitting offline", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("HQRegistrationFragment", "Input validation failed")
                    Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun setupDatePicker() {
        binding.dateOfBirthEditText.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                // Display the date in human-readable format
                val displayDate = displayDateFormat.format(selectedDate.time)
                binding.dateOfBirthEditText.setText(displayDate)

                // Store the API format date as a tag for later use
                val apiDate = apiDateFormat.format(selectedDate.time)
                binding.dateOfBirthEditText.tag = apiDate
            },
            year,
            month,
            day
        )

        // Set the maximum date to today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Last Name
        if (binding.lastNameEditText.text.toString().trim().isEmpty()) {
            binding.lastNameLayout.error = "Last name is required"
            isValid = false
        } else {
            binding.lastNameLayout.error = null
        }

        // Email
        val email = binding.emailEditText.text.toString().trim()
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Valid email is required"
            isValid = false
        } else {
            binding.emailLayout.error = null
        }

        // Other Name
        if (binding.otherNameEditText.text.toString().trim().isEmpty()) {
            binding.otherNameLayout.error = "Other name is required"
            isValid = false
        } else {
            binding.otherNameLayout.error = null
        }

        // Phone Number
        val phoneNumber = binding.phoneEditText.text.toString().trim()
        if (phoneNumber.isEmpty() || phoneNumber.length < 10) {
            binding.phoneLayout.error = "Valid phone number is required"
            isValid = false
        } else {
            binding.phoneLayout.error = null
        }

        // Staff Code
        if (binding.staffCodeEditText.text.toString().trim().isEmpty()) {
            binding.staffCodeLayout.error = "Staff code is required"
            isValid = false
        } else {
            binding.staffCodeLayout.error = null
        }

        // Education Level
        if (binding.educationEditText.text.toString().trim().isEmpty()) {
            binding.educationLayout.error = "Education level is required"
            isValid = false
        } else {
            binding.educationLayout.error = null
        }

        // ID Number
        val idNumber = binding.idNumberEditText.text.toString().trim()
        if (idNumber.isEmpty() || idNumber.toIntOrNull() == null) {
            binding.idNumberLayout.error = "Valid ID number is required"
            isValid = false
        } else {
            binding.idNumberLayout.error = null
        }

        // Role
        if (binding.roleEditText.text.toString().trim().isEmpty()) {
            binding.roleLayout.error = "Role is required"
            isValid = false
        } else {
            binding.roleLayout.error = null
        }

        // Date of Birth
        val dateOfBirth = binding.dateOfBirthEditText.text.toString().trim()
        if (dateOfBirth.isEmpty()) {
            binding.dateOfBirthLayout.error = "Date of birth is required"
            isValid = false
        } else {
            binding.dateOfBirthLayout.error = null
        }

        // Reporting To
        if (binding.reportingToEditText.text.toString().trim().isEmpty()) {
            binding.reportingToLayout.error = "Reporting to is required"
            isValid = false
        } else {
            binding.reportingToLayout.error = null
        }

        // Gender
        if (!binding.maleRadioButton.isChecked && !binding.femaleRadioButton.isChecked) {
            Toast.makeText(context, "Please select a gender", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Related Roles
        if (binding.relatedRolesSpinner.selectedItemPosition == 0) {
            Toast.makeText(context, "Please select a related role", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun formatDateForApi(date: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val parsedDate = inputFormat.parse(date)
        return outputFormat.format(parsedDate!!)
    }

    private fun registerHQUser() {
        val hqUsersRequest = HQUsersRequest(
            date_of_birth = binding.dateOfBirthEditText.tag as? String ?: "",
            department = "HQ",
            education_level = binding.educationEditText.text.toString().trim(),
            email = binding.emailEditText.text.toString().trim(),
            gender = if (binding.maleRadioButton.isChecked) "Male" else "Female",
            id_number = binding.idNumberEditText.text.toString().toIntOrNull() ?: 0,
            last_name = binding.lastNameEditText.text.toString().trim(),
            other_name = binding.otherNameEditText.text.toString().trim(),
            phone_number = binding.phoneEditText.text.toString().toIntOrNull() ?: 0,
            related_roles = binding.relatedRolesSpinner.selectedItem.toString(),
            reporting_to = binding.reportingToEditText.text.toString().trim(),
            role = binding.roleEditText.text.toString().trim(),
            staff_code = binding.staffCodeEditText.text.toString().trim()
        )

        context?.let { ctx ->
            RestClient.getApiService(ctx).registerHQUser(hqUsersRequest)
                .enqueue(object : Callback<HQUsersRequest> {
                    override fun onResponse(
                        call: Call<HQUsersRequest>,
                        response: Response<HQUsersRequest>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(ctx, "User registered successfully", Toast.LENGTH_SHORT)
                                .show()
                            clearForm() // Clear the form after successful registration
                        } else {
                            Toast.makeText(ctx, "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<HQUsersRequest>, t: Throwable) {
                        Toast.makeText(ctx, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun offlineRegisterHQUser() {
        // Directly create an HQUser object from form inputs
        val hqUser = HQUser().apply {
            other_name = binding.otherNameEditText.text.toString().trim()
            last_name = binding.lastNameEditText.text.toString().trim()
            email = binding.emailEditText.text.toString().trim()
            phone_number = binding.phoneEditText.text.toString().trim()
            gender = if (binding.maleRadioButton.isChecked) "Male" else "Female"
            role = binding.roleEditText.text.toString().trim()
            date_of_birth = binding.dateOfBirthEditText.tag as? String ?: ""
            id_number = binding.idNumberEditText.text.toString().trim()
            department = "HQ"
            staff_code = binding.staffCodeEditText.text.toString().trim()
            education_level = binding.educationEditText.text.toString().trim()
            reporting_to = binding.reportingToEditText.text.toString().trim()
            related_roles = binding.relatedRolesSpinner.selectedItem.toString()

            // Retrieve user ID from SharedPrefs
            val sharedPrefs = context?.let { SharedPrefs(it) }
            user_id = sharedPrefs?.getUserId()
        }

        // Insert the HQUser object into the local database
        val dbHandler = DBHandler(context)
        val isInserted = dbHandler.insertHQUser(hqUser)

        if (isInserted) {
            Toast.makeText(context, "Submitted offline", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save registration to local database", Toast.LENGTH_SHORT).show()
        }

        clearForm()
    }

    private fun clearForm() {
        binding.apply {
            lastNameEditText.text?.clear()
            emailEditText.text?.clear()
            otherNameEditText.text?.clear()
            phoneEditText.text?.clear()
            staffCodeEditText.text?.clear()
            educationEditText.text?.clear()
            idNumberEditText.text?.clear()
            roleEditText.text?.clear()
            dateOfBirthEditText.text?.clear()
            reportingToEditText.text?.clear()
            maleRadioButton.isChecked = false
            femaleRadioButton.isChecked = false
            relatedRolesSpinner.setSelection(0) // Assuming the first item is a default or placeholder
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
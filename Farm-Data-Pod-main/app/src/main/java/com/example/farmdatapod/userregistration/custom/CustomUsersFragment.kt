package com.example.farmdatapod.userregistration.custom

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.farmdatapod.databinding.FragmentCustomUsersBinding
import com.example.farmdatapod.models.CustomUserRequestModel
import com.example.farmdatapod.network.RestClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomUsersFragment : Fragment() {

    private var _binding: FragmentCustomUsersBinding? = null
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRolesDropdown()
        setupDatePicker()
        setupListeners()
    }

    private fun setupRolesDropdown() {
        val roles = listOf("Role 1", "Role 2", "Role 3")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        binding.actvRelatedRoles.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val displayDate = displayDateFormat.format(calendar.time)
            binding.etDateOfBirth.setText(displayDate)

            // Store the API format date as a tag for later use
            binding.etDateOfBirth.tag = apiDateFormat.format(calendar.time)
        }

        binding.etDateOfBirth.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.btnSubmit.setOnClickListener {
            if (validateFields()) {
                submitForm()
            }
        }
    }

    private fun validateFields(): Boolean {
        with(binding) {
            return when {
                etLastName.text.isNullOrEmpty() -> {
                    tilLastName.error = "Last Name is required"
                    false
                }
                etEmail.text.isNullOrEmpty() -> {
                    tilEmail.error = "Email is required"
                    false
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(etEmail.text.toString()).matches() -> {
                    tilEmail.error = "Invalid email format"
                    false
                }
                etOtherName.text.isNullOrEmpty() -> {
                    tilOtherName.error = "Other Name is required"
                    false
                }
                etPhoneNo.text.isNullOrEmpty() -> {
                    tilPhoneNo.error = "Phone No. is required"
                    false
                }
                etStaffCode.text.isNullOrEmpty() -> {
                    tilStaffCode.error = "Staff Code is required"
                    false
                }
                etEducation.text.isNullOrEmpty() -> {
                    tilEducation.error = "Education level is required"
                    false
                }
                etIdNumber.text.isNullOrEmpty() -> {
                    tilIdNumber.error = "ID Number is required"
                    false
                }
                etReportingTo.text.isNullOrEmpty() -> {
                    tilReportingTo.error = "Reporting to field is required"
                    false
                }
                etDateOfBirth.text.isNullOrEmpty() -> {
                    tilDateOfBirth.error = "Date of Birth is required"
                    false
                }
                actvRelatedRoles.text.isNullOrEmpty() -> {
                    tilRelatedRoles.error = "Related Roles is required"
                    false
                }
                else -> {
                    clearErrors()
                    true
                }
            }
        }
    }

    private fun submitForm() {
        val customUserRequest = CustomUserRequestModel(
            other_name = binding.etOtherName.text.toString(),
            last_name = binding.etLastName.text.toString(),
            staff_code = binding.etStaffCode.text.toString(),
            id_number = binding.etIdNumber.text.toString().toIntOrNull() ?: 0,
            gender = if (binding.rbMale.isChecked) "Male" else "Female",
            date_of_birth = binding.etDateOfBirth.tag as? String ?: "",
            email = binding.etEmail.text.toString(),
            phone_number = binding.etPhoneNo.text.toString().toIntOrNull() ?: 0,
            education_level = binding.etEducation.text.toString(),
            role = binding.actvRelatedRoles.text.toString(),
            reporting_to = binding.etReportingTo.text.toString()
        )

        context?.let { ctx ->
            RestClient.getApiService(ctx).registerCustomUser(customUserRequest)
                .enqueue(object : Callback<CustomUserRequestModel> {
                    override fun onResponse(
                        call: Call<CustomUserRequestModel>,
                        response: Response<CustomUserRequestModel>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "User registered successfully", Toast.LENGTH_SHORT).show()
                            clearFields()
                            // Navigate back or to another screen if needed
                            // findNavController().navigate(R.id.action_customUserFragment_to_someOtherFragment)
                        } else {
                            Toast.makeText(context, "Registration failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<CustomUserRequestModel>, t: Throwable) {
                        Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun clearErrors() {
        with(binding) {
            tilLastName.error = null
            tilEmail.error = null
            tilOtherName.error = null
            tilPhoneNo.error = null
            tilStaffCode.error = null
            tilEducation.error = null
            tilIdNumber.error = null
            tilReportingTo.error = null
            tilDateOfBirth.error = null
            tilRelatedRoles.error = null
        }
    }

    private fun clearFields() {
        with(binding) {
            etLastName.text?.clear()
            etEmail.text?.clear()
            etOtherName.text?.clear()
            etPhoneNo.text?.clear()
            etStaffCode.text?.clear()
            etEducation.text?.clear()
            etIdNumber.text?.clear()
            etReportingTo.text?.clear()
            etDateOfBirth.text?.clear()
            actvRelatedRoles.text?.clear()
            rbMale.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
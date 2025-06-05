package com.example.farmdatapod.userregistration.hubusers

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.HubUser
import com.example.farmdatapod.R
import com.example.farmdatapod.data.DataSyncManager
import com.example.farmdatapod.databinding.FragmentHubUserRegistrationBinding
import com.example.farmdatapod.models.HubUserResponse
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.utils.SharedPrefs
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HubUserRegistrationFragment : Fragment() {

    private var _binding: FragmentHubUserRegistrationBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private lateinit var dbHandler: DBHandler
    private lateinit var networkViewModel: NetworkViewModel
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHubUserRegistrationBinding.inflate(inflater, container, false)
        dbHandler = DBHandler(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.editDateOfBirth.setOnClickListener {
            showDatePicker()
        }

        setupSpinners()

        setupSubmitButton()
    }

    private fun setupSubmitButton() {
        // Observe network connectivity changes
        networkViewModel.networkLiveData.observe(viewLifecycleOwner) { isNetworkAvailable ->
            Log.d("HubUserRegistrationFragment", "Network status changed: $isNetworkAvailable")

            // Set up the submit button click listener
            binding.buttonSubmit.setOnClickListener {
                if (validateInputs()) {
                    if (networkViewModel.networkLiveData.value == true) {
                        val dateOfBirth = binding.editDateOfBirth.tag as? String ?: ""


                        val hubUserResponse = HubUserResponse(
                            buying_center = binding.spinnerBuyingCenter.text.toString(),
                            code = binding.editCode.text.toString(),
                            county = binding.spinnerCounty.text.toString(),
                            date_of_birth = dateOfBirth,
                            education_level = binding.editEducation.text.toString(),
                            email = binding.editEmail.text.toString(),
                            gender = if (binding.radioMale.isChecked) "Male" else "Female",
                            hub = binding.spinnerHub.text.toString(),
                            id_number = binding.editIdNumber.text.toString().toIntOrNull() ?: 0,
                            last_name = binding.editLastName.text.toString(),
                            other_name = binding.editOtherName.text.toString(),
                            phone_number = binding.editPhone.text.toString().toIntOrNull() ?: 0,
                            role = binding.editRole.text.toString(),
                            sub_county = binding.editSubCounty.text.toString(),
                            village = binding.editVillage.text.toString(),
                            ward = binding.editWard.text.toString()
                        )

                        context?.let { ctx ->
                            RestClient.getApiService(ctx).registerHubAttendant(hubUserResponse).enqueue(object : Callback<HubUserResponse> {
                                override fun onResponse(call: Call<HubUserResponse>, response: Response<HubUserResponse>) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(requireContext(), "Registration Successful", Toast.LENGTH_SHORT).show()
                                        clearFields()
                                    } else {
                                        if (response.code() == 404) {
                                            Toast.makeText(requireContext(), "Endpoint not found (404)", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(requireContext(), "Registration Failed: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                override fun onFailure(call: Call<HubUserResponse>, t: Throwable) {
                                    Toast.makeText(requireContext(), "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    } else {
                        handleOfflineRegistration()
                        Log.d("HubUserRegistrationFragment", "No network available. Showing offline message.")
                    }
                } else {
                    Log.d("HubUserRegistrationFragment", "Input validation failed")
                    Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun offlineRegisterHubUser(hubUser: HubUser) {
        Log.d("HubUserRegistrationFragment", "Offline function triggered.")
        val dbHandler = DBHandler(requireContext())
        val isInserted = dbHandler.insertHubUser(hubUser)

        if (isInserted) {
            Toast.makeText(context, "Data saved offline successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save data offline", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleOfflineRegistration() {
        val dateOfBirth = binding.editDateOfBirth.tag as? String ?: ""


        val hubUser = HubUser(
            id = 0,
            other_name = binding.editOtherName.text.toString(),
            last_name = binding.editLastName.text.toString(),
            code = binding.editCode.text.toString(),
            role = binding.editRole.text.toString(),
            id_number = binding.editIdNumber.text.toString(),
            gender = if (binding.radioMale.isChecked) "Male" else "Female",
            date_of_birth = dateOfBirth,
            email = binding.editEmail.text.toString(),
            phone_number = binding.editPhone.text.toString(),
            education_level = binding.editEducation.text.toString(),
            hub = binding.spinnerHub.text.toString(),
            buying_center = binding.spinnerBuyingCenter.text.toString(),
            county = binding.spinnerCounty.text.toString(),
            sub_county = binding.editSubCounty.text.toString(),
            ward = binding.editWard.text.toString(),
            village = binding.editVillage.text.toString(),
            is_offline = 1,
            user_id = "user_id_placeholder"
        )

        offlineRegisterHubUser(hubUser)
    }

    private fun setupSpinners() {
        // Retrieve hub names from dbHandler
        val hubNames = dbHandler.allHubNames

        // Create an ArrayAdapter for the AutoCompleteTextView
        val hubAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line, // Use simple_dropdown_item_1line for AutoCompleteTextView
            hubNames
        )

        // Reference the AutoCompleteTextView and set the adapter
        binding.spinnerHub.setAdapter(hubAdapter)

        // Retrieve buying center names from dbHandler
        val buyingCenterNames = dbHandler.allBuyingCenterNames

        // Create an ArrayAdapter for the AutoCompleteTextView
        val buyingCenterAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            buyingCenterNames
        )

        // Reference the AutoCompleteTextView and set the adapter
        binding.spinnerBuyingCenter.setAdapter(buyingCenterAdapter)

        // County Spinner
        val countyNames = resources.getStringArray(R.array.region_array)

        val countyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            countyNames
        )

        binding.spinnerCounty.setAdapter(countyAdapter)

    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateOfBirthField()
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateOfBirthField() {
        val displayDate = displayDateFormat.format(calendar.time)
        binding.editDateOfBirth.setText(displayDate)

        // Store the API format date as a tag for later use
        val apiDate = apiDateFormat.format(calendar.time)
        binding.editDateOfBirth.tag = apiDate
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        fun validateField(value: String, layout: TextInputLayout, errorMsg: String): Boolean {
            return if (TextUtils.isEmpty(value)) {
                layout.error = errorMsg
                false
            } else {
                layout.error = null
                true
            }
        }

        isValid = validateField(binding.spinnerHub.text.toString(), binding.tilHub, "Please select a hub") && isValid
        isValid = validateField(binding.editLastName.text.toString(), binding.tilHubName, "Please enter last name") && isValid
        isValid = validateField(binding.editOtherName.text.toString(), binding.tilOtherName, "Please enter other name") && isValid

        val idNumber = binding.editIdNumber.text.toString()
        if (TextUtils.isEmpty(idNumber)) {
            binding.tilIdNumber.error = "Please enter ID number"
            isValid = false
        } else if (idNumber.length != 8) {
            binding.tilIdNumber.error = "ID number must be 8 digits"
            isValid = false
        } else {
            binding.tilIdNumber.error = null
        }

        isValid = validateField(binding.editDateOfBirth.text.toString(), binding.tilDateOfBirth, "Please enter date of birth") && isValid
        isValid = validateField(binding.editEmail.text.toString(), binding.tilEmail, "Please enter email") && isValid
        isValid = validateField(binding.editPhone.text.toString(), binding.tilPhone, "Please enter phone number") && isValid

        if (binding.radioGroup.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Please select gender", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        isValid = validateField(binding.editEducation.text.toString(), binding.tilEducation, "Please enter education level") && isValid
        isValid = validateField(binding.spinnerBuyingCenter.text.toString(), binding.tilBuyingCenter, "Please enter buying center") && isValid
        isValid = validateField(binding.spinnerCounty.text.toString(), binding.tilCounty, "Please enter county") && isValid
        isValid = validateField(binding.editSubCounty.text.toString(), binding.tilSubCounty, "Please enter sub county") && isValid
        isValid = validateField(binding.editWard.text.toString(), binding.tilWard, "Please enter ward") && isValid
        isValid = validateField(binding.editVillage.text.toString(), binding.tilVillage, "Please enter village") && isValid
        isValid = validateField(binding.editRole.text.toString(), binding.tilRole, "Please enter role") && isValid
       // isValid = validateField(binding.editCode.text.toString(), binding.tilCode, "Please enter code") && isValid

        return isValid
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun clearFields() {
        binding.spinnerHub.setText("")
        binding.editLastName.setText("")
        binding.editOtherName.setText("")
        binding.editCode.setText("")
        binding.editIdNumber.setText("")
        binding.editDateOfBirth.setText("")
        binding.editEmail.setText("")
        binding.editPhone.setText("")
        binding.radioGroup.clearCheck()
        binding.editEducation.setText("")
        binding.spinnerBuyingCenter.setText("")
        binding.spinnerCounty.setText("")
        binding.editSubCounty.setText("")
        binding.editWard.setText("")
        binding.editVillage.setText("")
        binding.editRole.setText("")
    }


}
package com.example.farmdatapod.ruralworker

import android.app.DatePickerDialog
import android.os.Bundle
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
import com.example.farmdatapod.BuyingCustomer
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.R
import com.example.farmdatapod.RuralWorker
import com.example.farmdatapod.databinding.FragmentRuralWorkerInformationBinding
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.models.RuralWorkerRequest // Import the correct class
import com.example.farmdatapod.models.SellingRequestModel
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class RuralWorkerInformationFragment : Fragment() {

    private var _binding: FragmentRuralWorkerInformationBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkViewModel: NetworkViewModel
    private lateinit var dbHandler: DBHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRuralWorkerInformationBinding.inflate(inflater, container, false)
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackButton()
        setupDatePicker()
        setupDropdowns()
        setupSubmitButton()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.dateOfBirthInput.setText(dateFormat.format(calendar.time))
        }

        binding.dateOfBirthInput.setOnClickListener {
            DatePickerDialog(
                requireContext(), dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupDropdowns() {
        // Services dropdown
        val services = arrayOf("Service A", "Service B", "Service C")
        val serviceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, services)
        (binding.serviceInput as? AutoCompleteTextView)?.setAdapter(serviceAdapter)

        // Counties dropdown
        val counties = arrayOf("County A", "County B", "County C")
        val countyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, counties)
        (binding.countyInput as? AutoCompleteTextView)?.setAdapter(countyAdapter)

        // Region dropdown
        val regions = resources.getStringArray(R.array.region_array)
        val regionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, regions)
        (binding.countyInput as? AutoCompleteTextView)?.setAdapter(regionAdapter)
    }

    private fun setupSubmitButton() {
        // Observe network connectivity changes
        networkViewModel.networkLiveData.observe(viewLifecycleOwner) { isNetworkAvailable ->
            Log.d("RuralWorkerFragment", "Network status changed: $isNetworkAvailable")

            binding.submitButton.setOnClickListener {
                Log.d("RuralWorkerFragment", "Submit button clicked")

                if (validateFields()) {
                    Log.d("RuralWorkerFragment", "Fields validated. Checking network availability...")

                    if (isNetworkAvailable) {
                        Log.d("RuralWorkerFragment", "Network available. Proceeding with online submission.")
                        submitRuralWorkerData()
                    } else {
                        Log.d("RuralWorkerFragment", "No network available. Handling offline submission.")
                        handleOfflineRuralWorkerSubmission()
                    }
                } else {
                    Log.d("RuralWorkerFragment", "Field validation failed")
                    Toast.makeText(context, "Please fill all required fields correctly", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleOfflineRuralWorkerSubmission() {
        // Log that the form submission is being handled offline
        Log.d("RuralWorkerFragment", "Form submitted offline. The data will be saved locally or queued for later submission.")

        // Initialize SharedPrefs to get the user_id
        val sharedPrefs = SharedPrefs(requireContext())
        val userId = sharedPrefs.getUserId() ?: "Unknown User"

        // Log the current user ID for debugging
        Log.d("RuralWorkerFragment", "Current User ID: $userId")

        // Populate the RuralWorker model
        val ruralWorker = RuralWorker().apply {
            other_name = binding.firstNameInput.text.toString()
            last_name = binding.lastNameInput.text.toString()
            rural_worker_code = binding.ruralWorkerCodeInput.text.toString()
            id_number = binding.idNumberInput.text.toString().toIntOrNull()?.toString() ?: "0"
            gender = if (binding.maleRadioButton.isChecked) "Male" else "Female"
            date_of_birth = formatDateForApi(binding.dateOfBirthInput.text.toString())
            email = binding.emailInput.text.toString()
            phone_number = binding.phoneNoInput.text.toString().toIntOrNull()?.toString() ?: "0"
            education_level = binding.levelOfEducationInput.text.toString()
            service = binding.serviceInput.text.toString()
            other = binding.otherInput.text.toString()
            county = binding.countyInput.text.toString()
            sub_county = binding.subCountyInput.text.toString()
            ward = binding.wardInput.text.toString()
            village = binding.villageInput.text.toString()
            user_id = userId
        }

        // Log the populated rural worker for debugging
        Log.d("RuralWorkerFragment", "RuralWorker model populated: $ruralWorker")

        // Insert the rural worker data into the local database using DBHandler
        val dbHandler = DBHandler(context)
        try {
            // Insert the rural worker into the database
            val isInserted = dbHandler.insertRuralWorker(ruralWorker)

            // Check if the data was successfully inserted
            if (isInserted) {
                Toast.makeText(context, "Rural worker data saved offline successfully.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save rural worker data offline.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("RuralWorkerFragment", "Error inserting RuralWorker into database", e)
            Toast.makeText(context, "Error occurred while saving data offline.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitRuralWorkerData() {
        val ruralWorkerRequest = RuralWorkerRequest(
            county = binding.countyInput.text.toString(),
            date_of_birth = formatDateForApi(binding.dateOfBirthInput.text.toString()),
            education_level = binding.levelOfEducationInput.text.toString(),
            email = binding.emailInput.text.toString(),
            gender = if (binding.maleRadioButton.isChecked) "Male" else "Female",
            id_number = binding.idNumberInput.text.toString().toIntOrNull() ?: 0,
            last_name = binding.lastNameInput.text.toString(),
            other = binding.otherInput.text.toString(),
            other_name = binding.firstNameInput.text.toString(),
            phone_number = binding.phoneNoInput.text.toString().toIntOrNull() ?: 0,
            rural_worker_code = binding.ruralWorkerCodeInput.text.toString(),
            service = binding.serviceInput.text.toString(),
            sub_county = binding.subCountyInput.text.toString(),
            village = binding.villageInput.text.toString(),
            ward = binding.wardInput.text.toString()
        )

        context?.let { ctx ->
            RestClient.getApiService(ctx).registerRuralWorker(ruralWorkerRequest)
                .enqueue(object : Callback<RuralWorkerRequest> {
                    override fun onResponse(call: Call<RuralWorkerRequest>, response: Response<RuralWorkerRequest>) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Rural worker registered successfully", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        } else {
                            Toast.makeText(context, "Failed to register rural worker", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<RuralWorkerRequest>, t: Throwable) {
                        Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        // First Name
        if (binding.firstNameInput.text.toString().trim().isEmpty()) {
            binding.firstNameLayout.error = "First name is required"
            isValid = false
        } else {
            binding.firstNameLayout.error = null
        }

        // Last Name
        if (binding.lastNameInput.text.toString().trim().isEmpty()) {
            binding.lastNameLayout.error = "Last name is required"
            isValid = false
        } else {
            binding.lastNameLayout.error = null
        }

        // Rural Worker Code
        if (binding.ruralWorkerCodeInput.text.toString().trim().isEmpty()) {
            binding.ruralWorkerCodeLayout.error = "Rural Worker Code is required"
            isValid = false
        } else {
            binding.ruralWorkerCodeLayout.error = null
        }

        // ID Number
        if (binding.idNumberInput.text.toString().trim().isEmpty()) {
            binding.idNumberLayout.error = "ID Number is required"
            isValid = false
        } else {
            binding.idNumberLayout.error = null
        }

        // Email
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        if (!binding.emailInput.text.toString().trim().matches(emailPattern.toRegex())) {
            binding.emailLayout.error = "Enter a valid email address"
            isValid = false
        } else {
            binding.emailLayout.error = null
        }


        // Phone Number
        val phoneNumber = binding.phoneNoInput.text.toString().trim()
        if (phoneNumber.length != 10 || !phoneNumber.all { it.isDigit() }) {
            binding.phoneNoLayout.error = "Enter a valid 10-digit phone number"
            isValid = false
        } else {
            binding.phoneNoLayout.error = null
        }
        // Date of Birth
        if (binding.dateOfBirthInput.text.toString().trim().isEmpty()) {
            binding.dateOfBirthLayout.error = "Date of Birth is required"
            isValid = false
        } else {
            binding.dateOfBirthLayout.error = null
        }

        // Gender
        if (binding.genderRadioGroup.checkedRadioButtonId == -1) {
            Toast.makeText(context, "Please select a gender", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Level of Education
        if (binding.levelOfEducationInput.text.toString().trim().isEmpty()) {
            binding.levelOfEducationLayout.error = "Level of Education is required"
            isValid = false
        } else {
            binding.levelOfEducationLayout.error = null
        }

        // Service
        if (binding.serviceInput.text.toString().trim().isEmpty()) {
            binding.serviceLayout.error = "Service is required"
            isValid = false
        } else {
            binding.serviceLayout.error = null
        }

        // County
        if (binding.countyInput.text.toString().trim().isEmpty()) {
            binding.countyLayout.error = "County is required"
            isValid = false
        } else {
            binding.countyLayout.error = null
        }

        // Sub County
        if (binding.subCountyInput.text.toString().trim().isEmpty()) {
            binding.subCountyLayout.error = "Sub County is required"
            isValid = false
        } else {
            binding.subCountyLayout.error = null
        }

        // Ward
        if (binding.wardInput.text.toString().trim().isEmpty()) {
            binding.wardLayout.error = "Ward is required"
            isValid = false
        } else {
            binding.wardLayout.error = null
        }

        // Village
        if (binding.villageInput.text.toString().trim().isEmpty()) {
            binding.villageLayout.error = "Village is required"
            isValid = false
        } else {
            binding.villageLayout.error = null
        }

        return isValid
    }

    private fun formatDateForApi(inputDate: String): String {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(inputDate)
        return outputFormat.format(date ?: Date())
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
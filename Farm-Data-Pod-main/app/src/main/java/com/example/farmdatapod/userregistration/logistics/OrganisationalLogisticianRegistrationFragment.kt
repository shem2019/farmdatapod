package com.example.farmdatapod.userregistration.logistics

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.OrganisationalLogistician
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.CarRegistrationAdapter
import com.example.farmdatapod.databinding.FragmentOrganisationalLogisticianRegistrationBinding
import com.example.farmdatapod.models.OrganisationLogisticianRequest
import com.example.farmdatapod.models.Car
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.network.RestClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrganisationalLogisticianRegistrationFragment : Fragment() {

    private var _binding: FragmentOrganisationalLogisticianRegistrationBinding? = null
    private val binding get() = _binding!!
    private lateinit var carAdapter: CarRegistrationAdapter
    private lateinit var dbHandler: DBHandler
    private lateinit var networkViewModel: NetworkViewModel
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private var selectedDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentOrganisationalLogisticianRegistrationBinding.inflate(inflater, container, false)
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHandler = DBHandler(requireContext()) // Initialize dbHandler here

        setupRecyclerView()
        setupSpinners()
        setupAddCarButton()
        setupBackButton()
        setupSubmitButton()
        setupDatePicker()



        binding.etDateOfRegistration.setOnClickListener {
            showDatePicker()
        }
    }

    private fun setupDatePicker() {
        binding.etDateOfRegistration.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = selectedDate ?: Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDate = calendar
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        selectedDate?.let { calendar ->
            val displayDate = displayDateFormat.format(calendar.time)
            binding.etDateOfRegistration.setText(displayDate)
        }
    }

    private fun getApiFormattedDate(): String? {
        return selectedDate?.let { calendar ->
            apiDateFormat.format(calendar.time)
        }
    }

    private fun setupSpinners() {
        // Hub Spinner
        val hubNames = dbHandler.allHubNames
        val hubAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            hubNames
        )

        binding.actvHub.setAdapter(hubAdapter)

        // Region Spinner
        val regionNames = resources.getStringArray(R.array.region_array)
        val regionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            regionNames
        )
        binding.actvRegion.setAdapter(regionAdapter)
    }

    private fun setupRecyclerView() {
        carAdapter = CarRegistrationAdapter(mutableListOf())
        binding.carsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = carAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupAddCarButton() {
        binding.addCarButton.setOnClickListener {
            carAdapter.addCar()
            binding.carsRecyclerView.scrollToPosition(carAdapter.itemCount - 1)
        }
    }

    private fun setupBackButton() {
        binding.toolbar.findViewById<View>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        val dateOfRegistration = binding.etDateOfRegistration.text.toString()
        if (dateOfRegistration.isBlank()) {
            binding.tilDateOfRegistration.error = "Date of Registration is required"
            isValid = false
        } else {
            try {
                dateFormat.parse(dateOfRegistration)
                binding.tilDateOfRegistration.error = null
            } catch (e: Exception) {
                binding.tilDateOfRegistration.error =
                    "Invalid date format. Use yyyy-MM-dd'T'HH:mm:ss"
                isValid = false
            }
        }
        if (binding.etOrganisationName.text.isNullOrBlank()) {
            binding.tilOrganisationName.error = "Organisation Name is required"
            isValid = false
        } else {
            binding.tilOrganisationName.error = null
        }

        if (binding.etDateOfRegistration.text.isNullOrBlank()) {
            binding.tilDateOfRegistration.error = "Date of Registration is required"
            isValid = false
        } else {
            binding.tilDateOfRegistration.error = null
        }

        if (binding.etLogisticianCode.text.isNullOrBlank()) {
            binding.tilLogisticianCode.error = "Logistician Code is required"
            isValid = false
        } else {
            binding.tilLogisticianCode.error = null
        }

        if (binding.etAddress.text.isNullOrBlank()) {
            binding.tilAddress.error = "Address is required"
            isValid = false
        } else {
            binding.tilAddress.error = null
        }

        if (binding.etRegistrationNumber.text.isNullOrBlank()) {
            binding.tilRegistrationNumber.error = "Registration Number is required"
            isValid = false
        } else {
            binding.tilRegistrationNumber.error = null
        }

        if (binding.actvHub.text.isNullOrBlank()) {
            binding.tilHub.error = "Hub is required"
            isValid = false
        } else {
            binding.tilHub.error = null
        }

        if (binding.etEmail.text.isNullOrBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(
                binding.etEmail.text.toString()
            ).matches()
        ) {
            binding.tilEmail.error = "Valid Email is required"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (binding.actvRegion.text.isNullOrBlank()) {
            binding.tilRegion.error = "Region is required"
            isValid = false
        } else {
            binding.tilRegion.error = null
        }

        if (binding.etPhoneNo.text.isNullOrBlank()) {
            binding.tilPhoneNo.error = "Phone Number is required"
            isValid = false
        } else {
            binding.tilPhoneNo.error = null
        }

        if (carAdapter.itemCount == 0) {
            showToast("Please add at least one car")
            isValid = false
        }

        return isValid
    }

    private fun setupSubmitButton() {
        // Observe network connectivity changes
        networkViewModel.networkLiveData.observe(viewLifecycleOwner) { isNetworkAvailable ->
            Log.d("OrgLogisticianFragment", "Network status changed: $isNetworkAvailable")

            binding.submitLogisticianButton.setOnClickListener {
                Log.d("OrgLogisticianFragment", "Submit button clicked")
                if (validateForm()) {
                    Log.d("OrgLogisticianFragment", "Form validated. Checking network availability...")
                    if (isNetworkAvailable) {
                        Log.d("OrgLogisticianFragment", "Network available. Proceeding with submission.")
                        submitRegistration()
                    } else {
                        Log.d("OrgLogisticianFragment", "No network available. Proceeding with offline submission.")
                        offlineSubmitLogistician()
                    }
                } else {
                    Log.d("OrgLogisticianFragment", "Form validation failed")
                    Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun submitRegistration() {
        val cars = carAdapter.getCars().map { car ->
            Car(
                car_body_type = car.car_body_type,
                car_model = car.car_model,
                driver1_name = car.driver1_name,
                driver2_name = car.driver2_name,
                number_plate = car.number_plate
            )
        }

        val requestModel = OrganisationLogisticianRequest(
            address = binding.etAddress.text.toString(),
            cars = cars,
            date_of_registration = getApiFormattedDate() ?: "",
            email = binding.etEmail.text.toString(),
            hub = binding.actvHub.text.toString(),
            logistician_code = binding.etLogisticianCode.text.toString(),
            name = binding.etOrganisationName.text.toString(),
            phone_number = binding.etPhoneNo.text.toString().toInt(),
            region = binding.actvRegion.text.toString(),
            registration_number = binding.etRegistrationNumber.text.toString().toInt()
        )

        context?.let { ctx ->
            RestClient.getApiService(ctx).registerOrganisationalLogistician(requestModel)
                .enqueue(object : Callback<OrganisationLogisticianRequest> {
                    override fun onResponse(
                        call: Call<OrganisationLogisticianRequest>,
                        response: Response<OrganisationLogisticianRequest>
                    ) {
                        if (response.isSuccessful) {
                            showToast("Registration successful")
                            clearForm()
                            // Handle successful registration (e.g., navigate to another screen)
                        } else {
                            showToast("Registration failed: ${response.message()}")
                        }
                    }

                    override fun onFailure(
                        call: Call<OrganisationLogisticianRequest>,
                        t: Throwable
                    ) {
                        showToast("Registration failed: ${t.message}")
                    }
                })
        } ?: run {
            showToast("Unable to access context")
        }
    }

    private fun offlineSubmitLogistician() {
        // Extract form values
        val address = binding.etAddress.text.toString()
        val dateOfRegistration = binding.etDateOfRegistration.text.toString()
        val email = binding.etEmail.text.toString()
        val hub = binding.actvHub.text.toString()
        val logisticianCode = binding.etLogisticianCode.text.toString()
        val organisationName = binding.etOrganisationName.text.toString()
        val phoneNumberString = binding.etPhoneNo.text.toString()
        val region = binding.actvRegion.text.toString()
        val registrationNumberString = binding.etRegistrationNumber.text.toString()

        // Convert phone number and registration number to integers with validation
        val phoneNumber = try {
            phoneNumberString.toInt()
        } catch (e: NumberFormatException) {
            showToast("Invalid phone number format. Please enter only digits.")
            return
        }

        val registrationNumber = try {
            registrationNumberString.toInt()
        } catch (e: NumberFormatException) {
            showToast("Invalid registration number format. Please enter only digits.")
            return
        }

        // Format the date for offline storage
        val parsedDate = displayDateFormat.parse(dateOfRegistration)
        val formattedDateOfRegistration = apiDateFormat.format(parsedDate!!)

        // Convert cars from com.example.farmdatapod.models.Car to com.example.farmdatapod.Car
        val carList: List<com.example.farmdatapod.Car> = carAdapter.getCars().map { modelCar ->
            com.example.farmdatapod.Car(
                id = 0,
                car_body_type = modelCar.car_body_type,
                car_model = modelCar.car_model,
                number_plate = modelCar.number_plate,
                driver1_name = modelCar.driver1_name,
                driver2_name = modelCar.driver2_name,
                is_offline = 1,
                individual_logistician_id = 0,
                organisation_logistician_id = 0
            )
        }

        // Create the OrganisationLogistician object for offline storage
        val logistician = OrganisationalLogistician().apply {
            this.address = address
            this.date_of_registration = formattedDateOfRegistration
            this.email = email
            this.hub = hub
            this.logistician_code = logisticianCode
            this.name = organisationName
            this.phone_number = phoneNumber.toString()
            this.region = region
            this.registration_number = registrationNumber.toString()
            this.cars = carList
        }

        // Create an instance of the database handler
        val dbHandler = DBHandler(context)

        // Insert logistician into the database
        val isInserted = dbHandler.insertOrganisationalLogistician(logistician)

        // Provide feedback
        if (isInserted) {
            Toast.makeText(context, "Form submitted offline", Toast.LENGTH_SHORT).show()
            clearForm()
        } else {
            Toast.makeText(context, "Failed to save registration to local database", Toast.LENGTH_SHORT).show()
        }

        Log.d("OrgLogisticianFragment", "Form submitted offline")
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun clearForm() {
        binding.apply {
            etOrganisationName.text?.clear()
            etDateOfRegistration.text?.clear()
            etLogisticianCode.text?.clear()
            etAddress.text?.clear()
            etRegistrationNumber.text?.clear()
            actvHub.text?.clear()
            etEmail.text?.clear()
            actvRegion.text?.clear()
            etPhoneNo.text?.clear()
        }
        carAdapter.clearCars()
    }
}
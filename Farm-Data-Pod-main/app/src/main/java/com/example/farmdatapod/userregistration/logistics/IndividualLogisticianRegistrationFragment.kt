package com.example.farmdatapod.userregistration.logistics

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.IndividualLogistician
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.CarRegistrationAdapter
import com.example.farmdatapod.databinding.FragmentIndividualLogisticianRegistrationBinding
import com.example.farmdatapod.models.Car
import com.example.farmdatapod.models.IndividualLogisticianRequestModel
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.network.RestClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class IndividualLogisticianFragment : Fragment() {

    private var _binding: FragmentIndividualLogisticianRegistrationBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkViewModel: NetworkViewModel

    private lateinit var carAdapter: CarRegistrationAdapter
    private val cars = mutableListOf<Car>()
    private lateinit var dbHandler: DBHandler

    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            dbHandler = DBHandler(context)
        } catch (e: Exception) {
            showToast("Error initializing database: ${e.message}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentIndividualLogisticianRegistrationBinding.inflate(inflater, container, false)
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton: ImageView = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        setupCarRecyclerView()
        setupAddCarButton()
        setupSpinners()
        setupSubmitButton()
        setupDatePicker()
    }

    private fun setupCarRecyclerView() {
        carAdapter = CarRegistrationAdapter(cars)
        binding.carsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = carAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        addInitialCarForm() // Add initial form
    }

    private fun addInitialCarForm() {
        val newCar = Car("", "", "", "", "")
        cars.add(newCar)
        carAdapter.notifyItemInserted(cars.size - 1)
        binding.carsRecyclerView.scrollToPosition(cars.size - 1)
    }

    private fun setupAddCarButton() {
        binding.addCarButton.setOnClickListener {
            addCar()
        }
    }

    private fun addCar() {
        val newCar = Car("", "", "", "", "")
        cars.add(newCar)
        carAdapter.notifyItemInserted(cars.size - 1)
        binding.carsRecyclerView.scrollToPosition(cars.size - 1)
    }

    private fun setupSpinners() {
        try {
            // Hub Spinner
            val hubNames = dbHandler.allHubNames
            val hubAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                hubNames
            )
            binding.hubInput.setAdapter(hubAdapter)

            // Region Spinner
            val regionNames = resources.getStringArray(R.array.region_array)
            val regionAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                regionNames
            )
            binding.regionInput.setAdapter(regionAdapter)
        } catch (e: Exception) {
            showToast("Error setting up spinners: ${e.message}")
        }
    }



    private fun setupDatePicker() {
        binding.dateOfBirthInput.setOnClickListener {
            showDatePicker()
        }
        binding.dateOfBirthInput.isFocusable = false
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val pickedDateTime = Calendar.getInstance()
            pickedDateTime.set(selectedYear, selectedMonth, selectedDay)

            // Display the date in human-readable format
            val displayDate = displayDateFormat.format(pickedDateTime.time)
            binding.dateOfBirthInput.setText(displayDate)

            // Store the API format date as a tag for later use
            val apiDate = apiDateFormat.format(pickedDateTime.time)
            binding.dateOfBirthInput.tag = apiDate
        }, year, month, day).show()
    }


    private fun isValidDateOfBirth(dateString: String): Boolean {
        return try {
            val dob = displayDateFormat.parse(dateString)
            if (dob == null) {
                showToast("Invalid date format")
                return false
            }

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, -18)

            if (dob.after(calendar.time)) {
                showToast("Must be at least 18 years old")
                return false
            }

            true
        } catch (e: Exception) {
            showToast("Error parsing date: ${e.message}")
            false
        }
    }

    private fun validateCar(car: Car): Boolean {
        when {
            car.car_body_type.isBlank() -> {
                showToast("Car body type is required")
                return false
            }

            car.car_model.isBlank() -> {
                showToast("Car model is required")
                return false
            }

            car.driver1_name.isBlank() -> {
                showToast("First driver name is required")
                return false
            }

            car.driver2_name.isBlank() -> {
                showToast("Second driver name is required")
                return false
            }

            car.number_plate.isBlank() -> {
                showToast("Number plate is required")
                return false
            }

            !isValidNumberPlate(car.number_plate) -> {
                showToast("Invalid number plate format. Expected format: ABC 123D")
                return false
            }
        }
        return true
    }

    private fun isValidNumberPlate(numberPlate: String): Boolean {
        val numberPlateRegex = Regex("^[A-Z]{3}\\s\\d{3}[A-Z]$")
        return numberPlateRegex.matches(numberPlate)
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (binding.addressInput.text.isNullOrBlank()) {
            binding.addressInput.error = "Address is required"
            isValid = false
        } else {
            binding.addressInput.error = null
        }

        if (!isValidDateOfBirth(binding.dateOfBirthInput.text.toString())) {
            binding.dateOfBirthInput.error = "Invalid date of birth"
            isValid = false
        } else {
            binding.dateOfBirthInput.error = null
        }

        if (!isValidEmail(binding.emailInput.text.toString())) {
            binding.emailInput.error = "Invalid email address"
            isValid = false
        } else {
            binding.emailInput.error = null
        }

        if (binding.hubInput.text.isNullOrBlank()) {
            binding.hubInput.error = "Hub is required"
            isValid = false
        } else {
            binding.hubInput.error = null
        }

        if (!isValidIdNumber(binding.idNumberInput.text.toString())) {
            binding.idNumberInput.error = "Invalid ID number"
            isValid = false
        } else {
            binding.idNumberInput.error = null
        }

        if (binding.lastNameInput.text.isNullOrBlank()) {
            binding.lastNameInput.error = "Last name is required"
            isValid = false
        } else {
            binding.lastNameInput.error = null
        }

        if (binding.logisticianCodeInput.text.isNullOrBlank()) {
            binding.logisticianCodeInput.error = "Logistician code is required"
            isValid = false
        } else {
            binding.logisticianCodeInput.error = null
        }

        if (binding.otherNameInput.text.isNullOrBlank()) {
            binding.otherNameInput.error = "Other name is required"
            isValid = false
        } else {
            binding.otherNameInput.error = null
        }

        if (!isValidPhoneNumber(binding.phoneNoInput.text.toString())) {
            binding.phoneNoInput.error = "Invalid phone number"
            isValid = false
        } else {
            binding.phoneNoInput.error = null
        }

        if (binding.regionInput.text.isNullOrBlank()) {
            binding.regionInput.error = "Region is required"
            isValid = false
        } else {
            binding.regionInput.error = null
        }

        if (cars.isEmpty()) {
            showToast("Please add at least one car")
            isValid = false
        } else {
            for (car in cars) {
                if (!validateCar(car)) {
                    isValid = false
                    break
                }
            }
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidIdNumber(idNumber: String): Boolean {
        return idNumber.length == 8 && idNumber.all { it.isDigit() }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val phoneRegex = Regex("^\\+?\\d{10,14}$")
        return phoneRegex.matches(phoneNumber)
    }

    private fun setupSubmitButton() {
        // Observe network connectivity changes
        networkViewModel.networkLiveData.observe(viewLifecycleOwner) { isNetworkAvailable ->
            Log.d("LogisticianFragment", "Network status changed: $isNetworkAvailable")

            binding.submitLogisticianButton.setOnClickListener {
                Log.d("LogisticianFragment", "Submit button clicked")
                if (validateForm()) {
                    Log.d("LogisticianFragment", "Form validated. Checking network availability...")
                    if (isNetworkAvailable) {
                        Log.d(
                            "LogisticianFragment",
                            "Network available. Proceeding with submission."
                        )
                        submitForm()
                    } else {
                        Log.d(
                            "LogisticianFragment",
                            "No network available. Proceeding with offline submission."
                        )
                        offlineSubmitLogistician()
                    }
                } else {
                    Log.d("LogisticianFragment", "Form validation failed")
                    Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun submitForm() {
        val address = binding.addressInput.text.toString()
        val dateOfBirth = binding.dateOfBirthInput.tag as? String ?: ""
        val email = binding.emailInput.text.toString()
        val hub = binding.hubInput.text.toString()
        val idNumber = binding.idNumberInput.text.toString().toInt()
        val lastName = binding.lastNameInput.text.toString()
        val logisticianCode = binding.logisticianCodeInput.text.toString()
        val otherName = binding.otherNameInput.text.toString()
        val phoneNumberString = binding.phoneNoInput.text.toString()
        val region = binding.regionInput.text.toString()

        val phoneNumber = try {
            phoneNumberString.toInt()
        } catch (e: NumberFormatException) {
            showToast("Invalid phone number format. Please enter only digits.")
            return
        }

        val carList = cars

        val parsedDate = displayDateFormat.parse(dateOfBirth)
        val formattedDateOfBirth = apiDateFormat.format(parsedDate!!)

        val requestModel = IndividualLogisticianRequestModel(
            address, carList, formattedDateOfBirth, email, hub, idNumber, lastName,
            logisticianCode, otherName, phoneNumber, region
        )

        context?.let { ctx ->
            RestClient.getApiService(ctx).registerIndividualLogistician(requestModel)
                .enqueue(object : Callback<IndividualLogisticianRequestModel> {
                    override fun onResponse(
                        call: Call<IndividualLogisticianRequestModel>,
                        response: Response<IndividualLogisticianRequestModel>
                    ) {
                        if (response.isSuccessful) {
                            showToast("Registration successful")
                            displaySubmittedInfo(requestModel)
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("API_ERROR", "Error response: $errorBody")
                            showToast("Registration failed: ${response.code()} - ${response.message()}")
                        }
                    }

                    override fun onFailure(
                        call: Call<IndividualLogisticianRequestModel>,
                        t: Throwable
                    ) {
                        Log.e("API_ERROR", "Network error", t)
                        showToast("Network error: ${t.message}")
                    }
                })
        }
    }

    private fun offlineSubmitLogistician() {
        // Extract logistician form values
        val addressString = binding.addressInput.text.toString()
        val dateOfBirth = binding.dateOfBirthInput.tag as? String ?: ""
        val emailString = binding.emailInput.text.toString()
        val hubString = binding.hubInput.text.toString()
        val idNumberString = binding.idNumberInput.text.toString()
        val lastName = binding.lastNameInput.text.toString()
        val logisticianCode = binding.logisticianCodeInput.text.toString()
        val otherName = binding.otherNameInput.text.toString()
        val phoneNumberString = binding.phoneNoInput.text.toString()
        val regionString = binding.regionInput.text.toString()

        // Convert idNumber and phoneNumber to strings with validation
        val idNumber = idNumberString.toIntOrNull() ?: 0
        val phoneNumber = try {
            phoneNumberString.toInt()
        } catch (e: NumberFormatException) {
            showToast("Invalid phone number format. Please enter only digits.")
            return
        }

        // Format the date for offline storage
        val parsedDate = displayDateFormat.parse(dateOfBirth)
        val formattedDateOfBirth = apiDateFormat.format(parsedDate!!)

        // Convert cars from com.example.farmdatapod.models.Car to com.example.farmdatapod.Car
        val carList: List<com.example.farmdatapod.Car> = cars.map { modelCar ->
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

        // Create the IndividualLogistician object for offline storage
        val logistician = IndividualLogistician().apply {
            address = addressString
            date_of_birth = formattedDateOfBirth
            email = emailString
            hub = hubString
            id_number = idNumber.toString()
            last_name = lastName
            logistician_code = logisticianCode
            other_name = otherName
            phone_number = phoneNumber.toString()
            region = regionString
            cars = carList
        }

        // Create an instance of the database handler
        val dbHandler = DBHandler(context)

        // Insert logistician into the database
        val isInserted = dbHandler.insertIndividualLogistician(logistician)

        // Provide feedback
        if (isInserted) {
            Toast.makeText(context, "Form submitted offline", Toast.LENGTH_SHORT).show()
            clearForm()
        } else {
            Toast.makeText(
                context,
                "Failed to save registration to local database",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun formatDateForDisplay(apiDateString: String): String {
        val apiDate = apiDateFormat.parse(apiDateString)
        return displayDateFormat.format(apiDate!!)
    }

    private fun displaySubmittedInfo(model: IndividualLogisticianRequestModel) {
        val message = """
    Last Name: ${model.last_name}
    Other Name: ${model.other_name}
    Logistician Code: ${model.logistician_code}
    ID Number: ${model.id_number}
    Email: ${model.email}
    Phone Number: ${model.phone_number}
    Date of Birth: ${formatDateForDisplay(model.date_of_birth)}
    Address: ${model.address}
    Hub: ${model.hub}
    Region: ${model.region}
    
    Cars:
    ${
            model.cars.joinToString("\n") { car ->
                "- ${car.number_plate} (${car.car_body_type} ${car.car_model})"
            }
        }
""".trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Submitted Information")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                clearForm()
            }
            .show()
    }

    private fun clearForm() {
        binding.apply {
            lastNameInput.text?.clear()
            otherNameInput.text?.clear()
            logisticianCodeInput.text?.clear()
            idNumberInput.text?.clear()
            emailInput.text?.clear()
            phoneNoInput.text?.clear()
            dateOfBirthInput.text?.clear()
            addressInput.text?.clear()
            hubInput.text?.clear()
            regionInput.text?.clear()
        }
        cars.clear()
        carAdapter.notifyDataSetChanged()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
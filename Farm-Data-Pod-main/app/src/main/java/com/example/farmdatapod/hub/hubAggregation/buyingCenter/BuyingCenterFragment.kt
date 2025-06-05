package com.example.farmdatapod.hub.hubAggregation.buyingCenter

import android.app.DatePickerDialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.KeyContact
import com.example.farmdatapod.adapter.KeyContactAdapter
import com.example.farmdatapod.databinding.FragmentBuyingCenterBinding
import com.example.farmdatapod.hub.hubAggregation.buyingCenter.data.BuyingCenterEntity
import com.example.farmdatapod.hub.hubAggregation.buyingCenter.data.BuyingCenterViewModel
import com.example.farmdatapod.hub.hubRegistration.data.HubRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BuyingCenterFragment : Fragment() {

    private var _binding: FragmentBuyingCenterBinding? = null
    private val binding get() = _binding!!
    private lateinit var keyContactAdapter: KeyContactAdapter
    private val viewModel: BuyingCenterViewModel by viewModels()
    private var selectedHubId: Int? = null  // Add this property to the Fragment
    private lateinit var hubRepository: HubRepository
    private val calendar = Calendar.getInstance()
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuyingCenterBinding.inflate(inflater, container, false)
        hubRepository = HubRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupDropdowns()
        setupYearEstablished()
        setupKeyContacts()
        setupValidation()
        setupObservers()
    }
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.savingState.collect { state ->
                when (state) {
                    is BuyingCenterViewModel.SavingState.Idle -> {
                        // Initial state, no action needed
                    }
                    is BuyingCenterViewModel.SavingState.Saving -> {
                        // Show loading indicator
                        binding.register.isEnabled = false
                        binding.register.text = "Saving..."
                    }
                    is BuyingCenterViewModel.SavingState.Success -> {
                        Toast.makeText(requireContext(), "Saved successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    is BuyingCenterViewModel.SavingState.Error -> {
                        binding.register.isEnabled = true
                        binding.register.text = "Register"
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorState.collect { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }


    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupDropdowns() {
        // Set up county dropdown
        val counties = resources.getStringArray(R.array.region_array)
        val countyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, counties)
        (binding.inputCounty as? AutoCompleteTextView)?.setAdapter(countyAdapter)

        // Set up ownership dropdown
        val ownershipOptions = arrayOf("Owned", "Leased")
        val ownershipAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ownershipOptions)
        (binding.inputOwnership as? AutoCompleteTextView)?.setAdapter(ownershipAdapter)

        // Set up building type dropdown
        val buildingTypes = resources.getStringArray(R.array.building_type_array)
        val buildingTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, buildingTypes)
        (binding.inputTypeOfBuilding as? AutoCompleteTextView)?.setAdapter(buildingTypeAdapter)

        // Fetch and setup hubs dropdown
        lifecycleScope.launch {
            hubRepository.getAllHubs().collect { hubList ->
                val hubAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, hubList.map { it.hubName })
                (binding.inputHub as? AutoCompleteTextView)?.apply {
                    setAdapter(hubAdapter)
                    setOnItemClickListener { _, _, position, _ ->
                        selectedHubId = hubList[position].id
                    }
                }
            }
        }
    }

    private fun setupYearEstablished() {
        // Initialize the input with current value if any
        val calendar = Calendar.getInstance()

        binding.inputYearEstablished.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, _, _ ->
                    // Set the date to January 1st of the selected year
                    calendar.set(Calendar.YEAR, selectedYear)
                    calendar.set(Calendar.MONTH, Calendar.JANUARY)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)

                    // Store the API format date (hidden from user)
                    val apiDate = apiDateFormat.format(calendar.time)
                    viewModel.yearEstablishedApiFormat = apiDate  // Add this property to your ViewModel

                    // Show only the year to the user
                    val displayYear = selectedYear.toString()
                    binding.inputYearEstablished.setText(displayYear)
                },
                year, 0, 1
            )

            // Modify the DatePicker to show only the year
            datePickerDialog.datePicker.apply {
                // Hide day and month spinners
                val daySpinnerId = Resources.getSystem().getIdentifier("day", "id", "android")
                val monthSpinnerId = Resources.getSystem().getIdentifier("month", "id", "android")
                findViewById<View>(daySpinnerId)?.visibility = View.GONE
                findViewById<View>(monthSpinnerId)?.visibility = View.GONE
            }

            datePickerDialog.show()
        }
    }

    private fun setupKeyContacts() {
        // Initialize RecyclerView with LinearLayoutManager
        binding.recyclerViewKeyContacts.layoutManager = LinearLayoutManager(context)

        // Initialize adapter with one empty contact
        keyContactAdapter = KeyContactAdapter(
            mutableListOf(KeyContact()),
            ::showContactDatePicker
        )
        binding.recyclerViewKeyContacts.adapter = keyContactAdapter

        binding.btnAddAnotherContact.setOnClickListener {
            keyContactAdapter.addContact()
        }
    }

    private fun showContactDatePicker(editText: TextInputEditText, position: Int) {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val apiDate = apiDateFormat.format(calendar.time)
                val displayDate = displayDateFormat.format(calendar.time)
                keyContactAdapter.updateContactDate(position, apiDate, displayDate)
                editText.setText(displayDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun setupValidation() {
        binding.register.setOnClickListener {
            if (validateInputs()) {
                saveData()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        with(binding) {
            // Validate required fields
            if (inputHub.text.isNullOrBlank()) {
                hubLayout.error = "Hub is required"
                isValid = false
            } else {
                hubLayout.error = null
            }

            if (inputCounty.text.isNullOrBlank()) {
                countyLayout.error = "County is required"
                isValid = false
            } else {
                countyLayout.error = null
            }

            if (inputBuyingCenterName.text.isNullOrBlank()) {
                inputLayoutBuyingCenterName.error = "Buying center name is required"
                isValid = false
            } else {
                inputLayoutBuyingCenterName.error = null
            }

            if (inputBuyingCenterCode.text.isNullOrBlank()) {
                inputLayoutHubCode.error = "Buying center code is required"
                isValid = false
            } else {
                inputLayoutHubCode.error = null
            }

            if (inputYearEstablished.text.isNullOrBlank()) {
                inputLayoutYearEstablished.error = "Year established is required"
                isValid = false
            } else {
                inputLayoutYearEstablished.error = null
            }

            if (inputOwnership.text.isNullOrBlank()) {
                inputLayoutOwnership.error = "Ownership is required"
                isValid = false
            } else {
                inputLayoutOwnership.error = null
            }
        }

        // Validate key contacts
        val contacts = keyContactAdapter.getContacts()
        if (contacts.isEmpty() || contacts.all { it.firstName.isBlank() || it.lastName.isBlank() }) {
            Toast.makeText(requireContext(), "At least one key contact is required", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }


    private fun saveData() {
        with(binding) {
            try {
                // Check if hub is selected
                if (selectedHubId == null) {
                    Toast.makeText(requireContext(), "Please select a hub first", Toast.LENGTH_SHORT).show()
                    return
                }

                val contacts = keyContactAdapter.getContacts().first()

                val buyingCenter = BuyingCenterEntity(
                    hubId = selectedHubId!!,  // Now we have the hubId
                    hub = inputHub.text.toString(),
                    county = inputCounty.text.toString(),
                    subCounty = inputSubCounty.text.toString(),
                    ward = inputWard.text.toString(),
                    village = inputVillage.text.toString(),
                    buyingCenterName = inputBuyingCenterName.text.toString(),
                    buyingCenterCode = inputBuyingCenterCode.text.toString(),
                    address = inputAddress.text.toString(),
                    yearEstablished = viewModel.yearEstablishedApiFormat ?: "1970-01-01T00:00:00",  // Use default if not set
                    ownership = inputOwnership.text.toString(),
                    floorSize = inputFloorSize.text.toString(),
                    facilities = inputFacilities.text.toString(),
                    inputCenter = inputCenter.text.toString(),
                    typeOfBuilding = inputTypeOfBuilding.text.toString(),
                    location = inputLocation.text.toString(),
                    userId = null,
                    syncStatus = false,
                    contactOtherName = contacts.firstName,
                    contactLastName = contacts.lastName,
                    contactGender = contacts.gender,
                    contactRole = contacts.role,
                    contactDateOfBirth = contacts.dateOfBirth,
                    contactEmail = contacts.email,
                    contactPhoneNumber = contacts.phoneNumber,
                    contactIdNumber = contacts.idNumber.toIntOrNull() ?: 0,
                    lastModified = System.currentTimeMillis()
                )

                viewModel.saveBuyingCenter(buyingCenter)
                binding.register.isEnabled = false

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error creating buying center: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
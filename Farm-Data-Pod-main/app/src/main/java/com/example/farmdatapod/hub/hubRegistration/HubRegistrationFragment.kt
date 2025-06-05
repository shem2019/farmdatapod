package com.example.farmdatapod.hub.hubRegistration

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.hub.hubRegistration.data.Hub
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.KeyContactAdapter
import com.example.farmdatapod.databinding.FragmentHubRegistrationBinding
import com.example.farmdatapod.hub.hubRegistration.data.HubRepository
import com.example.farmdatapod.utils.NetworkUtils
import com.example.farmdatapod.utils.SharedPrefs
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HubRegistrationFragment : Fragment(R.layout.fragment_hub_registration) {

    private var _binding: FragmentHubRegistrationBinding? = null
    private val binding get() = _binding!!
    private lateinit var keyContactAdapter: KeyContactAdapter
    private lateinit var hubRepository: HubRepository

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHubRegistrationBinding.inflate(inflater, container, false)
        hubRepository = HubRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        setupToolbar()
        setupBackNavigation()
        setupSpinners()
        setupDatePicker()
        setupKeyContacts()
        setupRegisterButton()
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(
                ContextCompat.getDrawable(requireContext(), R.drawable.baseline_arrow_back_24)
                    ?.apply {
                        setTint(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
            )
        }
    }

    private fun setupBackNavigation() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun navigateBack() {
        requireActivity().supportFragmentManager.popBackStack()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupSpinners() {
        // Region dropdown
        val regionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.region_array)
        )
        (binding.inputRegion.editText as? AutoCompleteTextView)?.setAdapter(regionAdapter)

        // Building type dropdown
        val buildingAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.building_type_array)
        )
        (binding.inputTypeOfBuilding.editText as? AutoCompleteTextView)?.setAdapter(buildingAdapter)

        // Ownership dropdown
        val ownershipOptions = arrayOf("Owned", "Leased")
        val ownershipAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            ownershipOptions
        )
        binding.inputOwnership.setAdapter(ownershipAdapter)
    }

    private fun setupDatePicker() {
        binding.inputYearEstablished.setOnClickListener {
            showDatePickerDialog(binding.inputYearEstablished, null)
        }
    }

    private fun showDatePickerDialog(editText: TextInputEditText, position: Int? = null) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)

            val apiDate = apiDateFormat.format(selectedDate.time)
            val displayDate = displayDateFormat.format(selectedDate.time)

            editText.setText(displayDate)

            if (position != null) {
                keyContactAdapter.updateContactDate(position, apiDate, displayDate)
            } else {
                editText.tag = apiDate
            }
        }, year, month, day).show()
    }

    private fun setupKeyContacts() {
        keyContactAdapter = KeyContactAdapter(mutableListOf()) { editText, position ->
            showDatePickerDialog(editText, position)
        }
        binding.recyclerViewKeyContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = keyContactAdapter
        }

        keyContactAdapter.addContact()

        binding.btnAddAnotherContact.setOnClickListener {
            keyContactAdapter.addContact()
        }
    }

    private fun setupRegisterButton() {
        binding.register.setOnClickListener {
            Log.d("HubRegistrationFragment", "Register button clicked")
            if (validateForms()) {
                Log.d("HubRegistrationFragment", "Forms validated. Proceeding with save...")
                saveHub()
            } else {
                Log.d("HubRegistrationFragment", "Form validation failed")
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun saveHub() {
        val hub = createHub()
        val isOnline = NetworkUtils.isNetworkAvailable(requireContext())

        lifecycleScope.launch {
            try {
                val result = hubRepository.saveHub(hub, isOnline)
                result.fold(
                    onSuccess = {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                if (isOnline) "Hub registered successfully" else "Hub saved offline",
                                Toast.LENGTH_SHORT
                            ).show()
                            clearFormFields()
                        }
                    },
                    onFailure = { exception ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Error saving hub: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error saving hub: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun createHub(): Hub {
        val firstContact = keyContactAdapter.getContacts().firstOrNull()

        return Hub(
            region = (binding.inputRegion.editText as? AutoCompleteTextView)?.text?.toString() ?: "",
            hubName = binding.inputHubName.text.toString(),
            hubCode = binding.inputHubCode.text.toString(),
            address = binding.inputAddress.text.toString(),
            yearEstablished = binding.inputYearEstablished.tag as? String
                ?: binding.inputYearEstablished.text.toString(),
            ownership = binding.inputOwnership.text.toString(),
            floorSize = binding.inputFloorSize.text.toString(),
            facilities = binding.inputFacilities.text.toString(),
            inputCenter = binding.inputCenter.text.toString(),
            typeOfBuilding = (binding.inputTypeOfBuilding.editText as? AutoCompleteTextView)?.text?.toString() ?: "",
            longitude = binding.inputLocation.text.toString(),
            latitude = binding.inputLocation.text.toString(),
            userId = context?.let { SharedPrefs(it).getUserId() }?.toString() ?: "0",
            syncStatus = false,
            contactOtherName = firstContact?.firstName ?: "",
            contactLastName = firstContact?.lastName ?: "",
            contactGender = firstContact?.gender ?: "",
            contactRole = firstContact?.role ?: "",
            contactDateOfBirth = firstContact?.dateOfBirth ?: "",
            contactEmail = firstContact?.email ?: "",
            contactPhoneNumber = firstContact?.phoneNumber ?: "",
            contactIdNumber = firstContact?.idNumber?.toIntOrNull() ?: 0,
            hubId = null,
            buyingCenterId = null
        )
    }

    private fun clearFormFields() {
        binding.apply {
            (inputRegion.editText as? AutoCompleteTextView)?.text?.clear()
            inputHubName.text?.clear()
            inputHubCode.text?.clear()
            inputAddress.text?.clear()
            inputYearEstablished.text?.clear()
            inputOwnership.text?.clear()
            inputFloorSize.text?.clear()
            inputLocation.text?.clear()
            inputFacilities.text?.clear()
            inputCenter.text?.clear()
            (inputTypeOfBuilding.editText as? AutoCompleteTextView)?.text?.clear()
        }
        keyContactAdapter.setContacts(mutableListOf())
        keyContactAdapter.addContact()
    }

    private fun validateForms(): Boolean {
        if (binding.inputHubName.text.toString().isEmpty() ||
            binding.inputHubCode.text.toString().isEmpty() ||
            binding.inputYearEstablished.text.toString().isEmpty() ||
            binding.inputFloorSize.text.toString().isEmpty() ||
            binding.inputLocation.text.toString().isEmpty() ||
            binding.inputFacilities.text.toString().isEmpty() ||
            binding.inputCenter.text.toString().isEmpty()
        ) {
            return false
        }

        for (contact in keyContactAdapter.getContacts()) {
            if (contact.firstName.isEmpty() ||
                contact.lastName.isEmpty() ||
                contact.gender.isEmpty() ||
                contact.role.isEmpty() ||
                contact.dateOfBirth.isEmpty() ||
                contact.email.isEmpty() ||
                contact.phoneNumber.isEmpty() ||
                contact.idNumber.isEmpty()
            ) {
                return false
            }
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
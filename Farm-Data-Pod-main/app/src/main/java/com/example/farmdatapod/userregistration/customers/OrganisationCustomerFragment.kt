package com.example.farmdatapod.userregistration.customers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentOrganisationCustomerBinding
import com.example.farmdatapod.models.OrganisationCustomerRequestModel
import com.example.farmdatapod.models.Product
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.adapter.ProductAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class OrganisationCustomerFragment : Fragment() {

    private var _binding: FragmentOrganisationCustomerBinding? = null
    private val binding get() = _binding!!
    private lateinit var productAdapter: ProductAdapter
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrganisationCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupBackButton()
        setupDatePickers()
        setupCountyDropdown()
        setupSubmitButton()
        setupAddProductButton()

        // Add initial product form
        addInitialProductForm()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter()
        binding.productsRecyclerView.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupDatePickers() {
        val dateFields = listOf(
            binding.dateOfRegistrationEditText,
            binding.director1DateOfBirthEditText,
            binding.director2DateOfBirthEditText,
            binding.etKeyContactDateOfBirth
        )

        dateFields.forEach { dateField ->
            dateField.setOnClickListener {
                showDatePicker(dateField)
            }
        }
    }

    private fun showDatePicker(dateField: TextInputEditText) {
        val datePicker = MaterialDatePicker.Builder.datePicker().build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            val displayDate = displayDateFormat.format(date)
            dateField.setText(displayDate)

            // Store the API format date as a tag for later use
            val apiDate = apiDateFormat.format(date)
            dateField.tag = apiDate
        }
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun setupCountyDropdown() {
        val counties = resources.getStringArray(R.array.region_array)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, counties)
        binding.countyAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateAllFields()) {
                submitCustomerData(createCustomerData())
            }
        }
    }

    private fun setupAddProductButton() {
        binding.addProductButton.setOnClickListener {
            val newProduct = Product(
                category = "",
                packaging = "",
                products_interested_in = "",
                quality = "",
                volume_in_kgs = "",
                frequency = ""
            )
            productAdapter.addProduct(newProduct)
        }
    }

    private fun addInitialProductForm() {
        val initialProduct = Product(
            category = "",
            packaging = "",
            products_interested_in = "",
            quality = "",
            volume_in_kgs = "",
            frequency = ""
        )
        productAdapter.addProduct(initialProduct)
    }

    private fun validateAllFields(): Boolean {
        var isValid = true

        // Validate all fields (company info, director info, key contact info)
        isValid = validateField(binding.companyNameEditText, "Company Name") && isValid
        isValid = validateField(binding.addressEditText, "Address") && isValid
        isValid = validateField(binding.registrationNumberEditText, "Registration Number") && isValid
        isValid = validateField(binding.countyAutoCompleteTextView, "County") && isValid
        isValid = validateField(binding.sectorEditText, "Sector") && isValid
        isValid = validateField(binding.subCountyEditText, "Sub County") && isValid
        isValid = validateEmail(binding.emailEditText) && isValid
        isValid = validateField(binding.wardEditText, "Ward") && isValid
        isValid = validatePhone(binding.phoneNoEditText) && isValid
        isValid = validateField(binding.villageEditText, "Village") && isValid
        isValid = validateField(binding.dateOfRegistrationEditText, "Date of Registration") && isValid

        // Director 1
        isValid = validateField(binding.director1OtherNameEditText, "Director 1 Other Name") && isValid
        isValid = validateField(binding.director1LastNameEditText, "Director 1 Last Name") && isValid
        isValid = validateField(binding.director1IdNumberEditText, "Director 1 ID Number") && isValid
        isValid = validateEmail(binding.director1EmailEditText) && isValid
        isValid = validatePhone(binding.director1PhoneEditText) && isValid
        isValid = validateField(binding.director1DateOfBirthEditText, "Director 1 Date of Birth") && isValid

        // Director 2
        isValid = validateField(binding.director2OtherNameEditText, "Director 2 Other Name") && isValid
        isValid = validateField(binding.director2LastNameEditText, "Director 2 Last Name") && isValid
        isValid = validateField(binding.director2IdNumberEditText, "Director 2 ID Number") && isValid
        isValid = validateEmail(binding.director2EmailEditText) && isValid
        isValid = validatePhone(binding.director2PhoneEditText) && isValid
        isValid = validateField(binding.director2DateOfBirthEditText, "Director 2 Date of Birth") && isValid

        // Key Contact
        isValid = validateField(binding.etKeyContactFirstName, "Key Contact First Name") && isValid
        isValid = validateField(binding.etKeyContactLastName, "Key Contact Last Name") && isValid
        isValid = validateField(binding.etKeyContactGender, "Key Contact Gender") && isValid
        isValid = validateField(binding.etKeyContactRole, "Key Contact Role") && isValid
        isValid = validateField(binding.etKeyContactDateOfBirth, "Key Contact Date of Birth") && isValid
        isValid = validateEmail(binding.etKeyContactEmail) && isValid
        isValid = validatePhone(binding.etKeyContactPhoneNumber) && isValid
        isValid = validateField(binding.etKeyContactIdNumber, "Key Contact ID Number") && isValid

        return isValid
    }

    private fun validateField(view: View, fieldName: String): Boolean {
        return when (view) {
            is TextInputEditText -> {
                if (view.text.toString().trim().isEmpty()) {
                    view.error = "$fieldName is required"
                    false
                } else {
                    view.error = null
                    true
                }
            }
            is com.google.android.material.textfield.MaterialAutoCompleteTextView -> {
                if (view.text.toString().trim().isEmpty()) {
                    view.error = "$fieldName is required"
                    false
                } else {
                    view.error = null
                    true
                }
            }
            else -> true
        }
    }

    private fun validateEmail(view: TextInputEditText): Boolean {
        val emailRegex = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        val email = view.text.toString().trim()
        return when {
            email.isEmpty() -> {
                view.error = "Email is required"
                false
            }
            !email.matches(emailRegex.toRegex()) -> {
                view.error = "Invalid email format"
                false
            }
            else -> {
                view.error = null
                true
            }
        }
    }

    private fun validatePhone(view: TextInputEditText): Boolean {
        val phone = view.text.toString().trim()
        return when {
            phone.isEmpty() -> {
                view.error = "Phone number is required"
                false
            }
            phone.length < 10 -> {
                view.error = "Phone number should be at least 10 digits"
                false
            }
            else -> {
                view.error = null
                true
            }
        }
    }

    private fun createCustomerData(): OrganisationCustomerRequestModel {
        return OrganisationCustomerRequestModel(
            company_name = binding.companyNameEditText.text.toString(),
            county = binding.countyAutoCompleteTextView.text.toString(),
            customer_code = generateCustomerCode(),
            date_of_birth1 = binding.director1DateOfBirthEditText.tag as? String ?: "",
            date_of_birth2 = binding.director2DateOfBirthEditText.tag as? String ?: "",
            date_of_birth3 = binding.etKeyContactDateOfBirth.tag as? String ?: "",
            date_of_registration = binding.dateOfRegistrationEditText.tag as? String ?: "",
            email = binding.emailEditText.text.toString(),
            email1 = binding.director1EmailEditText.text.toString(),
            email2 = binding.director2EmailEditText.text.toString(),
            email3 = binding.etKeyContactEmail.text.toString(),
            gender1 = binding.director1GenderRadioGroup.checkedRadioButtonId.let { if (it == R.id.maleRadioButton) "Male" else "Female" },
            gender2 = binding.director2GenderRadioGroup.checkedRadioButtonId.let { if (it == R.id.maleRadioButton) "Male" else "Female" },
            gender3 = binding.etKeyContactGender.text.toString(),
            id_number1 = binding.director1IdNumberEditText.text.toString().toIntOrNull() ?: 0,
            id_number2 = binding.director2IdNumberEditText.text.toString().toIntOrNull() ?: 0,
            id_number3 = binding.etKeyContactIdNumber.text.toString().toIntOrNull() ?: 0,
            last_name1 = binding.director1LastNameEditText.text.toString(),
            last_name2 = binding.director2LastNameEditText.text.toString(),
            last_name3 = binding.etKeyContactLastName.text.toString(),
            other_name1 = binding.director1OtherNameEditText.text.toString(),
            other_name2 = binding.director2OtherNameEditText.text.toString(),
            other_name3 = binding.etKeyContactFirstName.text.toString(),
            phone_number = binding.phoneNoEditText.text.toString().toIntOrNull() ?: 0,
            phone_number1 = binding.director1PhoneEditText.text.toString().toLongOrNull() ?: 0,
            phone_number2 = binding.director2PhoneEditText.text.toString().toLongOrNull() ?: 0,
            phone_number3 = binding.etKeyContactPhoneNumber.text.toString().toLongOrNull() ?: 0,
            products = getProductsList(),
            registration_number = binding.registrationNumberEditText.text.toString().toIntOrNull() ?: 0,
            sector = binding.sectorEditText.text.toString(),
            sub_county = binding.subCountyEditText.text.toString(),
            village = binding.villageEditText.text.toString(),
            ward = binding.wardEditText.text.toString()
        )
    }

    private fun getProductsList(): List<Product> {
        return productAdapter.getProducts()
    }

    private fun generateCustomerCode(): String {
        // Implement your logic to generate a customer code
        return "CUST${System.currentTimeMillis()}"
    }

    private fun submitCustomerData(customerData: OrganisationCustomerRequestModel) {
        context?.let { ctx ->
            RestClient.getApiService(ctx).registerOrganisationCustomer(customerData)
                .enqueue(object : Callback<OrganisationCustomerRequestModel> {
                    override fun onResponse(
                        call: Call<OrganisationCustomerRequestModel>,
                        response: Response<OrganisationCustomerRequestModel>
                    ) {
                        if (response.isSuccessful) {
                            Snackbar.make(binding.root, "Form submitted successfully", Snackbar.LENGTH_LONG).show()
                            // Navigate to next screen or clear form
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Snackbar.make(binding.root, "Error: $errorBody", Snackbar.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<OrganisationCustomerRequestModel>, t: Throwable) {
                        Snackbar.make(binding.root, "Network error: ${t.message}", Snackbar.LENGTH_LONG).show()
                    }
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
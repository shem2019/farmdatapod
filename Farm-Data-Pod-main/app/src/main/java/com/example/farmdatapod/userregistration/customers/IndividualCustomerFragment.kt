package com.example.farmdatapod.userregistration.customers

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentIndividualCustomerBinding
import com.example.farmdatapod.adapter.ProductAdapter
import com.example.farmdatapod.models.Product
import com.example.farmdatapod.models.IndividualCustomerRequestModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.utils.DialogUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class IndividualCustomerFragment : Fragment() {

    private var _binding: FragmentIndividualCustomerBinding? = null
    private val binding get() = _binding!!
    private lateinit var productAdapter: ProductAdapter
    private var customerCode: String = ""
    private lateinit var loadingDialog: AlertDialog
    private lateinit var dialogUtils: DialogUtils
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIndividualCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialogUtils = DialogUtils(requireContext())
        loadingDialog = dialogUtils.buildLoadingDialog()

        setupCountyDropdown()
        setupProductRecyclerView()
        setupAddProductButton()
        setupBackButton()
        setupSubmitButton()
        setupDatePicker()
        generateCustomerCode()
    }

    private fun setupCountyDropdown() {
        val counties = arrayOf("County1", "County2", "County3") // Replace with actual county list
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, counties)
        binding.countyAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupProductRecyclerView() {
        productAdapter = ProductAdapter()
        binding.productRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    private fun setupAddProductButton() {
        binding.addProductButton.setOnClickListener {
            val newProduct = Product("", "", "", "", "", "")
            productAdapter.addProduct(newProduct)
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun setupDatePicker() {
        binding.dateOfBirthEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val displayDate = displayDateFormat.format(calendar.time)
                binding.dateOfBirthEditText.setText(displayDate)

                // Store the API format date as a tag for later use
                val apiDate = apiDateFormat.format(calendar.time)
                binding.dateOfBirthEditText.tag = apiDate
            }, year, month, day).show()
        }
    }

    private fun generateCustomerCode() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val lastCode = sharedPref.getInt("LAST_CUSTOMER_CODE", 49)
        val newCode = lastCode + 1
        customerCode = "H-$newCode"
    }

    private fun incrementCustomerCode() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val lastCode = sharedPref.getInt("LAST_CUSTOMER_CODE", 49)
        with(sharedPref.edit()) {
            putInt("LAST_CUSTOMER_CODE", lastCode + 1)
            apply()
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateFields()) {
                val customerData = createCustomerData()
                submitCustomerData(customerData)
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        // Last Name
        if (binding.lastNameEditText.text.isNullOrBlank()) {
            binding.lastNameLayout.error = "Last name is required"
            isValid = false
        } else {
            binding.lastNameLayout.error = null
        }

        // Other Name
        if (binding.otherNameEditText.text.isNullOrBlank()) {
            binding.otherNameLayout.error = "Other name is required"
            isValid = false
        } else {
            binding.otherNameLayout.error = null
        }

        // ID Number
        if (binding.idNumberEditText.text.isNullOrBlank()) {
            binding.idNumberLayout.error = "ID number is required"
            isValid = false
        } else {
            binding.idNumberLayout.error = null
        }

        // Email
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        if (binding.emailEditText.text.isNullOrBlank() || !binding.emailEditText.text.toString().matches(emailPattern.toRegex())) {
            binding.emailLayout.error = "Valid email is required"
            isValid = false
        } else {
            binding.emailLayout.error = null
        }

        // Phone Number
        if (binding.phoneNoEditText.text.isNullOrBlank() || binding.phoneNoEditText.text.toString().length != 10) {
            binding.phoneNoLayout.error = "Valid 10-digit phone number is required"
            isValid = false
        } else {
            binding.phoneNoLayout.error = null
        }

        // Date of Birth
        if (binding.dateOfBirthEditText.text.isNullOrBlank()) {
            binding.dateOfBirthLayout.error = "Date of birth is required"
            isValid = false
        } else {
            binding.dateOfBirthLayout.error = null
        }

        // Address
        if (binding.addressEditText.text.isNullOrBlank()) {
            binding.addressLayout.error = "Address is required"
            isValid = false
        } else {
            binding.addressLayout.error = null
        }

        // County
        if (binding.countyAutoCompleteTextView.text.isNullOrBlank()) {
            binding.countyLayout.error = "County is required"
            isValid = false
        } else {
            binding.countyLayout.error = null
        }

        // Sub County
        if (binding.subCountyEditText.text.isNullOrBlank()) {
            binding.subCountyLayout.error = "Sub County is required"
            isValid = false
        } else {
            binding.subCountyLayout.error = null
        }

        // Ward
        if (binding.wardEditText.text.isNullOrBlank()) {
            binding.wardLayout.error = "Ward is required"
            isValid = false
        } else {
            binding.wardLayout.error = null
        }

        // Village
        if (binding.villageEditText.text.isNullOrBlank()) {
            binding.villageLayout.error = "Village is required"
            isValid = false
        } else {
            binding.villageLayout.error = null
        }

        // Gender
        if (binding.genderRadioGroup.checkedRadioButtonId == -1) {
            Toast.makeText(context, "Please select a gender", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Products
        if (productAdapter.getProducts().isEmpty()) {
            Toast.makeText(context, "Please add at least one product", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun createCustomerData(): IndividualCustomerRequestModel {
        return IndividualCustomerRequestModel(
            county = binding.countyAutoCompleteTextView.text.toString(),
            customer_code = customerCode,
            date_of_birth = binding.dateOfBirthEditText.tag as? String ?: "",
            email = binding.emailEditText.text.toString(),
            gender = if (binding.maleRadioButton.isChecked) "Male" else "Female",
            id_number = binding.idNumberEditText.text.toString().toInt(),
            last_name = binding.lastNameEditText.text.toString(),
            other_name = binding.otherNameEditText.text.toString(),
            phone_number = binding.phoneNoEditText.text.toString().toInt(),
            products = productAdapter.getProducts(),
            sub_county = binding.subCountyEditText.text.toString(),
            village = binding.villageEditText.text.toString(),
            ward = binding.wardEditText.text.toString()
        )
    }

    private fun submitCustomerData(customerData: IndividualCustomerRequestModel) {
        loadingDialog.show()
        binding.submitButton.isEnabled = false

        context?.let { ctx ->
            RestClient.getApiService(ctx).registerIndividualCustomer(customerData)
                .enqueue(object : Callback<IndividualCustomerRequestModel> {
                    override fun onResponse(
                        call: Call<IndividualCustomerRequestModel>,
                        response: Response<IndividualCustomerRequestModel>
                    ) {
                        loadingDialog.dismiss()
                        binding.submitButton.isEnabled = true

                        if (response.isSuccessful) {
                            Toast.makeText(context, "Form submitted successfully. Customer Code: ${customerData.customer_code}", Toast.LENGTH_SHORT).show()
                            incrementCustomerCode()
                            generateCustomerCode() // Generate new code for next customer
                            clearForm()
                        } else {
                            Toast.makeText(context, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<IndividualCustomerRequestModel>, t: Throwable) {
                        loadingDialog.dismiss()
                        binding.submitButton.isEnabled = true
                        Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun clearForm() {
        binding.lastNameEditText.text?.clear()
        binding.otherNameEditText.text?.clear()
        binding.idNumberEditText.text?.clear()
        binding.emailEditText.text?.clear()
        binding.phoneNoEditText.text?.clear()
        binding.dateOfBirthEditText.text?.clear()
        binding.addressEditText.text?.clear()
        binding.countyAutoCompleteTextView.text?.clear()
        binding.subCountyEditText.text?.clear()
        binding.wardEditText.text?.clear()
        binding.villageEditText.text?.clear()
        binding.genderRadioGroup.clearCheck()
        productAdapter.clearProducts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
        _binding = null
    }
}
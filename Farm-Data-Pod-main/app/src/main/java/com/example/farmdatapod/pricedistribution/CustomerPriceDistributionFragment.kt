package com.example.farmdatapod.pricedistribution

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.CustomerPriceDistribution
import com.example.farmdatapod.FarmerPriceDistribution
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentCustomerPriceDistributionBinding
import com.example.farmdatapod.models.CustomerPriceRequestModel
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.utils.SharedPrefs
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomerPriceDistributionFragment : Fragment() {

    private var _binding: FragmentCustomerPriceDistributionBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHandler: DBHandler
    private lateinit var sharedPrefs: SharedPrefs
    private lateinit var networkViewModel: NetworkViewModel
    private var produceIdCounter = 6 // Initialize the counter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHandler = DBHandler(requireContext())
        sharedPrefs = SharedPrefs(requireContext())
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)
        // Load the counter from SharedPreferences
        produceIdCounter = sharedPrefs.getProduceIdCounter()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCustomerPriceDistributionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHubSpinner()
        setupBuyingCenterSpinner()
        setupUnitSpinner()
        setupSubmitButton()

        binding.dateInput.setOnClickListener {
            showDatePickerDialog()
        }

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupHubSpinner() {
        val hubNames = dbHandler.allHubNames
        val hubAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            hubNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.hubInput.setAdapter(hubAdapter)
    }

    private fun setupBuyingCenterSpinner() {
        val buyingCenterNames = dbHandler.allBuyingCenterNames
        val buyingCenterAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            buyingCenterNames
        )
        binding.buyingCentersInput.setAdapter(buyingCenterAdapter)

        binding.buyingCentersInput.setOnItemClickListener { _, _, position, _ ->
            val selectedBuyingCenter = buyingCenterNames[position]
//            updateProductSpinnerForBuyingCenter(selectedBuyingCenter)
        }
    }

    private fun setupUnitSpinner() {
        val unitTypes = arrayOf("Crates", "Bags", "Nets")
        val unitAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            unitTypes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.selectUnitInput.setAdapter(unitAdapter)
    }

//    private fun updateProductSpinnerForBuyingCenter(buyingCenter: String) {
//        // Fetch market produces based on the selected buying center
//        val marketProduces = dbHandler.getMarketProducesForBuyingCenterCPD(buyingCenter)
//
//        // Filter out market produces that already exist in the customer distribution table
//        val filteredMarketProduces = marketProduces.filter { it.product != null && it.id != null }
//
//        // Check if the filtered list is empty
//        val productNames: Array<String> = if (filteredMarketProduces.isNotEmpty()) {
//            filteredMarketProduces.map { it.product!! }.distinct().toTypedArray()
//        } else {
//            arrayOf("No products for selected buying center") // Show this message if no products are found
//        }
//
//        // Map product names to their respective MarketProduce objects
//        val productIdMap = filteredMarketProduces.associateBy { it.product }
//
//        // Create and set the adapter for the product spinner (showing product names)
//        val productAdapter = ArrayAdapter(
//            requireContext(),
//            android.R.layout.simple_spinner_item,
//            productNames
//        ).apply {
//            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        }
//        binding.selectProductInput.setAdapter(productAdapter)
//
//        // Handle item selection in the product spinner
//        binding.selectProductInput.setOnItemClickListener { _, _, position, _ ->
//            val selectedProductName = productNames[position]
//
//            // Check if the "No products" placeholder is selected
//            if (selectedProductName != "No products for selected buying center") {
//                val selectedMarketProduce = productIdMap[selectedProductName]
//                if (selectedMarketProduce != null) {
//                    binding.selectProductInput.tag = selectedMarketProduce.id
//                }
//            }
//        }
//    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(selectedDate.time)
            binding.dateInput.setText(date)
        }, year, month, day).show()
    }

    private fun validateFields(): Boolean {
        return when {
            binding.hubInput.text.isNullOrEmpty() -> {
                Toast.makeText(context, "Please select a hub", Toast.LENGTH_SHORT).show()
                false
            }
            binding.selectUnitInput.text.isNullOrEmpty() -> {
                Toast.makeText(context, "Please select a unit", Toast.LENGTH_SHORT).show()
                false
            }
            binding.buyingCentersInput.text.isNullOrEmpty() -> {
                Toast.makeText(context, "Please select a buying center", Toast.LENGTH_SHORT).show()
                false
            }
            binding.dateInput.text.isNullOrEmpty() -> {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                false
            }
            binding.selectProductInput.text.isNullOrEmpty() -> {
                Toast.makeText(context, "Please select a product", Toast.LENGTH_SHORT).show()
                false
            }
            binding.onlinePriceInput.text.isNullOrEmpty() -> {
                Toast.makeText(context, "Please enter an online price", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun setupSubmitButton() {
        // Observe network connectivity changes
        networkViewModel.networkLiveData.observe(viewLifecycleOwner) { isNetworkAvailable ->

            // Set up the submit button click listener
            binding.submitButton.setOnClickListener {
                if (validateFields()) {
                    if (isNetworkAvailable) {
                        submitCustomerPriceRequest()
                    } else {
                        handleOfflineCustomerPriceSubmission()
                    }
                } else {
                    Toast.makeText(context, "Please fill all required fields correctly", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun submitCustomerPriceRequest() {
        if (!validateFields()) return

        val selectedProductId = binding.selectProductInput.tag as? Int ?: 0

        val customerPriceRequest = CustomerPriceRequestModel(
            buying_center = binding.buyingCentersInput.text.toString(),
            comments = binding.commentsInput.text.toString(),
            date = binding.dateInput.text.toString(),
            hub = binding.hubInput.text.toString(),
            online_price = binding.onlinePriceInput.text.toString(),
            produce_id = selectedProductId,
            sold = false, // Replace with actual sold status
            unit = binding.selectUnitInput.text.toString()
        )

        // Log the request data as JSON
        val requestJson = Gson().toJson(customerPriceRequest)
        Log.d("CustomerPriceRequest", "Posting JSON: $requestJson")

        context?.let { ctx ->
            RestClient.getApiService(ctx).registerCustomerPrice(customerPriceRequest).enqueue(object :
                Callback<CustomerPriceRequestModel> {
                override fun onResponse(
                    call: Call<CustomerPriceRequestModel>,
                    response: Response<CustomerPriceRequestModel>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                        // Increment the counter and save it
                        produceIdCounter++
                        sharedPrefs.saveProduceIdCounter(produceIdCounter)
                        // Clear inputs
                        binding.apply {
                            hubInput.setText("")
                            buyingCentersInput.setText("")
                            dateInput.setText("")
                            onlinePriceInput.setText("")
                            selectProductInput.setText("")
                            selectUnitInput.setText("")
                            commentsInput.setText("")
                        }
                    } else {
                        Toast.makeText(context, "Failed to register. Please try again later.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CustomerPriceRequestModel>, t: Throwable) {
                    Toast.makeText(context, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun handleOfflineCustomerPriceSubmission() {
        // Log that the form submission is being handled offline
        Log.d("CustomerPriceDistributionFragment", "Form submitted offline. The data will be saved locally or queued for later submission.")

        // Initialize SharedPrefs to get the user_id
        val sharedPrefs = SharedPrefs(requireContext())
        val userId = sharedPrefs.getUserId() ?: "Unknown User"

        val onlinePriceString = binding.onlinePriceInput.text.toString()
        val onlinePrice = onlinePriceString.toFloatOrNull() ?: 0.0f

        // Create a FarmerPriceDistribution object with the form data
        val customerPriceDistribution = CustomerPriceDistribution().apply {
            hub = binding.hubInput.text.toString()
            buying_center = binding.buyingCentersInput.text.toString()
            online_price = onlinePrice
            unit = binding.selectUnitInput.text.toString()
            date = binding.dateInput.text.toString()
            comments = binding.commentsInput.text.toString()
            produce_id = binding.selectProductInput.tag as? Int ?: 0
            sold = false
            user_id = userId
        }

        // Insert the CustomerPriceDistributionFragment into the local database using DBHandler
        val dbHandler = DBHandler(requireContext())
        val isInserted = dbHandler.insertCustomerPriceDistribution(customerPriceDistribution)

        // Check if the data was successfully inserted
        if (isInserted) {
            Toast.makeText(context, "Customer price distribution data saved offline successfully.", Toast.LENGTH_SHORT).show()
            clearFields()
        } else {
            Toast.makeText(context, "Failed to save customer price distribution data offline.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFields() {
        binding.hubInput.text.clear()
        binding.selectUnitInput.text.clear()
        binding.buyingCentersInput.text.clear()
        binding.dateInput.text?.clear()
        binding.selectProductInput.text.clear()
        binding.commentsInput.text?.clear()
        binding.onlinePriceInput.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.farmdatapod.makepayments

import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.BuildConfig
import com.example.farmdatapod.databinding.FragmentMakePaymentBinding
import com.example.farmdatapod.models.B2CPaymentRequest
import com.example.farmdatapod.network.MpesaApiService
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MakePaymentFragment : Fragment() {

    private var _binding: FragmentMakePaymentBinding? = null
    private val binding get() = _binding!!

    private lateinit var mpesaApiService: MpesaApiService
    private var accessToken: String? = null

    private val buyingCenters = listOf("Center 1", "Center 2", "Center 3")
    private val cigs = listOf("CIG 1", "CIG 2", "CIG 3")
    private val producersByCenters = mapOf(
        "Center 1" to listOf("Michael Limisi", "John Doe"),
        "Center 2" to listOf("Jane Smith", "Alice Johnson"),
        "Center 3" to listOf("Bob Brown", "Charlie Davis")
    )
    private val producersByCIG = mapOf(
        "CIG 1" to listOf("Eva Green", "Frank White"),
        "CIG 2" to listOf("Grace Lee", "Henry Ford"),
        "CIG 3" to listOf("Iris Blue", "Jack Black")
    )
    private val grnsByProducer = mapOf(
        "Michael Limisi" to listOf("GRN 1", "GRN 2"),
        "John Doe" to listOf("GRN 3", "GRN 4"),
        "Jane Smith" to listOf("GRN 5", "GRN 6"),
        "Alice Johnson" to listOf("GRN 7", "GRN 8"),
        "Bob Brown" to listOf("GRN 9", "GRN 10"),
        "Charlie Davis" to listOf("GRN 11", "GRN 12"),
        "Eva Green" to listOf("GRN 13", "GRN 14"),
        "Frank White" to listOf("GRN 15", "GRN 16"),
        "Grace Lee" to listOf("GRN 17", "GRN 18"),
        "Henry Ford" to listOf("GRN 19", "GRN 20"),
        "Iris Blue" to listOf("GRN 21", "GRN 22"),
        "Jack Black" to listOf("GRN 23", "GRN 24")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMakePaymentBinding.inflate(inflater, container, false)
        return binding.root
    }
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMpesaApiService()
        getAccessToken()
        setupTabLayout()
        setupDropdowns()
        setupButtons()
        setupToolbar()
    }

    private fun setupMpesaApiService() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://sandbox.safaricom.co.ke/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        mpesaApiService = retrofit.create(MpesaApiService::class.java)
    }

    private fun getAccessToken() {
        lifecycleScope.launch {
            try {
                val credentials = "${BuildConfig.MPESA_API_USERNAME}:${BuildConfig.MPESA_API_PASSWORD}"
                val authString = "Basic " + Base64.encodeToString(
                    credentials.toByteArray(),
                    Base64.NO_WRAP
                )
                val response = mpesaApiService.getAccessToken(authString)
                accessToken = response.access_token
                Toast.makeText(requireContext(), "Access token obtained successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to get access token: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> setupSelectionDropdown(buyingCenters)
                    1 -> setupSelectionDropdown(cigs)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupDropdowns() {
        setupSelectionDropdown(buyingCenters)
        setupProducerDropdown()
        setupGRNDropdown()
    }

    private fun setupSelectionDropdown(items: List<String>) {
        setupDropdown(binding.tilSelection, items) { position ->
            val selectedItem = items[position]
            val producers = if (binding.tabLayout.selectedTabPosition == 0) {
                producersByCenters[selectedItem] ?: emptyList()
            } else {
                producersByCIG[selectedItem] ?: emptyList()
            }
            setupProducerDropdown(producers)
        }
    }

    private fun setupProducerDropdown(producers: List<String> = emptyList()) {
        setupDropdown(binding.tilSelectProducer, producers) { position ->
            val selectedProducer = producers[position]
            binding.tvProducerDetails.apply {
                text = "1st Name: ${selectedProducer.split(" ")[0]}\n" +
                        "2nd Name: ${selectedProducer.split(" ")[1]}\n" +
                        "Farmer Code: ${300000 + position}"
                visibility = View.VISIBLE
            }
            setupGRNDropdown(grnsByProducer[selectedProducer] ?: emptyList())
        }
    }

    private fun setupGRNDropdown(grns: List<String> = emptyList()) {
        setupDropdown(binding.tilSelectGRN, grns) { position ->
            binding.tvGRNDetails.apply {
                text = "Unit: Crates\nWeight (kg): ${500 + position * 50}"
                visibility = View.VISIBLE
            }
        }
    }

    private fun setupDropdown(
        textInputLayout: TextInputLayout,
        items: List<String>,
        onItemSelected: ((Int) -> Unit)? = null
    ) {
        val autoCompleteTextView = (textInputLayout.editText as? AutoCompleteTextView)?.apply {
            setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items))
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                onItemSelected?.invoke(position)
            }
        }

        textInputLayout.editText?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateInput(textInputLayout)
            }
        }
    }

    private fun validateInput(textInputLayout: TextInputLayout) {
        val editText = textInputLayout.editText
        if (editText?.text.isNullOrBlank()) {
            textInputLayout.error = "This field is required"
        } else {
            textInputLayout.error = null
        }
    }

    private fun setupButtons() {
        binding.btnMakePayment.setOnClickListener {
            binding.llPaymentDetails.visibility = View.VISIBLE
        }

        binding.btnConfirmPayment.setOnClickListener {
            initiatePayment()
        }
    }

    private fun initiatePayment() {
        if (accessToken == null) {
            lifecycleScope.launch {
                try {
                    val credentials = "${BuildConfig.MPESA_API_USERNAME}:${BuildConfig.MPESA_API_PASSWORD}"
                    val authString = "Basic " + Base64.encodeToString(
                        credentials.toByteArray(),
                        Base64.NO_WRAP
                    )
                    val response = mpesaApiService.getAccessToken(authString)
                    accessToken = response.access_token
                    Toast.makeText(requireContext(), "Access token obtained successfully", Toast.LENGTH_SHORT).show()
                    proceedWithPayment()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to get access token: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            proceedWithPayment()
        }
    }

    fun formatPhoneNumber(phoneNumber: String): String {
        return if (phoneNumber.startsWith("0")) {
            "+254" + phoneNumber.substring(1)
        } else {
            phoneNumber
        }
    }
    private fun proceedWithPayment() {
        val amount = binding.etAmount.text.toString().toIntOrNull()
        val phoneNumber = binding.etPhoneNumber.text.toString()

        if (amount == null || phoneNumber.isBlank()) {
            Toast.makeText(requireContext(), "Please enter valid amount and phone number", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedPhoneNumber = formatPhoneNumber(phoneNumber)

        val paymentRequest = B2CPaymentRequest(
            InitiatorName = "testapi",
            SecurityCredential = "qJ9APg6ot+QvgcH0axi4OKz2XtBsoXXaPi+OMfUxTHIeqZ2rIZEpVjbX52ojN9UduJvyjB/4NdYvLh6nPcsiIm/R/u7eF+FxHvM9yCwTubR4UhgrIUz6cVzQ0szrV4vAkysLqWb/l5/IJGsPnwklhUdqy9TkI+fMJJRJq7sm2B0AXGxuUAiRABz5qrtmODZXBXgQv8ZdwdfHi4Ye756AxdGJbxSyyqIy8/3ZSIfsHibAlGl8hCSGzWFv8YvaUk5RBVv/elaGi6ffSzbGfKyYhXJEY8fGNW72DAg1rlVnNw1Sonn4Wlv6lnNI0qpJzq8zvn18+monkQbZYIE/L7xCPg==",
            CommandID = "BusinessPayment",
            Amount = amount,
            PartyA = 600998,
            PartyB = formattedPhoneNumber.toLong(),
            Remarks = "Payment for produce",
            QueueTimeOutURL = "https://mydomain.com/b2c/queue",
            ResultURL = "https://mydomain.com/b2c/result",
            Occasion = "null"
        )

        lifecycleScope.launch {
            try {
                binding.btnConfirmPayment.isEnabled = false
                binding.btnConfirmPayment.text = "Processing..."

                val response = mpesaApiService.makeB2CPayment("Bearer $accessToken", paymentRequest)

                Toast.makeText(requireContext(), "Payment initiated: ${response.ResponseDescription}", Toast.LENGTH_LONG).show()

                // Reset UI
                binding.llPaymentDetails.visibility = View.GONE
                binding.etAmount.text?.clear()
                binding.etPhoneNumber.text?.clear()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Payment failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnConfirmPayment.isEnabled = true
                binding.btnConfirmPayment.text = "Confirm Payment"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
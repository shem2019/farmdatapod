package com.example.farmdatapod.receivepayments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentReceivePaymentBinding
import com.example.farmdatapod.models.STKPushRequest
import com.example.farmdatapod.network.RestClient
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ReceivePaymentFragment : Fragment() {

    private var _binding: FragmentReceivePaymentBinding? = null
    private val binding get() = _binding!!

    private val villages = listOf("Village 1", "Village 2", "Village 3")
    private val customersByVillage = mapOf(
        "Village 1" to listOf("Customer 1A", "Customer 1B", "Customer 1C"),
        "Village 2" to listOf("Customer 2A", "Customer 2B", "Customer 2C"),
        "Village 3" to listOf("Customer 3A", "Customer 3B", "Customer 3C")
    )
    private val grnsByCustomer = mapOf(
        "Customer 1A" to listOf("GRN 1A1", "GRN 1A2", "GRN 1A3"),
        "Customer 1B" to listOf("GRN 1B1", "GRN 1B2", "GRN 1B3"),
        "Customer 1C" to listOf("GRN 1C1", "GRN 1C2", "GRN 1C3"),
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReceivePaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        setupSpinners()
        setupValidation()
        setupButtons()
    }

    private fun setupSpinners() {
        setupVillageSpinner()
        setupCustomerSpinner()
        setupGrnSpinner()
    }

    private fun setupVillageSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, villages)
        (binding.tilVillageEstate.editText as? AutoCompleteTextView)?.setAdapter(adapter)
        (binding.tilVillageEstate.editText as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedVillage = villages[position]
            updateCustomerSpinner(selectedVillage)
            clearCustomerDetails()
            clearGrnSpinner()
            hideAdditionalButtons()
            hidePaymentInputs()
        }
    }

    private fun setupCustomerSpinner() {
        (binding.tilCustomer.editText as? AutoCompleteTextView)?.setOnItemClickListener { _, _, _, _ ->
            val selectedCustomer = (binding.tilCustomer.editText as? AutoCompleteTextView)?.text.toString()
            updateCustomerDetails(selectedCustomer)
            updateGrnSpinner(selectedCustomer)
            hideAdditionalButtons()
            hidePaymentInputs()
        }
    }

    private fun setupGrnSpinner() {
        (binding.tilGrn.editText as? AutoCompleteTextView)?.setOnItemClickListener { _, _, _, _ ->
            val selectedGrn = (binding.tilGrn.editText as? AutoCompleteTextView)?.text.toString()
            updateGrnDetails(selectedGrn)
            showAdditionalButtons()
            hidePaymentInputs()
        }
    }

    private fun updateCustomerSpinner(village: String) {
        val customers = customersByVillage[village] ?: emptyList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, customers)
        (binding.tilCustomer.editText as? AutoCompleteTextView)?.apply {
            setAdapter(adapter)
            text.clear()
        }
    }

    private fun updateGrnSpinner(customer: String) {
        val grns = grnsByCustomer[customer] ?: emptyList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, grns)
        (binding.tilGrn.editText as? AutoCompleteTextView)?.apply {
            setAdapter(adapter)
            text.clear()
        }
    }

    private fun clearCustomerDetails() {
        binding.tvCustomerDetails.visibility = View.GONE
    }

    private fun clearGrnSpinner() {
        (binding.tilGrn.editText as? AutoCompleteTextView)?.apply {
            setAdapter(null)
            text.clear()
        }
        binding.tvGRNDetails.visibility = View.GONE
    }

    private fun updateCustomerDetails(customer: String) {
        binding.tvCustomerDetails.apply {
            text = "Customer: $customer\n1st Name: John\n2nd Name: Doe\nFarmer Code: 12345"
            visibility = View.VISIBLE
        }
    }

    private fun updateGrnDetails(grn: String) {
        binding.tvGRNDetails.apply {
            text = "GRN: $grn\nUnit: Kg\nPrice per kg (KES): 50"
            visibility = View.VISIBLE
        }
    }

    private fun setupValidation() {
        listOf(binding.tilVillageEstate, binding.tilCustomer, binding.tilGrn,
            binding.tilPhoneNumber, binding.tilAmount).forEach { textInputLayout ->
            textInputLayout.editText?.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) validateInput(textInputLayout)
            }
        }
    }

    private fun validateInput(textInputLayout: TextInputLayout): Boolean {
        val text = textInputLayout.editText?.text.toString()
        return if (text.isEmpty()) {
            textInputLayout.error = "This field is required"
            false
        } else {
            textInputLayout.error = null
            true
        }
    }

    private fun setupButtons() {
        binding.btnReceivePayment.setOnClickListener {
            if (binding.llPaymentInputs.visibility == View.VISIBLE) {
                initiatePayment()
            } else {
                showPaymentInputs()
            }
        }
        binding.btnMetadata.setOnClickListener { showMetadata() }
        binding.btnTransactionDetails.setOnClickListener { showTransactionDetails() }
    }

    private fun showAdditionalButtons() {
        binding.llAdditionalButtons.visibility = View.VISIBLE
    }

    private fun hideAdditionalButtons() {
        binding.llAdditionalButtons.visibility = View.GONE
        binding.tvAdditionalInfo.visibility = View.GONE
    }

    private fun showMetadata() {
        binding.tvAdditionalInfo.apply {
            text = "Customer metadata"
            visibility = View.VISIBLE
        }
    }

    private fun showTransactionDetails() {
        binding.tvAdditionalInfo.apply {
            text = "Customer transaction history"
            visibility = View.VISIBLE
        }
    }

    private fun showPaymentInputs() {
        binding.llPaymentInputs.visibility = View.VISIBLE
        binding.btnReceivePayment.text = "Confirm Payment"

        val selectedCustomer = binding.tilCustomer.editText?.text.toString()
        binding.etPhoneNumber.setText(getPhoneNumberForCustomer(selectedCustomer))

        val amount = binding.tvGRNDetails.text.toString().substringAfter("Price per kg (KES): ").trim()
        binding.etAmount.setText(amount)
    }

    private fun hidePaymentInputs() {
        binding.llPaymentInputs.visibility = View.GONE
        binding.btnReceivePayment.text = "Receive Payment"
    }

    private fun getPhoneNumberForCustomer(customerName: String): String {
        // Implement logic to fetch the phone number for the selected customer
        return "254712345678"  // Replace with actual logic to get the phone number
    }

    private fun initiatePayment() {
        val phone = binding.etPhoneNumber.text.toString()
        val amount = binding.etAmount.text.toString()

        if (phone.isBlank() || amount.isBlank()) {
            Toast.makeText(requireContext(), "Please enter both phone number and amount", Toast.LENGTH_SHORT).show()
            return
        }

        val stkPushRequest = STKPushRequest(
            phone = phone,
            amount = amount
        )

        lifecycleScope.launch {
            try {
                val response = RestClient.getApiService(requireContext()).initiateSTKPush(stkPushRequest)
                Toast.makeText(requireContext(), "STK Push initiated: ${response.customerMessage}", Toast.LENGTH_LONG).show()
                hidePaymentInputs()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "STK Push initiation failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
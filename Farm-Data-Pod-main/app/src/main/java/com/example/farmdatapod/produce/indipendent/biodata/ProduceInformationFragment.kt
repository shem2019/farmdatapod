package com.example.farmdatapod.produce.indipendent.biodata

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentProduceInformationBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class ProduceInformationFragment : Fragment() {

    private var _binding: FragmentProduceInformationBinding? = null
    private val binding get() = _binding!!

    private lateinit var marketProduceAdapter: ProduceAdapter
    private lateinit var ownConsumptionAdapter: ProduceAdapter

    private val marketProduceList = mutableListOf<ProduceItem>()
    private val ownConsumptionList = mutableListOf<ProduceItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProduceInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupButtonListeners()
    }

    private fun setupRecyclerViews() {
        marketProduceAdapter = ProduceAdapter(marketProduceList)
        binding.marketProduceRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = marketProduceAdapter
        }

        ownConsumptionAdapter = ProduceAdapter(ownConsumptionList)
        binding.ownConsumptionRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ownConsumptionAdapter
        }
    }

    private fun setupButtonListeners() {
        binding.backButton.setOnClickListener {
            // Handle back navigation
            // For example: findNavController().navigateUp()
        }

        binding.addMarketProduceButton.setOnClickListener {
            showAddProduceDialog(isMarketProduce = true)
        }

        binding.addOwnConsumptionButton.setOnClickListener {
            showAddProduceDialog(isMarketProduce = false)
        }

    }

    private fun showAddProduceDialog(isMarketProduce: Boolean) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.item_produce, null)
        val productEditText: TextInputEditText = dialogView.findViewById(R.id.productEditText)
        val productCategoryAutoCompleteTextView: AutoCompleteTextView = dialogView.findViewById(R.id.productCategoryAutoCompleteTextView)
        val acreageEditText: TextInputEditText = dialogView.findViewById(R.id.acreageEditText)

        // Setup AutoCompleteTextView with categories from strings.xml
        val categories = resources.getStringArray(R.array.product_categories)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        productCategoryAutoCompleteTextView.setAdapter(adapter)

        // Create an AlertDialog
        AlertDialog.Builder(requireContext())
            .setTitle(if (isMarketProduce) "Add Market Produce" else "Add Own Consumption")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val productName = productEditText.text.toString()
                val productCategory = productCategoryAutoCompleteTextView.text.toString()
                val acreage = acreageEditText.text.toString().toIntOrNull() ?: 0

                if (productName.isNotBlank() && productCategory.isNotBlank()) {
                    val newProduce = ProduceItem(productName, acreage.toString(), productCategory)
                    if (isMarketProduce) {
                        marketProduceList.add(newProduce)
                        marketProduceAdapter.notifyItemInserted(marketProduceList.size - 1)
                    } else {
                        ownConsumptionList.add(newProduce)
                        ownConsumptionAdapter.notifyItemInserted(ownConsumptionList.size - 1)
                    }

                } else {
                    Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun validateForm(): Boolean {
        if (marketProduceList.isEmpty() && ownConsumptionList.isEmpty()) {
            Snackbar.make(binding.root, "Please add at least one produce item", Snackbar.LENGTH_LONG).show()
            return false
        }
        return true
    }

    fun saveData() {
        if (validateForm()) {
            // Access ViewModel and save data
            val sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

            // Add data to ViewModel
            marketProduceList.forEach { item ->
                sharedViewModel.addMarketProduce(item)
            }

            ownConsumptionList.forEach { item ->
                sharedViewModel.addOwnConsumption(item)
            }

            // Notify user
            Snackbar.make(binding.root, "Produce data saved successfully", Snackbar.LENGTH_LONG).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class ProduceItem(
    val name: String,
    val quantity: String,
    val unit: String
)

class ProduceAdapter(private val produceList: List<ProduceItem>) : androidx.recyclerview.widget.RecyclerView.Adapter<ProduceAdapter.ProduceViewHolder>() {

    class ProduceViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        // TODO: Implement ViewHolder
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProduceViewHolder {
        // TODO: Implement onCreateViewHolder
        return ProduceViewHolder(View(parent.context))
    }

    override fun onBindViewHolder(holder: ProduceViewHolder, position: Int) {
        // TODO: Implement onBindViewHolder
    }

    override fun getItemCount() = produceList.size
}
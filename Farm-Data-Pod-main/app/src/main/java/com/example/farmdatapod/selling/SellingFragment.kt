package com.example.farmdatapod.selling

import CerealsAdapter
import RabbitAndPoultryAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.BuyingCustomer
import com.example.farmdatapod.BuyingFarmer
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.*
import com.example.farmdatapod.databinding.FragmentSellingBinding
import com.example.farmdatapod.models.*
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.utils.DialogUtils
import com.example.farmdatapod.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SellingFragment : Fragment() {

    private var _binding: FragmentSellingBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkViewModel: NetworkViewModel
    private lateinit var dbHandler: DBHandler

    private lateinit var loadingDialog: AlertDialog
    private lateinit var customerAdapter: ArrayAdapter<String>
    private val selectedCustomers = mutableSetOf<String>()

    private lateinit var currentQuality: Quality
    private lateinit var sellingRequestModel: SellingRequestModel

    private val defaultAdapter by lazy { DefaultAdapter(listOf()) }
    private val honeyAdapter by lazy { HoneyAdapter(listOf()) }
    private val rabbitAndPoultryAdapter by lazy { RabbitAndPoultryAdapter(listOf()) }
    private val nutsAdapter by lazy { NutsAdapter(listOf()) }
    private val meatAdapter by lazy { MeatAdapter(listOf()) }
    private val cerealsAdapter by lazy { CerealsAdapter(listOf()) }
    private val vegetableAdapter by lazy { VegetableAdapter(listOf()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellingBinding.inflate(inflater, container, false)
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            loadingDialog = DialogUtils(requireContext()).buildLoadingDialog()
            setupToolbar()
            initializeQualityData()
            setupDropdowns()
            setupCustomerSelection()
            initializeAdapters()
            setupSubmitButton()

//            setupSpinners()

        } catch (e: Exception) {
            Log.e("SellingFragment", "Error in onViewCreated", e)
            Toast.makeText(context, "Error setting up the form", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupToolbar() {
        try {
            (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
            (activity as? AppCompatActivity)?.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
                title = "Selling Information"
            }
        } catch (e: Exception) {
            Log.e("SellingFragment", "Error setting up toolbar", e)
        }
    }

    private fun initializeQualityData() {
        Log.d("SellingFragment", "initializeQualityData() called")
        currentQuality = Quality(
            x = X("", "", "", "", "", "", "", "", ""),
            cerealsDiv = CerealsDiv("","","","","","","","","",""),
            vegetableDiv = VegetableDiv("", "", "", "", "", "", ""),
            honeyDiv = HoneyDiv("", "", "", "","","",""),
            meatDiv = MeatDiv("", "", "", "", "","","",""),
            nutsDiv = NutsDiv("", "", "", "","","","","","",""),
            rabbitAndPoultryDiv = RabbitAndPoultryDiv("", "", "", "","",""),
            defaultDiv = DefaultDiv("", "", "", "","","","","","")
        )
    }

    private fun setupDropdowns() {
        try {
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listOf("Beans", "Maize", "Wheat", "Honey", "Meat", "Vegetables", "Nuts", "Rabbit", "Poultry")
            ).also { adapter ->
                binding.actvSelectProduce.setAdapter(adapter)
            }

            binding.actvSelectProduce.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, _, _ ->
                    onProduceSelected()
                }

            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listOf("Crates", "Bags", "Nets")
            ).also { adapter ->
                binding.actvSelectUnit.setAdapter(adapter)
            }
        } catch (e: Exception) {
            Log.e("SellingFragment", "Error setting up dropdowns", e)
        }
    }

    private fun setupCustomerSelection() {
        try {
            customerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, mutableListOf())
            binding.lvCustomers.adapter = customerAdapter
            binding.lvCustomers.choiceMode = ListView.CHOICE_MODE_MULTIPLE

            binding.lvCustomers.setOnItemClickListener { _, _, position, _ ->
                val customer = customerAdapter.getItem(position) ?: return@setOnItemClickListener
                if (binding.lvCustomers.isItemChecked(position)) {
                    selectedCustomers.add(customer)
                } else {
                    selectedCustomers.remove(customer)
                }
                Log.d("SellingFragment", "Selected customers: $selectedCustomers")
            }
        } catch (e: Exception) {
            Log.e("SellingFragment", "Error in setupCustomerSelection", e)
        }
    }

    private var selectedCategory: String? = "Vegetables"
    private var selectedCustomer: String? = null
    private var selectedProduceId: Int? = null
    private var selectedProduceCategory: String? = null

//    private fun setupSpinners() {
//        val dbHandler = DBHandler(requireContext())
//
//        // Populate Customer Spinner
//        val customerList = dbHandler.getCustomers()
//        val customerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, customerList)
//        val customerDropdown: AutoCompleteTextView = view?.findViewById(R.id.actvSelectCustomer) ?: return
//        customerDropdown.setAdapter(customerAdapter)
//
//        customerDropdown.setOnItemClickListener { parent, view, position, id ->
//            selectedCustomer = customerList[position] // Store the selected customer
//            Log.d("CustomerSpinner", "Selected Customer: $selectedCustomer")
//        }
//
//        // Populate Produce Spinner
//        val marketProduceList = dbHandler.getAvailableMarketProduce()
//
//        // Create a list of display names (just product names) for the spinner
//        val produceDisplayList = marketProduceList.map { it.getDisplayString() }
//        val produceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, produceDisplayList)
//        val produceDropdown: AutoCompleteTextView = view?.findViewById(R.id.actvSelectProduce) ?: return
//        produceDropdown.setAdapter(produceAdapter)
//
//        // Handle the selection of a produce item
//        produceDropdown.setOnItemClickListener { parent, view, position, id ->
//            val selectedProduce = marketProduceList[position]
//
//            // Store the selected produce ID and category
//            selectedProduceId = selectedProduce.id
//            selectedProduceCategory = selectedProduce.product_category
//
//            // Update the adapters and UI based on selected category
//            initializeAdapters()
//            selectedProduceCategory?.let {
//                populateQualityInputs(it)
//                updateQualityInputVisibility(it)
//            }
//
//            Log.d("ProduceSpinner", "Selected Produce ID: $selectedProduceId, Category: $selectedProduceCategory")
//        }
//    }


    private fun initializeAdapters() {
        Log.d("BuyingInfoFragment", "initializeAdapters() called")
        Log.d("BuyingInfoFragment", "Selected category inside initializeAdapters(): $selectedCategory")

        binding.apply {
            // Set up layout managers and adapters
            defaultRecyclerView.layoutManager = LinearLayoutManager(context)
            defaultRecyclerView.adapter = defaultAdapter
            Log.d("BuyingInfoFragment", "Default adapter set")

            honeyRecyclerView.layoutManager = LinearLayoutManager(context)
            honeyRecyclerView.adapter = honeyAdapter
            Log.d("BuyingInfoFragment", "Honey adapter set")

            rabbitPoultryRecyclerView.layoutManager = LinearLayoutManager(context)
            rabbitPoultryRecyclerView.adapter = rabbitAndPoultryAdapter
            Log.d("BuyingInfoFragment", "Rabbit and Poultry adapter set")

            nutsRecyclerView.layoutManager = LinearLayoutManager(context)
            nutsRecyclerView.adapter = nutsAdapter
            Log.d("BuyingInfoFragment", "Nuts adapter set")

            meatRecyclerView.layoutManager = LinearLayoutManager(context)
            meatRecyclerView.adapter = meatAdapter
            Log.d("BuyingInfoFragment", "Meat adapter set")

            cerealsRecyclerView.layoutManager = LinearLayoutManager(context)
            cerealsRecyclerView.adapter = cerealsAdapter
            Log.d("BuyingInfoFragment", "Cereals adapter set")

            vegetableRecyclerView.layoutManager = LinearLayoutManager(context)
            vegetableRecyclerView.adapter = vegetableAdapter
            Log.d("BuyingInfoFragment", "Vegetable adapter set")

            // Hide all RecyclerViews initially
            defaultRecyclerView.visibility = View.GONE
            honeyRecyclerView.visibility = View.GONE
            rabbitPoultryRecyclerView.visibility = View.GONE
            nutsRecyclerView.visibility = View.GONE
            meatRecyclerView.visibility = View.GONE
            cerealsRecyclerView.visibility = View.GONE
            vegetableRecyclerView.visibility = View.GONE

            // Show the appropriate RecyclerView based on selectedCategory
            updateQualityInputVisibility(selectedCategory ?: "Vegetables")
        }
    }

    private fun onProduceSelected() {
        Log.d("BuyingInfoFragment", "onProduceSelected() called")
        activity?.runOnUiThread {
            setupQualityInputHandling()
            selectedCategory?.let {
                populateQualityInputs(it)
                updateQualityInputVisibility(it)
            }
        }
    }

    private fun updateQualityInputVisibility(category: String) {
        // Use the provided category directly
        val extractedCategory = category.split(",").getOrElse(1) { "" }.trim()


        Log.d("BuyingInfoFragment", "updateQualityInputVisibility() called with extractedCategory: $extractedCategory")

        binding.apply {
            cerealsRecyclerView.visibility = if (extractedCategory.contains("Cereals/Pulses")) {
                Log.d("BuyingInfoFragment", "Cereal RecyclerView set to VISIBLE")
                View.VISIBLE
            } else View.GONE

            vegetableRecyclerView.visibility = if (extractedCategory.contains("Vegetables") || extractedCategory.contains("Fruits")) {
                Log.d("BuyingInfoFragment", "Vegetable RecyclerView set to VISIBLE")
                View.VISIBLE
            } else View.GONE

            honeyRecyclerView.visibility = if (extractedCategory.contains("Honey")) {
                Log.d("BuyingInfoFragment", "Honey RecyclerView set to VISIBLE")
                View.VISIBLE
            } else View.GONE

            meatRecyclerView.visibility = if (extractedCategory.contains("Meat")) {
                Log.d("BuyingInfoFragment", "Meat RecyclerView set to VISIBLE")
                View.VISIBLE
            } else View.GONE

            nutsRecyclerView.visibility = if (extractedCategory.contains("Nuts/Oil crops")) {
                Log.d("BuyingInfoFragment", "Nuts RecyclerView set to VISIBLE")
                View.VISIBLE
            } else View.GONE

            rabbitPoultryRecyclerView.visibility = if (extractedCategory.contains("Poultry") || extractedCategory.contains("Rabbit") || extractedCategory.contains("Fish")) {
                Log.d("BuyingInfoFragment", "Rabbit/Poultry RecyclerView set to VISIBLE")
                View.VISIBLE
            } else View.GONE

            // Show default RecyclerView if no specific category matches
            defaultRecyclerView.visibility = if (extractedCategory.isEmpty() ||
                !extractedCategory.contains("Cereals/Pulses") &&
                !extractedCategory.contains("Vegetables") &&
                !extractedCategory.contains("Fruits") &&
                !extractedCategory.contains("Honey") &&
                !extractedCategory.contains("Meat") &&
                !extractedCategory.contains("Nuts/Oil crops") &&
                !extractedCategory.contains("Poultry") &&
                !extractedCategory.contains("Rabbit") &&
                !extractedCategory.contains("Fish")) {
                Log.d("BuyingInfoFragment", "Default RecyclerView set to VISIBLE")
                View.VISIBLE
            } else View.GONE
        }
    }

    private fun setupQualityInputHandling() {
        val selectedProduce = binding.actvSelectProduce.text.toString()
        Log.d("SellingFragment", "setupQualityInputHandling() called with produce: $selectedProduce")

        when {

        }
    }

    private fun setupHoneyQualityInputs() {
        Log.d("SellingFragment", "setupHoneyQualityInputs() called")
        honeyAdapter.setOnItemChangedListener { position, value ->
            Log.d("SellingFragment", "Honey quality item changed: position=$position, value=$value")
            when (position) {
                0 -> currentQuality.honeyDiv.crystallization = value
                1 -> currentQuality.honeyDiv.foreign_matter = value
                2 -> currentQuality.honeyDiv.grade = value
                3 -> currentQuality.honeyDiv.other1 = value
                4 -> currentQuality.honeyDiv.other2 = value
                5 -> currentQuality.honeyDiv.size = value
                6 -> currentQuality.honeyDiv.viscosity = value
            }
        }
    }

    private fun setupVegetableQualityInputs() {
        Log.d("SellingFragment", "setupVegetableQualityInputs() called")
        vegetableAdapter.setOnItemChangedListener { position, value ->
            Log.d("SellingFragment", "Vegetable quality item changed: position=$position, value=$value")
            when (position) {
                0 -> currentQuality.vegetableDiv.foreign_matter = value
                1 -> currentQuality.vegetableDiv.grade = value
                2 -> currentQuality.vegetableDiv.maturity = value
                3 -> currentQuality.vegetableDiv.mechanical_damage = value
                4 -> currentQuality.vegetableDiv.mould = value
                5 -> currentQuality.vegetableDiv.pest_and_disease = value
                6 -> currentQuality.vegetableDiv.size = value
            }
        }
    }

    private fun setupNutsQualityInputs() {
        Log.d("SellingFragment", "setupNutsQualityInputs() called")
        nutsAdapter.setOnItemChangedListener { position, value ->
            Log.d("SellingFragment", "Nuts quality item changed: position=$position, value=$value")
            when (position) {
                0 -> currentQuality.nutsDiv.foreign_matter = value
                1 -> currentQuality.nutsDiv.grade = value
                2 -> currentQuality.nutsDiv.maturity = value
                3 -> currentQuality.nutsDiv.mechanical_damage = value
                4 -> currentQuality.nutsDiv.mould = value
                5 -> currentQuality.nutsDiv.oil_content = value
                6 -> currentQuality.nutsDiv.other1 = value
                7 -> currentQuality.nutsDiv.other2 = value
                8 -> currentQuality.nutsDiv.pest_and_disease = value
                9 -> currentQuality.nutsDiv.size = value
            }
        }
    }

    private fun setupMeatQualityInputs() {
        Log.d("SellingFragment", "setupMeatQualityInputs() called")
        meatAdapter.setOnItemChangedListener { position, value ->
            Log.d("SellingFragment", "Meat quality item changed: position=$position, value=$value")
            when (position) {
                0 -> currentQuality.meatDiv.foreign_matter = value
                1 -> currentQuality.meatDiv.grade = value
                2 -> currentQuality.meatDiv.maturity = value
                3 -> currentQuality.meatDiv.mechanical_damage = value
                4 -> currentQuality.meatDiv.moisture = value
                5 -> currentQuality.meatDiv.other1 = value
                6 -> currentQuality.meatDiv.other2 = value
                7 -> currentQuality.meatDiv.size = value
            }
        }
    }

    private fun setupRabbitAndPoultryQualityInputs() {
        Log.d("SellingFragment", "setupRabbitAndPoultryQualityInputs() called")
        rabbitAndPoultryAdapter.setOnItemChangedListener { position, value ->
            Log.d("SellingFragment", "Rabbit and Poultry quality item changed: position=$position, value=$value")
            when (position) {
                0 -> currentQuality.rabbitAndPoultryDiv.foreign_matter = value.toString()
                1 -> currentQuality.rabbitAndPoultryDiv.grade = value.toString()
                2 -> currentQuality.rabbitAndPoultryDiv.moisture = value.toString()
                3 -> currentQuality.rabbitAndPoultryDiv.other1 = value.toString()
                4 -> currentQuality.rabbitAndPoultryDiv.other2 = value.toString()
                5 -> currentQuality.rabbitAndPoultryDiv.size = value.toString()
            }
        }
    }

    private fun setupDefaultQualityInputs() {
        Log.d("SellingFragment", "setupDefaultQualityInputs() called")
        defaultAdapter.setOnItemChangedListener { position, value ->
            Log.d("SellingFragment", "Default quality item changed: position=$position, value=$value")
            when (position) {
                0 -> currentQuality.defaultDiv.foreign_matter = value.toString()
                1 -> currentQuality.defaultDiv.grade = value.toString()
                2 -> currentQuality.defaultDiv.maturity = value.toString()
                3 -> currentQuality.defaultDiv.moisture = value.toString()
                4 -> currentQuality.defaultDiv.mould = value.toString()
                5 -> currentQuality.defaultDiv.other1 = value.toString()
                6 -> currentQuality.defaultDiv.other2 = value.toString()
                7 -> currentQuality.defaultDiv.pest_and_disease = value.toString()
                8 -> currentQuality.defaultDiv.size = value.toString()
            }
        }
    }

    private fun populateQualityInputs(category: String) {
        // Use the provided category directly
        val extractedCategory = category.split(",").getOrElse(1) { "" }.trim()


        Log.d("BuyingInfoFragment", "populateQualityInputs() called with category: $extractedCategory")

        when {
            extractedCategory.contains("Cereals/Pulses") -> populateCerealsQualityInputs()
            extractedCategory.contains("Vegetables") || extractedCategory.contains("Fruits") -> populateVegetableQualityInputs()
            extractedCategory.contains("Honey") -> populateHoneyQualityInputs()
            extractedCategory.contains("Meat") -> populateMeatQualityInputs()
            extractedCategory.contains("Nuts/Oil crops") -> populateNutsQualityInputs()
            extractedCategory.contains("Poultry") || extractedCategory.contains("Rabbit") || extractedCategory.contains("Fish") -> populateRabbitAndPoultryQualityInputs()
            else -> populateDefaultQualityInputs()
        }
    }

    private fun populateCerealsQualityInputs() {
        Log.d("BuyingInfoFragment", "populateCerealsQualityInputs() called")
        cerealsAdapter.updateItems(listOf(currentQuality.cerealsDiv))
    }

    private fun populateVegetableQualityInputs() {
        Log.d("BuyingInfoFragment", "populateVegetableQualityInputs() called")
        vegetableAdapter.updateItems(listOf(currentQuality.vegetableDiv))
    }

    private fun populateHoneyQualityInputs() {
        Log.d("BuyingInfoFragment", "populateHoneyQualityInputs() called")
        honeyAdapter.updateItems(listOf(currentQuality.honeyDiv))
    }

    private fun populateMeatQualityInputs() {
        Log.d("BuyingInfoFragment", "populateMeatQualityInputs() called")
        meatAdapter.updateItems(listOf(currentQuality.meatDiv))
    }

    private fun populateNutsQualityInputs() {
        Log.d("BuyingInfoFragment", "populateNutsQualityInputs() called")
        nutsAdapter.updateItems(listOf(currentQuality.nutsDiv))
    }

    private fun populateRabbitAndPoultryQualityInputs() {
        Log.d("BuyingInfoFragment", "populateRabbitAndPoultryQualityInputs() called")
        rabbitAndPoultryAdapter.updateItems(listOf(currentQuality.rabbitAndPoultryDiv))
    }

    private fun populateDefaultQualityInputs() {
        Log.d("BuyingInfoFragment", "populateDefaultQualityInputs() called")
        defaultAdapter.updateItems(listOf(currentQuality.defaultDiv))
    }

    private fun setupCerealsQualityInputs() {
        Log.d("SellingFragment", "setupCerealsQualityInputs() called")
        cerealsAdapter.setOnItemChangedListener { position, value ->
            Log.d("SellingFragment", "Cereals quality item changed: position=$position, value=$value")
            when (position) {
                0 -> currentQuality.cerealsDiv.grade = value
                1 -> currentQuality.cerealsDiv.moisture = value
                2 -> currentQuality.cerealsDiv.maturity = value
                3 -> currentQuality.cerealsDiv.foreign_matter = value
                4 -> currentQuality.cerealsDiv.mechanical_damage = value
                5 -> currentQuality.cerealsDiv.size = value
                6 -> currentQuality.cerealsDiv.pest_and_disease = value
                7 -> currentQuality.cerealsDiv.mould = value
                8 -> currentQuality.cerealsDiv.other1 = value
                9 -> currentQuality.cerealsDiv.other2 = value
            }
        }
    }

    // Implement similar setup methods for other produce types...

    private fun setupSubmitButton() {
        // Observe network connectivity changes
        networkViewModel.networkLiveData.observe(viewLifecycleOwner) { isNetworkAvailable ->
            Log.d("SellingFragment", "Network status changed: $isNetworkAvailable")

            binding.btnSubmit.setOnClickListener {
                Log.d("SellingFragment", "Submit button clicked")

                if (validateInputs()) {
                    Log.d("SellingFragment", "Inputs validated. Checking network availability...")

                    if (isNetworkAvailable) {
                        Log.d("SellingFragment", "Network available. Proceeding with online submission.")
                        populateSellingRequestModel()
                        submitSellingRequest()
                    } else {
                        Log.d("SellingFragment", "No network available. Handling offline submission.")
                        handleOfflineSellingSubmission() // Implement this method for offline handling
                    }
                } else {
                    Log.d("SellingFragment", "Input validation failed")
                    Toast.makeText(context, "Please fill all required fields correctly", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleOfflineSellingSubmission() {
        // Log that the form submission is being handled offline
        Log.d("SellingFragment", "Form submitted offline. The data will be saved locally or queued for later submission.")

        // Log the current quality to inspect its structure
        Log.d("SellingFragment", "Current Quality Structure: $currentQuality")

        // Initialize SharedPrefs to get the user_id
        val sharedPrefs = SharedPrefs(requireContext())
        val userId = sharedPrefs.getUserId() ?: "Unknown User"

        // Convert the weight input from String to Double (handling potential errors)
        val weightDouble = try {
            binding.etWeight.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }

        // Convert the currentQuality object to a map
        val qualityMap: Map<String, Map<String, String>> = try {
            convertQualityToMap(currentQuality)
        } catch (e: Exception) {
            Log.e("SellingFragment", "Error converting currentQuality to Map", e)
            emptyMap()
        }

        val customerValue = selectedCustomer ?: ""
        Log.d("SellingFragment", "Selected Customer: $customerValue")
        val produceValue = if (selectedProduceId != null && selectedProduceCategory != null) {
            "${selectedProduceId}, ${selectedProduceCategory}"
        } else {
            ""
        }

        // Populate the SellingRequestModel
        val sellingRequestModel = SellingRequestModel(
            action = when (binding.rgQuarantineAction.checkedRadioButtonId) {
                R.id.rbQuarantine -> "Quarantine"
                R.id.rbAccept -> "Accept"
                R.id.rbReject -> "Reject"
                else -> ""
            },
            customer = customerValue,
            grn_number = binding.etGRNNumber.text.toString(),
            loaded = false,
            online_price = "320",
            produce = produceValue,
            quality = currentQuality,
            unit = binding.actvSelectUnit.text.toString(),
            weight = weightDouble.toString()
        )

        // Convert SellingRequestModel into SellingFarmer for local database storage
        val sellingFarmer = BuyingCustomer().apply {
            produce = sellingRequestModel.produce
            customer = sellingRequestModel.customer
            grn_number = sellingRequestModel.grn_number
            unit = sellingRequestModel.unit
            quality = qualityMap
            action = sellingRequestModel.action
            weight = weightDouble
            loaded = sellingRequestModel.loaded
            user_id = userId
        }

        // Insert the selling data into the local database using DBHandler
        val dbHandler = DBHandler(context)
        try {
            val isInserted = dbHandler.insertBuyingCustomer(sellingFarmer)

            // Check if the data was successfully inserted
            if (isInserted) {
                Toast.makeText(context, "Selling data saved offline successfully.", Toast.LENGTH_SHORT).show()
                clearForm()
            } else {
                Toast.makeText(context, "Failed to save selling data offline.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SellingFragment", "Error inserting SellingFarmer into database", e)
            Toast.makeText(context, "Error occurred while saving data offline.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertQualityToMap(quality: Quality): Map<String, Map<String, String>> {
        return mapOf(
            "X" to mapOf(
                "foreign_matter" to quality.x.foreign_matter,
                "grade" to quality.x.grade,
                "maturity" to quality.x.maturity,
                "moisture" to quality.x.moisture,
                "mould" to quality.x.mould,
                "other1" to quality.x.other1,
                "other2" to quality.x.other2,
                "pest_and_disease" to quality.x.pest_and_disease,
                "size" to quality.x.size
            ),
            "CerealsDiv" to mapOf(
                "foreign_matter" to quality.cerealsDiv.foreign_matter,
                "grade" to quality.cerealsDiv.grade,
                "maturity" to quality.cerealsDiv.maturity,
                "mechanical_damage" to quality.cerealsDiv.mechanical_damage,
                "moisture" to quality.cerealsDiv.moisture,
                "mould" to quality.cerealsDiv.mould,
                "other1" to quality.cerealsDiv.other1,
                "other2" to quality.cerealsDiv.other2,
                "pest_and_disease" to quality.cerealsDiv.pest_and_disease,
                "size" to quality.cerealsDiv.size
            ),
            "DefaultDiv" to mapOf(
                "foreign_matter" to quality.defaultDiv.foreign_matter,
                "grade" to quality.defaultDiv.grade,
                "maturity" to quality.defaultDiv.maturity,
                "moisture" to quality.defaultDiv.moisture,
                "mould" to quality.defaultDiv.mould,
                "other1" to quality.defaultDiv.other1,
                "other2" to quality.defaultDiv.other2,
                "pest_and_disease" to quality.defaultDiv.pest_and_disease,
                "size" to quality.defaultDiv.size
            ),
            "HoneyDiv" to mapOf(
                "crystallization" to quality.honeyDiv.crystallization,
                "foreign_matter" to quality.honeyDiv.foreign_matter,
                "grade" to quality.honeyDiv.grade,
                "other1" to quality.honeyDiv.other1,
                "other2" to quality.honeyDiv.other2,
                "size" to quality.honeyDiv.size,
                "viscosity" to quality.honeyDiv.viscosity
            ),
            "MeatDiv" to mapOf(
                "foreign_matter" to quality.meatDiv.foreign_matter,
                "grade" to quality.meatDiv.grade,
                "maturity" to quality.meatDiv.maturity,
                "mechanical_damage" to quality.meatDiv.mechanical_damage,
                "moisture" to quality.meatDiv.moisture,
                "other1" to quality.meatDiv.other1,
                "other2" to quality.meatDiv.other2,
                "size" to quality.meatDiv.size
            ),
            "NutsDiv" to mapOf(
                "foreign_matter" to quality.nutsDiv.foreign_matter,
                "grade" to quality.nutsDiv.grade,
                "maturity" to quality.nutsDiv.maturity,
                "mechanical_damage" to quality.nutsDiv.mechanical_damage,
                "mould" to quality.nutsDiv.mould,
                "oil_content" to quality.nutsDiv.oil_content,
                "other1" to quality.nutsDiv.other1,
                "other2" to quality.nutsDiv.other2,
                "pest_and_disease" to quality.nutsDiv.pest_and_disease,
                "size" to quality.nutsDiv.size
            ),
            "RabbitAndPoultryDiv" to mapOf(
                "foreign_matter" to quality.rabbitAndPoultryDiv.foreign_matter,
                "grade" to quality.rabbitAndPoultryDiv.grade,
                "moisture" to quality.rabbitAndPoultryDiv.moisture,
                "other1" to quality.rabbitAndPoultryDiv.other1,
                "other2" to quality.rabbitAndPoultryDiv.other2,
                "size" to quality.rabbitAndPoultryDiv.size
            ),
            "VegetableDiv" to mapOf(
                "foreign_matter" to quality.vegetableDiv.foreign_matter,
                "grade" to quality.vegetableDiv.grade,
                "maturity" to quality.vegetableDiv.maturity,
                "mechanical_damage" to quality.vegetableDiv.mechanical_damage,
                "mould" to quality.vegetableDiv.mould,
                "pest_and_disease" to quality.vegetableDiv.pest_and_disease,
                "size" to quality.vegetableDiv.size
            )
        )
    }

    private fun validateInputs(): Boolean {
        Log.d("SellingFragment", "validateInputs() called")
        var isValid = true

        if (binding.etGRNNumber.text.isNullOrBlank()) {
            binding.tilGRNNumber.error = "GRN Number is required"
            isValid = false
        } else {
            binding.tilGRNNumber.error = null
        }

        if (binding.actvSelectProduce.text.isNullOrBlank()) {
            binding.tilSelectProduce.error = "Produce must be selected"
            isValid = false
        } else {
            binding.tilSelectProduce.error = null
        }

        if (binding.actvSelectUnit.text.isNullOrBlank()) {
            binding.tilSelectUnit.error = "Unit must be selected"
            isValid = false
        } else {
            binding.tilSelectUnit.error = null
        }

        if (binding.etWeight.text.isNullOrBlank()) {
            binding.tilWeight.error = "Weight is required"
            isValid = false
        } else if (binding.etWeight.text.toString().toDoubleOrNull() == null) {
            binding.tilWeight.error = "Weight must be a valid number"
            isValid = false
        } else {
            binding.tilWeight.error = null
        }

        if (binding.rgQuarantineAction.checkedRadioButtonId == -1) {
            Toast.makeText(context, "Please select a quarantine action", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Add quality validation here if needed

        Log.d("SellingFragment", "validateInputs() result: $isValid")
        return isValid
    }

    private fun populateSellingRequestModel() {
        Log.d("SellingFragment", "populateSellingRequestModel() called")

        // Handle nullability with fallback values
        val customerValue = selectedCustomer ?: ""
        val produceValue = if (selectedProduceId != null && selectedProduceCategory != null) {
            "${selectedProduceId}, ${selectedProduceCategory}"
        } else {
            ""
        }

        sellingRequestModel = SellingRequestModel(
            action = when (binding.rgQuarantineAction.checkedRadioButtonId) {
                R.id.rbQuarantine -> "Quarantine"
                R.id.rbAccept -> "Accept"
                R.id.rbReject -> "Reject"
                else -> ""
            },
            customer = customerValue,
            grn_number = binding.etGRNNumber.text.toString(),
            loaded = false,
            online_price = "320",
            produce = produceValue,
            quality = currentQuality,
            unit = binding.actvSelectUnit.text.toString(),
            weight = binding.etWeight.text.toString()
        )

        Log.d("SellingFragment", "SellingRequestModel populated: $sellingRequestModel")
    }

    private fun submitSellingRequest() {
        Log.d("SellingFragment", "submitSellingRequest() called")
        binding.btnSubmit.isEnabled = false
        loadingDialog.show()

        context?.let { ctx ->
            RestClient.getApiService(ctx).registerSelling(sellingRequestModel)
                .enqueue(object : Callback<SellingRequestModel> {
                    override fun onResponse(
                        call: Call<SellingRequestModel>,
                        response: Response<SellingRequestModel>
                    ) {
                        binding.btnSubmit.isEnabled = true
                        loadingDialog.dismiss()

                        if (response.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Sale registered successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            clearForm()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Toast.makeText(
                                context,
                                "Failed to register sale: $errorBody",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("SellingFragment", "Error registering sale: $errorBody")
                        }
                    }

                    override fun onFailure(call: Call<SellingRequestModel>, t: Throwable) {
                        binding.btnSubmit.isEnabled = true
                        loadingDialog.dismiss()
                        Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_LONG)
                            .show()
                        Log.e("SellingFragment", "Network error", t)
                    }
                })
        } ?: run {
            Log.e("SellingFragment", "Context is null")
            Toast.makeText(context, "Unable to process request", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearForm() {
        try {
            binding.apply {
                etGRNNumber.text?.clear()
                actvSelectProduce.text?.clear()
                actvSelectUnit.text?.clear()
                etWeight.text?.clear()

                rgQuarantineAction.clearCheck()

                selectedCustomers.clear()
                for (i in 0 until lvCustomers.count) {
                    lvCustomers.setItemChecked(i, false)
                }

                // Clear all adapters
                defaultAdapter.updateItems(listOf())
                honeyAdapter.updateItems(listOf())
                rabbitAndPoultryAdapter.updateItems(listOf())
                nutsAdapter.updateItems(listOf())
                meatAdapter.updateItems(listOf())
                cerealsAdapter.updateItems(listOf())
                vegetableAdapter.updateItems(listOf())

                // Hide all RecyclerViews
                defaultRecyclerView.visibility = View.GONE
                honeyRecyclerView.visibility = View.GONE
                rabbitPoultryRecyclerView.visibility = View.GONE
                nutsRecyclerView.visibility = View.GONE
                meatRecyclerView.visibility = View.GONE
                cerealsRecyclerView.visibility = View.GONE
                vegetableRecyclerView.visibility = View.GONE
            }

            initializeQualityData()
            clearErrorMessages()

            Log.d("SellingFragment", "Form cleared successfully")
        } catch (e: Exception) {
            Log.e("SellingFragment", "Error clearing form", e)
            Toast.makeText(context, "Error clearing form", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearErrorMessages() {
        binding.apply {
            tilGRNNumber.error = null
            tilSelectProduce.error = null
            tilSelectUnit.error = null
            tilWeight.error = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
        _binding = null
    }
}
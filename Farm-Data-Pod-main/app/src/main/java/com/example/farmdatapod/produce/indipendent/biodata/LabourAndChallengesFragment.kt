package com.example.farmdatapod.produce.indipendent.biodata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentLabourAndChallengesBinding

class LabourAndChallengesFragment : Fragment() {

    private var _binding: FragmentLabourAndChallengesBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabourAndChallengesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAutoCompleteTextViews()
        setupButtonListeners()
    }

    private fun setupAutoCompleteTextViews() {
        // *** FIX: Changed the options to a simple "Yes" or "No" array ***
        // This ensures the data sent to the server matches its expectations.
        val options = arrayOf("Yes", "No")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)

        val autoCompleteTextViews: List<AutoCompleteTextView> = listOf(
            binding.knowledgeRelatedAutoCompleteTextView,
            binding.qualityRelatedAutoCompleteTextView,
            binding.soilRelatedAutoCompleteTextView,
            binding.marketRelatedAutoCompleteTextView,
            binding.compostRelatedAutoCompleteTextView,
            binding.foodLossRelatedAutoCompleteTextView,
            binding.nutritionRelatedAutoCompleteTextView,
            binding.financeRelatedAutoCompleteTextView,
            binding.pestsRelatedAutoCompleteTextView,
            binding.weatherRelatedAutoCompleteTextView,
            binding.diseaseRelatedAutoCompleteTextView
        )

        autoCompleteTextViews.forEach { autoCompleteTextView ->
            autoCompleteTextView.setAdapter(adapter)
        }
    }

    private fun setupButtonListeners() {
        // Listeners can be added here if needed in the future
    }

    fun validateForm(): Boolean {
        var isValid = true

        // Validate Family Labor
        if (binding.familyLaborEditText.text.isNullOrBlank() || binding.familyLaborEditText.text.toString().toIntOrNull() == null) {
            binding.familyLaborInputLayout.error = "Family Labor is required and must be a number"
            isValid = false
        } else {
            binding.familyLaborInputLayout.error = null
        }

        // Validate Hired Labor
        if (binding.hiredLaborEditText.text.isNullOrBlank() || binding.hiredLaborEditText.text.toString().toIntOrNull() == null) {
            binding.hiredLaborInputLayout.error = "Hired Labor is required and must be a number"
            isValid = false
        } else {
            binding.hiredLaborInputLayout.error = null
        }

        // Validate Challenges
        val challenges = listOf(
            binding.knowledgeRelatedAutoCompleteTextView,
            binding.qualityRelatedAutoCompleteTextView,
            binding.soilRelatedAutoCompleteTextView,
            binding.marketRelatedAutoCompleteTextView,
            binding.compostRelatedAutoCompleteTextView,
            binding.foodLossRelatedAutoCompleteTextView,
            binding.nutritionRelatedAutoCompleteTextView,
            binding.financeRelatedAutoCompleteTextView,
            binding.pestsRelatedAutoCompleteTextView,
            binding.weatherRelatedAutoCompleteTextView,
            binding.diseaseRelatedAutoCompleteTextView
        )

        if (challenges.any { it.text.isBlank() }) {
            showToast("Please fill out all challenge fields.")
            isValid = false
        }

        return isValid
    }

    fun saveData() {
        // Collect data from form
        val familyLabor = binding.familyLaborEditText.text.toString().toIntOrNull() ?: 0
        val hiredLabor = binding.hiredLaborEditText.text.toString().toIntOrNull() ?: 0

        val knowledge = binding.knowledgeRelatedAutoCompleteTextView.text.toString()
        val quality = binding.qualityRelatedAutoCompleteTextView.text.toString()
        val soil = binding.soilRelatedAutoCompleteTextView.text.toString()
        val market = binding.marketRelatedAutoCompleteTextView.text.toString()
        val compost = binding.compostRelatedAutoCompleteTextView.text.toString()
        val foodLoss = binding.foodLossRelatedAutoCompleteTextView.text.toString()
        val nutrition = binding.nutritionRelatedAutoCompleteTextView.text.toString()
        val finance = binding.financeRelatedAutoCompleteTextView.text.toString()
        val pests = binding.pestsRelatedAutoCompleteTextView.text.toString()
        val weather = binding.weatherRelatedAutoCompleteTextView.text.toString()
        val disease = binding.diseaseRelatedAutoCompleteTextView.text.toString()

        // Save data to ViewModel
        sharedViewModel.setFamilyLabor(familyLabor)
        sharedViewModel.setHiredLabor(hiredLabor)

        sharedViewModel.setKnowledgeRelated(knowledge)
        sharedViewModel.setQualityRelated(quality)
        sharedViewModel.setSoilRelated(soil)
        sharedViewModel.setMarketRelated(market)
        sharedViewModel.setCompostRelated(compost)
        sharedViewModel.setFoodLossRelated(foodLoss)
        sharedViewModel.setNutritionRelated(nutrition)
        sharedViewModel.setFinanceRelated(finance)
        sharedViewModel.setPestsRelated(pests)
        sharedViewModel.setWeatherRelated(weather)
        sharedViewModel.setDiseaseRelated(disease)

        // Provide feedback
        showToast("Data saved successfully")
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
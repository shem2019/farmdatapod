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
        val autoCompleteTextViews: List<Pair<AutoCompleteTextView, Int>> = listOf(
            binding.knowledgeRelatedAutoCompleteTextView to R.array.knowledge_related_options,
            binding.qualityRelatedAutoCompleteTextView to R.array.knowledge_related_options,
            binding.soilRelatedAutoCompleteTextView to R.array.knowledge_related_options,
            binding.marketRelatedAutoCompleteTextView to R.array.knowledge_related_options,
            binding.compostRelatedAutoCompleteTextView to R.array.knowledge_related_options,
            binding.foodLossRelatedAutoCompleteTextView to R.array.knowledge_related_options,
            binding.nutritionRelatedAutoCompleteTextView to R.array.knowledge_related_options,
            binding.financeRelatedAutoCompleteTextView to R.array.knowledge_related_options,
            binding.pestsRelatedAutoCompleteTextView to R.array.knowledge_related_options,
            binding.weatherRelatedAutoCompleteTextView to R.array.knowledge_related_options,
            binding.diseaseRelatedAutoCompleteTextView to R.array.knowledge_related_options
        )

        autoCompleteTextViews.forEach { (autoCompleteTextView, optionsArrayResId) ->
            val options = resources.getStringArray(optionsArrayResId)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
            autoCompleteTextView.setAdapter(adapter)
        }
    }

    private fun setupButtonListeners() {

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
        val challenges = mapOf(
            "Knowledge" to binding.knowledgeRelatedAutoCompleteTextView.text.toString(),
            "Quality" to binding.qualityRelatedAutoCompleteTextView.text.toString(),
            "Soil" to binding.soilRelatedAutoCompleteTextView.text.toString(),
            "Market" to binding.marketRelatedAutoCompleteTextView.text.toString(),
            "Compost" to binding.compostRelatedAutoCompleteTextView.text.toString(),
            "Food Loss" to binding.foodLossRelatedAutoCompleteTextView.text.toString(),
            "Nutrition" to binding.nutritionRelatedAutoCompleteTextView.text.toString(),
            "Finance" to binding.financeRelatedAutoCompleteTextView.text.toString(),
            "Pests" to binding.pestsRelatedAutoCompleteTextView.text.toString(),
            "Weather" to binding.weatherRelatedAutoCompleteTextView.text.toString(),
            "Disease" to binding.diseaseRelatedAutoCompleteTextView.text.toString()
        )

        if (challenges.values.any { it.isBlank() }) {
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

package com.example.farmdatapod.produce.indipendent.biodata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.farmdatapod.databinding.FragmentLivestockInformationBinding
import com.google.android.material.snackbar.Snackbar

class LivestockInformationFragment : Fragment() {

    private var _binding: FragmentLivestockInformationBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLivestockInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        binding.backButton.setOnClickListener {
            // Handle back navigation, e.g., findNavController().navigateUp()
        }

    }

    fun validateForm(): Boolean {
        var isValid = true

        val fields = listOf(
            binding.dairyCattleInputLayout to binding.dairyCattleEditText,
            binding.beefCattleInputLayout to binding.beefCattleEditText,
            binding.sheepInputLayout to binding.sheepEditText,
            binding.goatsInputLayout to binding.goatsEditText,
            binding.pigsInputLayout to binding.pigsEditText,
            binding.poultryInputLayout to binding.poultryEditText,
            binding.camelsInputLayout to binding.camelsEditText,
            binding.aquacultureInputLayout to binding.aquacultureEditText,
            binding.rabbitsInputLayout to binding.rabbitsEditText,
            binding.beehivesInputLayout to binding.beehivesEditText,
            binding.donkeysInputLayout to binding.donkeysEditText
        )

        fields.forEach { (inputLayout, editText) ->
            val value = editText.text.toString()
            if (value.isBlank()) {
                inputLayout.error = "This field is required"
                isValid = false
            } else if (value.toIntOrNull() == null || value.toInt() < 0) {
                inputLayout.error = "Please enter a valid non-negative number"
                isValid = false
            } else {
                inputLayout.error = null
            }
        }

        return isValid
    }

    fun saveData() {
        // Collect data from form
        val dairyCattle = binding.dairyCattleEditText.text.toString().toIntOrNull() ?: 0
        val beefCattle = binding.beefCattleEditText.text.toString().toIntOrNull() ?: 0
        val sheep = binding.sheepEditText.text.toString().toIntOrNull() ?: 0
        val goats = binding.goatsEditText.text.toString().toIntOrNull() ?: 0
        val pigs = binding.pigsEditText.text.toString().toIntOrNull() ?: 0
        val poultry = binding.poultryEditText.text.toString().toIntOrNull() ?: 0
        val camels = binding.camelsEditText.text.toString().toIntOrNull() ?: 0
        val aquaculture = binding.aquacultureEditText.text.toString().toIntOrNull() ?: 0
        val rabbits = binding.rabbitsEditText.text.toString().toIntOrNull() ?: 0
        val beehives = binding.beehivesEditText.text.toString().toIntOrNull() ?: 0
        val donkeys = binding.donkeysEditText.text.toString().toIntOrNull() ?: 0

        // Save data to ViewModel
        sharedViewModel.setDairyCattle(dairyCattle)
        sharedViewModel.setBeefCattle(beefCattle)
        sharedViewModel.setSheep(sheep)
        sharedViewModel.setGoats(goats)
        sharedViewModel.setPigs(pigs)
        sharedViewModel.setPoultry(poultry)
        sharedViewModel.setCamels(camels)
        sharedViewModel.setAquaculture(aquaculture)
        sharedViewModel.setRabbits(rabbits)
        sharedViewModel.setBeehives(beehives)
        sharedViewModel.setDonkeys(donkeys)

        // Provide feedback
        Snackbar.make(binding.root, "Livestock data saved successfully", Snackbar.LENGTH_LONG).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
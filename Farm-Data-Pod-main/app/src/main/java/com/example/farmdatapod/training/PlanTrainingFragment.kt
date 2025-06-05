package com.example.farmdatapod.training

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.R
import com.example.farmdatapod.Training
import com.example.farmdatapod.adapter.ParticipantsAdapter
import com.example.farmdatapod.databinding.FragmentPlanTrainingBinding
import com.example.farmdatapod.models.Participants
import com.example.farmdatapod.models.TrainingRequestModel
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class PlanTrainingFragment : Fragment() {

    private var _binding: FragmentPlanTrainingBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkViewModel: NetworkViewModel
    private lateinit var dbHandler: DBHandler
    private val calendar = Calendar.getInstance()
    private val participantNames = mutableListOf<String>()
    private lateinit var participantsAdapter: ParticipantsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Handle arguments if any
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlanTrainingBinding.inflate(inflater, container, false)
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)
        dbHandler = DBHandler(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button functionality
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Add participant button functionality
        binding.addParticipantButton.setOnClickListener {
            addParticipant()
        }

        // Set up RecyclerView for participants
        participantsAdapter = ParticipantsAdapter(participantNames)
        binding.participantsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.participantsRecyclerView.adapter = participantsAdapter

        // Retrieve buying center names from dbHandler
        val buyingCenterNames = dbHandler.allBuyingCenterNames

        // Create an ArrayAdapter for the AutoCompleteTextView
        val buyingCenterAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            buyingCenterNames
        )

        // Set the adapter to the AutoCompleteTextView
        binding.buyingCentersInput.setAdapter(buyingCenterAdapter)

        // Set up date picker for training dates
        binding.trainingDatesInput.setOnClickListener {
            showDatePickerDialog()
        }

        setupSubmitButton()
    }

    private fun showDatePickerDialog() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val startDate = calendar.time

            DatePickerDialog(
                requireContext(),
                { _, endYear, endMonth, endDayOfMonth ->
                    calendar.set(Calendar.YEAR, endYear)
                    calendar.set(Calendar.MONTH, endMonth)
                    calendar.set(Calendar.DAY_OF_MONTH, endDayOfMonth)
                    val endDate = calendar.time

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formattedDate = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
                    binding.trainingDatesInput.setText(formattedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (binding.trainerNameInput.text.isNullOrEmpty()) {
            binding.trainerNameLayout.error = "Trainer Name is required"
            isValid = false
        } else {
            binding.trainerNameLayout.error = null
        }

        if (binding.courseNameInput.text.isNullOrEmpty()) {
            binding.courseNameLayout.error = "Course Name is required"
            isValid = false
        } else {
            binding.courseNameLayout.error = null
        }

        if (binding.buyingCentersInput.text.isNullOrEmpty()) {
            binding.buyingCentersLayout.error = "Buying Centers is required"
            isValid = false
        } else {
            binding.buyingCentersLayout.error = null
        }

        if (binding.courseDescriptionInput.text.isNullOrEmpty()) {
            binding.courseDescriptionLayout.error = "Course Description is required"
            isValid = false
        } else {
            binding.courseDescriptionLayout.error = null
        }

        if (binding.trainingDatesInput.text.isNullOrEmpty()) {
            binding.trainingDatesLayout.error = "Training Dates are required"
            isValid = false
        } else {
            binding.trainingDatesLayout.error = null
        }

        if (binding.contentOfTrainingInput.text.isNullOrEmpty()) {
            binding.contentOfTrainingLayout.error = "Content of Training is required"
            isValid = false
        } else {
            binding.contentOfTrainingLayout.error = null
        }

        if (binding.venueInput.text.isNullOrEmpty()) {
            binding.venueLayout.error = "Venue is required"
            isValid = false
        } else {
            binding.venueLayout.error = null
        }

        return isValid
    }

    private fun addParticipant() {
        val participantName = "Participant ${participantNames.size + 1}" // Replace with actual participant name input
        participantNames.add(participantName)
        participantsAdapter.notifyItemInserted(participantNames.size - 1)
        Toast.makeText(requireContext(), "$participantName added", Toast.LENGTH_SHORT).show()
    }

    private fun clearFields() {
        binding.trainerNameInput.setText("")
        binding.courseNameInput.setText("")
        binding.buyingCentersInput.setText("")
        binding.courseDescriptionInput.setText("")
        binding.trainingDatesInput.setText("")
        binding.contentOfTrainingInput.setText("")
        binding.venueInput.setText("")
        participantNames.clear()
        participantsAdapter.notifyDataSetChanged()
    }

    private fun setupSubmitButton() {
        // Observe network connectivity changes
        networkViewModel.networkLiveData.observe(viewLifecycleOwner) { isNetworkAvailable ->
            Log.d("PlanTrainingFragment", "Network status changed: $isNetworkAvailable")

            binding.submitButton.setOnClickListener {
                Log.d("PlanTrainingFragment", "Submit button clicked")
                if (validateInputs()) {
                    Log.d("PlanTrainingFragment", "Inputs validated. Checking network availability...")
                    if (isNetworkAvailable) {
                        Log.d("PlanTrainingFragment", "Network available. Proceeding with online submission.")
                        submitTrainingData()
                    } else {
                        Log.d("PlanTrainingFragment", "No network available. Handling offline submission.")
                        handleOfflineTrainingSubmission()
                    }
                } else {
                    Log.d("PlanTrainingFragment", "Input validation failed")
                    Toast.makeText(context, "Please fill all required fields correctly", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun submitTrainingData() {

        val participants = Participants(
            participant_1 = participantNames.getOrNull(0) ?: "",
            participant_2 = participantNames.getOrNull(1) ?: "",
            participant_3 = participantNames.getOrNull(2) ?: ""
        )

        val trainingRequest = TrainingRequestModel(
            buying_center = binding.buyingCentersInput.text.toString(),
            content_of_training = binding.contentOfTrainingInput.text.toString(),
            course_description = binding.courseDescriptionInput.text.toString(),
            course_name = binding.courseNameInput.text.toString(),
            date_of_training = binding.trainingDatesInput.text.toString(),
            participants = participants,
            trainer_name = binding.trainerNameInput.text.toString(),
            venue = binding.venueInput.text.toString()
        )

        context?.let { ctx ->
            RestClient.getApiService(ctx).registerTraining(trainingRequest)
                .enqueue(object : Callback<TrainingRequestModel> {
                    override fun onResponse(call: Call<TrainingRequestModel>, response: Response<TrainingRequestModel>) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Training registered successfully", Toast.LENGTH_SHORT).show()
                            clearFields()
                        } else {
                            Toast.makeText(context, "Failed to register training", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<TrainingRequestModel>, t: Throwable) {
                        Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun handleOfflineTrainingSubmission() {
        // Log that the form submission is being handled offline
        Log.d("PlanTrainingFragment", "Form submitted offline. The data will be saved locally or queued for later submission.")

        // Initialize SharedPrefs to get the user_id
        val sharedPrefs = SharedPrefs(requireContext())
        val userId = sharedPrefs.getUserId() ?: "Unknown User"

        val training = Training().apply {
            course_name = binding.courseNameInput.text.toString()
            trainer_name = binding.trainerNameInput.text.toString()
            buying_center = binding.buyingCentersInput.text.toString()
            course_description = binding.courseDescriptionInput.text.toString()
            date_of_training = binding.trainingDatesInput.text.toString()
            content_of_training = binding.contentOfTrainingInput.text.toString()
            venue = binding.venueInput.text.toString()

            // Map participants from the input
            participants = mapOf(
                "participant_1" to (participantNames.getOrNull(0) ?: ""),
                "participant_2" to (participantNames.getOrNull(1) ?: ""),
                "participant_3" to (participantNames.getOrNull(2) ?: "")
            )

            user_id = userId
        }

        // Insert the training into the local database using DBHandler
        val dbHandler = DBHandler(context)
        val isInserted = dbHandler.insertTraining(training)

        // Check if the training data was successfully inserted
        if (isInserted) {
            Toast.makeText(context, "Training data saved offline successfully.", Toast.LENGTH_SHORT).show()
            clearFields()
        } else {
            Toast.makeText(context, "Failed to save training data offline.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
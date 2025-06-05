package com.example.farmdatapod.training

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.R
import com.example.farmdatapod.Training
import com.example.farmdatapod.adapter.ViewTrainingAdapter

class ViewTrainingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ViewTrainingAdapter
    private lateinit var dbHandler: DBHandler
    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ActivityResultLauncher for image selection
        selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    Log.d("ViewTrainingFragment", "Image URI: $it")
                    // Handle image upload independently of any selected training
                    adapter.uploadImage(requireContext(), it)
                } ?: run {
                    Toast.makeText(requireContext(), "Failed to get image URI", Toast.LENGTH_SHORT).show()
                    Log.e("ViewTrainingFragment", "Image URI is null when trying to upload image")
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout
        val view = inflater.inflate(R.layout.fragment_view_training, container, false)

        // Initialize DBHandler and RecyclerView
        dbHandler = DBHandler(requireContext())
        recyclerView = view.findViewById(R.id.recyclerViewTrainings)

        // Load and display the list of trainings
        loadTrainings()

        return view
    }

    private fun loadTrainings() {
        // Fetch trainings from the database
        val trainings = dbHandler.getAllTrainings()

        // Initialize the adapter with training data and action handlers
        adapter = ViewTrainingAdapter(
            trainings,
            { training, action ->
                when (action) {
                    "View Training" -> viewTraining(training)
                    "Update Attendance for Ongoing Trainings" -> updateAttendanceForOngoing(training)
                    "Upload Image" -> {
                        // Launch the image picker without setting selectedTraining
                        launchImagePicker()
                    }
                }
            },
            requireContext(),
            selectImageLauncher
        )

        // Set up the RecyclerView with the adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun launchImagePicker() {
        // Create an intent to pick an image
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        // Launch the image picker using the ActivityResultLauncher
        selectImageLauncher.launch(intent)
    }

    private fun showPopupMenu(training: Training, view: View) {
        // Create and show a popup menu for training options
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.training_options_menu, popupMenu.menu)

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_view_training -> {
                    viewTraining(training)
                    true
                }
                R.id.menu_update_attendance -> {
                    updateAttendanceForOngoing(training)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun viewTraining(training: Training) {
        // Implement viewing training details
        Toast.makeText(requireContext(), "Viewing details for training: ${training.course_name}", Toast.LENGTH_SHORT).show()
    }

    private fun updateAttendanceForOngoing(training: Training) {
        // Implement updating attendance for ongoing trainings
        // Toast.makeText(requireContext(), "Updating attendance for training: ${training.course_name}", Toast.LENGTH_SHORT).show()
    }
}

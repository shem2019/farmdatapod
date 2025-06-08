package com.example.farmdatapod.hub.hubAggregation.cig

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentCigBinding
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIG
import com.example.farmdatapod.hub.hubRegistration.data.HubRepository
import com.example.farmdatapod.models.MemberRequest
import com.example.farmdatapod.models.UploadResponse
import com.example.farmdatapod.network.RestClient
import jxl.Workbook
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CIGFragment : Fragment() {

    private val TAG = "CIGFragment"
    private var _binding: FragmentCigBinding? = null
    private val binding get() = _binding!!

    // ViewModel is now the primary point of interaction for logic
    private lateinit var viewModel: CIGViewModel

    // Store the list of members parsed from Excel as the correct data type
    private var membersFromExcel: List<MemberRequest> = emptyList()

    // Store the URLs of uploaded files
    private var constitutionUrl: String? = null
    private var registrationUrl: String? = null
    private var certificateUrl: String? = null
    private var electionsHeldUrl: String? = null

    // To track which file upload is currently active
    private var currentUploadType: String? = null

    // Modern way to handle activity results for picking files
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                when (currentUploadType) {
                    "excel" -> processExcelFile(uri)
                    "constitution", "registration", "elections", "certificate" -> uploadImageToCloudinary(uri)
                    else -> Log.w(TAG, "Unknown file upload type: $currentUploadType")
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCigBinding.inflate(inflater, container, false)
        // Initialize the ViewModel here
        viewModel = ViewModelProvider(this)[CIGViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup Hub Spinner
        val hubRepository = HubRepository(requireContext())
        lifecycleScope.launch {
            hubRepository.getAllHubs().collect { hubs ->
                val hubNames = hubs.map { it.hubName }
                val hubAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, hubNames)
                binding.inputHub.setAdapter(hubAdapter)
            }
        }

        // Setup other spinners like frequency
        val frequencyOptions = listOf("Weekly", "Bi-Weekly", "Monthly")
        val frequencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencyOptions)
        binding.inputCigFrequency.setAdapter(frequencyAdapter)

        // Setup Date and Time Pickers
        binding.cigYearEst.setOnClickListener { showDatePickerDialog(it as EditText) }
        binding.cigLastElectionDate.setOnClickListener { showDatePickerDialog(it as EditText) }
        binding.inputScheduledMeetingTime.setOnClickListener { showTimePickerDialog(it as EditText) }

        setupFileUploadListeners()

        // This button now triggers a specific file picker for Excel
        binding.buttonMembershipFile.setOnClickListener {
            currentUploadType = "excel"
            openFilePicker("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }

        // The save button triggers the submission process
        binding.buttonSave.setOnClickListener {
            submitCigDataToViewModel()
        }
    }

    /**
     * This function observes the LiveData from the ViewModel and updates the UI accordingly.
     * The Fragment's only job is to react to these state changes.
     */
    private fun observeViewModel() {
        viewModel.registrationState.observe(viewLifecycleOwner) { state ->
            // Assumes you have a ProgressBar with id 'progressBar' in your layout
           // binding.progressBar.isVisible = state is RegistrationState.Loading
            binding.buttonSave.isEnabled = state !is RegistrationState.Loading

            when (state) {
                is RegistrationState.Success -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    findNavController().popBackStack() // Go back to the previous screen on success
                }
                is RegistrationState.Error -> {
                    Toast.makeText(requireContext(), "Error: ${state.errorMessage}", Toast.LENGTH_LONG).show()
                }
                is RegistrationState.Idle, is RegistrationState.Loading -> { /* Handled by visibility bindings above */ }
            }
        }
    }

    private fun setupFileUploadListeners() {
        // Constitution
        binding.radioGroupConstitution.setOnCheckedChangeListener { _, id ->
            val isYes = (id == R.id.radio_constitution_yes)
            binding.uploadConstitution.visibility = if(isYes) View.VISIBLE else View.GONE
            binding.uploadConstitutionText.visibility = if(isYes) View.VISIBLE else View.GONE
        }
        binding.uploadConstitution.setOnClickListener {
            currentUploadType = "constitution"
            openFilePicker("image/*")
        }

        // Registration
        binding.radioGroupRegistration.setOnCheckedChangeListener { _, id ->
            val isYes = (id == R.id.radio_registration_yes)
            binding.uploadRegistration.visibility = if(isYes) View.VISIBLE else View.GONE
            binding.uploadRegistrationText.visibility = if(isYes) View.VISIBLE else View.GONE
        }
        binding.uploadRegistration.setOnClickListener {
            currentUploadType = "registration"
            openFilePicker("image/*")
        }

        // Elections Held
        binding.radioGroupElectionsHeld.setOnCheckedChangeListener { _, id ->
            val isYes = (id == R.id.radio_election_held_yes)
            binding.uploadElectionsHeld.visibility = if(isYes) View.VISIBLE else View.GONE
            binding.uploadElectionsHeldText.visibility = if(isYes) View.VISIBLE else View.GONE
        }
        binding.uploadElectionsHeld.setOnClickListener {
            currentUploadType = "elections"
            openFilePicker("image/*")
        }

        // TODO: Add UI for certificate upload (RadioGroup, ImageView, TextView) in your fragment_cig.xml
        // It will be similar to the Constitution section above. For now, its URL will be null.
    }

    /**
     * Opens the system file picker with specified MIME types.
     */
    private fun openFilePicker(vararg mimeTypes: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // General type
            if (mimeTypes.isNotEmpty()) {
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes) // Specific types
            }
        }
        filePickerLauncher.launch(intent)
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        val file = getFileFromUri(uri) ?: return
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val textViewToUpdate = when (currentUploadType) {
            "constitution" -> binding.uploadConstitutionText
            "registration" -> binding.uploadRegistrationText
            "elections" -> binding.uploadElectionsHeldText
            // "certificate" -> binding.uploadCertificateText // Add this to your XML
            else -> null
        }
        textViewToUpdate?.text = "Uploading..."

        RestClient.getApiService(requireContext()).uploadToCloudinary(body).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                file.delete() // Clean up the temp file
                val secureUrl = response.body()?.secureUrl
                if (response.isSuccessful && secureUrl != null) {
                    when (currentUploadType) {
                        "constitution" -> constitutionUrl = secureUrl
                        "registration" -> registrationUrl = secureUrl
                        "elections" -> electionsHeldUrl = secureUrl
                        "certificate" -> certificateUrl = secureUrl
                    }
                    textViewToUpdate?.text = "File Uploaded"
                } else {
                    textViewToUpdate?.text = "Upload Failed"
                    Log.e(TAG, "Cloudinary upload failed: ${response.errorBody()?.string()}")
                }
            }
            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                file.delete() // Clean up the temp file
                textViewToUpdate?.text = "Upload Failed"
                Log.e(TAG, "Cloudinary upload error", t)
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val destinationFile = File(requireContext().cacheDir, "temp_${System.currentTimeMillis()}")
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destinationFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create temp file from URI", e)
            Toast.makeText(requireContext(), "Failed to access file", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun processExcelFile(uri: Uri) {
        try {
            val memberMaps = requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook: Workbook = Workbook.getWorkbook(inputStream)
                val sheet = workbook.getSheet(0)
                (1 until sheet.rows).map { i -> // map directly, skip header
                    val row = sheet.getRow(i)
                    if (row.size < 10) null else mapOf(
                        "other_name" to row[1].contents, "last_name" to row[2].contents,
                        "gender" to row[3].contents, "date_of_birth" to row[4].contents,
                        "email" to row[5].contents, "phone_number" to row[6].contents,
                        "id_number" to row[7].contents, "product_involved" to row[8].contents,
                        "hectorage_registered_under_cig" to row[9].contents
                    )
                }.filterNotNull() // Filter out any null rows
            } ?: emptyList()

            this.membersFromExcel = convertMapToMemberRequest(memberMaps)
            binding.textviewFileChosen.text = "${this.membersFromExcel.size} members loaded from file."
            binding.inputNoMembers.setText(this.membersFromExcel.size.toString())

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to read or parse Excel file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun convertMapToMemberRequest(memberMaps: List<Map<String, String>>): List<MemberRequest> {
        return memberMaps.mapNotNull { map ->
            try {
                MemberRequest(
                    otherName = map["other_name"] ?: "", lastName = map["last_name"] ?: "",
                    gender = map["gender"] ?: "", dateOfBirth = formatDateForAPI(map["date_of_birth"]),
                    email = map["email"] ?: "", phoneNumber = map["phone_number"]?.toLongOrNull() ?: 0L,
                    idNumber = map["id_number"]?.toLongOrNull() ?: 0L,
                    productInvolved = map["product_involved"] ?: "",
                    hectorageRegisteredUnderCig = map["hectorage_registered_under_cig"] ?: ""
                )
            } catch (e: Exception) {
                Log.e(TAG, "Skipping invalid member data from Excel: $map", e)
                null
            }
        }
    }

    private fun submitCigDataToViewModel() {
        if (!validateInputs()) {
            Toast.makeText(context, "Please fix the errors and fill all required fields.", Toast.LENGTH_LONG).show()
            return
        }

        // Create a CIG object with data from the UI
        val localCig = CIG(
            cigName = binding.cigName.text.toString(),
            hub = binding.inputHub.text.toString(),
            numberOfMembers = binding.inputNoMembers.text.toString().toInt(),
            dateEstablished = (binding.cigYearEst.tag as? String) ?: "",
            constitution = if (binding.radioGroupConstitution.checkedRadioButtonId == R.id.radio_constitution_yes) constitutionUrl else "No",
            registration = if (binding.radioGroupRegistration.checkedRadioButtonId == R.id.radio_registration_yes) registrationUrl else "No",
            certificate = certificateUrl ?: "No", // Pass certificate URL
            membershipRegister = "Membership details provided via uploaded register.",
            electionsHeld = if (binding.radioGroupElectionsHeld.checkedRadioButtonId == R.id.radio_election_held_yes) electionsHeldUrl else "No",
            dateOfLastElections = (binding.cigLastElectionDate.tag as? String) ?: "",
            meetingVenue = binding.cigMeetingVenue.text.toString(),
            frequency = binding.inputCigFrequency.text.toString(),
            scheduledMeetingDay = binding.inputScheduledMeetingDay.text.toString(),
            scheduledMeetingTime = binding.inputScheduledMeetingTime.text.toString(),
            userId = null, // Can be fetched from a user manager if needed
            membersJson = null // ViewModel will populate this
        )

        // Pass the CIG data and the parsed member list to the ViewModel
        viewModel.submitCIGData(localCig, membersFromExcel)
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
            val displayFormat = SimpleDateFormat("d MMMM, yyyy", Locale.US)
            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            editText.setText(displayFormat.format(selectedDate.time))
            editText.tag = apiFormat.format(selectedDate.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, hour, minute ->
            val time = String.format(Locale.US, "%02d:%02d:00", hour, minute) // HH:MM:SS format
            editText.setText(time)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    // This is a placeholder for a more robust date parsing/formatting logic if needed
    private fun formatDateForAPI(excelDate: String?): String {
        return excelDate ?: ""
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        if (binding.cigName.text.isNullOrBlank()) {
            binding.cigName.error = "CIG Name is required"; isValid = false
        }
        if (membersFromExcel.isEmpty()) {
            Toast.makeText(requireContext(), "Please upload a membership register file.", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        // ... Add comprehensive validation for all other required fields ...
        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
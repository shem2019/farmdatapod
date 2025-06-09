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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.FarmDataPodApplication
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentCigBinding
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIG
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIGViewModel
import com.example.farmdatapod.hub.hubAggregation.cig.data.RegistrationState
import com.example.farmdatapod.hub.hubRegistration.data.HubRepository
import com.example.farmdatapod.models.MemberRequest
import com.example.farmdatapod.models.UploadResponse
import com.example.farmdatapod.network.RestClient
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CIGFragment : Fragment() {

    private val TAG = "CIGFragment"
    private var _binding: FragmentCigBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CIGViewModel
    private lateinit var hubRepository: HubRepository
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    // State variables to hold form data
    private var membersFromExcel: List<MemberRequest> = emptyList()
    private var constitutionUrl: String? = null
    private var registrationUrl: String? = null
    private var certificateUrl: String? = null
    private var electionsHeldUrl: String? = null
    private var currentUploadType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use modern ActivityResultLauncher for all file picking
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    Log.d(TAG, "File selected with URI: $uri")
                    when (currentUploadType) {
                        "excel" -> processExcelFile(uri)
                        "constitution", "registration", "elections", "certificate" -> uploadImageToCloudinary(uri)
                        else -> Log.w(TAG, "Unknown file upload type: $currentUploadType")
                    }
                }
            } else {
                Log.w(TAG, "File selection cancelled or failed.")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCigBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[CIGViewModel::class.java]
        hubRepository = HubRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Populate Hub Spinner
        lifecycleScope.launch {
            hubRepository.getAllHubs().collect { hubs ->
                val hubNames = hubs.map { it.hubName }
                val hubAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, hubNames)
                binding.inputHub.setAdapter(hubAdapter)
            }
        }

        // Populate Frequency Spinner
        val frequencyOptions = listOf("Weekly", "Bi-Weekly", "Monthly")
        val frequencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencyOptions)
        binding.inputCigFrequency.setAdapter(frequencyAdapter)

        // Setup click listeners
        binding.cigYearEst.setOnClickListener { showDatePickerDialog(it as EditText) }
        binding.cigLastElectionDate.setOnClickListener { showDatePickerDialog(it as EditText) }
        binding.inputScheduledMeetingTime.setOnClickListener { showTimePickerDialog(it as EditText) }

        setupFileUploadListeners()

        binding.buttonMembershipFile.setOnClickListener {
            currentUploadType = "excel"
            openFilePicker("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }

        binding.buttonSave.setOnClickListener {
            submitCigDataToViewModel()
        }
    }

    private fun observeViewModel() {
        viewModel.registrationState.observe(viewLifecycleOwner) { state ->
            // Assumes you have a ProgressBar with id 'progressBar' in your fragment_cig.xml
            //binding.progressBar.isVisible = state is RegistrationState.Loading
            binding.buttonSave.isEnabled = state !is RegistrationState.Loading

            when (state) {
                is RegistrationState.Success -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                is RegistrationState.Error -> {
                    Toast.makeText(requireContext(), "Error: ${state.errorMessage}", Toast.LENGTH_LONG).show()
                }
                is RegistrationState.Idle, is RegistrationState.Loading -> {}
            }
        }
    }

    private fun setupFileUploadListeners() {
        // Constitution
        binding.radioGroupConstitution.setOnCheckedChangeListener { _, id ->
            val showUpload = (id == R.id.radio_constitution_yes)
            binding.uploadConstitution.visibility = if(showUpload) View.VISIBLE else View.GONE
            binding.uploadConstitutionText.visibility = if(showUpload) View.VISIBLE else View.GONE
        }
        binding.uploadConstitution.setOnClickListener {
            currentUploadType = "constitution"
            openFilePicker("image/*")
        }

        // Registration
        binding.radioGroupRegistration.setOnCheckedChangeListener { _, id ->
            val showUpload = (id == R.id.radio_registration_yes)
            binding.uploadRegistration.visibility = if(showUpload) View.VISIBLE else View.GONE
            binding.uploadRegistrationText.visibility = if(showUpload) View.VISIBLE else View.GONE
        }
        binding.uploadRegistration.setOnClickListener {
            currentUploadType = "registration"
            openFilePicker("image/*")
        }

        // Elections
        binding.radioGroupElectionsHeld.setOnCheckedChangeListener { _, id ->
            val showUpload = (id == R.id.radio_election_held_yes)
            binding.uploadElectionsHeld.visibility = if(showUpload) View.VISIBLE else View.GONE
            binding.uploadElectionsHeldText.visibility = if(showUpload) View.VISIBLE else View.GONE
        }
        binding.uploadElectionsHeld.setOnClickListener {
            currentUploadType = "elections"
            openFilePicker("image/*")
        }
    }

    private fun openFilePicker(vararg mimeTypes: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
            if (mimeTypes.size > 1) {
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
        }
        filePickerLauncher.launch(intent)
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val destinationFile = File(requireContext().cacheDir, "temp_upload_${System.currentTimeMillis()}")
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destinationFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create temp file from URI", e)
            Toast.makeText(requireContext(), "Error accessing the selected file.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        val file = getFileFromUri(uri) ?: return // Exit if file is not created
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val textViewToUpdate: TextView? = when (currentUploadType) {
            "constitution" -> binding.uploadConstitutionText
            "registration" -> binding.uploadRegistrationText
            "elections" -> binding.uploadElectionsHeldText
            "certificate" -> null // TODO: Add a TextView for certificate in your XML
            else -> null
        }
        textViewToUpdate?.text = "Uploading, please wait..."

        RestClient.getApiService(requireContext()).uploadToCloudinary(body).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                file.delete()
                val secureUrl = response.body()?.secureUrl
                if (response.isSuccessful && secureUrl != null) {
                    when (currentUploadType) {
                        "constitution" -> constitutionUrl = secureUrl
                        "registration" -> registrationUrl = secureUrl
                        "elections" -> electionsHeldUrl = secureUrl
                        "certificate" -> certificateUrl = secureUrl
                    }
                    textViewToUpdate?.text = "Upload successful"
                } else {
                    textViewToUpdate?.text = "Upload failed"
                    Log.e(TAG, "Cloudinary upload failed: ${response.errorBody()?.string()}")
                }
            }
            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                file.delete()
                textViewToUpdate?.text = "Upload failed"
                Log.e(TAG, "Cloudinary upload exception", t)
            }
        })
    }

    private fun processExcelFile(uri: Uri) {
        try {
            val memberMaps = requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook: Workbook = Workbook.getWorkbook(inputStream)
                val sheet = workbook.getSheet(0)
                (1 until sheet.rows).mapNotNull { i ->
                    val row = sheet.getRow(i)
                    if (row.size < 10 || row[1].contents.isBlank()) null else mapOf(
                        "other_name" to row[1].contents, "last_name" to row[2].contents,
                        "gender" to row[3].contents, "date_of_birth" to row[4].contents,
                        "email" to row[5].contents, "phone_number" to row[6].contents,
                        "id_number" to row[7].contents, "product_involved" to row[8].contents,
                        "hectorage_registered_under_cig" to row[9].contents
                    )
                }
            } ?: emptyList()

            membersFromExcel = convertMapToMemberRequest(memberMaps)
            binding.textviewFileChosen.text = "${membersFromExcel.size} members loaded from file."
            binding.inputNoMembers.setText(membersFromExcel.size.toString())
            Toast.makeText(context, "${membersFromExcel.size} members successfully loaded.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error reading Excel file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun convertMapToMemberRequest(memberMaps: List<Map<String, String>>): List<MemberRequest> {
        return memberMaps.mapNotNull { map ->
            try {
                MemberRequest(
                    otherName = map["other_name"]!!, lastName = map["last_name"]!!,
                    gender = map["gender"]!!, dateOfBirth = formatDateForAPI(map["date_of_birth"]),
                    email = map["email"]!!, phoneNumber = map["phone_number"]!!.toLong(),
                    idNumber = map["id_number"]!!.toLong(),
                    productInvolved = map["product_involved"]!!,
                    hectorageRegisteredUnderCig = map["hectorage_registered_under_cig"]!!
                )
            } catch (e: Exception) {
                Log.e(TAG, "Skipping invalid member row from Excel: $map", e)
                null
            }
        }
    }

    private fun submitCigDataToViewModel() {
        if (!validateInputs()) {
            Toast.makeText(context, "Please fix all errors before saving.", Toast.LENGTH_LONG).show()
            return
        }

        val cigData = CIG(
            cigName = binding.cigName.text.toString(),
            hub = binding.inputHub.text.toString(),
            numberOfMembers = binding.inputNoMembers.text.toString().toInt(),
            dateEstablished = (binding.cigYearEst.tag as? String) ?: "",
            constitution = if (binding.radioGroupConstitution.checkedRadioButtonId == R.id.radio_constitution_yes) constitutionUrl else "No",
            registration = if (binding.radioGroupRegistration.checkedRadioButtonId == R.id.radio_registration_yes) registrationUrl else "No",
            certificate = certificateUrl ?: "No",
            membershipRegister = "Membership details from uploaded file.",
            electionsHeld = if (binding.radioGroupElectionsHeld.checkedRadioButtonId == R.id.radio_election_held_yes) electionsHeldUrl else "No",
            dateOfLastElections = (binding.cigLastElectionDate.tag as? String) ?: "",
            meetingVenue = binding.cigMeetingVenue.text.toString(),
            frequency = binding.inputCigFrequency.text.toString(),
            scheduledMeetingDay = binding.inputScheduledMeetingDay.text.toString(),
            scheduledMeetingTime = binding.inputScheduledMeetingTime.text.toString(),
            userId = null,
            membersJson = null
        )

        viewModel.submitCIGData(cigData, membersFromExcel)
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
            val displayFormat = SimpleDateFormat("d MMMM, yyyy", Locale.US)
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            editText.setText(displayFormat.format(selectedDate.time))
            editText.tag = apiFormat.format(selectedDate.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, hour, minute ->
            val time = String.format(Locale.US, "%02d:%02d:00", hour, minute)
            editText.setText(time)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun formatDateForAPI(excelDate: String?): String {
        if (excelDate.isNullOrBlank()) return ""
        val possibleFormats = listOf(SimpleDateFormat("M/d/yy", Locale.US), SimpleDateFormat("MM/dd/yyyy", Locale.US))
        val targetFormat = SimpleDateFormat("yyyy-MM-dd'T'00:00:00'", Locale.US)
        for (format in possibleFormats) {
            try {
                return format.parse(excelDate)?.let { targetFormat.format(it) } ?: excelDate
            } catch (e: ParseException) { /* try next format */ }
        }
        return excelDate
    }

    private fun validateInputs(): Boolean {
        // Implement comprehensive validation here...
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
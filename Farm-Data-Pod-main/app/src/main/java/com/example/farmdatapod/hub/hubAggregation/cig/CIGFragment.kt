package com.example.farmdatapod.hub.hubAggregation.cig

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.Member
import com.example.farmdatapod.R
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIG
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIGRepository
import com.example.farmdatapod.databinding.FragmentCigBinding
import com.example.farmdatapod.hub.hubRegistration.data.HubRepository
import com.example.farmdatapod.models.CIGRegistrationItem
import com.example.farmdatapod.models.UploadResponse
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.utils.NetworkUtils
import com.google.gson.Gson
import jxl.Sheet
import jxl.Workbook
import jxl.read.biff.BiffException
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CIGFragment : Fragment() {

    private var _binding: FragmentCigBinding? = null
    private val binding get() = _binding!!
    private lateinit var radioGroupElectionsHeld: RadioGroup
    private lateinit var uploadElectionsHeld: ImageView
    private lateinit var uploadElectionsHeldText: TextView
    private lateinit var radioGroupConstitution: RadioGroup
    private lateinit var uploadConstitution: ImageView
    private lateinit var uploadConstitutionText: TextView
    private lateinit var radioGroupRegistration: RadioGroup
    private lateinit var uploadRegistration: ImageView
    private lateinit var uploadRegistrationText: TextView
    private lateinit var dbHandler: DBHandler
    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private var currentUploadType: String? = null
    private var constitutionUrl: String? = null
    private var registrationUrl: String? = null
    private var electionsHeldUrl: String? = null
    private var membersList: List<Map<String, String>> = listOf()
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private lateinit var cigRepository: CIGRepository


    companion object {
        private const val REQUEST_CODE_UPLOAD_EXCEL = 1
        private const val PERMISSION_REQUEST_CODE = 2
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cigRepository = CIGRepository(requireContext())
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCigBinding.inflate(inflater, container, false)
        dbHandler = DBHandler(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize your views
        radioGroupElectionsHeld = view.findViewById(R.id.radio_group_elections_held)
        uploadElectionsHeld = view.findViewById(R.id.upload_elections_held)
        uploadElectionsHeldText = view.findViewById(R.id.upload_elections_held_text)
        radioGroupConstitution = view.findViewById(R.id.radio_group_constitution)
        uploadConstitution = view.findViewById(R.id.upload_constitution)
        uploadConstitutionText = view.findViewById(R.id.upload_constitution_text)
        radioGroupRegistration = view.findViewById(R.id.radio_group_registration)
        uploadRegistration = view.findViewById(R.id.upload_registration)
        uploadRegistrationText = view.findViewById(R.id.upload_registration_text)

        // Observe network connectivity changes
        binding.buttonSave.setOnClickListener {
            Log.d("CIGFragment", "Save button clicked")
            if (validateForm()) {
                saveCIG()
            } else {
                Log.d("CIGFragment", "Form validation failed")
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonDownloadMembershipFormat.setOnClickListener {

            downloadMembershipFormat()

        }

        binding.buttonMembershipFile.setOnClickListener {
            uploadExcelFile()
        }

        // Set up ActivityResultLauncher to handle image selection
        selectImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data
                    selectedImageUri = intent?.data
                    selectedImageUri?.let { uri ->
                        uploadImage(requireContext(), uri)
                    }
                }
            }

        // Log available assets
        logAvailableAssets()
        setupSpinners()
        setupRadioGroupListeners()
        setupDatePicker()
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        selectImageLauncher.launch(intent)
    }

    private fun setupRadioGroupListeners() {
        radioGroupElectionsHeld.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radio_election_held_yes -> {
                    uploadElectionsHeld.visibility = View.VISIBLE
                    uploadElectionsHeldText.visibility = View.VISIBLE
                    uploadElectionsHeld.setOnClickListener {
                        currentUploadType = "elections"
                        selectImage()
                    }
                }

                R.id.radio_elections_held_no -> {
                    uploadElectionsHeld.visibility = View.GONE
                    uploadElectionsHeldText.visibility = View.GONE
                }

                else -> {
                    Log.d("CIGFragment", "Unexpected checkedId: $checkedId")
                }
            }
        }

        radioGroupConstitution.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radio_constitution_yes -> {
                    uploadConstitution.visibility = View.VISIBLE
                    uploadConstitutionText.visibility = View.VISIBLE
                    uploadConstitution.setOnClickListener {
                        currentUploadType = "constitution"
                        selectImage()
                    }
                }

                R.id.radio_constitution_no -> {
                    uploadConstitution.visibility = View.GONE
                    uploadConstitutionText.visibility = View.GONE
                }

                else -> {
                    Log.d("CIGFragment", "Unexpected checkedId: $checkedId")
                }
            }
        }

        radioGroupRegistration.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radio_registration_yes -> {
                    uploadRegistration.visibility = View.VISIBLE
                    uploadRegistrationText.visibility = View.VISIBLE
                    uploadRegistration.setOnClickListener {
                        currentUploadType = "registration"
                        selectImage()
                    }
                }

                R.id.radio_registration_no -> {
                    uploadRegistration.visibility = View.GONE
                    uploadRegistrationText.visibility = View.GONE
                }

                else -> {
                    Log.d("CIGFragment", "Unexpected checkedId: $checkedId")
                }
            }
        }
    }

    // Test with phone debugging
//    private fun selectImage() {
//        val intent = Intent(Intent.ACTION_PICK).apply {
//            type = "image/*"
//        }
//        selectImageLauncher.launch(intent)
//    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val file = File(context.cacheDir, "temp_image")

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun uploadImage(context: Context, uri: Uri) {
        val file = getFileFromUri(context, uri)
        if (file != null) {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            // Show "Uploading Please Wait" message
            when (currentUploadType) {
                "elections" -> uploadElectionsHeldText.text = "Uploading Please Wait"
                "constitution" -> uploadConstitutionText.text = "Uploading Please Wait"
                "registration" -> uploadRegistrationText.text = "Uploading Please Wait"
            }

            val apiService = RestClient.getApiService(context)
            apiService.uploadToCloudinary(body).enqueue(object : Callback<UploadResponse> {
                override fun onResponse(
                    call: Call<UploadResponse>,
                    response: Response<UploadResponse>
                ) {
                    val secureUrl = response.body()?.secureUrl

                    // Update URL based on currentUploadType
                    when (currentUploadType) {
                        "elections" -> {
                            electionsHeldUrl = secureUrl
                            uploadElectionsHeldText.text = "File Uploaded"
                        }

                        "constitution" -> {
                            constitutionUrl = secureUrl
                            uploadConstitutionText.text = "File Uploaded"
                        }

                        "registration" -> {
                            registrationUrl = secureUrl
                            uploadRegistrationText.text = "File Uploaded"
                        }
                    }

                    if (response.isSuccessful) {
                        Log.d("Upload", "Image uploaded successfully: $secureUrl")
                    } else {
                        Log.e("Upload", "Upload failed: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    // Handle failure
                    when (currentUploadType) {
                        "elections" -> uploadElectionsHeldText.text = "Upload Failed"
                        "constitution" -> uploadConstitutionText.text = "Upload Failed"
                        "registration" -> uploadRegistrationText.text = "Upload Failed"
                    }

                    Log.e("Upload", "Upload error: ${t.message}")
                }
            })
        } else {
            Log.e("Upload", "Failed to convert URI to file")
        }
    }

    private fun setupSpinners() {
        val hubRepository = HubRepository(requireContext())

        lifecycleScope.launch {
            hubRepository.getAllHubs().collect { hubs ->
                // Extract hub names from the Hub objects
                val hubNames = hubs.map { it.hubName }

                // Create and set the adapter
                val hubAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,  // Changed this layout
                    hubNames
                )

                // Use setAdapter instead of adapter property
                binding.inputHub.setAdapter(hubAdapter)
            }
        }


        // Setting up the frequency spinner
        val frequencyOptions = listOf("Select frequency", "Weekly", "Bi-Weekly", "Monthly")
        val frequencyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,  // Changed layout to dropdown
            frequencyOptions
        )

// Use setAdapter instead of adapter property
        binding.inputCigFrequency.setAdapter(frequencyAdapter)
    }

    private fun setupDatePicker() {
        binding.cigYearEst.setOnClickListener {
            showDatePickerDialog(binding.cigYearEst, "yyyy-MM-dd'T'HH:mm:ss")
        }

        binding.cigLastElectionDate.setOnClickListener {
            showDatePickerDialog(binding.cigLastElectionDate, "yyyy-MM-dd'T'HH:mm:ss")
        }

        binding.inputScheduledMeetingTime.setOnClickListener {
            showTimePickerDialog(binding.inputScheduledMeetingTime, "HH:mm:ss")
        }
    }

    private fun showDatePickerDialog(editText: EditText, format: String) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                }
                val apiDate = apiDateFormat.format(selectedDate.time)
                val displayDate = displayDateFormat.format(selectedDate.time)
                editText.setText(displayDate)
                editText.tag = apiDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePickerDialog(editText: EditText, format: String) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                val time = SimpleDateFormat(format, Locale.getDefault()).format(selectedTime.time)
                editText.setText(time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // Use 24-hour format
        ).show()
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (binding.cigName.text.isNullOrEmpty()) {
            binding.cigName.error = "CIG Name is required"
            isValid = false
        }

        if (binding.cigYearEst.text.isNullOrEmpty()) {
            binding.cigYearEst.error = "Year of Establishment is required"
            isValid = false
        }

        if (binding.inputNoMembers.text.isNullOrEmpty()) {
            binding.inputNoMembers.error = "Number of Members is required"
            isValid = false
        }

        if (binding.radioGroupElectionsHeld.checkedRadioButtonId == -1) {
            Toast.makeText(context, "Please select if elections were held", Toast.LENGTH_SHORT)
                .show()
            isValid = false
        }

        if (binding.radioGroupConstitution.checkedRadioButtonId == -1) {
            Toast.makeText(context, "Please select if there is a constitution", Toast.LENGTH_SHORT)
                .show()
            isValid = false
        }

        if (binding.radioGroupRegistration.checkedRadioButtonId == -1) {
            Toast.makeText(context, "Please select if the group is registered", Toast.LENGTH_SHORT)
                .show()
            isValid = false
        }

        if (binding.cigLastElectionDate.text.isNullOrEmpty()) {
            binding.cigLastElectionDate.error = "Last Election Date is required"
            isValid = false
        }

        if (binding.cigMeetingVenue.text.isNullOrEmpty()) {
            binding.cigMeetingVenue.error = "Meeting Venue is required"
            isValid = false
        }

        if (binding.inputScheduledMeetingDay.text.isNullOrEmpty()) {
            binding.inputScheduledMeetingDay.error = "Scheduled Meeting Day is required"
            isValid = false
        }

        if (binding.inputScheduledMeetingTime.text.isNullOrEmpty()) {
            binding.inputScheduledMeetingTime.error = "Scheduled Meeting Time is required"
            isValid = false
        }

        return isValid
    }

    private fun createCIGRegistration(membersList: List<Map<String, String>>): CIGRegistrationItem {
        return CIGRegistrationItem(
            cig_name = binding.cigName.text.toString(),
            constitution = constitutionUrl ?: "No",
            date_established = binding.cigYearEst.tag as? String ?: binding.cigYearEst.text.toString(),
            date_of_last_elections = binding.cigLastElectionDate.tag as? String ?: binding.cigLastElectionDate.text.toString(),
            elections_held = electionsHeldUrl ?: "No",
            frequency = binding.inputCigFrequency.text.toString(),  // Changed from selectedItem to text
            hub = binding.inputHub.text.toString(),  // Changed from selectedItem to text
            id = 0,
            meeting_venue = binding.cigMeetingVenue.text.toString(),
            members = membersList,
            no_of_members = membersList.size,
            registration = registrationUrl ?: "No",
            scheduled_meeting_day = binding.inputScheduledMeetingDay.text.toString(),
            scheduled_meeting_time = binding.inputScheduledMeetingTime.text.toString(),
            user_id = ""
        )
    }

    private fun saveCIG() {
        lifecycleScope.launch {
            try {
                val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
                Log.d(TAG, "Network status: ${if (isOnline) "Online" else "Offline"}")

                val cig = createLocalCIG()

                val result = cigRepository.saveCIG(cig, isOnline)
                result.fold(
                    onSuccess = {
                        val message = if (isOnline) {
                            "CIG registered successfully"
                        } else {
                            "Saved offline, will sync later"
                        }
                        Log.d(TAG, "Save successful: $message")
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        // Optional: Clear form or navigate back
                        clearForm()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error saving CIG", error)
                        Toast.makeText(
                            context,
                            "Error saving CIG: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in saveCIG", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createLocalCIG(): CIG {
        val member = membersList.firstOrNull()
        return CIG(
            id = 0,
            serverId = null,
            cigName = binding.cigName.text.toString(),
            hub = binding.inputHub.text.toString(),  // Changed from selectedItem to text
            numberOfMembers = binding.inputNoMembers.text.toString().toIntOrNull() ?: 0,
            dateEstablished = binding.cigYearEst.tag as? String ?: binding.cigYearEst.text.toString(),
            constitution = constitutionUrl ?: "No",
            registration = registrationUrl ?: "No",
            electionsHeld = electionsHeldUrl ?: "No",
            dateOfLastElections = binding.cigLastElectionDate.tag as? String
                ?: binding.cigLastElectionDate.text.toString(),
            meetingVenue = binding.cigMeetingVenue.text.toString(),
            frequency = binding.inputCigFrequency.text.toString(),  // Changed from selectedItem to text
            scheduledMeetingDay = binding.inputScheduledMeetingDay.text.toString(),
            scheduledMeetingTime = binding.inputScheduledMeetingTime.text.toString(),
            userId = "",
            syncStatus = false,
            // Member fields
            memberOtherName = member?.get("other_name") ?: "",
            memberLastName = member?.get("last_name") ?: "",
            memberGender = member?.get("gender") ?: "",
            memberDateOfBirth = member?.get("date_of_birth") ?: "",
            memberEmail = member?.get("email") ?: "",
            memberPhoneNumber = member?.get("phone_number")?.toLongOrNull() ?: 0L,
            memberIdNumber = member?.get("id_number")?.toIntOrNull() ?: 0,
            productInvolved = member?.get("product_involved") ?: "",
            hectorageRegisteredUnderCig = member?.get("hectorage_registered_under_cig") ?: "",
            cigId = null
        )
    }

    private fun clearForm() {
        binding.apply {
            cigName.text?.clear()
            cigYearEst.text?.clear()
            inputNoMembers.text?.clear()
            cigLastElectionDate.text?.clear()
            cigMeetingVenue.text?.clear()
            inputScheduledMeetingDay.text?.clear()
            inputScheduledMeetingTime.text?.clear()
            // Reset spinners
            inputHub.setSelection(0)
            inputCigFrequency.setSelection(0)
            // Reset radio buttons
            radioGroupElectionsHeld.clearCheck()
            radioGroupConstitution.clearCheck()
            radioGroupRegistration.clearCheck()
            // Clear uploaded files
            constitutionUrl = null
            registrationUrl = null
            electionsHeldUrl = null
            membersList = listOf()
        }
    }

    private fun uploadExcelFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/vnd.ms-excel"
        startActivityForResult(intent, REQUEST_CODE_UPLOAD_EXCEL)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UPLOAD_EXCEL && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            uri?.let {
                processExcelFile(it)
            }
        }
    }

    private fun processExcelFile(uri: Uri) {
        context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
            parseExcelToMembers(inputStream)
        } ?: Toast.makeText(context, "Unable to open file", Toast.LENGTH_SHORT).show()
    }


    private fun parseExcelToMembers(inputStream: InputStream): List<Map<String, String>> {
        val membersArray = mutableListOf<Map<String, String>>()

        try {
            val workbook: Workbook = Workbook.getWorkbook(inputStream)
            val sheet: Sheet = workbook.getSheet(0) // Get the first sheet
            val rows = sheet.rows

            for (i in 1 until rows) { // Start from 1 to skip header row
                val row = sheet.getRow(i)
                if (row.size >= 10) { // Ensure there are enough columns
                    val member = mutableMapOf<String, String>()
                    member["other_name"] = row[1].contents
                    member["last_name"] = row[2].contents
                    member["gender"] = row[3].contents

                    // Format the date_of_birth to the desired structure
                    val dateOfBirth = row[4].contents
                    member["date_of_birth"] =
                        if (dateOfBirth.isNotEmpty()) formatDateString(dateOfBirth) else ""

                    member["email"] = row[5].contents
                    member["phone_number"] = row[6].contents
                    member["id_number"] = row[7].contents
                    member["product_involved"] = row[8].contents
                    member["hectorage_registered_under_cig"] = row[9].contents

                    membersArray.add(member)
                }
            }
        } catch (e: BiffException) {
            Log.e("CIGFragment", "Error parsing Excel file: ${e.message}", e)
        } catch (e: IOException) {
            Log.e("CIGFragment", "Error reading Excel file: ${e.message}", e)
        }

        return membersArray
    }

    private fun formatDateString(dateString: String): String {
        // Implement your date formatting logic here
        return dateString
    }

    private fun downloadMembershipFormat() {
        val assetManager = requireContext().assets
        val filename = "Members Register.xls"
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            Log.d("CIGFragment", "Opening asset file: $filename")
            inputStream = assetManager.open(filename)
            Log.d("CIGFragment", "Asset file opened successfully")

            // Get the standard Downloads directory
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            Log.d("CIGFragment", "Downloads directory: $downloadsDir")

            // Create a file in the Downloads directory
            val outFile = File(downloadsDir, filename)
            Log.d("CIGFragment", "Output file path: ${outFile.absolutePath}")

            outputStream = FileOutputStream(outFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            Log.d("CIGFragment", "File written successfully")

            // Notify the user that the file has been downloaded successfully
            Toast.makeText(
                requireContext(),
                "File downloaded to Downloads directory",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: IOException) {
            Log.e("CIGFragment", "Error downloading file: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "Failed to download file: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            try {
                inputStream?.close()
                Log.d("CIGFragment", "InputStream closed")
                outputStream?.close()
                Log.d("CIGFragment", "OutputStream closed")
            } catch (e: IOException) {
                Log.e("CIGFragment", "Error closing streams: ${e.message}", e)
            }
        }
    }

    private fun logAvailableAssets() {
        val assetManager = requireContext().assets
        val files = assetManager.list("")
        files?.forEach { file ->
            Log.d("CIGFragment", "Asset: $file")
        }
    }

    private fun logCigRegistrationJson(cigRegistration: CIGRegistrationItem) {
        val gson = Gson()
        val json = gson.toJson(cigRegistration)
        Log.d("CIGRegistrationJson", json)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadMembershipFormat()
            } else {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
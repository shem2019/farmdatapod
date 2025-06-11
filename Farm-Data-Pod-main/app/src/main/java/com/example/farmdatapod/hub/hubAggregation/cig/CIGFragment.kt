package com.example.farmdatapod.hub.hubAggregation.cig

import android.app.Activity
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
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentCigBinding
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIG
import com.example.farmdatapod.hub.hubRegistration.data.HubRepository
import com.example.farmdatapod.models.MemberRequest
import com.example.farmdatapod.models.UploadResponse
import com.example.farmdatapod.network.RestClient
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
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

    private var membersFromFile: List<MemberRequest> = emptyList()
    private var constitutionUrl: String? = null
    private var registrationUrl: String? = null
    private var certificateUrl: String? = null
    private var electionsHeldUrl: String? = null
    private var currentUploadType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    when (currentUploadType) {
                        "excel" -> processMemberFile(uri)
                        "constitution", "registration", "elections", "certificate" -> uploadImageToCloudinary(uri)
                    }
                }
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
        lifecycleScope.launch {
            hubRepository.getAllHubs().collect { hubs ->
                val hubNames = hubs.map { it.hubName }
                val hubAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, hubNames)
                binding.inputHub.setAdapter(hubAdapter)
            }
        }
        binding.inputCigFrequency.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("Weekly", "Bi-Weekly", "Monthly")))
        binding.spinnerContributionFrequency.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("Daily", "Weekly", "Monthly", "Annually")))
        binding.cigYearEst.setOnClickListener { showDatePickerDialog(it as EditText, "Date Established") }
        binding.cigLastElectionDate.setOnClickListener { showDatePickerDialog(it as EditText, "Date of Last Election") }
        binding.inputScheduledMeetingTime.setOnClickListener { showTimePickerDialog(it as EditText) }
        setupFileUploadListeners()
        binding.buttonMembershipFile.setOnClickListener {
            currentUploadType = "excel"
            openFilePicker("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }
        binding.buttonSave.setOnClickListener { submitCigDataToViewModel() }
    }

    private fun observeViewModel() {
        viewModel.registrationState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is RegistrationState.Loading
            binding.buttonSave.isEnabled = state !is RegistrationState.Loading
            when (state) {
                is RegistrationState.Success -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                is RegistrationState.Error -> Toast.makeText(requireContext(), "Error: ${state.errorMessage}", Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    private fun setupFileUploadListeners() {
        binding.radioGroupConstitution.setOnCheckedChangeListener { _, id -> binding.uploadConstitution.isVisible = (id == R.id.radio_constitution_yes) }
        binding.uploadConstitution.setOnClickListener {
            currentUploadType = "constitution"
            openFilePicker("image/*")
        }
        binding.radioGroupRegistration.setOnCheckedChangeListener { _, id -> binding.uploadRegistration.isVisible = (id == R.id.radio_registration_yes) }
        binding.uploadRegistration.setOnClickListener {
            currentUploadType = "registration"
            openFilePicker("image/*")
        }
        binding.radioGroupElectionsHeld.setOnCheckedChangeListener { _, id -> binding.uploadElectionsHeld.isVisible = (id == R.id.radio_election_held_yes) }
        binding.uploadElectionsHeld.setOnClickListener {
            currentUploadType = "elections"
            openFilePicker("image/*")
        }
        binding.radioGroupCertificate.setOnCheckedChangeListener { _, id -> binding.uploadCertificate.isVisible = (id == R.id.radio_certificate_yes) }
        binding.uploadCertificate.setOnClickListener {
            currentUploadType = "certificate"
            openFilePicker("image/*")
        }
    }

    private fun openFilePicker(vararg mimeTypes: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (mimeTypes.isEmpty()) "*/*" else mimeTypes.joinToString(separator = "|")
            if (mimeTypes.size > 1) {
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
        }
        filePickerLauncher.launch(intent)
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val destinationFile = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}")
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream -> inputStream.copyTo(outputStream) }
            }
            destinationFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create temp file from URI", e)
            null
        }
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        val file = getFileFromUri(uri) ?: return
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val textViewToUpdate: TextView? = when (currentUploadType) {
            "constitution" -> binding.uploadConstitutionText
            "registration" -> binding.uploadRegistrationText
            "elections" -> binding.uploadElectionsHeldText
            "certificate" -> binding.uploadCertificateText
            else -> null
        }
        textViewToUpdate?.text = "Uploading..."
        RestClient.getApiService(requireContext()).uploadToCloudinary(body).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                file.delete()
                if (response.isSuccessful && response.body()?.secureUrl != null) {
                    when (currentUploadType) {
                        "constitution" -> constitutionUrl = response.body()!!.secureUrl
                        "registration" -> registrationUrl = response.body()!!.secureUrl
                        "elections" -> electionsHeldUrl = response.body()!!.secureUrl
                        "certificate" -> certificateUrl = response.body()!!.secureUrl
                    }
                    textViewToUpdate?.text = "File Uploaded"
                } else {
                    textViewToUpdate?.text = "Upload Failed"
                }
            }
            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                file.delete()
                textViewToUpdate?.text = "Upload Failed"
            }
        })
    }

    // FINAL CORRECTED VERSION of column mapping.
    private fun processMemberFile(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                val dataFormatter = DataFormatter()
                val memberMaps = (1..sheet.lastRowNum).mapNotNull { i ->
                    val row: Row? = sheet.getRow(i)
                    // Use a key column like other_name (Column D) to validate the row
                    if (row == null || dataFormatter.formatCellValue(row.getCell(3)).isBlank()) {
                        null
                    } else {
                        mapOf(
                            "membership_number"   to dataFormatter.formatCellValue(row.getCell(1)),  // Column B
                            "membership_category" to dataFormatter.formatCellValue(row.getCell(2)),  // Column C
                            "other_name"          to dataFormatter.formatCellValue(row.getCell(3)),  // Column D
                            "last_name"           to dataFormatter.formatCellValue(row.getCell(4)),  // Column E
                            "gender"              to dataFormatter.formatCellValue(row.getCell(5)),  // Column F
                            "date_of_birth"       to dataFormatter.formatCellValue(row.getCell(6)),  // Column G
                            "email"               to dataFormatter.formatCellValue(row.getCell(7)),  // Column H
                            "phone_number"        to dataFormatter.formatCellValue(row.getCell(8)),  // Column I
                            "id_number"           to dataFormatter.formatCellValue(row.getCell(9)),  // Column J
                            "registration_date"   to dataFormatter.formatCellValue(row.getCell(10)), // Column K
                            "share_capital"       to dataFormatter.formatCellValue(row.getCell(11)), // Column L
                            "contribution"        to dataFormatter.formatCellValue(row.getCell(12)), // Column M
                            "membership_fee"      to dataFormatter.formatCellValue(row.getCell(13)), // Column N
                            "voting_rights"       to dataFormatter.formatCellValue(row.getCell(14)), // Column O
                            "position_held"       to dataFormatter.formatCellValue(row.getCell(15)), // Column P
                            "product_or_service"  to dataFormatter.formatCellValue(row.getCell(16))  // Column Q
                        )
                    }
                }
                membersFromFile = convertMapToMemberRequest(memberMaps)
                binding.textviewFileChosen.text = "${membersFromFile.size} members loaded from file."
                binding.inputNoMembers.setText(membersFromFile.size.toString())
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error processing Excel file. Ensure it's a valid .xls or .xlsx file and columns are in the correct order.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error processing Excel file", e)
        }
    }

    private fun convertMapToMemberRequest(memberMaps: List<Map<String, String?>>): List<MemberRequest> {
        return memberMaps.mapNotNull { map ->
            try {
                MemberRequest(
                    otherName = map.getValue("other_name")!!,
                    lastName = map.getValue("last_name")!!,
                    gender = map.getValue("gender")!!,
                    dateOfBirth = formatDateForAPI(map["date_of_birth"]),
                    membershipNumber = map.getValue("membership_number")!!,
                    registrationDate = formatDateForAPI(map["registration_date"]),
                    shareCapital = map["share_capital"]?.toDoubleOrNull() ?: 0.0,
                    contribution = map["contribution"]?.toDoubleOrNull() ?: 0.0,
                    membershipFee = map["membership_fee"]?.toDoubleOrNull() ?: 0.0,
                    phoneNumber = map["phone_number"]?.filter { it.isDigit() } ?: "",
                    email = map.getValue("email")!!,
                    votingRights = map["voting_rights"]?.equals("yes", ignoreCase = true) ?: false,
                    positionHeld = map.getValue("position_held")!!,
                    membershipCategory = map.getValue("membership_category")!!,
                    productOrService = map.getValue("product_or_service")!!
                )
            } catch (e: Exception) {
                Log.e(TAG, "Skipping invalid member row from Excel: $map", e)
                null
            }
        }
    }

    private fun submitCigDataToViewModel() {
        if (!validateInputs()) { return }

        val cigData = CIG(
            id = 0, serverId = null,
            cigName = binding.cigName.text.toString(),
            hub = binding.inputHub.text.toString(),
            numberOfMembers = binding.inputNoMembers.text.toString().toIntOrNull() ?: 0,
            dateEstablished = binding.cigYearEst.tag?.toString() ?: "",
            constitution = if (binding.radioGroupConstitution.checkedRadioButtonId == R.id.radio_constitution_yes) constitutionUrl else "No",
            registration = if (binding.radioGroupRegistration.checkedRadioButtonId == R.id.radio_registration_yes) registrationUrl else "No",
            certificate = if (binding.radioGroupCertificate.checkedRadioButtonId == R.id.radio_certificate_yes) certificateUrl else "No",
            membershipRegister = binding.editTextMembershipRegisterDescription.text.toString(),
            electionsHeld = if (binding.radioGroupElectionsHeld.checkedRadioButtonId == R.id.radio_election_held_yes) electionsHeldUrl else "No",
            dateOfLastElections = binding.cigLastElectionDate.tag?.toString() ?: "",
            meetingVenue = binding.cigMeetingVenue.text.toString(),
            meetingFrequency = binding.inputCigFrequency.text.toString(),
            scheduledMeetingDay = binding.inputScheduledMeetingDay.text.toString(),
            scheduledMeetingTime = binding.inputScheduledMeetingTime.text.toString(),
            membershipContributionAmount = binding.editTextContributionAmount.text.toString(),
            membershipContributionFrequency = binding.spinnerContributionFrequency.text.toString(),
            userId = null, membersJson = null, syncStatus = false
        )
        viewModel.submitCIGData(cigData, membersFromFile)
    }

    private fun showDatePickerDialog(editText: EditText, title: String) {
        val picker = MaterialDatePicker.Builder.datePicker().setTitleText(title).build()
        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
            val displayFormat = SimpleDateFormat("d MMMM, yyyy", Locale.US)
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            editText.setText(displayFormat.format(calendar.time))
            editText.tag = apiFormat.format(calendar.time)
        }
        picker.show(childFragmentManager, "DATE_PICKER_${editText.id}")
    }

    private fun showTimePickerDialog(editText: EditText) {
        val picker = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setTitleText("Select Meeting Time").build()
        picker.addOnPositiveButtonClickListener {
            val time = String.format(Locale.US, "%02d:%02d:00", picker.hour, picker.minute)
            editText.setText(time)
        }
        picker.show(childFragmentManager, "TIME_PICKER_${editText.id}")
    }

    private fun formatDateForAPI(dateString: String?): String {
        if (dateString.isNullOrBlank()) return ""
        val possibleFormats = listOf(
            SimpleDateFormat("M/d/yy", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("dd-MMM-yyyy", Locale.US)
        )
        val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (format in possibleFormats) {
            try {
                format.parse(dateString)?.let {
                    return "${dateOnlyFormat.format(it)}T00:00:00"
                }
            } catch (e: ParseException) { /* Ignore */ }
        }
        Log.w(TAG, "Could not parse date: $dateString with any known format.")
        return dateString
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        if (binding.cigName.text.isNullOrBlank()) {
            binding.cigName.error = "CIG Name is required"; isValid = false
        }
        if (binding.inputHub.text.isNullOrBlank()) {
            binding.inputHub.error = "Hub is required"; isValid = false
        }
        if (membersFromFile.isEmpty()) {
            Toast.makeText(requireContext(), "A valid membership file must be uploaded.", Toast.LENGTH_LONG).show()
            isValid = false
        }
        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
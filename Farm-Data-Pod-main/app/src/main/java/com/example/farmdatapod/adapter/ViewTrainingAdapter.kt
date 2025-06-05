package com.example.farmdatapod.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.Attendance
import com.example.farmdatapod.R
import com.example.farmdatapod.Training
import com.example.farmdatapod.models.UploadResponse
import com.example.farmdatapod.network.ApiService
import com.example.farmdatapod.network.RestClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ViewTrainingAdapter(
    private val trainings: List<Training>,
    private val onEllipsisClick: (Training, String) -> Unit,
    private val context: Context,
    private val selectImageLauncher: ActivityResultLauncher<Intent>
) : RecyclerView.Adapter<ViewTrainingAdapter.TrainingViewHolder>() {

    private val apiService: ApiService = RestClient.getApiService(context)
    private var selectedTraining: Training? = null
    private var uploadedImageUrl: String? = null

    inner class TrainingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseName: TextView = itemView.findViewById(R.id.textViewCourseName)
        val date: TextView = itemView.findViewById(R.id.textViewDate)
        val status: TextView = itemView.findViewById(R.id.textViewStatus)
        val ellipsis: ImageView = itemView.findViewById(R.id.imageViewEllipsis)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training, parent, false)
        return TrainingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        val training = trainings[position]
        holder.courseName.text = training.course_name
        holder.date.text = training.date_of_training

        // Calculate status based on date range and update the tag appearance
        val (statusText, statusColor) = getStatus(training.date_of_training)
        holder.status.text = statusText
        updateStatusTagAppearance(holder.status, statusColor)

        // Set ellipsis click listener
        holder.ellipsis.setOnClickListener {
            showPopupMenu(holder.itemView.context, training, holder.ellipsis, statusText)
        }
    }

    override fun getItemCount(): Int = trainings.size

    private fun getStatus(dateRange: String?): Pair<String, Int> {
        val currentDate = LocalDate.now()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return try {
            // Split date range
            val dates = dateRange?.split(" - ") ?: emptyList()
            if (dates.size == 2) {
                val startDate = LocalDate.parse(dates[0], dateFormat)
                val endDate = LocalDate.parse(dates[1], dateFormat)

                // Determine status and color
                when {
                    currentDate.isBefore(startDate) -> "Upcoming" to Color.parseColor("#9C27B0")
                    currentDate.isAfter(endDate) -> "Complete" to Color.parseColor("#4CAF50")
                    else -> "Ongoing" to Color.parseColor("#2196F3")
                }
            } else {
                "Invalid Date Range" to Color.parseColor("#B71C1C")
            }
        } catch (e: DateTimeParseException) {
            "Invalid Date Range" to Color.parseColor("#B71C1C")
        }
    }

    private fun updateStatusTagAppearance(statusTextView: TextView, borderColor: Int) {
        // Create the drawable for the border
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 75f
            setStroke(4, borderColor)
        }
        statusTextView.background = drawable
        statusTextView.setTextColor(borderColor)
    }

    private fun showPopupMenu(context: Context, training: Training, view: View, status: String) {
        val popupMenu = PopupMenu(view.context, view)
        val inflater = popupMenu.menuInflater

        // Inflate menu based on the training status
        inflater.inflate(R.menu.training_options_menu, popupMenu.menu)

        // If the status is "Ongoing", add the upload attendance option
        if (status == "Ongoing") {
            inflater.inflate(R.menu.ongoing_training_options_menu, popupMenu.menu)
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_view_training -> {
                    onEllipsisClick(training, "Update Attendance for Ongoing Trainings")
                    showTrainingDialog(context, training)
                    true
                }
                R.id.menu_update_attendance -> {
                    // Handle image selection for attendance upload
                    selectedTraining = training
                    selectImage()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showTrainingDialog(context: Context, training: Training) {
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Training Details")
        dialogBuilder.setMessage(""" 
            Course Name: ${training.course_name}
            Trainer Name: ${training.trainer_name}
            Buying Center: ${training.buying_center}
            Description: ${training.course_description}
            Date: ${training.date_of_training}
            Venue: ${training.venue}
            Status: ${getStatus(training.date_of_training).first}
        """.trimIndent())
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        selectImageLauncher.launch(intent)
    }

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

    fun uploadImage(context: Context, uri: Uri) {
        val file = getFileFromUri(context, uri)
        if (file != null) {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            // Show "Uploading Please Wait" message
            Toast.makeText(context, "Uploading Please Wait", Toast.LENGTH_SHORT).show()

            apiService.uploadToCloudinary(body).enqueue(object : Callback<UploadResponse> {
                override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                    val secureUrl = response.body()?.secureUrl

                    if (response.isSuccessful) {
                        Log.d("Upload", "Image uploaded successfully: $secureUrl")
                        uploadedImageUrl = secureUrl
                        selectedTraining?.let {
                            postAttendance(context, it, uploadedImageUrl)
                        }
                    } else {
                        Log.e("Upload", "Upload failed: ${response.errorBody()?.string()}")
                        Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    Log.e("Upload", "Upload error: ${t.message}")
                    Toast.makeText(context, "Upload error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Log.e("Upload", "Failed to convert URI to file")
        }
    }

    private fun postAttendance(context: Context, training: Training, imageUrl: String?) {
        if (imageUrl == null) {
            Toast.makeText(context, "Image upload failed. Attendance not posted.", Toast.LENGTH_SHORT).show()
            return
        }

        val attendance = Attendance(
            user_id = "user_id",
            attendance = imageUrl,
            training_id = training.id
        )

        apiService.postAttendance(attendance).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("Attendance", "Attendance posted successfully")
                    Toast.makeText(context, "Attendance posted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("Attendance", "Post failed: ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Attendance post failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Attendance", "Post error: ${t.message}")
                Toast.makeText(context, "Attendance post error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

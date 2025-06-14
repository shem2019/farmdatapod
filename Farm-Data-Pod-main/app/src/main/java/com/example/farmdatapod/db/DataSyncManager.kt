
package com.example.farmdatapod.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.AsyncTask
import android.util.Log
import com.example.farmdatapod.Attendance
import com.example.farmdatapod.BuyingCenter
import com.example.farmdatapod.BuyingCustomer
import com.example.farmdatapod.BuyingFarmer
import com.example.farmdatapod.CIG
import com.example.farmdatapod.CustomUser
import com.example.farmdatapod.CustomerPriceDistribution
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.FarmerPriceDistribution
import com.example.farmdatapod.HQUser
import com.example.farmdatapod.Hub
import com.example.farmdatapod.HubUser
import com.example.farmdatapod.IndividualCustomer
import com.example.farmdatapod.IndividualLogistician
import com.example.farmdatapod.KeyContact
import com.example.farmdatapod.Loading
import com.example.farmdatapod.Member
import com.example.farmdatapod.Offloading
import com.example.farmdatapod.OrganisationalCustomer
import com.example.farmdatapod.OrganisationalLogistician
import com.example.farmdatapod.PaymentCustomer
import com.example.farmdatapod.PaymentFarmer
import com.example.farmdatapod.ProcessingUser
import com.example.farmdatapod.Quarantine
import com.example.farmdatapod.RuralWorker
import com.example.farmdatapod.Training
import com.example.farmdatapod.network.ApiService
import com.example.farmdatapod.utils.SharedPrefs
import com.example.farmdatapod.dbmodels.UserResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets


class DataSyncManager(
    private val apiService: ApiService,
    private val sharedPrefs: SharedPrefs,
    private val dbHandler: DBHandler,
    private val context: Context
) {

    fun fetchAndSyncAllData() {
        val db = dbHandler.writableDatabase
        var dataPostedSuccessfully = false

        // Start the transaction for data posting
        db.beginTransaction()
        try {
            // Attempt to post offline data
            postOfflineData()
            dataPostedSuccessfully = true
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DataSyncManager", "Error posting offline data", e)
        } finally {
            db.endTransaction()
        }

        // Clear tables only if data was posted successfully
        if (dataPostedSuccessfully) {
            db.beginTransaction()
            try {
                clearTables(db)
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error clearing tables", e)
            } finally {
                db.endTransaction()
            }
        }

        // Proceed with fetching and syncing other data
        fetchAndSyncUserData()
        fetchAndSyncCustomUserData()
        fetchAndSyncHQUserData()
        fetchAndSyncHubUserData()
        fetchAndSyncProcessingUserData()
        fetchAndSyncLogisticianData()
        fetchAndSyncOrganisationLogisticianData()
        fetchAndSyncCustomerData()
        fetchAndSyncOrganizationalCustomerData()

        fetchAndSyncTrainingData()
        fetchAndSyncAttendanceData()
        fetchAndSyncFarmerPriceDistributionData()
        fetchAndSyncCustomerPriceDistributionData()

        fetchAndSyncSellingData()
        fetchAndSyncQuarantineData()
        fetchAndSyncPaymentData()
        fetchAndSyncReceivePaymentData()
//        fetchAndSyncPlanJourneyData()
//        fetchAndSyncLoadingData()
//        fetchAndSyncOffLoadingData()
        fetchAndSyncRuralWorkerData()
    }

    // Method to post offline data to the server
    private fun postOfflineData() {
        postHubData()
        postBuyingCenterData()
        postCIGData()
        postHubUserData()
        postHQUserData()
        postIndividualLogisticianData()
        postOrgLogisticianData()
        postTrainingData()
        postFarmerPriceDistributionData()
        postCustomerPriceDistributionData()
        postBuyingData()
        postSellingData()
//        postPlanJourneyData()
        postLoadingData()
        postOffloadingData()

        postRuralWorkerData()

    }

    // Syncing offline hubs
    // Post all offline hub data to the server
    private fun postHubData() {
        val offlineHubs = dbHandler.offlineHubs
        val offlineKeyContacts = dbHandler.offlineKeyContacts

        // Create an instance of SharedPrefs
        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostHubError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/hubs"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (hub in offlineHubs) {
                    val hubJson = prepareHubJson(hub, offlineKeyContacts)

                    // Prepare connection for each hub
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(hubJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostHubData", "Hub data posted successfully")
                        // Optionally handle the response
                    } else {
                        Log.e("PostHubError", "Failed to post hub data. Code: $responseCode")
                        // Optionally handle the error
                    }
                }
            } catch (e: IOException) {
                Log.e("PostHubFailure", "Error posting hub data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareHubJson(hub: Hub, keyContacts: List<KeyContact>): JSONObject {
        val keyContactsForCurrentHub = keyContacts.filter { it.hub_id == hub.id }

        val keyContactsJsonArray = JSONArray()
        for (keyContact in keyContactsForCurrentHub) {
            val keyContactJson = JSONObject().apply {
                put("other_name", keyContact.other_name)
                put("last_name", keyContact.last_name)
                put("gender", keyContact.gender)
                put("role", keyContact.role)
                put("date_of_birth", (keyContact.date_of_birth?.split("T")?.get(0)) + "T00:00:00")
                put("email", keyContact.email)
                put("phone_number", keyContact.phone_number)
                put("id_number", keyContact.id_number)
            }
            keyContactsJsonArray.put(keyContactJson)
        }

        return JSONObject().apply {
            put("region", hub.region)
            put("hub_name", hub.hub_name)
            put("hub_code", hub.hub_code)
            put("address", hub.address)
            put("year_established", (hub.year_established?.split("T")?.get(0)) + "T00:00:00")
            put("ownership", hub.ownership)
            put("floor_size", hub.floor_size)
            put("facilities", hub.facilities)
            put("input_center", hub.input_center)
            put("type_of_building", hub.type_of_building)
            put("longitude", hub.longitude)
            put("latitude", hub.latitude)
            put("key_contacts", keyContactsJsonArray)
        }
    }

    // Post all offline buying center data to the server
    private fun postBuyingCenterData() {
        val offlineBuyingCenters = dbHandler.offlineBuyingCenters
        val offlineKeyContacts = dbHandler.offlineKeyContacts

        // Create an instance of SharedPrefs
        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostBuyingCenterError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/buying-centers"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (buyingCenter in offlineBuyingCenters) {
                    // Prepare JSON for the current buying center
                    val buyingCenterJson = prepareBuyingCenterJson(buyingCenter, offlineKeyContacts)

                    // Log the JSON data
                    Log.d("PostBuyingCenterData", "Posting JSON: ${buyingCenterJson.toString(4)}") // Pretty print with indentation

                    // Prepare connection for each buying center
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(buyingCenterJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostBuyingCenterData", "Buying center data posted successfully")
                        // Optionally handle the response
                    } else {
                        Log.e("PostBuyingCenterError", "Failed to post buying center data. Code: $responseCode")
                        // Optionally handle the error
                    }
                }
            } catch (e: IOException) {
                Log.e("PostBuyingCenterFailure", "Error posting buying center data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareBuyingCenterJson(buyingCenter: BuyingCenter, keyContacts: List<KeyContact>): JSONObject {
        // Create a map to hold key contacts by buying center id
        val keyContactsMap = keyContacts.groupBy { it.buying_center_id }

        // Get the key contacts for the current buying center
        val keyContactsForCurrentCenter = keyContactsMap[buyingCenter.id] ?: emptyList()

        // Prepare JSON for the current buying center
        val buyingCenterJson = JSONObject().apply {
            put("hub", buyingCenter.hub)
            put("county", buyingCenter.county)
            put("sub_county", buyingCenter.sub_county)
            put("ward", buyingCenter.ward)
            put("village", buyingCenter.village)
            put("buying_center_name", buyingCenter.buying_center_name)
            put("buying_center_code", buyingCenter.buying_center_code)
            put("address", buyingCenter.address)
            put("year_established", buyingCenter.year_established)
            put("ownership", buyingCenter.ownership)
            put("floor_size", buyingCenter.floor_size)
            put("facilities", buyingCenter.facilities)
            put("input_center", buyingCenter.input_center)
            put("type_of_building", buyingCenter.type_of_building)
            put("location", buyingCenter.location ?: "Unknown Location") // Default if null

            // Create a JSON array for key contacts
            val keyContactsJsonArray = JSONArray()
            for (keyContact in keyContactsForCurrentCenter) {
                val keyContactJson = JSONObject().apply {
                    put("other_name", keyContact.other_name)
                    put("last_name", keyContact.last_name)
                    put("gender", keyContact.gender)
                    put("role", keyContact.role)
                    put("date_of_birth", keyContact.date_of_birth)
                    put("email", keyContact.email)
                    put("phone_number", keyContact.phone_number)
                    put("id_number", keyContact.id_number)
                }
                keyContactsJsonArray.put(keyContactJson)
            }
            put("key_contacts", keyContactsJsonArray)
        }

        // Log the JSON object
        Log.d("PrepareBuyingCenterJson", "Buying Center JSON: $buyingCenterJson")

        return buyingCenterJson
    }

    // Post offline cig data
    private fun postCIGData() {
        val offlineCIGs = dbHandler.offlineCIGs
        val offlineMembers = dbHandler.offlineMembers

        // Create an instance of SharedPrefs
        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostCIGDataError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/cigs"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (cig in offlineCIGs) {
                    // Prepare JSON for the current CIG
                    val cigJson = prepareCIGJson(cig, offlineMembers)

                    // Log the JSON data
                    Log.d("PostCIGData", "Posting JSON: ${cigJson.toString(4)}") // Pretty print with indentation

                    // Prepare connection for each CIG
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(cigJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostCIGData", "CIG data posted successfully")
                        // Optionally handle the response
                    } else {
                        Log.e("PostCIGDataError", "Failed to post CIG data. Code: $responseCode")
                        // Optionally handle the error
                    }
                }
            } catch (e: IOException) {
                Log.e("PostCIGDataFailure", "Error posting CIG data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareCIGJson(cig: CIG, members: List<Member>): JSONObject {
        // Create a map to hold members by CIG id
        val membersMap = members.groupBy { it.cig_id }

        // Get the members for the current CIG
        val membersForCurrentCIG = membersMap[cig.id] ?: emptyList()

        // Prepare JSON for the current CIG
        val cigJson = JSONObject().apply {
            put("hub", cig.hub)
            put("cig_name", cig.cig_name)
            put("no_of_members", cig.no_of_members)
            put("date_established", cig.date_established)
            put("constitution", cig.constitution)
            put("registration", cig.registration)
            put("elections_held", cig.elections_held)
            put("date_of_last_elections", cig.date_of_last_elections)
            put("meeting_venue", cig.meeting_venue)
            put("frequency", cig.frequency)
            put("scheduled_meeting_day", cig.scheduled_meeting_day)
            put("scheduled_meeting_time", cig.scheduled_meeting_time)
            put("user_id", cig.user_id)

            // Create a JSON array for members
            val membersJsonArray = JSONArray()
            for (member in membersForCurrentCIG) {
                val memberJson = JSONObject().apply {
                    put("other_name", member.other_name)
                    put("last_name", member.last_name)
                    put("gender", member.gender)
                    put("date_of_birth", member.date_of_birth)
                    put("email", member.email)
                    put("phone_number", member.phone_number)
                    put("id_number", member.id_number)
                    put("product_involved", member.product_involved)
                    put("hectorage_registered_under_cig", member.hectorage_registered_under_cig)
                }
                membersJsonArray.put(memberJson)
            }
            put("members", membersJsonArray)
        }

        // Log the JSON object
        Log.d("PrepareCIGJson", "CIG JSON: $cigJson")

        return cigJson
    }

    // Post offline hub user data
    private fun postHubUserData() {
        val offlineHubUsers = dbHandler.offlineHubUsers

        // Create an instance of SharedPrefs
        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostHubUserDataError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/hub-users"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (hubUser in offlineHubUsers) {
                    // Prepare JSON for the current HubUser
                    val hubUserJson = prepareHubUserJson(hubUser)

                    // Log the JSON data
                    Log.d("PostHubUserData", "Posting JSON: ${hubUserJson.toString(4)}") // Pretty print with indentation

                    // Prepare connection for each HubUser
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(hubUserJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostHubUserData", "HubUser data posted successfully")
                        // Optionally handle the response
                    } else {
                        Log.e("PostHubUserDataError", "Failed to post HubUser data. Code: $responseCode")
                        // Optionally handle the error
                    }
                }
            } catch (e: IOException) {
                Log.e("PostHubUserDataFailure", "Error posting HubUser data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareHubUserJson(hubUser: HubUser): JSONObject {
        return JSONObject().apply {
            put("other_name", hubUser.other_name)
            put("last_name", hubUser.last_name)
            put("code", hubUser.code)
            put("role", hubUser.role)
            put("id_number", hubUser.id_number)
            put("gender", hubUser.gender)
            put("date_of_birth", hubUser.date_of_birth)
            put("email", hubUser.email)
            put("phone_number", hubUser.phone_number)
            put("education_level", hubUser.education_level)
            put("hub", hubUser.hub)
            put("buying_center", hubUser.buying_center)
            put("county", hubUser.county)
            put("sub_county", hubUser.sub_county)
            put("ward", hubUser.ward)
            put("village", hubUser.village)
            put("user_id", hubUser.user_id)
        }
    }

    // Post offline HQ user data
    private fun postHQUserData() {
        val offlineHQUsers = dbHandler.offlineHQUsers

        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostHQUserDataError", "Access token not found")
            return
        }
        val url = "https://fdp-backend.onrender.com/hq-users"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (hqUser in offlineHQUsers) {
                    // Prepare JSON for the current HQUser
                    val hqUserJson = prepareHQUserJson(hqUser)

                    // Log the JSON data
                    Log.d("PostHQUserData", "Posting JSON: ${hqUserJson.toString(4)}") // Pretty print with indentation

                    // Prepare connection for each HQUser
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data to the output stream
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(hqUserJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostHQUserData", "HQUser data posted successfully")
                    } else {
                        Log.e("PostHQUserDataError", "Failed to post HQUser data. Code: $responseCode")
                    }
                }
            } catch (e: IOException) {
                Log.e("PostHQUserDataFailure", "Error posting HQUser data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareHQUserJson(hqUser: HQUser): JSONObject {
        return JSONObject().apply {
            put("other_name", hqUser.other_name)
            put("last_name", hqUser.last_name)
            put("staff_code", hqUser.staff_code)
            put("role", hqUser.role)
            put("id_number", hqUser.id_number)
            put("gender", hqUser.gender)
            put("date_of_birth", hqUser.date_of_birth)
            put("email", hqUser.email)
            put("phone_number", hqUser.phone_number)
            put("education_level", hqUser.education_level)
            put("department", hqUser.department)
            put("reporting_to", hqUser.reporting_to)
            put("related_roles", hqUser.related_roles)
            put("user_id", hqUser.user_id)
            put("is_offline", hqUser.is_offline)
        }
    }

    // Post offline individual logistician data
    private fun postIndividualLogisticianData() {
        val offlineIndividualLogisticianList = dbHandler.offlineIndividualLogistician
        val url = "https://fdp-backend.onrender.com/individual-logistician-users"
        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostIndividualLogisticianError", "Access token not found")
            return
        }

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (individualLogistician in offlineIndividualLogisticianList) {
                    // Prepare JSON for the current individual logistician
                    val individualLogisticianJson = prepareIndividualLogisticianJson(individualLogistician)

                    // Log the JSON data
                    Log.d("PostIndividualLogisticianData", "Posting JSON: ${individualLogisticianJson.toString(4)}") // Pretty print with indentation

                    // Prepare connection for each individual logistician
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(individualLogisticianJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostIndividualLogisticianData", "Individual logistician data posted successfully")
                        // Optionally handle the response
                    } else {
                        Log.e("PostIndividualLogisticianError", "Failed to post individual logistician data. Code: $responseCode")
                        // Optionally handle the error
                    }
                }
            } catch (e: IOException) {
                Log.e("PostIndividualLogisticianFailure", "Error posting individual logistician data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareIndividualLogisticianJson(individualLogistician: IndividualLogistician): JSONObject {
        // Prepare JSON for the current individual logistician
        val individualLogisticianJson = JSONObject().apply {
            put("address", individualLogistician.address)
            put("date_of_birth", individualLogistician.date_of_birth)
            put("email", individualLogistician.email)
            put("hub", individualLogistician.hub)
            put("id_number", individualLogistician.id_number)
            put("last_name", individualLogistician.last_name)
            put("logistician_code", individualLogistician.logistician_code)
            put("other_name", individualLogistician.other_name)
            put("phone_number", individualLogistician.phone_number)
            put("region", individualLogistician.region)

            // Create a JSON array for cars
            val carsJsonArray = JSONArray()
            val carsForCurrentLogistician = dbHandler.getCarsForLogistician(individualLogistician.id)
            for (car in carsForCurrentLogistician) {
                val carJson = JSONObject().apply {
                    put("car_body_type", car.car_body_type)
                    put("car_model", car.car_model)
                    put("driver1_name", car.driver1_name)
                    put("driver2_name", car.driver2_name)
                    put("number_plate", car.number_plate)
                }
                carsJsonArray.put(carJson)
            }
            put("cars", carsJsonArray)
        }

        // Log the JSON object
        Log.d("PrepareIndividualLogisticianJson", "Individual Logistician JSON: $individualLogisticianJson")

        return individualLogisticianJson
    }

    // Post offline org logistician data
    private fun postOrgLogisticianData() {
        val offlineOrgLogisticianList = dbHandler.offlineOrganisationalLogistician
        val url = "https://fdp-backend.onrender.com/organisation-logistician-users"
        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostOrgLogisticianError", "Access token not found")
            return
        }

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (orgLogistician in offlineOrgLogisticianList) {
                    // Prepare JSON for the current org logistician
                    val orgLogisticianJson = prepareOrgLogisticianJson(orgLogistician)

                    // Log the JSON data
                    Log.d("PostOrgLogisticianData", "Posting JSON: ${orgLogisticianJson.toString(4)}") // Pretty print with indentation

                    // Prepare connection for each org logistician
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(orgLogisticianJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostOrgLogisticianData", "Org logistician data posted successfully")
                        // Optionally handle the response
                    } else {
                        Log.e("PostOrgLogisticianError", "Failed to post org logistician data. Code: $responseCode")
                        // Optionally handle the error
                    }
                }
            } catch (e: IOException) {
                Log.e("PostOrgLogisticianFailure", "Error posting org logistician data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareOrgLogisticianJson(orgLogistician: OrganisationalLogistician): JSONObject {
        // Prepare JSON for the current org logistician
        val orgLogisticianJson = JSONObject().apply {
            put("name", orgLogistician.name)
            put("logistician_code", orgLogistician.logistician_code)
            put("registration_number", orgLogistician.registration_number)
            put("date_of_registration", orgLogistician.date_of_registration)
            put("email", orgLogistician.email)
            put("phone_number", orgLogistician.phone_number)
            put("address", orgLogistician.address)
            put("hub", orgLogistician.hub)
            put("region", orgLogistician.region)

            // Create a JSON array for cars
            val carsJsonArray = JSONArray()
            orgLogistician.cars?.forEach { car ->
                val carJson = JSONObject().apply {
                    put("car_body_type", car.car_body_type)
                    put("car_model", car.car_model)
                    put("driver1_name", car.driver1_name)
                    put("driver2_name", car.driver2_name)
                    put("number_plate", car.number_plate)
                }
                carsJsonArray.put(carJson)
            }
            put("cars", carsJsonArray)
        }

        // Log the JSON object
        Log.d("PrepareOrgLogisticianJson", "Org Logistician JSON: $orgLogisticianJson")

        return orgLogisticianJson
    }

    private fun postTrainingData() {
        // Retrieve offline training data from DBHandler
        val offlineTrainings = dbHandler.offlineTrainings

        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostTrainingDataError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/trainings"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (training in offlineTrainings) {
                    val trainingJson = prepareTrainingJson(training)

                    // Prepare connection for each training
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(trainingJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostTrainingData", "Training data posted successfully")

                        // Delete the successfully posted training data from local DB
                        dbHandler.deleteTraining(training.id)

                    } else {
                        Log.e("PostTrainingDataError", "Failed to post training data. Code: $responseCode")
                    }
                }
            } catch (e: IOException) {
                Log.e("PostTrainingDataFailure", "Error posting training data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    // Function to prepare JSON object for training data
    private fun prepareTrainingJson(training: Training): JSONObject {
        // Convert participants map to JSON
        val participantsJsonObject = JSONObject(training.participants)

        // Create JSON object for training
        return JSONObject().apply {
            put("course_name", training.course_name)
            put("trainer_name", training.trainer_name)
            put("buying_center", training.buying_center)
            put("course_description", training.course_description)
            put("date_of_training", training.date_of_training)
            put("content_of_training", training.content_of_training)
            put("venue", training.venue)
            put("participants", participantsJsonObject)
            put("user_id", training.user_id)
        }
    }

    // Post farmer price distribution offline data
    private fun postFarmerPriceDistributionData() {
        // Retrieve offline farmer price distribution data from DBHandler
        val offlineFarmerPriceDistributions = dbHandler.offlineFarmerPriceDistributions

        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostFarmerPriceDistributionDataError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/farmer-price-distributions"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (priceDistribution in offlineFarmerPriceDistributions) {
                    val priceDistributionJson = prepareFarmerPriceDistributionJson(priceDistribution)

                    // Prepare connection for each price distribution
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(priceDistributionJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostFarmerPriceDistributionData", "Farmer price distribution data posted successfully")

                        // Delete the successfully posted data from local DB
                        dbHandler.deleteFarmerPriceDistribution(priceDistribution.id)

                    } else {
                        Log.e("PostFarmerPriceDistributionDataError", "Failed to post farmer price distribution data. Code: $responseCode")
                    }
                }
            } catch (e: IOException) {
                Log.e("PostFarmerPriceDistributionDataFailure", "Error posting farmer price distribution data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareFarmerPriceDistributionJson(priceDistribution: FarmerPriceDistribution): JSONObject {
        return JSONObject().apply {
            put("hub", priceDistribution.hub)
            put("buying_center", priceDistribution.buying_center)
            put("online_price", priceDistribution.online_price)
            put("unit", priceDistribution.unit)
            put("date", priceDistribution.date)
            put("comments", priceDistribution.comments)
            put("sold", priceDistribution.sold)
            put("user_id", priceDistribution.user_id)
            put("produce_id", priceDistribution.produce_id)
        }
    }

    // Post customer price distribution offline data
    private fun postCustomerPriceDistributionData() {
        // Retrieve offline customer price distribution data from DBHandler
        val offlineCustomerPriceDistributions = dbHandler.offlineCustomerPriceDistributions

        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostCustomerPriceDistributionDataError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/customer-price-distributions"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (priceDistribution in offlineCustomerPriceDistributions) {
                    val priceDistributionJson = prepareCustomerPriceDistributionJson(priceDistribution)

                    // Prepare connection for each price distribution
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(priceDistributionJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostCustomerPriceDistributionData", "Customer price distribution data posted successfully")

                        // Delete the successfully posted data from local DB
                        dbHandler.deleteCustomerPriceDistribution(priceDistribution.id)

                    } else {
                        Log.e("PostCustomerPriceDistributionDataError", "Failed to post customer price distribution data. Code: $responseCode")
                    }
                }
            } catch (e: IOException) {
                Log.e("PostCustomerPriceDistributionDataFailure", "Error posting customer price distribution data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareCustomerPriceDistributionJson(priceDistribution: CustomerPriceDistribution): JSONObject {
        return JSONObject().apply {
            put("hub", priceDistribution.hub)
            put("buying_center", priceDistribution.buying_center)
            put("online_price", priceDistribution.online_price)
            put("unit", priceDistribution.unit)
            put("date", priceDistribution.date)
            put("comments", priceDistribution.comments)
            put("sold", priceDistribution.sold)
            put("user_id", priceDistribution.user_id)
            put("produce_id", priceDistribution.produce_id)
        }
    }

    // Post buying offline data
    private fun postBuyingData() {
        // Retrieve offline buying data from DBHandler
        val offlineBuyingFarmers = dbHandler.offlineBuyingFarmers

        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostBuyingDataError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/buying"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (buyingFarmer in offlineBuyingFarmers) {
                    val buyingFarmerJson = prepareBuyingFarmerJson(buyingFarmer)

                    // Prepare connection for each buying farmer
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(buyingFarmerJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostBuyingData", "Buying data posted successfully")

                        // Delete the successfully posted data from local DB
                        dbHandler.deleteBuyingFarmer(buyingFarmer.id)

                    } else {
                        Log.e("PostBuyingDataError", "Failed to post buying data. Code: $responseCode")
                    }
                }
            } catch (e: IOException) {
                Log.e("PostBuyingDataFailure", "Error posting buying data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareBuyingFarmerJson(buyingFarmer: BuyingFarmer): JSONObject {
        return JSONObject().apply {
            put("buying_center", buyingFarmer.buying_center)
            put("producer", buyingFarmer.producer)
            put("produce", buyingFarmer.produce)
            put("grn_number", buyingFarmer.grn_number)
            put("unit", buyingFarmer.unit)

            // Convert quality map to JSON
            val qualityJson = Gson().toJson(buyingFarmer.quality)
            put("quality", qualityJson)

            put("action", buyingFarmer.action)
            put("weight", buyingFarmer.weight)
            put("loaded", buyingFarmer.loaded)
            put("user_id", buyingFarmer.user_id)
        }
    }

    // Post selling offline data
    private fun postSellingData() {
        // Retrieve offline selling data from DBHandler
        val offlineSellingCustomers = dbHandler.offlineBuyingCustomers

        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostSellingDataError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/selling"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (sellingCustomer in offlineSellingCustomers) {
                    val sellingCustomerJson = prepareSellingCustomerJson(sellingCustomer)

                    // Prepare connection for each selling customer
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(sellingCustomerJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostSellingData", "Selling data posted successfully")

                        // Delete the successfully posted data from local DB
                        dbHandler.deleteBuyingCustomer(sellingCustomer.id)

                    } else {
                        Log.e("PostSellingDataError", "Failed to post selling data. Code: $responseCode")
                    }
                }
            } catch (e: IOException) {
                Log.e("PostSellingDataFailure", "Error posting selling data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareSellingCustomerJson(sellingCustomer: BuyingCustomer): JSONObject {
        return JSONObject().apply {
            put("customer", sellingCustomer.customer)
            put("produce", sellingCustomer.produce)
            put("grn_number", sellingCustomer.grn_number)
            put("unit", sellingCustomer.unit)

            // Convert quality map to JSON
            val qualityJson = Gson().toJson(sellingCustomer.quality)
            put("quality", qualityJson)

            put("action", sellingCustomer.action)
            put("weight", sellingCustomer.weight)
            put("online_price", sellingCustomer.online_price)
            put("loaded", sellingCustomer.loaded)
            put("user_id", sellingCustomer.user_id)
        }
    }

    // Post loading offline data
    private fun postLoadingData() {
        // Retrieve offline loading data from DBHandler
        val offlineLoadings = dbHandler.offlineLoadings

        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostLoadingDataError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/loading"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (loading in offlineLoadings) {
                    val loadingJson = prepareLoadingJson(loading)

                    // Prepare connection for each loading
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(loadingJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostLoadingData", "Loading data posted successfully")

                        // Delete the successfully posted data from local DB
                        dbHandler.deleteLoading(loading.id)
                    } else {
                        Log.e("PostLoadingDataError", "Failed to post loading data. Code: $responseCode")
                    }
                }
            } catch (e: IOException) {
                Log.e("PostLoadingDataFailure", "Error posting loading data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareLoadingJson(loading: Loading): JSONObject {
        return JSONObject().apply {
            put("grn", loading.grn)
            put("total_weight", loading.total_weight)
            put("truck_loading_number", loading.truck_loading_number)
            put("from_", loading.from_)
            put("to", loading.to)
            put("comment", loading.comment)
            put("offloaded", loading.offloaded)
            put("user_id", loading.user_id)
        }
    }

    // Post offloading offline data
    private fun postOffloadingData() {
        // Retrieve offline offloading data from DBHandler
        val offlineOffloadings = dbHandler.offlineOffloadings

        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostOffloadingDataError", "Access token not found")
            return
        }


        val url = "https://fdp-backend.onrender.com/offloading" // Update the URL accordingly


        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (offloading in offlineOffloadings) {
                    val offloadingJson = prepareOffloadingJson(offloading)

                    // Prepare connection for each offloading
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(offloadingJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostOffloadingData", "Offloading data posted successfully")

                        // Delete the successfully posted data from local DB
                        dbHandler.deleteOffloading(offloading.id)
                    } else {
                        Log.e("PostOffloadingDataError", "Failed to post offloading data. Code: $responseCode")
                    }
                }
            } catch (e: IOException) {
                Log.e("PostOffloadingDataFailure", "Error posting offloading data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareOffloadingJson(offloading: Offloading): JSONObject {
        return JSONObject().apply {
            put("offloaded_load", offloading.offloaded_load)
            put("total_weight", offloading.total_weight)
            put("truck_offloading_number", offloading.truck_offloading_number)
            put("comment", offloading.comment)
            put("user_id", offloading.user_id)
            // Add other necessary fields if any
        }
    }

    // Post rural worker offline data
    private fun postRuralWorkerData() {
        // Retrieve offline rural worker data from DBHandler
        val offlineRuralWorkers = dbHandler.offlineRuralWorkers

        val sharedPrefs = SharedPrefs(context)
        val authToken = sharedPrefs.getToken()

        if (authToken.isNullOrEmpty()) {
            Log.e("PostRuralWorkerDataError", "Access token not found")
            return
        }

        val url = "https://fdp-backend.onrender.com/rural-workers"

        AsyncTask.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                for (ruralWorker in offlineRuralWorkers) {
                    val ruralWorkerJson = prepareRuralWorkerJson(ruralWorker)

                    // Prepare connection for each rural worker
                    val apiUrl = URL(url)
                    urlConnection = apiUrl.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
                    urlConnection.doOutput = true

                    // Write data
                    urlConnection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
                            writer.write(ruralWorkerJson.toString())
                            writer.flush()
                        }
                    }

                    // Check response
                    val responseCode = urlConnection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Log.d("PostRuralWorkerData", "Rural worker data posted successfully")

                        // Delete the successfully posted data from local DB
                        dbHandler.deleteRuralWorker(ruralWorker.id)
                    } else {
                        Log.e("PostRuralWorkerDataError", "Failed to post rural worker data. Code: $responseCode")
                    }
                }
            } catch (e: IOException) {
                Log.e("PostRuralWorkerDataFailure", "Error posting rural worker data: ${e.message}")
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    private fun prepareRuralWorkerJson(ruralWorker: RuralWorker): JSONObject {
        return JSONObject().apply {
            put("other_name", ruralWorker.other_name)
            put("last_name", ruralWorker.last_name)
            put("rural_worker_code", ruralWorker.rural_worker_code)
            put("id_number", ruralWorker.id_number)
            put("gender", ruralWorker.gender)
            put("date_of_birth", ruralWorker.date_of_birth)
            put("email", ruralWorker.email)
            put("phone_number", ruralWorker.phone_number)
            put("education_level", ruralWorker.education_level)
            put("service", ruralWorker.service)
            put("other", ruralWorker.other)
            put("county", ruralWorker.county)
            put("sub_county", ruralWorker.sub_county)
            put("ward", ruralWorker.ward)
            put("village", ruralWorker.village)
            put("user_id", ruralWorker.user_id)
        }
    }
    // Syncing online data
    private fun fetchAndSyncUserData() {
        val authToken = sharedPrefs.getToken()

        apiService.getUsers(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val userResponse = parseUserResponse(responseBody)
                            syncUsersToLocalDatabase(userResponse)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing user response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncCustomUserData() {
        val authToken = sharedPrefs.getToken()

        apiService.getCustomUsers(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val customUserResponse = parseCustomUserResponse(responseBody)
                            syncCustomUsersToLocalDatabase(customUserResponse)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing custom user response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncHQUserData() {
        val authToken = sharedPrefs.getToken()

        apiService.getHQUsers(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val hqUserResponse = parseHQUserResponse(responseBody)
                            syncHQUsersToLocalDatabase(hqUserResponse)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing HQ user response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncHubUserData() {
        val authToken = sharedPrefs.getToken()

        apiService.getHubUsers(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val hubUserResponse = parseHubUserResponse(responseBody)
                            syncHubUsersToLocalDatabase(hubUserResponse)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing Hub user response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncProcessingUserData() {
        val authToken = sharedPrefs.getToken()

        apiService.getProcessingUsers(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val processingUserResponse = parseProcessingUserResponse(responseBody)
                            syncProcessingUsersToLocalDatabase(processingUserResponse)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing ProcessingUser response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncLogisticianData() {
        val authToken = sharedPrefs.getToken()

        apiService.getIndividualLogisticianUsers(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val logisticianResponse = parseLogisticianResponse(responseBody)
                            syncLogisticiansToLocalDatabase(logisticianResponse)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing logistician response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncOrganisationLogisticianData() {
        val authToken = sharedPrefs.getToken()

        apiService.getOrganisationLogisticianUsers(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val logisticianResponse = parseOrganisationalLogisticianResponse(responseBody)
                            syncOrganisationalLogisticiansToLocalDatabase(logisticianResponse)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing organisational logistician response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncCustomerData() {
        val authToken = sharedPrefs.getToken()

        apiService.getIndividualCustomerUsers(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val customerResponse = parseCustomerResponse(responseBody)
                            syncCustomersToLocalDatabase(customerResponse)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing customer response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncOrganizationalCustomerData() {
        val authToken = sharedPrefs.getToken()

        apiService.getOrganisationCustomerUsers(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val customerResponse = parseOrganizationalCustomerResponse(responseBody)
                            syncOrganizationalCustomersToLocalDatabase(customerResponse)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing organizational customer response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    fun fetchAndSyncTrainingData() {
        val authToken = sharedPrefs.getToken()

        apiService.getTrainings(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val trainingList = parseTrainingResponse(responseBody)
                            syncTrainingToLocalDatabase(trainingList)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing Training response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncAttendanceData() {
        val authToken = sharedPrefs.getToken()

        apiService.getAttendance(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val attendanceList = parseAttendanceResponse(responseBody)
                            syncAttendanceToLocalDatabase(attendanceList)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing Attendance response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncFarmerPriceDistributionData() {
        val authToken = sharedPrefs.getToken()

        apiService.getFarmerPriceDistributions(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val priceDistributionList = parseFarmerPriceDistributionResponse(responseBody)
                            syncFarmerPriceDistributionToLocalDatabase(priceDistributionList)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing Farmer Price Distribution response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncCustomerPriceDistributionData() {
        val authToken = sharedPrefs.getToken()

        apiService.getCustomerPriceDistributions(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val priceDistributionList = parseCustomerPriceDistributionResponse(responseBody)
                            syncCustomerPriceDistributionToLocalDatabase(priceDistributionList)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing Customer Price Distribution response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncSellingData() {
        val authToken = sharedPrefs.getToken()

        apiService.getSelling(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val responseString = responseBody.string() // Convert response body to string
                            val sellingList = parseSellingResponse(responseString)
                            syncSellingToLocalDatabase(sellingList)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing Selling response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncQuarantineData() {
        val authToken = sharedPrefs.getToken()

        apiService.getQuarantine(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val quarantineList = parseQuarantineResponse(responseBody)
                            syncQuarantineToLocalDatabase(quarantineList)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing Quarantine response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncPaymentData() {
        val authToken = sharedPrefs.getToken()

        apiService.getMakePayment(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val paymentList = parsePaymentResponse(responseBody)
                            syncPaymentToLocalDatabase(paymentList)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing Payment response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncReceivePaymentData() {
        val authToken = sharedPrefs.getToken()

        apiService.getReceivePayment(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val paymentList = parseReceivePaymentResponse(responseBody)
                            syncReceivePaymentToLocalDatabase(paymentList)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing Payment response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    private fun fetchAndSyncRuralWorkerData() {
        val authToken = sharedPrefs.getToken()

        apiService.getRuralWorkers(authToken)?.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val ruralWorkerResponse = parseRuralWorkerResponse(responseBody)
                            syncRuralWorkersToLocalDatabase(ruralWorkerResponse)
                        } catch (e: Exception) {
                            Log.e("DataSyncManager", "Error parsing Rural Worker response", e)
                        }
                    } ?: run {
                        Log.w("DataSyncManager", "Response body is null")
                    }
                } else {
                    Log.e("DataSyncManager", "Response was not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Log.e("DataSyncManager", "Network request failed", t)
            }
        })
    }

    // DataSyncManager.kt

// ... (other parts of your DataSyncManager class)

    private fun parseUserResponse(responseBody: ResponseBody): UserResponse? {
        val TAG = "DataSyncManager:UserParse"
        Log.d(TAG, "Attempting to parse User response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "User Response JSON: $jsonString")
            val userResponse = Gson().fromJson(jsonString, UserResponse::class.java)
            Log.d(TAG, "Successfully parsed User response.")
            return userResponse
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing User response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return null
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading User response body.", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing User response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return null
        }
    }

    private fun parseCustomUserResponse(responseBody: ResponseBody): List<CustomUser> {
        val TAG = "DataSyncManager:CustomUserParse"
        Log.d(TAG, "Attempting to parse Custom User response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "Custom User Response JSON: $jsonString")
            val type = object : TypeToken<List<CustomUser>>() {}.type
            val customUsers = Gson().fromJson<List<CustomUser>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Custom User response. Items: ${customUsers.size}")
            return customUsers
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing Custom User response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Custom User response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Custom User response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseHQUserResponse(responseBody: ResponseBody): List<HQUser> {
        val TAG = "DataSyncManager:HQUserParse"
        Log.d(TAG, "Attempting to parse HQ User response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "HQ User Response JSON: $jsonString")
            val type = object : TypeToken<List<HQUser>>() {}.type
            val hqUsers = Gson().fromJson<List<HQUser>>(jsonString, type)
            Log.d(TAG, "Successfully parsed HQ User response. Items: ${hqUsers.size}")
            return hqUsers
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing HQ User response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading HQ User response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing HQ User response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseHubUserResponse(responseBody: ResponseBody): List<HubUser> {
        val TAG = "DataSyncManager:HubUserParse"
        Log.d(TAG, "Attempting to parse Hub User response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "Hub User Response JSON: $jsonString")
            val type = object : TypeToken<List<HubUser>>() {}.type
            val hubUsers = Gson().fromJson<List<HubUser>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Hub User response. Items: ${hubUsers.size}")
            return hubUsers
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing Hub User response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Hub User response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Hub User response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseProcessingUserResponse(responseBody: ResponseBody): List<ProcessingUser> {
        val TAG = "DataSyncManager:ProcessingUserParse"
        Log.d(TAG, "Attempting to parse ProcessingUser response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "ProcessingUser Response JSON: $jsonString")
            val type = object : TypeToken<List<ProcessingUser>>() {}.type
            val processingUsers = Gson().fromJson<List<ProcessingUser>>(jsonString, type)
            Log.d(TAG, "Successfully parsed ProcessingUser response. Items: ${processingUsers.size}")
            return processingUsers
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing ProcessingUser response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading ProcessingUser response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing ProcessingUser response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseLogisticianResponse(responseBody: ResponseBody): List<IndividualLogistician> {
        val TAG = "DataSyncManager:IndLogisticianParse"
        Log.d(TAG, "Attempting to parse Individual Logistician response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "Individual Logistician Response JSON: $jsonString")
            val type = object : TypeToken<List<IndividualLogistician>>() {}.type
            val logisticians = Gson().fromJson<List<IndividualLogistician>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Individual Logistician response. Items: ${logisticians.size}")
            return logisticians
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing Individual Logistician response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Individual Logistician response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Individual Logistician response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseOrganisationalLogisticianResponse(responseBody: ResponseBody): List<OrganisationalLogistician> {
        val TAG = "DataSyncManager:OrgLogisticianParse"
        Log.d(TAG, "Attempting to parse Organisational Logistician response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "Organisational Logistician Response JSON: $jsonString")
            val type = object : TypeToken<List<OrganisationalLogistician>>() {}.type
            val orgLogisticians = Gson().fromJson<List<OrganisationalLogistician>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Organisational Logistician response. Items: ${orgLogisticians.size}")
            return orgLogisticians
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing Organisational Logistician response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Organisational Logistician response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Organisational Logistician response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseCustomerResponse(responseBody: ResponseBody): List<IndividualCustomer> {
        val TAG = "DataSyncManager:IndCustomerParse"
        Log.d(TAG, "Attempting to parse Individual Customer response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "Individual Customer Response JSON: $jsonString")
            val type = object : TypeToken<List<IndividualCustomer>>() {}.type
            val customers = Gson().fromJson<List<IndividualCustomer>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Individual Customer response. Items: ${customers.size}")
            return customers
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing Individual Customer response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Individual Customer response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Individual Customer response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseOrganizationalCustomerResponse(responseBody: ResponseBody): List<OrganisationalCustomer> {
        val TAG = "DataSyncManager:OrgCustomerParse"
        Log.d(TAG, "Attempting to parse Organizational Customer response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d("DataSyncManager on parseOrganizationalCustomerResponse", "Organizational Customer Response JSON: $jsonString")
            val type = object : TypeToken<List<OrganisationalCustomer>>() {}.type
            val customers = Gson().fromJson<List<OrganisationalCustomer>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Organizational Customer response. Items: ${customers.size}")
            return customers
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing Organizational Customer response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Organizational Customer response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Organizational Customer response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseTrainingResponse(responseBody: ResponseBody): List<Training> {
        val TAG = "DataSyncManager:TrainingParse"
        Log.d(TAG, "Attempting to parse Training response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "Training Response JSON: $jsonString")
            val type = object : TypeToken<List<Training>>() {}.type
            val trainings = Gson().fromJson<List<Training>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Training response. Items: ${trainings.size}")
            return trainings
        } catch (e: JsonSyntaxException) {
            Log.e("DataSyncManager on parseTrainingRespons ", "JSON Parsing Error on parseTrainingRespons. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Training response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Training response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseAttendanceResponse(responseBody: ResponseBody): List<Attendance> {
        val TAG = "DataSyncManager:AttendanceParse"
        Log.d(TAG, "Attempting to parse Attendance response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "Attendance Response JSON: $jsonString")
            val type = object : TypeToken<List<Attendance>>() {}.type
            val attendances = Gson().fromJson<List<Attendance>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Attendance response. Items: ${attendances.size}")
            return attendances
        } catch (e: JsonSyntaxException) {
            Log.e("DataSyncManager on parseAttendanceResponse", "JSON Parsing Error. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Attendance response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Attendance response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseFarmerPriceDistributionResponse(responseBody: ResponseBody): List<FarmerPriceDistribution> {
        val TAG = "DataSyncManager:FarmerPriceParse"
        Log.d(TAG, "Attempting to parse Farmer Price Distribution response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "Farmer Price Distribution Response JSON: $jsonString")
            val type = object : TypeToken<List<FarmerPriceDistribution>>() {}.type
            val distributions = Gson().fromJson<List<FarmerPriceDistribution>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Farmer Price Distribution response. Items: ${distributions.size}")
            return distributions
        } catch (e: JsonSyntaxException) {
            Log.e("DataSyncManager on parseFarmerPriceDistributionResponse", "JSON Parsing Error. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Farmer Price Distribution response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Farmer Price Distribution response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseCustomerPriceDistributionResponse(responseBody: ResponseBody): List<CustomerPriceDistribution> {
        val TAG = "DataSyncManager:CustPriceParse"
        Log.d(TAG, "Attempting to parse Customer Price Distribution response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d("DataSyncManager on parseCustomerPriceDistributionResponse", "Customer Price Distribution Response JSON: $jsonString")
            val type = object : TypeToken<List<CustomerPriceDistribution>>() {}.type
            val distributions = Gson().fromJson<List<CustomerPriceDistribution>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Customer Price Distribution response. Items: ${distributions.size}")
            return distributions
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing Customer Price Distribution response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Customer Price Distribution response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Customer Price Distribution response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseSellingResponse(responseString: String): List<BuyingCustomer> {
        val TAG = "DataSyncManager:SellingParse"
        Log.d(TAG, "Attempting to parse Selling (BuyingCustomer) response.")
        Log.d(TAG, "Selling (BuyingCustomer) Response JSON: $responseString")
        val gson = Gson()
        val sellingListType = object : TypeToken<List<BuyingCustomer>>() {}.type
        return try {
            val sellingList = gson.fromJson<List<BuyingCustomer>>(responseString, sellingListType)
            Log.d(TAG, "Successfully parsed Selling (BuyingCustomer) response. Items: ${sellingList.size}")
            sellingList
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing Selling (BuyingCustomer) response JSON: $responseString", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Selling (BuyingCustomer) response: $responseString", e)
            return emptyList()
        }
    }

    private fun parseQuarantineResponse(responseBody: ResponseBody): List<Quarantine> {
        val TAG = "DataSyncManager:QuarantineParse"
        Log.d(TAG, "Attempting to parse Quarantine response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "Quarantine Response JSON: $jsonString")
            val type = object : TypeToken<List<Quarantine>>() {}.type
            val quarantines = Gson().fromJson<List<Quarantine>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Quarantine response. Items: ${quarantines.size}")
            return quarantines
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing Quarantine response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Quarantine response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Quarantine response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parsePaymentResponse(responseBody: ResponseBody): List<PaymentFarmer> {
        val TAG = "DataSyncManager:PaymentFarmerParse"
        Log.d(TAG, "Attempting to parse PaymentFarmer response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "PaymentFarmer Response JSON: $jsonString")
            val type = object : TypeToken<List<PaymentFarmer>>() {}.type
            val payments = Gson().fromJson<List<PaymentFarmer>>(jsonString, type)
            Log.d(TAG, "Successfully parsed PaymentFarmer response. Items: ${payments.size}")
            return payments
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing PaymentFarmer response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading PaymentFarmer response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing PaymentFarmer response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseReceivePaymentResponse(responseBody: ResponseBody): List<PaymentCustomer> {
        val TAG = "DataSyncManager:PaymentCustParse"
        Log.d(TAG, "Attempting to parse PaymentCustomer response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "PaymentCustomer Response JSON: $jsonString")
            val type = object : TypeToken<List<PaymentCustomer>>() {}.type
            val payments = Gson().fromJson<List<PaymentCustomer>>(jsonString, type)
            Log.d(TAG, "Successfully parsed PaymentCustomer response. Items: ${payments.size}")
            return payments
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing PaymentCustomer response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading PaymentCustomer response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing PaymentCustomer response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }

    private fun parseRuralWorkerResponse(responseBody: ResponseBody): List<RuralWorker> {
        val TAG = "DataSyncManager:RuralWorkerParse"
        Log.d(TAG, "Attempting to parse Rural Worker response.")
        var jsonString: String? = null
        try {
            jsonString = responseBody.string()
            Log.d(TAG, "Rural Worker Response JSON: $jsonString")
            val type = object : TypeToken<List<RuralWorker>>() {}.type
            val ruralWorkers = Gson().fromJson<List<RuralWorker>>(jsonString, type)
            Log.d(TAG, "Successfully parsed Rural Worker response. Items: ${ruralWorkers.size}")
            return ruralWorkers
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing Rural Worker response JSON. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "IOException while reading Rural Worker response body.", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing Rural Worker response. Data: ${jsonString ?: "Error: JSON string was null or unreadable"}", e)
            return emptyList()
        }
    }


    private fun syncUsersToLocalDatabase(userResponse: UserResponse?) {
        val users = userResponse!!.users
        if (users.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${users.size} users to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                users.forEach { user ->
                    // Check if the user already exists in the database
                    if (!isUserExists(db, user.id)) {
                        val contentValues = ContentValues().apply {
                            put("id", user.id)
                            put("last_name", user.last_name)
                            put("other_name", user.other_name)
                            put("user_type", user.user_type)
                            put("email", user.email)
                            put("role", user.role)
                            put("email_verified", user.email_verified)
                            put("password", user.password)
                            put("verification_token", user.verification_token)
                            put("created_at", user.created_at)
                            put("updated_at", user.updated_at)
                        }

                        val id = db.insert("users", null, contentValues)
                        if (id != -1L) {
                            Log.d("DataSyncManager", "Inserted user with id: $id")
                        } else {
                            Log.e("DataSyncManager", "Failed to insert user: ${user.id}")
                        }
                    } else {
                        Log.d("DataSyncManager", "User with id ${user.id} already exists")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting users", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No users to insert")
        }
    }

    private fun isUserExists(db: SQLiteDatabase, userId: String): Boolean {
        val cursor = db.rawQuery("SELECT 1 FROM users WHERE id = ?", arrayOf(userId.toString()))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    private fun syncCustomUsersToLocalDatabase(customUsers: List<CustomUser>) {
        if (customUsers.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${customUsers.size} custom users to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                customUsers.forEach { user ->
                    // Insert CustomUser details into CustomUser table
                    val userValues = ContentValues().apply {
                        put(DBHandler.CUSTOM_USER_ID_COL, user.id)
                        put(DBHandler.CUSTOM_USER_OTHER_NAME_COL, user.other_name)
                        put(DBHandler.CUSTOM_USER_LAST_NAME_COL, user.last_name)
                        put(DBHandler.CUSTOM_USER_STAFF_CODE_COL, user.staff_code)
                        put(DBHandler.CUSTOM_USER_ID_NUMBER_COL, user.id_number)
                        put(DBHandler.CUSTOM_USER_GENDER_COL, user.gender)
                        put(DBHandler.CUSTOM_USER_DATE_OF_BIRTH_COL, user.date_of_birth)
                        put(DBHandler.CUSTOM_USER_EMAIL_COL, user.email)
                        put(DBHandler.CUSTOM_USER_PHONE_NUMBER_COL, user.phone_number)
                        put(DBHandler.CUSTOM_USER_EDUCATION_LEVEL_COL, user.education_level)
                        put(DBHandler.CUSTOM_USER_ROLE_COL, user.role)
                        put(DBHandler.CUSTOM_USER_REPORTING_TO_COL, user.reporting_to)
                        put(DBHandler.CUSTOM_USER_USER_ID_COL, user.user_id)
                    }

                    val userResult = db.insertWithOnConflict(DBHandler.CUSTOM_USER_TABLE_NAME, null, userValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (userResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert user ${user.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting custom users", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No custom users to insert")
        }
    }

    private fun syncHQUsersToLocalDatabase(hqUsers: List<HQUser>) {
        if (hqUsers.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${hqUsers.size} HQ users to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                hqUsers.forEach { user ->
                    // Insert HQUser details into HQUser table
                    val userValues = ContentValues().apply {
                        put(DBHandler.HQ_USER_ID_COL, user.id)
                        put(DBHandler.HQ_USER_OTHER_NAME_COL, user.other_name)
                        put(DBHandler.HQ_USER_LAST_NAME_COL, user.last_name)
                        put(DBHandler.HQ_USER_STAFF_CODE_COL, user.staff_code)
                        put(DBHandler.HQ_USER_DEPARTMENT_COL, user.department)
                        put(DBHandler.HQ_USER_ID_NUMBER_COL, user.id_number)
                        put(DBHandler.HQ_USER_GENDER_COL, user.gender)
                        put(DBHandler.HQ_USER_DATE_OF_BIRTH_COL, user.date_of_birth)
                        put(DBHandler.HQ_USER_EMAIL_COL, user.email)
                        put(DBHandler.HQ_USER_PHONE_NUMBER_COL, user.phone_number)
                        put(DBHandler.HQ_USER_EDUCATION_LEVEL_COL, user.education_level)
                        put(DBHandler.HQ_USER_ROLE_COL, user.role)
                        put(DBHandler.HQ_USER_REPORTING_TO_COL, user.reporting_to)
                        put(DBHandler.HQ_USER_RELATED_ROLES_COL, user.related_roles)
                        put(DBHandler.HQ_USER_USER_ID_COL, user.user_id)
                    }

                    val userResult = db.insertWithOnConflict(DBHandler.HQ_USER_TABLE_NAME, null, userValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (userResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert user ${user.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting HQ users", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No HQ users to insert")
        }
    }

    private fun syncHubUsersToLocalDatabase(hubUsers: List<HubUser>) {
        if (hubUsers.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${hubUsers.size} Hub users to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                hubUsers.forEach { user ->
                    // Insert HubUser details into HubUser table
                    val userValues = ContentValues().apply {
                        put(DBHandler.HUB_USER_ID_COL, user.id)
                        put(DBHandler.HUB_USER_OTHER_NAME_COL, user.other_name)
                        put(DBHandler.HUB_USER_LAST_NAME_COL, user.last_name)
                        put(DBHandler.HUB_USER_CODE_COL, user.code)
                        put(DBHandler.HUB_USER_ROLE_COL, user.role)
                        put(DBHandler.HUB_USER_ID_NUMBER_COL, user.id_number)
                        put(DBHandler.HUB_USER_GENDER_COL, user.gender)
                        put(DBHandler.HUB_USER_DATE_OF_BIRTH_COL, user.date_of_birth)
                        put(DBHandler.HUB_USER_EMAIL_COL, user.email)
                        put(DBHandler.HUB_USER_PHONE_NUMBER_COL, user.phone_number)
                        put(DBHandler.HUB_USER_EDUCATION_LEVEL_COL, user.education_level)
                        put(DBHandler.HUB_USER_HUB_COL, user.hub)
                        put(DBHandler.HUB_USER_BUYING_CENTER_COL, user.buying_center)
                        put(DBHandler.HUB_USER_COUNTY_COL, user.county)
                        put(DBHandler.HUB_USER_SUB_COUNTY_COL, user.sub_county)
                        put(DBHandler.HUB_USER_WARD_COL, user.ward)
                        put(DBHandler.HUB_USER_VILLAGE_COL, user.village)
                        put(DBHandler.HUB_USER_USER_ID_COL, user.user_id)
                    }

                    val userResult = db.insertWithOnConflict(DBHandler.HUB_USER_TABLE_NAME, null, userValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (userResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert user ${user.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting Hub users", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No Hub users to insert")
        }
    }

    private fun syncProcessingUsersToLocalDatabase(processingUsers: List<ProcessingUser>) {
        if (processingUsers.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${processingUsers.size} Processing users to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                processingUsers.forEach { user ->
                    // Insert ProcessingUser details into ProcessingUser table
                    val userValues = ContentValues().apply {
                        put(DBHandler.PROCESSING_USER_ID_COL, user.id)
                        put(DBHandler.PROCESSING_USER_OTHER_NAME_COL, user.other_name)
                        put(DBHandler.PROCESSING_USER_LAST_NAME_COL, user.last_name)
                        put(DBHandler.PROCESSING_USER_PROCESSOR_CODE_COL, user.processor_code)
                        put(DBHandler.PROCESSING_USER_PROCESSING_PLANT_COL, user.processing_plant)
                        put(DBHandler.PROCESSING_USER_ID_NUMBER_COL, user.id_number)
                        put(DBHandler.PROCESSING_USER_GENDER_COL, user.gender)
                        put(DBHandler.PROCESSING_USER_DATE_OF_BIRTH_COL, user.date_of_birth)
                        put(DBHandler.PROCESSING_USER_EMAIL_COL, user.email)
                        put(DBHandler.PROCESSING_USER_PHONE_NUMBER_COL, user.phone_number)
                        put(DBHandler.PROCESSING_USER_EDUCATION_LEVEL_COL, user.education_level)
                        put(DBHandler.PROCESSING_USER_HUB_COL, user.hub)
                        put(DBHandler.PROCESSING_USER_BUYING_CENTER_COL, user.buying_center)
                        put(DBHandler.PROCESSING_USER_COUNTY_COL, user.county)
                        put(DBHandler.PROCESSING_USER_SUB_COUNTY_COL, user.sub_county)
                        put(DBHandler.PROCESSING_USER_WARD_COL, user.ward)
                        put(DBHandler.PROCESSING_USER_VILLAGE_COL, user.village)
                        put(DBHandler.PROCESSING_USER_USER_ID_COL, user.user_id)
                    }

                    val userResult = db.insertWithOnConflict(DBHandler.PROCESSING_USER_TABLE_NAME, null, userValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (userResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert user ${user.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting Processing users", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No Processing users to insert")
        }
    }

    private fun syncLogisticiansToLocalDatabase(logisticians: List<IndividualLogistician>) {
        if (logisticians.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${logisticians.size} individual logisticians to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                logisticians.forEach { logistician ->
                    // Insert logistician details into logistician table
                    val logisticianValues = ContentValues().apply {
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_ID_COL, logistician.id)
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_OTHER_NAME_COL, logistician.other_name)
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_LAST_NAME_COL, logistician.last_name)
                        put(DBHandler.LOGISTICIAN_CODE_COL, logistician.logistician_code)
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_ID_NUMBER_COL, logistician.id_number)
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_DATE_OF_BIRTH_COL, logistician.date_of_birth)
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_EMAIL_COL, logistician.email)
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_PHONE_NUMBER_COL, logistician.phone_number)
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_ADDRESS_COL, logistician.address)
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_HUB_COL, logistician.hub)
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_REGION_COL, logistician.region)
                        put(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_USER_ID_COL, logistician.user_id)
                    }

                    val logisticianResult = db.insertWithOnConflict(DBHandler.INDIVIDUAL_LOGISTICIAN_USER_TABLE_NAME, null, logisticianValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (logisticianResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert logistician ${logistician.id}")
                    }

                    // Insert cars into cars table
                    logistician.cars?.forEach { car ->
                        val carValues = ContentValues().apply {
                            put(DBHandler.CAR_ID_COL, car.id)
                            put(DBHandler.CAR_BODY_TYPE_COL, car.car_body_type)
                            put(DBHandler.CAR_MODEL_COL, car.car_model)
                            put(DBHandler.CAR_DRIVER1_NAME_COL, car.driver1_name)
                            put(DBHandler.CAR_DRIVER2_NAME_COL, car.driver2_name)
                            put(DBHandler.CAR_NUMBER_PLATE_COL, car.number_plate)
                            put(DBHandler.INDIVIDUAL_LOGISTICIAN_ID_FK_COL, car.individual_logistician_id)
                        }

                        val carResult = db.insertWithOnConflict(DBHandler.CAR_TABLE_NAME, null, carValues, SQLiteDatabase.CONFLICT_REPLACE)
                        if (carResult == -1L) {
                            Log.e("DataSyncManager", "Failed to insert car ${car.id}")
                        }
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting logisticians", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No logisticians to insert")
        }
    }

    private fun syncOrganisationalLogisticiansToLocalDatabase(logisticians: List<OrganisationalLogistician>) {
        if (logisticians.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${logisticians.size} organisational logisticians to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                logisticians.forEach { logistician ->
                    // Insert logistician details into logistician table
                    val logisticianValues = ContentValues().apply {
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_ID_COL, logistician.id)
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_NAME_COL, logistician.name)
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_LOGISTICIAN_CODE_COL, logistician.logistician_code)
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_REGISTRATION_NUMBER_COL, logistician.registration_number)
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_DATE_OF_REGISTRATION_COL, logistician.date_of_registration)
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_EMAIL_COL, logistician.email)
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_PHONE_NUMBER_COL, logistician.phone_number)
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_ADDRESS_COL, logistician.address)
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_HUB_COL, logistician.hub)
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_REGION_COL, logistician.region)
                        put(DBHandler.ORGANISATION_LOGISTICIAN_USER_USER_ID_COL, logistician.user_id)
                    }

                    val logisticianResult = db.insertWithOnConflict(DBHandler.ORGANISATION_LOGISTICIAN_USER_TABLE_NAME, null, logisticianValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (logisticianResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert organisational logistician ${logistician.id}")
                    }

                    // Insert cars into cars table
                    logistician.cars?.forEach { car ->
                        val carValues = ContentValues().apply {
                            put(DBHandler.CAR_ID_COL, car.id)
                            put(DBHandler.CAR_BODY_TYPE_COL, car.car_body_type)
                            put(DBHandler.CAR_MODEL_COL, car.car_model)
                            put(DBHandler.CAR_DRIVER1_NAME_COL, car.driver1_name)
                            put(DBHandler.CAR_DRIVER2_NAME_COL, car.driver2_name)
                            put(DBHandler.CAR_NUMBER_PLATE_COL, car.number_plate)
                            put(DBHandler.ORGANISATION_LOGISTICIAN_ID_FK_COL, car.organisation_logistician_id)
                        }

                        val carResult = db.insertWithOnConflict(DBHandler.CAR_TABLE_NAME, null, carValues, SQLiteDatabase.CONFLICT_REPLACE)
                        if (carResult == -1L) {
                            Log.e("DataSyncManager", "Failed to insert car ${car.id}")
                        }
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting organisational logisticians", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No organisational logisticians to insert")
        }
    }

    private fun syncCustomersToLocalDatabase(customers: List<IndividualCustomer>) {
        if (customers.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${customers.size} individual customers to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                customers.forEach { customer ->
                    // Insert customer details into customer table
                    val customerValues = ContentValues().apply {
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_ID_COL, customer.id)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_OTHER_NAME_COL, customer.other_name)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_LAST_NAME_COL, customer.last_name)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_CUSTOMER_CODE_COL, customer.customer_code)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_ID_NUMBER_COL, customer.id_number)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_GENDER_COL, customer.gender)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_DATE_OF_BIRTH_COL, customer.date_of_birth)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_EMAIL_COL, customer.email)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_PHONE_NUMBER_COL, customer.phone_number)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_COUNTY_COL, customer.county)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_SUB_COUNTY_COL, customer.sub_county)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_WARD_COL, customer.ward)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_VILLAGE_COL, customer.village)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_USER_AUTHORISED_COL, customer.user_authorised)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_AUTHORISATION_TOKEN_COL, customer.authorisation_token)
                        put(DBHandler.IS_OFFLINE_COL, customer.is_offline)
                        put(DBHandler.INDIVIDUAL_CUSTOMER_USER_USER_ID_COL, customer.user_id)
                    }

                    val customerResult = db.insertWithOnConflict(DBHandler.INDIVIDUAL_CUSTOMER_USER_TABLE_NAME, null, customerValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (customerResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert customer ${customer.id}")
                    }

                    // Insert products into products table
                    customer.products?.forEach { product ->
                        val productValues = ContentValues().apply {
                            put(DBHandler.PRODUCT_ID_COL, product.id)
                            put(DBHandler.PRODUCT_CATEGORY_COL, product.category)
                            put(DBHandler.PRODUCT_PRODUCTS_INTERESTED_IN_COL, product.products_interested_in)
                            put(DBHandler.PRODUCT_VOLUME_IN_KGS_COL, product.volume_in_kgs)
                            put(DBHandler.PRODUCT_PACKAGING_COL, product.packaging)
                            put(DBHandler.PRODUCT_QUALITY_COL, product.quality)
                            put(DBHandler.PRODUCT_FREQUENCY_COL, product.frequency)
                            put(DBHandler.IS_OFFLINE_COL, product.is_offline)
                            put(DBHandler.INDIVIDUAL_CUSTOMER_ID_FK_COL, product.individual_customer_id)
                            put(DBHandler.ORGANISATION_CUSTOMER_ID_FK_COL, product.organisation_customer_id)
                        }

                        val productResult = db.insertWithOnConflict(DBHandler.PRODUCT_TABLE_NAME, null, productValues, SQLiteDatabase.CONFLICT_REPLACE)
                        if (productResult == -1L) {
                            Log.e("DataSyncManager", "Failed to insert product ${product.id}")
                        }
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting customers", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No customers to insert")
        }
    }

    private fun syncOrganizationalCustomersToLocalDatabase(customers: List<OrganisationalCustomer>) {
        if (customers.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${customers.size} organizational customers to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                customers.forEach { customer ->
                    // Insert organizational customer details into customer table
                    val customerValues = ContentValues().apply {
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_ID_COL, customer.id)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_COMPANY_NAME_COL, customer.company_name)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_CUSTOMER_CODE_COL, customer.customer_code)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_REGISTRATION_NUMBER_COL, customer.registration_number)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_SECTOR_COL, customer.sector)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_DATE_OF_REGISTRATION_COL, customer.date_of_registration)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_EMAIL_COL, customer.email)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_PHONE_NUMBER_COL, customer.phone_number)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_COUNTY_COL, customer.county)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_SUB_COUNTY_COL, customer.sub_county)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_WARD_COL, customer.ward)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_VILLAGE_COL, customer.village)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_OTHER_NAME1_COL, customer.other_name1)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_LAST_NAME1_COL, customer.last_name1)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_ID_NUMBER1_COL, customer.id_number1)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_GENDER1_COL, customer.gender1)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_DATE_OF_BIRTH1_COL, customer.date_of_birth1)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_EMAIL1_COL, customer.email1)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_PHONE_NUMBER1_COL, customer.phone_number1)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_OTHER_NAME2_COL, customer.other_name2)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_LAST_NAME2_COL, customer.last_name2)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_ID_NUMBER2_COL, customer.id_number2)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_GENDER2_COL, customer.gender2)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_DATE_OF_BIRTH2_COL, customer.date_of_birth2)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_EMAIL2_COL, customer.email2)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_PHONE_NUMBER2_COL, customer.phone_number2)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_OTHER_NAME3_COL, customer.other_name3)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_LAST_NAME3_COL, customer.last_name3)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_ID_NUMBER3_COL, customer.id_number3)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_GENDER3_COL, customer.gender3)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_DATE_OF_BIRTH3_COL, customer.date_of_birth3)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_EMAIL3_COL, customer.email3)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_PHONE_NUMBER3_COL, customer.phone_number3)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_USER_AUTHORISED_COL, if (customer.user_authorised) 1 else 0)
                        put(DBHandler.ORGANISATION_CUSTOMER_USER_AUTHORISATION_TOKEN_COL, customer.authorisation_token)
                    }

                    val customerResult = db.insertWithOnConflict(DBHandler.ORGANISATION_CUSTOMER_USER_TABLE_NAME, null, customerValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (customerResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert organizational customer ${customer.id}")
                    }

                    // Insert products into products table
                    customer.products?.forEach { product ->
                        val productValues = ContentValues().apply {
                            put(DBHandler.PRODUCT_ID_COL, product.id)
                            put(DBHandler.PRODUCT_CATEGORY_COL, product.category)
                            put(DBHandler.PRODUCT_PRODUCTS_INTERESTED_IN_COL, product.products_interested_in)
                            put(DBHandler.PRODUCT_VOLUME_IN_KGS_COL, product.volume_in_kgs)
                            put(DBHandler.PRODUCT_PACKAGING_COL, product.packaging)
                            put(DBHandler.PRODUCT_QUALITY_COL, product.quality)
                            put(DBHandler.PRODUCT_FREQUENCY_COL, product.frequency)
                            put(DBHandler.IS_OFFLINE_COL, if (product.is_offline) 1 else 0)
                            put(DBHandler.INDIVIDUAL_CUSTOMER_ID_FK_COL, product.individual_customer_id)
                            put(DBHandler.ORGANISATION_CUSTOMER_ID_FK_COL, product.organisation_customer_id)
                        }

                        val productResult = db.insertWithOnConflict(DBHandler.PRODUCT_TABLE_NAME, null, productValues, SQLiteDatabase.CONFLICT_REPLACE)
                        if (productResult == -1L) {
                            Log.e("DataSyncManager", "Failed to insert product ${product.id}")
                        }
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting organizational customers", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No organizational customers to insert")
        }
    }

    private fun syncTrainingToLocalDatabase(trainings: List<Training>) {
        if (trainings.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${trainings.size} Trainings to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                trainings.forEach { training ->
                    // Convert participants JSON object to a string
                    val participantsJson = Gson().toJson(training.participants)

                    // Insert Training details into Training table
                    val trainingValues = ContentValues().apply {
                        put(DBHandler.TRAINING_ID_COL, training.id)
                        put(DBHandler.TRAINING_COURSE_NAME_COL, training.course_name)
                        put(DBHandler.TRAINING_TRAINER_NAME_COL, training.trainer_name)
                        put(DBHandler.TRAINING_DATE_OF_TRAINING_COL, training.date_of_training)
                        put(DBHandler.TRAINING_CONTENT_OF_TRAINING_COL, training.content_of_training)
                        put(DBHandler.TRAINING_VENUE_COL, training.venue)
                        put(DBHandler.TRAINING_BUYING_CENTER_COL, training.buying_center)
                        put(DBHandler.TRAINING_COURSE_DESCRIPTION_COL, training.course_description)
                        put(DBHandler.TRAINING_PARTICIPANTS_COL, participantsJson) // Store JSON string
                        put(DBHandler.TRAINING_USER_ID_FK_COL, training.user_id)
                        put(DBHandler.IS_OFFLINE_COL, 0) // Default value for is_offline
                    }

                    val trainingResult = db.insertWithOnConflict(DBHandler.TRAINING_TABLE_NAME, null, trainingValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (trainingResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert training ${training.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting Trainings", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No Trainings to insert")
        }
    }

    private fun syncAttendanceToLocalDatabase(attendances: List<Attendance>) {
        if (attendances.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${attendances.size} Attendances to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                attendances.forEach { attendance ->
                    // Insert Attendance details into Attendance table
                    val attendanceValues = ContentValues().apply {
                        put(DBHandler.ATTENDANCE_ID_COL, attendance.id)
                        put(DBHandler.ATTENDANCE_ATTENDANCE_COL, attendance.attendance)
                        put(DBHandler.ATTENDANCE_TRAINING_ID_COL, attendance.training_id)
                        put(DBHandler.ATTENDANCE_USER_ID_FK_COL, attendance.user_id)
                        put(DBHandler.IS_OFFLINE_COL, 0) // Default value for is_offline
                    }

                    val attendanceResult = db.insertWithOnConflict(DBHandler.ATTENDANCE_TABLE_NAME, null, attendanceValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (attendanceResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert attendance ${attendance.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting Attendances", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No Attendances to insert")
        }
    }

    private fun syncFarmerPriceDistributionToLocalDatabase(priceDistributions: List<FarmerPriceDistribution>) {
        if (priceDistributions.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${priceDistributions.size} Farmer Price Distributions to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                priceDistributions.forEach { priceDistribution ->
                    // Insert Farmer Price Distribution details into the table
                    val priceDistributionValues = ContentValues().apply {
                        put(DBHandler.FARMER_PRICE_DISTRIBUTION_ID_COL, priceDistribution.id)
                        put(DBHandler.FARMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL, priceDistribution.produce_id)
                        put(DBHandler.FARMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL, priceDistribution.online_price)
                        put(DBHandler.FARMER_PRICE_DISTRIBUTION_HUB_COL, priceDistribution.hub)
                        put(DBHandler.FARMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL, priceDistribution.buying_center)
                        put(DBHandler.FARMER_PRICE_DISTRIBUTION_UNIT_COL, priceDistribution.unit)
                        put(DBHandler.FARMER_PRICE_DISTRIBUTION_DATE_COL, priceDistribution.date)
                        put(DBHandler.FARMER_PRICE_DISTRIBUTION_COMMENTS_COL, priceDistribution.comments)
                        put(DBHandler.FARMER_PRICE_DISTRIBUTION_SOLD_COL, if (priceDistribution.sold) 1 else 0)
                        put(DBHandler.FARMER_PRICE_DISTRIBUTION_USER_ID_FK_COL, priceDistribution.user_id)
                        put(DBHandler.IS_OFFLINE_COL, 0) // Default value for is_offline
                    }

                    val priceDistributionResult = db.insertWithOnConflict(DBHandler.FARMER_PRICE_DISTRIBUTION_TABLE_NAME, null, priceDistributionValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (priceDistributionResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert Farmer Price Distribution ${priceDistribution.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting Farmer Price Distributions", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No Farmer Price Distributions to insert")
        }
    }

    private fun syncCustomerPriceDistributionToLocalDatabase(priceDistributions: List<CustomerPriceDistribution>) {
        if (priceDistributions.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${priceDistributions.size} Customer Price Distributions to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                priceDistributions.forEach { priceDistribution ->
                    // Insert Customer Price Distribution details into the table
                    val priceDistributionValues = ContentValues().apply {
                        put(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_ID_COL, priceDistribution.id)
                        put(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL, priceDistribution.produce_id)
                        put(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL, priceDistribution.online_price)
                        put(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_HUB_COL, priceDistribution.hub)
                        put(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL, priceDistribution.buying_center)
                        put(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_UNIT_COL, priceDistribution.unit)
                        put(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_DATE_COL, priceDistribution.date)
                        put(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_COMMENTS_COL, priceDistribution.comments)
                        put(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_SOLD_COL, if (priceDistribution.sold) 1 else 0)
                        put(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_USER_ID_FK_COL, priceDistribution.user_id)
                        put(DBHandler.IS_OFFLINE_COL, 0) // Default value for is_offline
                    }

                    val priceDistributionResult = db.insertWithOnConflict(DBHandler.CUSTOMER_PRICE_DISTRIBUTION_TABLE_NAME, null, priceDistributionValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (priceDistributionResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert Customer Price Distribution ${priceDistribution.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting Customer Price Distributions", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No Customer Price Distributions to insert")
        }
    }

    private fun syncSellingToLocalDatabase(sellings: List<BuyingCustomer>) {
        if (sellings.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${sellings.size} Sellings to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                sellings.forEach { selling ->
                    // Convert quality object to a JSON string
                    val qualityJson = Gson().toJson(selling.quality)

                    // Insert Selling details into Selling table
                    val sellingValues = ContentValues().apply {
                        // id is auto-incremented, so it's not included here
                        put(DBHandler.BUYING_CUSTOMER_PRODUCE_COL, selling.produce)
                        put(DBHandler.BUYING_CUSTOMER_CUSTOMER_COL, selling.customer)
                        put(DBHandler.BUYING_CUSTOMER_GRN_NUMBER_COL, selling.grn_number ?: "UNKNOWN") // Provide default for GRN if necessary

                        put(DBHandler.BUYING_CUSTOMER_UNIT_COL, selling.unit)
                        put(DBHandler.BUYING_CUSTOMER_WEIGHT_COL, selling.weight)

                        // *** Provide a default value for action if it's null ***
                        put(DBHandler.BUYING_CUSTOMER_ACTION_COL, selling.action ?: "DEFAULT_ACTION") // Or any other sensible default string

                        put(DBHandler.BUYING_CUSTOMER_ONLINE_PRICE_COL, selling.online_price)
                        put(DBHandler.BUYING_CUSTOMER_LOADED_COL, if (selling.loaded) 1 else 0)
                        put(DBHandler.BUYING_CUSTOMER_USER_ID_FK_COL, selling.user_id)
                        put(DBHandler.BUYING_CUSTOMER_QUALITY_COL, qualityJson)
                        // Assuming these records from the server are not "offline" in the local sense
                        put(DBHandler.IS_OFFLINE_COL, 0)
                    }

                    val sellingResult = db.insertWithOnConflict(DBHandler.BUYING_CUSTOMER_TABLE_NAME, null, sellingValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (sellingResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert Selling ${selling.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting Sellings", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No Sellings to insert")
        }
    }

    private fun syncQuarantineToLocalDatabase(quarantines: List<Quarantine>) {
        if (quarantines.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${quarantines.size} Quarantines to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                quarantines.forEach { quarantine ->
                    // Insert Quarantine details into Quarantine table
                    val quarantineValues = ContentValues().apply {
                        put(DBHandler.QUARANTINE_ID_COL, quarantine.id)
                        put(DBHandler.QUARANTINE_ACTION_COL, quarantine.action)
                        put(DBHandler.QUARANTINE_APPROVED_BY_COL, quarantine.quarantine_approved_by)
                        put(DBHandler.QUARANTINE_NEW_WEIGHT_IN_COL, quarantine.new_weight_in_after_sorting_or_regrading)
                        put(DBHandler.QUARANTINE_NEW_WEIGHT_OUT_COL, quarantine.new_weight_out_after_sorting_or_regrading)
                        put(DBHandler.QUARANTINE_BUYING_FARMER_ID_FK_COL, quarantine.buying_farmer_id)
                        put(DBHandler.QUARANTINE_BUYING_CUSTOMER_ID_FK_COL, quarantine.buying_customer_id)
                    }

                    val quarantineResult = db.insertWithOnConflict(DBHandler.QUARANTINE_TABLE_NAME, null, quarantineValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (quarantineResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert quarantine ${quarantine.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting Quarantines", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No Quarantines to insert")
        }
    }

    private fun syncPaymentToLocalDatabase(payments: List<PaymentFarmer>) {
        if (payments.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${payments.size} Payments to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                payments.forEach { payment ->
                    val paymentValues = ContentValues().apply {
                        put(DBHandler.PAYMENT_FARMER_ID_COL, payment.id)
                        put(DBHandler.PAYMENT_FARMER_BUYING_CENTER_COL, payment.buying_center ?: "UNKNOWN_BUYING_CENTER")
                        put(DBHandler.PAYMENT_FARMER_CIG_COL, payment.cig ?: "UNKNOWN_CIG")
                        put(DBHandler.PAYMENT_FARMER_PRODUCER_COL, payment.producer ?: "UNKNOWN_PRODUCER")
                        put(DBHandler.PAYMENT_FARMER_GRN_COL, payment.grn ?: "UNKNOWN_GRN") // Ensure this has a default
                        put(DBHandler.PAYMENT_FARMER_NET_BALANCE_COL, payment.net_balance)
                        put(DBHandler.PAYMENT_FARMER_PAYMENT_TYPE_COL, payment.payment_type ?: "UNKNOWN_PAYMENT_TYPE") // Added default
                        put(DBHandler.PAYMENT_FARMER_OUTSTANDING_LOAN_AMOUNT_COL, payment.outstanding_loan_amount)
                        put(DBHandler.PAYMENT_FARMER_PAYMENT_DUE_COL, payment.payment_due)
                        put(DBHandler.PAYMENT_FARMER_SET_LOAN_DEDUCTION_COL, payment.set_loan_deduction)
                        put(DBHandler.PAYMENT_FARMER_NET_BALANCE_BEFORE_COL, payment.net_balance_before)
                        put(DBHandler.PAYMENT_FARMER_NET_BALANCE_AFTER_LOAN_DEDUCTION_COL, payment.net_balance_after_loan_deduction)
                        put(DBHandler.PAYMENT_FARMER_COMMENT_COL, payment.comment)
                        put(DBHandler.PAYMENT_FARMER_USER_ID_FK_COL, payment.user_id)
                        put(DBHandler.IS_OFFLINE_COL, 0)
                    }

                    val paymentResult = db.insertWithOnConflict(DBHandler.PAYMENT_FARMER_TABLE_NAME, null, paymentValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (paymentResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert payment ${payment.id}")
                    }
                }
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting Payments for Farmers", e) // Clarified log
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No Farmer Payments to insert") // Clarified log
        }
    }
    private fun syncReceivePaymentToLocalDatabase(payments: List<PaymentCustomer>) {
        if (payments.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${payments.size} Received Customer Payments to local database.") // Clarified log

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                payments.forEach { payment ->
                    val paymentValues = ContentValues().apply {
                        put(DBHandler.PAYMENT_CUSTOMER_ID_COL, payment.id)
                        put(DBHandler.PAYMENT_CUSTOMER_VILLAGE_OR_ESTATE_COL, payment.village_or_estate ?: "UNKNOWN_VILLAGE_OR_ESTATE")
                        put(DBHandler.PAYMENT_CUSTOMER_CUSTOMER_COL, payment.customer ?: "UNKNOWN_CUSTOMER")
                        put(DBHandler.PAYMENT_CUSTOMER_GRN_COL, payment.grn ?: "UNKNOWN_GRN")
                        put(DBHandler.PAYMENT_CUSTOMER_AMOUNT_COL, payment.amount)
                        put(DBHandler.PAYMENT_CUSTOMER_NET_BALANCE_COL, payment.net_balance)
                        put(DBHandler.PAYMENT_CUSTOMER_PAYMENT_TYPE_COL, payment.payment_type ?: "UNKNOWN_PAYMENT_TYPE") // Applied fix
                        put(DBHandler.PAYMENT_CUSTOMER_ENTER_AMOUNT_COL, payment.enter_amount)
                        put(DBHandler.PAYMENT_CUSTOMER_NET_BALANCE_BEFORE_COL, payment.net_balance_before)
                        put(DBHandler.PAYMENT_CUSTOMER_NET_BALANCE_AFTER_COL, payment.net_balance_after)
                        put(DBHandler.PAYMENT_CUSTOMER_COMMENT_COL, payment.comment)
                        put(DBHandler.PAYMENT_CUSTOMER_USER_ID_FK_COL, payment.user_id)
                        put(DBHandler.IS_OFFLINE_COL, 0)
                    }

                    val paymentResult = db.insertWithOnConflict(DBHandler.PAYMENT_CUSTOMER_TABLE_NAME, null, paymentValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (paymentResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert PaymentCustomer ${payment.id}")
                    }
                }
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting PaymentCustomers", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No PaymentCustomers to insert")
        }
    }
    private fun syncRuralWorkersToLocalDatabase(ruralWorkers: List<RuralWorker>) {
        if (ruralWorkers.isNotEmpty()) {
            Log.d("DataSyncManager", "Syncing ${ruralWorkers.size} Rural Workers to local database.")

            val db = dbHandler.writableDatabase
            db.beginTransaction()
            try {
                ruralWorkers.forEach { worker ->
                    // Insert RuralWorker details into RuralWorker table
                    val workerValues = ContentValues().apply {
                        put(DBHandler.RURAL_WORKER_ID_COL, worker.id)
                        put(DBHandler.RURAL_WORKER_OTHER_NAME_COL, worker.other_name)
                        put(DBHandler.RURAL_WORKER_LAST_NAME_COL, worker.last_name)
                        put(DBHandler.RURAL_WORKER_RURAL_WORKER_CODE_COL, worker.rural_worker_code)
                        put(DBHandler.RURAL_WORKER_ID_NUMBER_COL, worker.id_number)
                        put(DBHandler.RURAL_WORKER_GENDER_COL, worker.gender)
                        put(DBHandler.RURAL_WORKER_DATE_OF_BIRTH_COL, worker.date_of_birth)
                        put(DBHandler.RURAL_WORKER_EMAIL_COL, worker.email)
                        put(DBHandler.RURAL_WORKER_PHONE_NUMBER_COL, worker.phone_number)
                        put(DBHandler.RURAL_WORKER_EDUCATION_LEVEL_COL, worker.education_level)
                        put(DBHandler.RURAL_WORKER_SERVICE_COL, worker.service)
                        put(DBHandler.RURAL_WORKER_OTHER_COL, worker.other)
                        put(DBHandler.RURAL_WORKER_COUNTY_COL, worker.county)
                        put(DBHandler.RURAL_WORKER_SUB_COUNTY_COL, worker.sub_county)
                        put(DBHandler.RURAL_WORKER_WARD_COL, worker.ward)
                        put(DBHandler.RURAL_WORKER_VILLAGE_COL, worker.village)
                        put(DBHandler.RURAL_WORKER_USER_ID_FK_COL, worker.user_id)
                    }

                    val workerResult = db.insertWithOnConflict(DBHandler.RURAL_WORKER_TABLE_NAME, null, workerValues, SQLiteDatabase.CONFLICT_REPLACE)
                    if (workerResult == -1L) {
                        Log.e("DataSyncManager", "Failed to insert worker ${worker.id}")
                    }
                }

                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DataSyncManager", "Error inserting Rural Workers", e)
            } finally {
                db.endTransaction()
            }
        } else {
            Log.d("DataSyncManager", "No Rural Workers to insert")
        }
    }

    private fun clearTables(db: SQLiteDatabase) {
        try {
            // Clear tables that have the is_offline column
            db.execSQL("DELETE FROM HUB_TABLE_NAME WHERE is_offline = 0 OR is_offline IS NULL")
            db.execSQL("DELETE FROM BUYING_CENTER_TABLE_NAME WHERE is_offline = 0 OR is_offline IS NULL")
            db.execSQL("DELETE FROM KEY_CONTACTS_TABLE_NAME WHERE is_offline = 0 OR is_offline IS NULL")

            // Clear tables that do not have the is_offline column
            db.execSQL("DELETE FROM usersTableName")
            db.execSQL("DELETE FROM table_without_is_offline_column_1")
            db.execSQL("DELETE FROM table_without_is_offline_column_2")

            // Add more tables without is_offline column as needed
        } catch (e: SQLiteException) {
            // Log.e("DataSyncManager", "Error clearing tables", e)
        }
    }
}

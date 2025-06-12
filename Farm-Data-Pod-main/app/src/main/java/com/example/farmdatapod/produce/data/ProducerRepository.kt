package com.example.farmdatapod.produce.data


import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.ProducerBiodataRequest
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.produce.indipendent.biodata.ProduceItem
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class ProducerRepository(private val context: Context) : SyncableRepository {
    private val TAG = "ProducerRepository"
    private val producerDao = AppDatabase.getInstance(context).producerDao()
    private val apiService = RestClient.getApiService(context)

    suspend fun saveProducer(producer: ProducerEntity, isOnline: Boolean): Result<ProducerEntity> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to save producer: ${producer.otherName}, Online mode: $isOnline")

                // Check if producer already exists by farmer code
                val existingProducer = producer.farmerCode?.let {
                    producerDao.getProducerByFarmerCode(it)
                }

                val localId = if (existingProducer != null) {
                    // Update existing producer
                    producerDao.updateProducer(producer.copy(
                        id = existingProducer.id,
                        syncStatus = false
                    ))
                    existingProducer.id.toLong()
                } else {
                    // Insert new producer
                    producerDao.insertProducer(producer)
                }

                Log.d(TAG, "Producer ${if (existingProducer != null) "updated" else "saved"} locally with ID: $localId")

                if (isOnline) {
                    try {
                        Log.d(TAG, "Attempting to sync with server...")
                        val producerRequest = createProducerRequest(producer)
                        val response = apiService.postProducerBiodata(producerRequest).execute()

                        if (response.isSuccessful) {
                            response.body()?.let { serverProducer ->
                                // Note: The response for postProducerBiodata is currently Call<Unit>, meaning no body is returned.
                                // If the API actually returns the server ID, you would parse it here.
                                // For now, we'll assume success means it's synced.
                                producerDao.updateSyncStatus(localId, true)
                                Log.d(TAG, "Producer synced successfully with server ")
                            }
                        } else {
                            Log.e(TAG, "Server sync failed: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during server sync", e)
                    }
                } else {
                    Log.d(TAG, "Offline mode - Producer will sync later")
                }
                Result.success(producer)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving producer", e)
                Result.failure(e)
            }
        }


    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedProducers = producerDao.getUnsyncedProducers()
            Log.d(TAG, "Found ${unsyncedProducers.size} unsynced producers")

            var successCount = 0
            var failureCount = 0

            unsyncedProducers.forEach { producer ->
                try {
                    val producerRequest = createProducerRequest(producer)
                    val response = apiService.postProducerBiodata(producerRequest).execute()

                    if (response.isSuccessful) {
                        // Assuming postProducerBiodata doesn't return the server ID for now.
                        // If it did, you'd update producer.serverId here.
                        producerDao.updateSyncStatus(producer.id.toLong(), true) // Use producer.id for local ID
                        successCount++
                        Log.d(TAG, "Successfully synced ${producer.otherName}")
                    } else {
                        failureCount++
                        Log.e(TAG, "Failed to sync ${producer.otherName}: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing ${producer.otherName}", e)
                }
            }

            Result.success(SyncStats(successCount, failureCount, 0, successCount > 0))
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            Result.failure(e)
        }
    }

    override suspend fun syncFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var savedCount = 0

            // Clean duplicates first
            withContext(Dispatchers.IO) {
                try {
                    val duplicates = producerDao.findDuplicateProducers()
                    if (duplicates.isNotEmpty()) {
                        Log.d(TAG, "Found ${duplicates.size} producers with duplicates")
                        duplicates.forEach { dupInfo ->
                            Log.d(TAG, "Producer ${dupInfo.farmerCode} has ${dupInfo.count} duplicates")
                        }

                        val deletedCount = producerDao.deleteDuplicateProducers()
                        Log.d(TAG, "Cleaned $deletedCount duplicate records")
                    } else {
                        Log.d(TAG, "No duplicate producers found")

                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning duplicates", e)
                }
            }

            Log.d(TAG, "Starting GET request to fetch producers")
            val response = apiService.getProducersBiodata(null)?.execute()

            when (response?.code()) {
                200 -> {
                    val responseBody = response.body()?.string()
                    if (responseBody == null) {
                        Log.d(TAG, "Server returned empty response")
                        return@withContext Result.success(0)
                    }

                    try {
                        val gson = Gson()
                        val type = object : TypeToken<List<ProducerBiodataRequest>>() {}.type
                        val serverProducers: List<ProducerBiodataRequest> = gson.fromJson(responseBody, type)
                        Log.d(TAG, "Received ${serverProducers.size} producers from server")

                        // Process in transaction
                        AppDatabase.getInstance(context).runInTransaction {
                            // Get distinct producers by farmer code
                            val distinctProducers = serverProducers
                                .distinctBy { it.farmer_code }
                                .filterNot { it.farmer_code.isNullOrBlank() }

                            Log.d(TAG, "Processing ${distinctProducers.size} distinct producers")

                            distinctProducers.forEach { serverProducer ->
                                try {
                                    val localProducer = convertToLocalProducer(serverProducer)
                                    val existingProducer = serverProducer.farmer_code?.let {
                                        runBlocking { producerDao.getProducerByFarmerCode(it) }
                                    }

                                    if (existingProducer != null) {
                                        if (hasProducerChanged(existingProducer, localProducer)) {
                                            runBlocking {
                                                producerDao.updateProducer(localProducer.copy(
                                                    id = existingProducer.id,
                                                    syncStatus = true,
                                                    userId = existingProducer.userId, // Preserve local userId
                                                    producerType = existingProducer.producerType // Preserve local producerType
                                                ))
                                            }
                                            Log.d(TAG, "Updated producer: FarmerCode=${serverProducer.farmer_code}")
                                            savedCount++
                                        }
                                    } else {
                                        runBlocking {
                                            producerDao.insertProducer(localProducer.copy(syncStatus = true))
                                        }
                                        Log.d(TAG, "Inserted new producer: FarmerCode=${serverProducer.farmer_code}")
                                        savedCount++
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing producer ${serverProducer.other_name}", e)
                                }
                            }
                        }

                        Result.success(savedCount)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing server response", e)
                        Result.failure(e)
                    }
                }
                401, 403 -> {
                    Log.e(TAG, "Authentication error: ${response.code()}")
                    Result.failure(Exception("Authentication error"))
                }
                else -> {
                    Log.e(TAG, "Unexpected response: ${response?.code()}")
                    Result.failure(Exception("Server error"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Result.failure(e)
        }
    }
    private fun hasProducerChanged(existing: ProducerEntity, new: ProducerEntity): Boolean {
        return existing.otherName != new.otherName ||
                existing.lastName != new.lastName ||
                existing.phoneNumber != new.phoneNumber ||
                existing.email != new.email ||
                existing.hub != new.hub ||
                existing.buyingCenter != new.buyingCenter ||
                existing.totalLandSize != new.totalLandSize ||
                existing.cultivatedLandSize != new.cultivatedLandSize ||
                existing.location != new.location ||
                existing.county != new.county ||
                existing.subCounty != new.subCounty ||
                existing.ward != new.ward ||
                existing.village != new.village ||
                existing.cropList != new.cropList ||
                existing.accessToIrrigation != new.accessToIrrigation ||
                existing.bankName != new.bankName || // Added
                existing.bankAccountNumber != new.bankAccountNumber || // Added
                existing.bankAccountHolder != new.bankAccountHolder || // Added
                existing.mobileMoneyProvider != new.mobileMoneyProvider || // Added
                existing.mobileMoneyNumber != new.mobileMoneyNumber // Added
    }

    private fun convertToLocalProducer(serverProducer: ProducerBiodataRequest): ProducerEntity {
        return ProducerEntity(
            serverId = serverProducer.id,
            otherName = serverProducer.other_name ?: "",
            lastName = serverProducer.last_name ?: "",
            idNumber = serverProducer.id_number,
            farmerCode = serverProducer.farmer_code ?: "",
            email = serverProducer.email,
            phoneNumber = serverProducer.phone_number ?: "",
            location = serverProducer.location ?: "",
            dateOfBirth = serverProducer.date_of_birth ?: "",
            gender = serverProducer.gender ?: "",
            educationLevel = serverProducer.education_level ?: "",
            county = serverProducer.county ?: "",
            subCounty = serverProducer.sub_county ?: "",
            ward = serverProducer.ward ?: "",
            village = serverProducer.village ?: "",
            hub = serverProducer.hub ?: "",
            buyingCenter = serverProducer.buying_center ?: "",
            totalLandSize = serverProducer.total_land_size?.replace("ha", "")?.trim()?.toFloatOrNull() ?: 0f,
            cultivatedLandSize = serverProducer.cultivate_land_size?.replace("ha", "")?.trim()?.toFloatOrNull() ?: 0f,
            uncultivatedLandSize = serverProducer.uncultivated_land_size?.replace("ha", "")?.trim()?.toFloatOrNull() ?: 0f,
            homesteadSize = serverProducer.homestead_size?.replace("ha", "")?.trim()?.toFloatOrNull() ?: 0f,
            farmAccessibility = serverProducer.farm_accessibility ?: "",
            cropList = serverProducer.crop_list ?: "",
            accessToIrrigation = serverProducer.access_to_irrigation ?: "",
            extensionServices = serverProducer.farmer_interest_in_extension ?: "",
            familyLabor = serverProducer.number_of_family_workers?.toIntOrNull() ?: 0,
            hiredLabor = serverProducer.number_of_hired_workers?.toIntOrNull() ?: 0,
            dairyCattle = serverProducer.dairy_cattle?.toIntOrNull() ?: 0,
            beefCattle = serverProducer.beef_cattle?.toIntOrNull() ?: 0,
            sheep = serverProducer.sheep?.toIntOrNull() ?: 0,
            goats = serverProducer.goats?.toIntOrNull() ?: 0,
            pigs = serverProducer.pigs?.toIntOrNull() ?: 0,
            poultry = serverProducer.poultry?.toIntOrNull() ?: 0,
            camels = serverProducer.camels?.toIntOrNull() ?: 0,
            aquaculture = if (serverProducer.aquaculture == "Yes") 1 else 0,
            rabbits = serverProducer.rabbits?.toIntOrNull() ?: 0,
            beehives = serverProducer.beehives?.toIntOrNull() ?: 0,
            donkeys = serverProducer.donkeys?.toIntOrNull() ?: 0,
            knowledgeRelated = serverProducer.knowledge_related ?: "",
            qualityRelated = serverProducer.quality_related ?: "",
            soilRelated = serverProducer.soil_related ?: "",
            marketRelated = serverProducer.market_related ?: "",
            compostRelated = serverProducer.compost_related ?: "",
            foodLossRelated = serverProducer.food_loss_related ?: "",
            nutritionRelated = serverProducer.nutrition_related ?: "",
            financeRelated = serverProducer.finance_related ?: "",
            pestsRelated = serverProducer.pests_related ?: "",
            weatherRelated = serverProducer.weather_related ?: "",
            diseaseRelated = serverProducer.disease_related ?: "",
            housingType = serverProducer.housing_type,
            housingFloor = serverProducer.housing_floor,
            housingRoof = serverProducer.housing_roof,
            lightingFuel = serverProducer.lighting_fuel,
            cookingFuel = serverProducer.cooking_fuel,
            waterFilter = serverProducer.water_filter,
            waterTank = serverProducer.water_tank_greater_than_5000lts,
            handWashingFacilities = serverProducer.hand_washing_facilities,
            ppes = serverProducer.ppes,
            waterWell = serverProducer.water_well_or_weir,
            irrigationPump = serverProducer.irrigation_pump,
            harvestingEquipment = serverProducer.harvesting_equipment,
            transportationType = serverProducer.transportation_type,
            toiletFloor = serverProducer.toilet_floor,
            userId = "", // Set from app context
            syncStatus = true,
            producerType = "", // Set based on app logic
            primaryProducer = serverProducer.primary_producer,
            marketProduceList = serverProducer.commercialProduces?.map {
                ProduceItem(
                    name = it["product"] as? String ?: "",
                    unit = it["product_category"] as? String ?: "",
                    quantity = it["acerage"] as? String ?: ""
                )
            },
            ownConsumptionList = serverProducer.domesticProduces?.map {
                ProduceItem(
                    name = it["product"] as? String ?: "",
                    unit = it["product_category"] as? String ?: "",
                    quantity = it["acerage"] as? String ?: ""
                )
            },
            bankName = serverProducer.bank_name, // Added
            bankAccountNumber = serverProducer.bank_account_number, // Added
            bankAccountHolder = serverProducer.bank_account_holder, // Added
            mobileMoneyProvider = serverProducer.mobile_money_provider, // Added
            mobileMoneyNumber = serverProducer.mobile_money_number // Added
        )
    }

    private fun createProducerRequest(producer: ProducerEntity): ProducerBiodataRequest {
        // Helper function to convert date from "dd/MM/yyyy" to "yyyy-MM-dd'T'HH:mm:ss"
        fun formatServerDate(dateString: String): String {
            if (dateString.isBlank()) return ""
            return try {
                val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: ""
            } catch (e: Exception) {
                // If format is already correct or something else, return as is.
                // Consider logging this error.
                Log.e(TAG, "Could not parse date: $dateString", e)
                "" // Or return dateString if you expect it might already be in correct format
            }
        }

        return ProducerBiodataRequest(
            id = producer.serverId,
            other_name = producer.otherName,
            last_name = producer.lastName,
            id_number = producer.idNumber, // Convert to Long
            farmer_code = producer.farmerCode,
            date_of_birth = formatServerDate(producer.dateOfBirth), // Format the date
            email = producer.email,
            phone_number = producer.phoneNumber,
            location = producer.location,
            hub = producer.hub,
            buying_center = producer.buyingCenter,
            gender = producer.gender,
            education_level = producer.educationLevel,
            county = producer.county,
            sub_county = producer.subCounty,
            ward = producer.ward,
            village = producer.village,
            primary_producer = producer.primaryProducer,
            total_land_size = "${producer.totalLandSize} ha", // Append unit
            cultivate_land_size = "${producer.cultivatedLandSize} ha", // Append unit
            homestead_size = "${producer.homesteadSize} ha", // Append unit
            uncultivated_land_size = "${producer.uncultivatedLandSize} ha", // Append unit
            farm_accessibility = producer.farmAccessibility,
            number_of_family_workers = producer.familyLabor.toString(),
            number_of_hired_workers = producer.hiredLabor.toString(),
            farmer_interest_in_extension = producer.extensionServices,
            access_to_irrigation = producer.accessToIrrigation,
            crop_list = producer.cropList,
            knowledge_related = producer.knowledgeRelated,
            soil_related = producer.soilRelated,
            compost_related = producer.compostRelated,
            nutrition_related = producer.nutritionRelated,
            pests_related = producer.pestsRelated,
            disease_related = producer.diseaseRelated,
            quality_related = producer.qualityRelated,
            market_related = producer.marketRelated,
            food_loss_related = producer.foodLossRelated,
            finance_related = producer.financeRelated,
            weather_related = producer.weatherRelated,
            dairy_cattle = producer.dairyCattle.toString(),
            beef_cattle = producer.beefCattle.toString(),
            sheep = producer.sheep.toString(),
            goats = producer.goats.toString(),
            pigs = producer.pigs.toString(),
            poultry = producer.poultry.toString(),
            camels = producer.camels.toString(),
            aquaculture = producer.aquaculture.toString(), // Send as number string "1" or "0"
            rabbits = producer.rabbits.toString(),
            beehives = producer.beehives.toString(),
            donkeys = producer.donkeys.toString(),
            housing_type = producer.housingType,
            housing_floor = producer.housingFloor,
            housing_roof = producer.housingRoof,
            lighting_fuel = producer.lightingFuel,
            cooking_fuel = producer.cookingFuel,
            water_filter = producer.waterFilter,
            water_tank_greater_than_5000lts = producer.waterTank,
            hand_washing_facilities = producer.handWashingFacilities,
            ppes = producer.ppes,
            water_well_or_weir = producer.waterWell,
            irrigation_pump = producer.irrigationPump,
            harvesting_equipment = producer.harvestingEquipment,
            transportation_type = producer.transportationType,
            toilet_floor = producer.toiletFloor,
            commercialProduces = producer.marketProduceList?.map {
                mapOf(
                    "product" to it.name,
                    "product_category" to it.unit,
                    "acreage" to it.quantity // FIX: Corrected typo "acerage" to "acreage"
                )
            } ?: emptyList(),
            domesticProduces = producer.ownConsumptionList?.map {
                mapOf(
                    "product" to it.name,
                    "product_category" to it.unit,
                    "acreage" to it.quantity // FIX: Corrected typo "acerage" to "acreage"
                )
            } ?: emptyList(),
            bank_name = producer.bankName, // Added
            bank_account_number = producer.bankAccountNumber, // Added
            bank_account_holder = producer.bankAccountHolder, // Added
            mobile_money_provider = producer.mobileMoneyProvider, // Added
            mobile_money_number = producer.mobileMoneyNumber // Added
        )
    }

    override suspend fun performFullSync(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val uploadResult = syncUnsynced()
            val downloadResult = syncFromServer()

            Result.success(
                SyncStats(
                    uploadedCount = uploadResult.getOrNull()?.uploadedCount ?: 0,
                    uploadFailures = uploadResult.getOrNull()?.uploadFailures ?: 0,
                    downloadedCount = downloadResult.getOrNull() ?: 0,
                    successful = uploadResult.isSuccess && downloadResult.isSuccess
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Local database operations
    fun getAllProducers(): Flow<List<ProducerEntity>> = producerDao.getAllProducers()

    suspend fun getProducerById(id: Int): ProducerEntity? = withContext(Dispatchers.IO) {
        producerDao.getProducerById(id)
    }

    suspend fun getProducersByHub(hubId: Int): List<ProducerEntity> = withContext(Dispatchers.IO) {
        producerDao.getProducersByHub(hubId)
    }

    suspend fun getProducersByType(producerType: String): List<ProducerEntity> =
        withContext(Dispatchers.IO) {
            producerDao.getProducersByType(producerType)
        }

    suspend fun searchProducers(query: String): List<ProducerEntity> = withContext(Dispatchers.IO) {
        producerDao.searchProducers(query)
    }
}
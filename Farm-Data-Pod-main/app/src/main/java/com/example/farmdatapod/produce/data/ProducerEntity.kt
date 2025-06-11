package com.example.farmdatapod.produce.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.farmdatapod.produce.indipendent.biodata.ProduceItem


@Entity(tableName = "producers")
data class ProducerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,

    // Basic Info
    @ColumnInfo(name = "other_name")
    val otherName: String,

    @ColumnInfo(name = "last_name")
    val lastName: String,

    @ColumnInfo(name = "id_number")
    val idNumber: String,

    @ColumnInfo(name = "farmer_code")
    val farmerCode: String,

    @ColumnInfo(name = "email")
    val email: String?,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "location")
    val location: String,

    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: String,

    @ColumnInfo(name = "gender")
    val gender: String,

    @ColumnInfo(name = "education_level")
    val educationLevel: String,

    // Location Info
    @ColumnInfo(name = "county")
    val county: String,

    @ColumnInfo(name = "sub_county")
    val subCounty: String,

    @ColumnInfo(name = "ward")
    val ward: String,

    @ColumnInfo(name = "village")
    val village: String,

    @ColumnInfo(name = "hub")
    val hub: String,

    @ColumnInfo(name = "buying_center")
    val buyingCenter: String,

    // Land Information
    @ColumnInfo(name = "total_land_size")
    val totalLandSize: Float,

    @ColumnInfo(name = "cultivated_land_size")
    val cultivatedLandSize: Float,

    @ColumnInfo(name = "uncultivated_land_size")
    val uncultivatedLandSize: Float,

    @ColumnInfo(name = "homestead_size")
    val homesteadSize: Float,

    @ColumnInfo(name = "farm_accessibility")
    val farmAccessibility: String,

    // Farming Details
    @ColumnInfo(name = "crop_list")
    val cropList: String,

    @ColumnInfo(name = "access_to_irrigation")
    val accessToIrrigation: String,

    @ColumnInfo(name = "extension_services")
    val extensionServices: String,

    // Labor
    @ColumnInfo(name = "family_labor")
    val familyLabor: Int,

    @ColumnInfo(name = "hired_labor")
    val hiredLabor: Int,

    // Livestock
    @ColumnInfo(name = "dairy_cattle")
    val dairyCattle: Int,

    @ColumnInfo(name = "beef_cattle")
    val beefCattle: Int,

    @ColumnInfo(name = "sheep")
    val sheep: Int,

    @ColumnInfo(name = "goats")
    val goats: Int,

    @ColumnInfo(name = "pigs")
    val pigs: Int,

    @ColumnInfo(name = "poultry")
    val poultry: Int,

    @ColumnInfo(name = "camels")
    val camels: Int,

    @ColumnInfo(name = "aquaculture")
    val aquaculture: Int,

    @ColumnInfo(name = "rabbits")
    val rabbits: Int,

    @ColumnInfo(name = "beehives")
    val beehives: Int,

    @ColumnInfo(name = "donkeys")
    val donkeys: Int,

    // Challenges
    @ColumnInfo(name = "knowledge_related")
    val knowledgeRelated: String,

    @ColumnInfo(name = "quality_related")
    val qualityRelated: String,

    @ColumnInfo(name = "soil_related")
    val soilRelated: String,

    @ColumnInfo(name = "market_related")
    val marketRelated: String,

    @ColumnInfo(name = "compost_related")
    val compostRelated: String,

    @ColumnInfo(name = "food_loss_related")
    val foodLossRelated: String,

    @ColumnInfo(name = "nutrition_related")
    val nutritionRelated: String,

    @ColumnInfo(name = "finance_related")
    val financeRelated: String,

    @ColumnInfo(name = "pests_related")
    val pestsRelated: String,

    @ColumnInfo(name = "weather_related")
    val weatherRelated: String,

    @ColumnInfo(name = "disease_related")
    val diseaseRelated: String,

    // Infrastructure
    @ColumnInfo(name = "housing_type")
    val housingType: String?,

    @ColumnInfo(name = "housing_floor")
    val housingFloor: String?,

    @ColumnInfo(name = "housing_roof")
    val housingRoof: String?,

    @ColumnInfo(name = "lighting_fuel")
    val lightingFuel: String?,

    @ColumnInfo(name = "cooking_fuel")
    val cookingFuel: String?,

    @ColumnInfo(name = "water_filter")
    val waterFilter: String?,

    @ColumnInfo(name = "water_tank")
    val waterTank: String?,

    @ColumnInfo(name = "hand_washing_facilities")
    val handWashingFacilities: String?,

    @ColumnInfo(name = "ppes")
    val ppes: String?,

    @ColumnInfo(name = "water_well")
    val waterWell: String?,

    @ColumnInfo(name = "irrigation_pump")
    val irrigationPump: String?,

    @ColumnInfo(name = "harvesting_equipment")
    val harvestingEquipment: String?,

    @ColumnInfo(name = "transportation_type")
    val transportationType: String?,

    @ColumnInfo(name = "toilet_floor")
    val toiletFloor: String?,

    // NEW FINANCIAL FIELDS (added based on user's request format)
    @ColumnInfo(name = "bank_name")
    val bankName: String?,

    @ColumnInfo(name = "bank_account_number")
    val bankAccountNumber: String?,

    @ColumnInfo(name = "bank_account_holder")
    val bankAccountHolder: String?,

    @ColumnInfo(name = "mobile_money_provider")
    val mobileMoneyProvider: String?,

    @ColumnInfo(name = "mobile_money_number")
    val mobileMoneyNumber: String?,

    // System fields
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "sync_status")
    val syncStatus: Boolean = false,

    @ColumnInfo(name = "producer_type")
    val producerType: String,
    @ColumnInfo(name = "primary_producer")
    val primaryProducer: List<Map<String, Any>>?,

    @ColumnInfo(name = "market_produce_list")
    val marketProduceList: List<ProduceItem>?,

    @ColumnInfo(name = "own_consumption_list")
    val ownConsumptionList: List<ProduceItem>?,

    // For relationships
    @ColumnInfo(name = "hub_id")
    val hubId: Int? = null,

    @ColumnInfo(name = "buying_center_id")
    val buyingCenterId: Int? = null
)
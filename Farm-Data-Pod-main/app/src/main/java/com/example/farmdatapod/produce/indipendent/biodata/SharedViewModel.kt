package com.example.farmdatapod.produce.indipendent.biodata

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.data.ProducerRepository
import com.example.farmdatapod.utils.NetworkUtils
import com.example.farmdatapod.utils.SharedPrefs
import kotlinx.coroutines.launch

class SharedViewModel : ViewModel() {

    // Producer biodata fields
    private val _otherName = MutableLiveData<String>()
    val otherName: LiveData<String> get() = _otherName

    private val _lastName = MutableLiveData<String>()
    val lastName: LiveData<String> get() = _lastName

    private val _idNumber = MutableLiveData<Long>()
    val idNumber: LiveData<Long> get() = _idNumber

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> get() = _email

    private val _phone = MutableLiveData<String>()
    val phone: LiveData<String> get() = _phone

    private val _location = MutableLiveData<String>()
    val location: LiveData<String> get() = _location

    private val _educationLevel = MutableLiveData<String>()
    val educationLevel: LiveData<String> get() = _educationLevel

    private val _dateOfBirth = MutableLiveData<String>()
    val dateOfBirth: LiveData<String> get() = _dateOfBirth

    private val _county = MutableLiveData<String>()
    val county: LiveData<String> get() = _county

    private val _subCounty = MutableLiveData<String>()
    val subCounty: LiveData<String> get() = _subCounty

    private val _ward = MutableLiveData<String>()
    val ward: LiveData<String> get() = _ward

    private val _village = MutableLiveData<String>()
    val village: LiveData<String> get() = _village

    private val _hub = MutableLiveData<String>()
    val hub: LiveData<String> get() = _hub

    private val _buyingCenter = MutableLiveData<String>()
    val buyingCenter: LiveData<String> get() = _buyingCenter

    // Baseline information fields
    private val _totalLandSize = MutableLiveData<Float>()
    val totalLandSize: LiveData<Float> get() = _totalLandSize

    private val _uncultivatedLandSize = MutableLiveData<Float>()
    val uncultivatedLandSize: LiveData<Float> get() = _uncultivatedLandSize

    private val _cultivatedLandSize = MutableLiveData<Float>()
    val cultivatedLandSize: LiveData<Float> get() = _cultivatedLandSize

    private val _homesteadSize = MutableLiveData<Float>()
    val homesteadSize: LiveData<Float> get() = _homesteadSize

    private val _farmAccessibility = MutableLiveData<String>()
    val farmAccessibility: LiveData<String> get() = _farmAccessibility

    private val _cropList = MutableLiveData<String>()
    val cropList: LiveData<String> get() = _cropList

    private val _accessToIrrigation = MutableLiveData<String>()
    val accessToIrrigation: LiveData<String> get() = _accessToIrrigation

    private val _extensionServices = MutableLiveData<String>()
    val extensionServices: LiveData<String> get() = _extensionServices

    private val _livestockData = MutableLiveData<Map<String, Int>>()
    val livestockData: LiveData<Map<String, Int>> get() = _livestockData

    private val _gender = MutableLiveData<String>()
    val gender: LiveData<String> get() = _gender

    private val _primaryProducer = MutableLiveData<List<Map<String, Any>>>()
    val primaryProducer: LiveData<List<Map<String, Any>>> get() = _primaryProducer

    // Setters for producer biodata
    fun setFirstName(value: String) {
        _otherName.value = value
    }

    fun setLastName(value: String) {
        _lastName.value = value
    }

    fun setIdNumber(value: Long) {
        _idNumber.value = value
    }

    fun setEmail(value: String) {
        _email.value = value
    }

    fun setPhone(value: String) {
        _phone.value = value
    }

    fun setLocation(value: String) {
        _location.value = value
    }

    fun setEducationLevel(value: String) {
        _educationLevel.value = value
    }

    fun setDateOfBirth(value: String) {
        _dateOfBirth.value = value
    }

    fun setCounty(value: String) {
        _county.value = value
    }

    fun setSubCounty(value: String) {
        _subCounty.value = value
    }

    fun setWard(value: String) {
        _ward.value = value
    }

    fun setVillage(value: String) {
        _village.value = value
    }

    fun setHub(value: String) {
        _hub.value = value
        Log.d("SharedViewModel", "Hub value set to: $value")
    }

    fun setBuyingCenter(value: String) {
        _buyingCenter.value = value
        Log.d("SharedViewModel", "Buying Center value set to: $value")
    }

    // Setters for baseline information
    fun setTotalLandSize(value: Float) {
        _totalLandSize.value = value
    }

    fun setUncultivatedLandSize(value: Float) {
        _uncultivatedLandSize.value = value
    }

    fun setCultivatedLandSize(value: Float) {
        _cultivatedLandSize.value = value
    }

    fun setHomesteadSize(value: Float) {
        _homesteadSize.value = value
    }

    fun setFarmAccessibility(value: String) {
        _farmAccessibility.value = value
    }

    fun setCropList(value: String) {
        _cropList.value = value
    }

    fun setAccessToIrrigation(value: String) {
        _accessToIrrigation.value = value
    }

    fun setExtensionServices(value: String) {
        _extensionServices.value = value
    }

    fun setGender(gender: String) {
        _gender.value = gender
    }

    fun setPrimaryProducer(data: List<Map<String, Any>>) {
        _primaryProducer.value = data
    }

    // Labor and Challenges fields
    private val _familyLabor = MutableLiveData<Int>()
    val familyLabor: LiveData<Int> get() = _familyLabor

    private val _hiredLabor = MutableLiveData<Int>()
    val hiredLabor: LiveData<Int> get() = _hiredLabor

    // Setters for labor and challenges
    fun setFamilyLabor(value: Int) {
        _familyLabor.value = value
    }

    fun setHiredLabor(value: Int) {
        _hiredLabor.value = value
    }

    private val _knowledgeRelated = MutableLiveData<String>()
    val knowledgeRelated: LiveData<String> get() = _knowledgeRelated

    private val _qualityRelated = MutableLiveData<String>()
    val qualityRelated: LiveData<String> get() = _qualityRelated

    private val _soilRelated = MutableLiveData<String>()
    val soilRelated: LiveData<String> get() = _soilRelated

    private val _marketRelated = MutableLiveData<String>()
    val marketRelated: LiveData<String> get() = _marketRelated

    private val _compostRelated = MutableLiveData<String>()
    val compostRelated: LiveData<String> get() = _compostRelated

    private val _foodLossRelated = MutableLiveData<String>()
    val foodLossRelated: LiveData<String> get() = _foodLossRelated

    private val _nutritionRelated = MutableLiveData<String>()
    val nutritionRelated: LiveData<String> get() = _nutritionRelated

    private val _financeRelated = MutableLiveData<String>()
    val financeRelated: LiveData<String> get() = _financeRelated

    private val _pestsRelated = MutableLiveData<String>()
    val pestsRelated: LiveData<String> get() = _pestsRelated

    private val _weatherRelated = MutableLiveData<String>()
    val weatherRelated: LiveData<String> get() = _weatherRelated

    private val _diseaseRelated = MutableLiveData<String>()
    val diseaseRelated: LiveData<String> get() = _diseaseRelated

    private val _disease = MutableLiveData<String>()
    val disease: LiveData<String> get() = _disease

    private val _dairyCattle = MutableLiveData<Int>()
    val dairyCattle: LiveData<Int> get() = _dairyCattle

    private val _beefCattle = MutableLiveData<Int>()
    val beefCattle: LiveData<Int> get() = _beefCattle

    private val _sheep = MutableLiveData<Int>()
    val sheep: LiveData<Int> get() = _sheep

    private val _goats = MutableLiveData<Int>()
    val goats: LiveData<Int> get() = _goats

    private val _pigs = MutableLiveData<Int>()
    val pigs: LiveData<Int> get() = _pigs

    private val _poultry = MutableLiveData<Int>()
    val poultry: LiveData<Int> get() = _poultry

    private val _camels = MutableLiveData<Int>()
    val camels: LiveData<Int> get() = _camels

    private val _aquaculture = MutableLiveData<Int>()
    val aquaculture: LiveData<Int> get() = _aquaculture

    private val _rabbits = MutableLiveData<Int>()
    val rabbits: LiveData<Int> get() = _rabbits

    private val _beehives = MutableLiveData<Int>()
    val beehives: LiveData<Int> get() = _beehives

    private val _donkeys = MutableLiveData<Int>()
    val donkeys: LiveData<Int> get() = _donkeys

    // Methods to set data
    fun setDairyCattle(value: Int) {
        _dairyCattle.value = value
    }

    fun setBeefCattle(value: Int) {
        _beefCattle.value = value
    }

    fun setSheep(value: Int) {
        _sheep.value = value
    }

    fun setGoats(value: Int) {
        _goats.value = value
    }

    fun setPigs(value: Int) {
        _pigs.value = value
    }

    fun setPoultry(value: Int) {
        _poultry.value = value
    }

    fun setCamels(value: Int) {
        _camels.value = value
    }

    fun setAquaculture(value: Int) {
        _aquaculture.value = value
    }

    fun setRabbits(value: Int) {
        _rabbits.value = value
    }

    fun setBeehives(value: Int) {
        _beehives.value = value
    }

    fun setDonkeys(value: Int) {
        _donkeys.value = value
    }

    // Methods to set each challenge
    fun setKnowledgeRelated(value: String) {
        _knowledgeRelated.value = value
    }

    fun setQualityRelated(value: String) {
        _qualityRelated.value = value
    }

    fun setSoilRelated(value: String) {
        _soilRelated.value = value
    }

    fun setMarketRelated(value: String) {
        _marketRelated.value = value
    }

    fun setCompostRelated(value: String) {
        _compostRelated.value = value
    }

    fun setFoodLossRelated(value: String) {
        _foodLossRelated.value = value
    }

    fun setNutritionRelated(value: String) {
        _nutritionRelated.value = value
    }

    fun setFinanceRelated(value: String) {
        _financeRelated.value = value
    }

    fun setPestsRelated(value: String) {
        _pestsRelated.value = value
    }

    fun setWeatherRelated(value: String) {
        _weatherRelated.value = value
    }

    fun setDiseaseRelated(value: String) {
        _diseaseRelated.value = value
    }

    fun setLivestockData(data: Map<String, Int>) {
        _livestockData.value = data
    }

    private val _infrastructureData = MutableLiveData<Map<String, String>>()
    val infrastructureData: LiveData<Map<String, String>> get() = _infrastructureData

    // Method to set infrastructure data
    fun setInfrastructureData(data: Map<String, String>) {
        _infrastructureData.value = data
    }

    // Method to retrieve infrastructure data (optional)
    fun getInfrastructureData(): Map<String, String>? {
        return _infrastructureData.value
    }

    val _marketProduceList = MutableLiveData<MutableList<ProduceItem>>(mutableListOf())
    val marketProduceList: LiveData<MutableList<ProduceItem>> = _marketProduceList

    val _ownConsumptionList = MutableLiveData<MutableList<ProduceItem>>(mutableListOf())
    val ownConsumptionList: LiveData<MutableList<ProduceItem>> = _ownConsumptionList

    fun addMarketProduce(item: ProduceItem) {
        _marketProduceList.value?.apply {
            add(item)
            _marketProduceList.value = this
        }
    }

    fun addOwnConsumption(item: ProduceItem) {
        _ownConsumptionList.value?.apply {
            add(item)
            _ownConsumptionList.value = this
        }
    }

    fun clearProduceLists() {
        _marketProduceList.value?.clear()
        _ownConsumptionList.value?.clear()
        _marketProduceList.value = _marketProduceList.value
        _ownConsumptionList.value = _ownConsumptionList.value
    }

    private lateinit var producerRepository: ProducerRepository

    // NEW: LiveData for financial fields
    private val _bankName = MutableLiveData<String?>()
    val bankName: LiveData<String?> = _bankName

    private val _bankAccountNumber = MutableLiveData<String?>()
    val bankAccountNumber: LiveData<String?> = _bankAccountNumber

    private val _bankAccountHolder = MutableLiveData<String?>()
    val bankAccountHolder: LiveData<String?> = _bankAccountHolder

    private val _mobileMoneyProvider = MutableLiveData<String?>()
    val mobileMoneyProvider: LiveData<String?> = _mobileMoneyProvider

    private val _mobileMoneyNumber = MutableLiveData<String?>()
    val mobileMoneyNumber: LiveData<String?> = _mobileMoneyNumber

    // NEW: Setter functions for financial fields
    fun setBankName(name: String?) {
        _bankName.value = name
    }

    fun setBankAccountNumber(number: String?) {
        _bankAccountNumber.value = number
    }

    fun setBankAccountHolder(holder: String?) {
        _bankAccountHolder.value = holder
    }

    fun setMobileMoneyProvider(provider: String?) {
        _mobileMoneyProvider.value = provider
    }

    fun setMobileMoneyNumber(number: String?) {
        _mobileMoneyNumber.value = number
    }


    // Add repository initialization
    fun initRepository(context: Context) {
        producerRepository = ProducerRepository(context)
    }

    fun submitProducerData(context: Context, producerType: String) {
        viewModelScope.launch {
            try {
                val isOnline = NetworkUtils.isNetworkAvailable(context)

                val producer = ProducerEntity(
                    otherName = _otherName.value ?: "",
                    lastName = _lastName.value ?: "",
                    idNumber = _idNumber.value ?: 0L,
                    farmerCode = _idNumber.value?.toString() ?: "",
                    email = _email.value,
                    phoneNumber = _phone.value ?: "",
                    location = _location.value ?: "",
                    dateOfBirth = _dateOfBirth.value ?: "",
                    gender = _gender.value ?: "",
                    educationLevel = _educationLevel.value ?: "",
                    county = _county.value ?: "",
                    subCounty = _subCounty.value ?: "",
                    ward = _ward.value ?: "",
                    village = _village.value ?: "",
                    hub = _hub.value ?: "",
                    buyingCenter = _buyingCenter.value ?: "",
                    totalLandSize = _totalLandSize.value ?: 0f,
                    cultivatedLandSize = _cultivatedLandSize.value ?: 0f,
                    uncultivatedLandSize = _uncultivatedLandSize.value ?: 0f,
                    homesteadSize = _homesteadSize.value ?: 0f,
                    farmAccessibility = _farmAccessibility.value ?: "",
                    cropList = _cropList.value ?: "",
                    accessToIrrigation = _accessToIrrigation.value ?: "",
                    extensionServices = _extensionServices.value ?: "",
                    familyLabor = _familyLabor.value ?: 0,
                    hiredLabor = _hiredLabor.value ?: 0,
                    dairyCattle = _dairyCattle.value ?: 0,
                    beefCattle = _beefCattle.value ?: 0,
                    sheep = _sheep.value ?: 0,
                    goats = _goats.value ?: 0,
                    pigs = _pigs.value ?: 0,
                    poultry = _poultry.value ?: 0,
                    camels = _camels.value ?: 0,
                    aquaculture = _aquaculture.value ?: 0,
                    rabbits = _rabbits.value ?: 0,
                    beehives = _beehives.value ?: 0,
                    donkeys = _donkeys.value ?: 0,
                    knowledgeRelated = _knowledgeRelated.value ?: "",
                    qualityRelated = _qualityRelated.value ?: "",
                    soilRelated = _soilRelated.value ?: "",
                    marketRelated = _marketRelated.value ?: "",
                    compostRelated = _compostRelated.value ?: "",
                    foodLossRelated = _foodLossRelated.value ?: "",
                    nutritionRelated = _nutritionRelated.value ?: "",
                    financeRelated = _financeRelated.value ?: "",
                    pestsRelated = _pestsRelated.value ?: "",
                    weatherRelated = _weatherRelated.value ?: "",
                    diseaseRelated = _diseaseRelated.value ?: "",
                    housingType = infrastructureData.value?.get("Housing Type"),
                    housingFloor = infrastructureData.value?.get("Housing Floor"),
                    housingRoof = infrastructureData.value?.get("Housing Roof"),
                    lightingFuel = infrastructureData.value?.get("Lighting Fuel"),
                    cookingFuel = infrastructureData.value?.get("Cooking Fuel"),
                    waterFilter = infrastructureData.value?.get("Water Filter"),
                    waterTank = infrastructureData.value?.get("Water Tank > 5000L"),
                    handWashingFacilities = infrastructureData.value?.get("Hand Washing Facilities"),
                    ppes = infrastructureData.value?.get("PPE's"),
                    waterWell = infrastructureData.value?.get("Water Wells or Weirs"),
                    irrigationPump = infrastructureData.value?.get("Irrigation Pump"),
                    harvestingEquipment = infrastructureData.value?.get("Harvesting Equipment"),
                    transportationType = infrastructureData.value?.get("Transportation Type"),
                    toiletFloor = infrastructureData.value?.get("Toilet Floor"),
                    userId = SharedPrefs(context).getUserId(),
                    producerType = producerType,
                    primaryProducer = _primaryProducer.value,
                    marketProduceList = _marketProduceList.value?.toList(),
                    ownConsumptionList = _ownConsumptionList.value?.toList(),
                    // NEW: Pass values for financial fields
                    bankName = _bankName.value,
                    bankAccountNumber = _bankAccountNumber.value,
                    bankAccountHolder = _bankAccountHolder.value,
                    mobileMoneyProvider = _mobileMoneyProvider.value,
                    mobileMoneyNumber = _mobileMoneyNumber.value
                )

                producerRepository.saveProducer(producer, isOnline).fold(
                    onSuccess = {
                        Toast.makeText(
                            context,
                            if (isOnline) "Data saved and synced successfully"
                            else "Data saved offline successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        clearForm()
                    },
                    onFailure = { exception ->
                        Toast.makeText(
                            context,
                            "Error saving data: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("SharedViewModel", "Error saving producer", exception)
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("SharedViewModel", "Error in submitProducerData", e)
            }
        }
    }

    // Add sync method
    fun syncData(context: Context) {
        viewModelScope.launch {
            producerRepository.performFullSync().fold(
                onSuccess = { stats ->
                    Toast.makeText(
                        context,
                        "Sync completed: ${stats.uploadedCount} uploaded, " +
                                "${stats.downloadedCount} downloaded",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        context,
                        "Sync failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun clearForm() {
        _otherName.value = ""
        _lastName.value = ""
        _idNumber.value = 0L
        _email.value = ""
        _phone.value = ""
        _location.value = ""
        _dateOfBirth.value = ""
        _gender.value = ""
        _educationLevel.value = ""
        _county.value = ""
        _subCounty.value = ""
        _ward.value = ""
        _village.value = ""
        _hub.value = ""
        _buyingCenter.value = ""
        _totalLandSize.value = 0f
        _cultivatedLandSize.value = 0f
        _uncultivatedLandSize.value = 0f
        _homesteadSize.value = 0f
        _farmAccessibility.value = ""
        _cropList.value = ""
        _accessToIrrigation.value = ""
        _extensionServices.value = ""
        _familyLabor.value = 0
        _hiredLabor.value = 0
        _dairyCattle.value = 0
        _beefCattle.value = 0
        _sheep.value = 0
        _goats.value = 0
        _pigs.value = 0
        _poultry.value = 0
        _camels.value = 0
        _aquaculture.value = 0
        _rabbits.value = 0
        _beehives.value = 0
        _donkeys.value = 0
        _knowledgeRelated.value = ""
        _qualityRelated.value = ""
        _soilRelated.value = ""
        _marketRelated.value = ""
        _compostRelated.value = ""
        _foodLossRelated.value = ""
        _nutritionRelated.value = ""
        _financeRelated.value = ""
        _pestsRelated.value = ""
        _weatherRelated.value = ""
        _diseaseRelated.value = ""
        _infrastructureData.value = null
        _primaryProducer.value = null
        _livestockData.value = null
        clearProduceLists()
        // NEW: Clear financial fields
        _bankName.value = null
        _bankAccountNumber.value = null
        _bankAccountHolder.value = null
        _mobileMoneyProvider.value = null
        _mobileMoneyNumber.value = null
    }
}
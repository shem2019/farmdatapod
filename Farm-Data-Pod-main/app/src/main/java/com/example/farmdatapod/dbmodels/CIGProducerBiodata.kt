package com.example.farmdatapod

import CIGFarmerFieldRegistration

class CIGProducerBiodata {
    // Getters and Setters
    var id: Int = 0
    var otherName: String? = null
    var lastName: String? = null
    var farmerCode: String? = null
    var idNumber: Int = 0
    var gender: String? = null
    var dateOfBirth: String? = null
    var email: String? = null
    var phoneNumber: Int = 0
    var hub: String? = null
    var buyingCenter: String? = null
    var educationLevel: String? = null
    var county: String? = null
    var subCounty: String? = null
    var ward: String? = null
    var village: String? = null
    var primaryProducer: String? = null
    var totalLandSize: String? = null
    var cultivateLandSize: String? = null
    var homesteadSize: String? = null
    var uncultivatedLandSize: String? = null
    var farmAccessibility: String? = null
    var numberOfFamilyWorkers: Int = 0
    var numberOfHiredWorkers: Int = 0
    var accessToIrrigation: String? = null
    var cropList: String? = null
    var farmerInterestInExtension: String? = null
    var knowledgeRelated: String? = null
    var soilRelated: String? = null
    var compostRelated: String? = null
    var nutritionRelated: String? = null
    var pestsRelated: String? = null
    var diseaseRelated: String? = null
    var qualityRelated: String? = null
    var marketRelated: String? = null
    var foodLossRelated: String? = null
    var financeRelated: String? = null
    var weatherRelated: String? = null
    var dairyCattle: String? = null
    var beefCattle: String? = null
    var sheep: String? = null
    var poultry: String? = null
    var pigs: String? = null
    var rabbits: String? = null
    var beehives: String? = null
    var donkeys: String? = null
    var goats: String? = null
    var camels: String? = null
    var aquaculture: String? = null
    var housingType: String? = null
    var housingFloor: String? = null
    var housingRoof: String? = null
    var lightingFuel: String? = null
    var cookingFuel: String? = null
    var waterFilter: String? = null
    var waterTankGreaterThan5000lts: String? = null
    var handWashingFacilities: String? = null
    var ppes: String? = null
    var waterWellOrWeir: String? = null
    var irrigationPump: String? = null
    var harvestingEquipment: String? = null
    var transportationType: String? = null
    var toiletFloor: String? = null
    var userApproved: Int = 0
    var ta: String? = null
    var userId: Int = 0
    private var domesticProduces: List<DomesticProduce>? = null
    var commercialProduces: List<CommercialProduce>? = null
    var cIGFarmerFieldRegistrations: List<CIGFarmerFieldRegistration>? = null

    val domesticeProduces: List<Any>?
        get() = domesticProduces

    fun setDomesticProduces(domesticProduces: List<DomesticProduce>?) {
        this.domesticProduces = domesticProduces
    }
}
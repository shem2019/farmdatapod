package com.example.farmdatapod

class ProducerBiodata {
    var id: Int = 0
    var other_name: String? = null
    var last_name: String? = null
    var farmer_code: String? = null
    var id_number: String? = null  // Changed from Int
    var gender: String? = null
    var date_of_birth: String? = null
    var email: String? = null
    var phone_number: String? = null
    var hub: String? = null
    var buying_center: String? = null
    var education_level: String? = null
    var county: String? = null
    var sub_county: String? = null
    var ward: String? = null
    var village: String? = null
    var primary_producer: List<PrimaryProducer>? = null
    var total_land_size: String? = null
    var cultivate_land_size: String? = null
    var homestead_size: String? = null
    var uncultivated_land_size: String? = null
    var farm_accessibility: String? = null
    var number_of_family_workers: String? = null
    var number_of_hired_workers: String? = null
    var access_to_irrigation: String? = null
    var crop_list: String? = null
    var farmer_interest_in_extension: String? = null
    var knowledge_related: String? = null
    var soil_related: String? = null
    var compost_related: String? = null
    var nutrition_related: String? = null
    var pests_related: String? = null
    var disease_related: String? = null
    var quality_related: String? = null
    var market_related: String? = null
    var food_loss_related: String? = null
    var finance_related: String? = null
    var weather_related: String? = null
    var dairy_cattle: String? = null
    var beef_cattle: String? = null
    var sheep: String? = null
    var poultry: String? = null
    var pigs: String? = null
    var rabbits: String? = null
    var beehives: String? = null
    var donkeys: String? = null
    var goats: String? = null
    var camels: String? = null
    var aquaculture: String? = null
    var housing_type: String? = null
    var housing_floor: String? = null
    var housing_roof: String? = null
    var lighting_fuel: String? = null
    var cooking_fuel: String? = null
    var water_filter: String? = null
    var water_tank_greater_than_5000lts: String? = null
    var hand_washing_facilities: String? = null
    var ppes: String? = null
    var water_well_or_weir: String? = null
    var irrigation_pump: String? = null
    var harvesting_equipment: String? = null
    var transportation_type: String? = null
    var toilet_floor: String? = null
    var user_approved: Boolean = false
    var ta: String? = null
    var user_id: String? = null
    var domesticProduces: List<DomesticProduce>? = null
    var commercialProduces: List<CommercialProduce>? = null

    override fun toString(): String {
        return "ProducerBiodata(id=$id, other_name=$other_name, last_name=$last_name, farmer_code=$farmer_code, " +
                "id_number=$id_number, gender=$gender, date_of_birth=$date_of_birth, email=$email, phone_number=$phone_number, " +
                "hub=$hub, buying_center=$buying_center, education_level=$education_level, county=$county, sub_county=$sub_county, " +
                "ward=$ward, village=$village, primary_producer=$primary_producer, total_land_size=$total_land_size, " +
                "cultivate_land_size=$cultivate_land_size, homestead_size=$homestead_size, uncultivated_land_size=$uncultivated_land_size, " +
                "farm_accessibility=$farm_accessibility, number_of_family_workers=$number_of_family_workers, " +
                "number_of_hired_workers=$number_of_hired_workers, access_to_irrigation=$access_to_irrigation, crop_list=$crop_list, " +
                "farmer_interest_in_extension=$farmer_interest_in_extension, knowledge_related=$knowledge_related, soil_related=$soil_related, " +
                "compost_related=$compost_related, nutrition_related=$nutrition_related, pests_related=$pests_related, " +
                "disease_related=$disease_related, quality_related=$quality_related, market_related=$market_related, " +
                "food_loss_related=$food_loss_related, finance_related=$finance_related, weather_related=$weather_related, " +
                "dairy_cattle=$dairy_cattle, beef_cattle=$beef_cattle, sheep=$sheep, poultry=$poultry, pigs=$pigs, rabbits=$rabbits, " +
                "beehives=$beehives, donkeys=$donkeys, goats=$goats, camels=$camels, aquaculture=$aquaculture, housing_type=$housing_type, " +
                "housing_floor=$housing_floor, housing_roof=$housing_roof, lighting_fuel=$lighting_fuel, cooking_fuel=$cooking_fuel, " +
                "water_filter=$water_filter, water_tank_greater_than_5000lts=$water_tank_greater_than_5000lts, " +
                "hand_washing_facilities=$hand_washing_facilities, ppes=$ppes, water_well_or_weir=$water_well_or_weir, " +
                "irrigation_pump=$irrigation_pump, harvesting_equipment=$harvesting_equipment, transportation_type=$transportation_type, " +
                "toilet_floor=$toilet_floor, user_approved=$user_approved, ta=$ta, user_id=$user_id, " +
                "domesticProduces=$domesticProduces, commercialProduces=$commercialProduces)"
    }
}

class PrimaryProducer {
    var response: String? = null
    var firstname: String? = null
    var other_name: String? = null
    var id_number: String? = null
    var phone_number: String? = null
    var gender: String? = null
    var email: String? = null
    var date_of_birth: String? = null

    override fun toString(): String {
        return "PrimaryProducer(response=$response, firstname=$firstname, other_name=$other_name, id_number=$id_number, " +
                "phone_number=$phone_number, gender=$gender, email=$email, date_of_birth=$date_of_birth)"
    }
}
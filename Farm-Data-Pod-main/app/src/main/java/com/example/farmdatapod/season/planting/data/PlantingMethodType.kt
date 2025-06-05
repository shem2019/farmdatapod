package com.example.farmdatapod.season.planting.data


enum class PlantingMethodType(val requiresLabor: Boolean, val requiresUnits: Boolean) {
    MANUAL(true, false),
    TRACTOR(false, true),
    SPREADER(false, true),
    CART_HAND_ANIMAL(true, false),
    DRILL(false, true),
    BROADCASTING(false, false),
    HYDROPONICS(false, true),
    AEROPONICS(false, true),
    NO_TILL(false, false);

    companion object {
        fun fromString(method: String): PlantingMethodType {
            return values().first { it.name.equals(method, ignoreCase = true) }
        }
    }
}
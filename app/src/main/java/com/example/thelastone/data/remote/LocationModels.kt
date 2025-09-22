// data/remote/LocationModels.kt
package com.example.thelastone.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class Rectangle(
    val low: LatLng,    // southwest
    val high: LatLng    // northeast
)

// v1: locationRestriction 可以是 circle 或 rectangle 擇一
@Serializable
data class LocationRestriction(
    val circle: Circle? = null,
    val rectangle: Rectangle? = null
)
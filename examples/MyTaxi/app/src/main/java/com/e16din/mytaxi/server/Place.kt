package com.e16din.mytaxi.server

import kotlinx.serialization.Serializable

@Serializable
class Place(
    val name: String,
    val addition: String,
    val location: Location,
) : java.io.Serializable {
    @Serializable
    class Location(
        val latitude: Double,
        val longitude: Double
    ) : java.io.Serializable
}
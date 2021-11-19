package com.e16din.mytaxi.server

import kotlinx.serialization.Serializable

@Serializable
class OrderResult(
    val success: Boolean,
    val message: String,
    val car: Car
) {
    @Serializable
    class Car(
        val carNumber: String,
        val carModel: String,
        val carColor: String
    ) : java.io.Serializable
}
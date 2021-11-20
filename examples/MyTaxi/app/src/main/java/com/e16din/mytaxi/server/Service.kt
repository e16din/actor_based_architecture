package com.e16din.mytaxi.server

import kotlinx.serialization.Serializable

@Serializable
class Service(
    val name: String,
    val carType: Int = CarType.Light.type,
    val price: Float
) {
    enum class CarType(val type: Int) {
        Light(0),
        Comfort(1),
        Business(2)
    }
}
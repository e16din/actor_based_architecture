package com.e16din.mytaxi.support

import android.location.Location
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure

@Serializer(forClass = Location::class)
class AndroidLocationSerializer : KSerializer<Location> {

    override val descriptor = buildClassSerialDescriptor("Location") {
        element<Double>("latitude")
        element<Double>("longitude")
    }

    override fun deserialize(decoder: Decoder): Location {
        val composite = decoder.beginStructure(descriptor)

        var lat = 0.0
        var lon = 0.0

        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                0 -> lat = composite.decodeDoubleElement(descriptor, 0)
                1 -> lon = composite.decodeDoubleElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }

        val location = Location(Location::class.qualifiedName)
        location.latitude = lat
        location.longitude = lon

        return location
    }

    override fun serialize(encoder: Encoder, value: Location) {
        encoder.encodeStructure(descriptor) {
            encodeDoubleElement(descriptor, 0, value.latitude)
            encodeDoubleElement(descriptor, 1, value.longitude)
        }
    }
}
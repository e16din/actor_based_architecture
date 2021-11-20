package com.e16din.mytaxi.server

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.observer.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.random.Random

object HttpClient {

    const val HOST = "https://test.mytaxi.com"

    const val GET_PLACES = "$HOST/places"
    const val GET_BONUSES_COUNT = "$HOST/bonuses_count"
    const val GET_LAST_PLACES = "$HOST/last_places"
    const val GET_SERVICES = "$HOST/services"
    const val POST_ORDER = "$HOST/order"
    const val GET_CAR_DATA = "$HOST/car_data"

    const val PARAM_COUNT = "count"
    const val PARAM_QUERY = "query"

    // todo: replace with real addresses (where to get addresses?
    private val places = listOf(// todo: add English Localization
        Place("Тестовое место, 1", Place.Location(0.0, 0.0)),
        Place("Тестовое место, 2", Place.Location(1.0, 1.0)),
        Place("Тестовое место, 3", Place.Location(2.0, 2.0))
    )

    // todo: replace stubs with data from server
    val instance = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                val responseHeaders =
                    headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
                when (HOST + request.url.encodedPath) {
                    GET_PLACES -> {
                        val query = request.url.parameters[PARAM_QUERY]
                        val result = places
                        val resultJson =
                            Json.encodeToString(ListSerializer(Place.serializer()), result)
                        respond(resultJson, HttpStatusCode.OK, responseHeaders)
                    }
                    GET_BONUSES_COUNT -> {
                        val result = 1200
                        val resultJson = Json.encodeToString(Int.serializer(), result)
                        respond(resultJson, HttpStatusCode.OK, responseHeaders)
                    }
                    GET_LAST_PLACES -> {
                        val count = request.url.parameters[PARAM_COUNT]?.toInt()
                        val result = count?.let { places.subList(0, it) } ?: emptyList()
                        val resultJson =
                            Json.encodeToString(ListSerializer(Place.serializer()), result)
                        respond(resultJson, HttpStatusCode.OK, responseHeaders)
                    }
                    GET_SERVICES -> {
                        val random = Random(System.currentTimeMillis())
                        val result = listOf(
                            Service("Эконом", 0, random.nextInt(100, 300).toFloat()),
                            Service("Комфорт", 1, random.nextInt(301, 500).toFloat()),
                            Service("Бизнес", 2, random.nextInt(501, 700).toFloat()),
                        )
                        val resultJson =
                            Json.encodeToString(ListSerializer(Service.serializer()), result)
                        respond(resultJson, HttpStatusCode.OK, responseHeaders)
                    }
                    POST_ORDER -> {
                        val bodyJson = String(request.body.toByteArray())
                        Log.d("temp", "json $bodyJson")
                        val service = Json.decodeFromString(Service.serializer(), bodyJson)

                        val result = OrderResult(
                            true,
                            "Мы нашли для вас лучшую машину!",
                            OrderResult.Car(
                                "o001aa",
                                "Mitsubishi Lancer",
                                "Белый"
                            )
                        )

                        val resultJson = Json.encodeToString(OrderResult.serializer(), result)
                        delay(5000)
                        respond(resultJson, HttpStatusCode.OK, responseHeaders)
                    }
                    GET_CAR_DATA -> {
                        // todo: add web sockets handler
                        respond("resultJson", HttpStatusCode.OK, responseHeaders)
                    }
                    else -> {
                        error("Unhandled ${request.url.encodedPath}")
                    }
                }
            }
        }

        install(JsonFeature) {
            serializer = KotlinxSerializer(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.v("Ktor =>", message)
                }
            }
            level = LogLevel.ALL
        }

        install(ResponseObserver) {
            onResponse { response ->
                Log.d("HTTP status:", "${response.status.value}")
            }
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        install(WebSockets) {
            // Configure WebSockets
        }
    }
}
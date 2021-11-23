package com.e16din.mytaxi.screens.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.e16din.mytaxi.databinding.FragmentSelectRouteBinding
import com.e16din.mytaxi.server.HttpClient
import com.e16din.mytaxi.server.Place
import com.e16din.mytaxi.support.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import java.io.Serializable


class SelectRouteFragment : Fragment(), DataKey {

    companion object {
        const val KEY_INITIAL_DATA = "KEY_INITIAL_DATA"
        const val KEY_RESULT_DATA = "KEY_DATA_RESULT"
    }

    private val appAgent = AppAgent()
    private val screenAgent = SelectRouteScreenAgent()
    private val mainScreenAgent = MainScreenAgent()
    private val userAgent = UserAgent()
    private val deviceAgent = DeviceAgent()
    private val serverAgent = ServerAgent()

    private lateinit var binding: FragmentSelectRouteBinding

    init {
        deviceAgent.onCreateView = {
            // do nothing
        }
        deviceAgent.onViewCreated = { savedInstanceState ->
            screenAgent.data = deviceAgent.restoreData(savedInstanceState)
                ?: SelectRouteScreenData(route = mainScreenAgent.getSelectedRoute())

            val route = screenAgent.data.route
            userAgent.lookAtStartPlace(route.startPlace?.name)
            userAgent.onStartPlaceChanged = { place ->
                screenAgent.data.route.startPlace = place
                onAnyPlaceChanged(place.name)
            }

            userAgent.lookAtFinishPlace(route.finishPlace?.name)
            userAgent.onFinishPlaceChanged = { place ->
                screenAgent.data.route.finishPlace = place
                onAnyPlaceChanged(place.name)
            }

            lifecycleScope.launch {
                val places = serverAgent.getPlaces(screenAgent.data.currentPlaceQuery)
                userAgent.lookAtPlacesList(places)
            }
        }
        deviceAgent.onSaveInstanceState = { outState ->
            deviceAgent.saveData(outState)
        }
    }

    private fun onAnyPlaceChanged(placeName: String) {
        screenAgent.data.currentPlaceQuery = placeName

        val areAllPlacesSelected = screenAgent.data.route.finishPlace != null
                && screenAgent.data.route.startPlace != null
        if (areAllPlacesSelected) {
            userAgent.lookAtMainScreen(screenAgent.data.route)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        deviceAgent.onCreateView.invoke()
        binding = FragmentSelectRouteBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceAgent.onViewCreated.invoke(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        deviceAgent.onSaveInstanceState.invoke(outState)
        super.onSaveInstanceState(outState)
    }

    // NOTE: агент для актора работающего приожения
    inner class AppAgent {
    }

    class SelectRouteScreenData(
        var route: Route,
        var currentPlaceQuery: String = ""
    ) : Serializable

    // NOTE: агент для актора экрана Select Route
    inner class SelectRouteScreenAgent {
        lateinit var data: SelectRouteScreenData
    }

    // NOTE: агент для актора экрана Main
    inner class MainScreenAgent {
        fun getSelectedRoute(): Route {
            return requireArguments().getSerializable(KEY_INITIAL_DATA) as Route
        }
    }

    // NOTE: агент для актора реального пользователя
    inner class UserAgent {
        lateinit var onStartPlaceChanged: (place: Place) -> Unit
        lateinit var onFinishPlaceChanged: (place: Place) -> Unit //todo: use as parameter of function

        fun lookAtStartPlace(name: String?) {
            // todo
        }

        fun lookAtFinishPlace(name: String?) {
            // todo
        }

        fun lookAtPlacesList(places: List<Place>) {
            // todo
        }

        fun lookAtMainScreen(route: Route) {
            setNavigationResult(KEY_RESULT_DATA, route)
            findNavController().popBackStack()
        }
    }

    // NOTE: агент для актора реального сервера
    inner class ServerAgent {
        private val httpClient by lazy { getApplication().httpClient() }

        suspend fun getPlaces(currentPlaceQuery: String): List<Place> {
            val url = "${HttpClient.GET_PLACES}?${HttpClient.PARAM_QUERY}=${currentPlaceQuery}"
            return httpClient.get(url) {
                accept(ContentType.Application.Json)
            }
        }
    }

    // NOTE: агент для актора операционной системы телефона
    inner class DeviceAgent {
        lateinit var onCreateView: () -> Unit
        lateinit var onViewCreated: (savedInstanceState: Bundle?) -> Unit
        lateinit var onSaveInstanceState: (outState: Bundle) -> Unit

        fun restoreData(savedInstanceState: Bundle?): SelectRouteScreenData? {
            return savedInstanceState?.getSerializable(dataKey) as SelectRouteScreenData?
        }

        fun saveData(outState: Bundle) {
            outState.putSerializable(dataKey, screenAgent.data)
        }
    }
}
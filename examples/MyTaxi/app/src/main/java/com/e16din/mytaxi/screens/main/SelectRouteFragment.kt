package com.e16din.mytaxi.screens.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.e16din.mytaxi.databinding.FragmentSelectRouteBinding
import com.e16din.mytaxi.server.HttpClient
import com.e16din.mytaxi.server.Place
import com.e16din.mytaxi.support.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
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
            fun requestPlaces() {
                lifecycleScope.launch {
                    val places = serverAgent.getPlaces(screenAgent.data.currentPlaceQuery)
                    userAgent.lookAtPlacesList(places)
                }
            }

            screenAgent.data = deviceAgent.restoreData(savedInstanceState)
                ?: SelectRouteScreenData(route = mainScreenAgent.getSelectedRoute())

            val route = screenAgent.data.route
            userAgent.lookAtStartPlace(route.startPlace?.name)
            userAgent.lookAtFinishPlace(route.finishPlace?.name)
            userAgent.onSelectOtherField = { selectedFieldType ->
                screenAgent.data.selectedFieldType = selectedFieldType
                lifecycleScope.launch(Dispatchers.IO) {
                    val places = serverAgent.getPlaces(screenAgent.data.currentPlaceQuery)
                    launch(Dispatchers.Main) {
                        userAgent.lookAtPlacesList(places)
                    }
                }
            }
            userAgent.onQueryChanged = { query ->
                screenAgent.data.currentPlaceQuery = query
                requestPlaces()
            }
            userAgent.onPlaceSelected = { place ->
                if (screenAgent.data.selectedFieldType == SelectRouteScreenData.FieldType.From) {
                    screenAgent.data.route.startPlace = place
                    userAgent.lookAtStartPlace(place.name)
                } else {
                    screenAgent.data.route.finishPlace = place
                    userAgent.lookAtFinishPlace(place.name)
                }

                screenAgent.data.currentPlaceQuery = place.name
                val areAllPlacesSelected = screenAgent.data.route.finishPlace != null
                        && screenAgent.data.route.startPlace != null
                if (areAllPlacesSelected) {
                    userAgent.lookAtMainScreen(screenAgent.data.route)
                }
            }

            requestPlaces()
        }
        deviceAgent.onSaveInstanceState = { outState ->
            deviceAgent.saveData(outState)
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
        var currentPlaceQuery: String = "",
        var selectedFieldType: FieldType = when {
            route.startPlace != null && route.finishPlace == null -> FieldType.From
            route.startPlace == null && route.finishPlace != null -> FieldType.To
            else -> FieldType.None
        }
    ) : Serializable {
        enum class FieldType { None, From, To }
    }

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
        lateinit var onPlaceSelected: (place: Place) -> Unit
        lateinit var onSelectOtherField: (selectedFieldType: SelectRouteScreenData.FieldType) -> Unit
        lateinit var onQueryChanged: (query: String) -> Unit

        fun lookAtStartPlace(name: String?) {
            binding.startPlaceField.setText(name)
            binding.startPlaceField.onFocusChangeListener =
                View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        onSelectOtherField.invoke(SelectRouteScreenData.FieldType.From)
                    }
                }
            binding.startPlaceField.doOnTextChanged { text, start, before, count ->
                onQueryChanged.invoke(text.toString())
            }
        }

        fun lookAtFinishPlace(name: String?) {
            binding.finishPlaceField.setText(name)
            binding.finishPlaceField.onFocusChangeListener =
                View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        onSelectOtherField.invoke(SelectRouteScreenData.FieldType.To)
                    }
                }
            binding.finishPlaceField.doOnTextChanged { text, start, before, count ->
                onQueryChanged.invoke(text.toString())
            }
        }

        fun lookAtPlacesList(places: List<Place>) {
            if (binding.placesList.adapter == null) {
                binding.placesList.adapter = PlacesAdapter(places) { selectedPlace ->
                    onPlaceSelected.invoke(selectedPlace)
                }
            } else {
                val placesAdapter = binding.placesList.adapter as PlacesAdapter
                placesAdapter.places = places
                placesAdapter.notifyDataSetChanged()
            }
        }

        fun lookAtMainScreen(route: Route) {
            setNavigationResult(KEY_RESULT_DATA, route)
            findNavController().popBackStack()
        }
    }

    // NOTE: агент для актора реального сервера
    inner class ServerAgent {
        private val httpClient by lazy { getApplication().httpClient() }

        suspend fun getPlaces(query: String): List<Place> {
            val url = "${HttpClient.GET_PLACES}?${HttpClient.PARAM_QUERY}=${query}"
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
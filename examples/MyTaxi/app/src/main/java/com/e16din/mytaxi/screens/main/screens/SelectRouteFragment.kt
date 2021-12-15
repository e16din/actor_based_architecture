package com.e16din.mytaxi.screens.main.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.e16din.mytaxi.databinding.FragmentSelectRouteBinding
import com.e16din.mytaxi.screens.main.Route
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
        val KEY_INITIAL_DATA = "${SelectRouteFragment::class.simpleName}_DATA"
        val KEY_RESULT_DATA = "${SelectRouteFragment::class.simpleName}_RESULT"
    }

    private val appAgent = AppAgent()
    private val selectRouteScreenAgent = SelectRouteScreenAgent()
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
            selectRouteScreenAgent.data = deviceAgent.restoreData(savedInstanceState)
                ?: SelectRouteScreenData(route = mainScreenAgent.getInitialData())

            fun updatePlaces(query: String) {
                if (query.isBlank()) {
                    userAgent.lookAtPlacesList(emptyList())

                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val places = serverAgent.getPlaces(query)
                        launch(Dispatchers.Main) {
                            userAgent.lookAtPlacesList(places)
                        }
                    }
                }
            }

            userAgent.onStartPlaceQueryChanged = { query ->
                selectRouteScreenAgent.data.startPlaceQuery = query
                updatePlaces(query)
            }
            userAgent.onFinishPlaceQueryChanged = { query ->
                selectRouteScreenAgent.data.finishPlaceQuery = query
                updatePlaces(query)
            }

            val initialRoute = selectRouteScreenAgent.data.route
            initialRoute.startPlace?.let { place ->
                userAgent.lookAtStartPlaceButton(place)

            } ?: run {
                userAgent.lookAtStartPlaceField("")
            }

            initialRoute.finishPlace?.let { place ->
                userAgent.lookAtFinishPlaceButton(place)

            } ?: run {
                userAgent.lookAtFinishPlaceField("")
            }

            userAgent.lookAtPlacesList(emptyList())

            userAgent.onStartPlaceClick = {
                val startPlace = selectRouteScreenAgent.data.route.startPlace
                userAgent.lookAtStartPlaceField(startPlace?.name)
            }

            userAgent.onFinishPlaceClick = {
                val finishPlace = selectRouteScreenAgent.data.route.finishPlace
                userAgent.lookAtFinishPlaceField(finishPlace?.name)
            }

            userAgent.onStartPlaceFieldSelected = {
                selectRouteScreenAgent.data.selectedFieldType =
                    SelectRouteScreenData.FieldType.Start
                lifecycleScope.launch(Dispatchers.IO) {
                    val places = serverAgent.getPlaces(selectRouteScreenAgent.data.startPlaceQuery)
                    launch(Dispatchers.Main) {
                        userAgent.lookAtPlacesList(places)
                    }
                }
            }

            userAgent.onFinishPlaceFieldSelected = {
                selectRouteScreenAgent.data.selectedFieldType =
                    SelectRouteScreenData.FieldType.Finish
                lifecycleScope.launch(Dispatchers.IO) {
                    val places = serverAgent.getPlaces(selectRouteScreenAgent.data.startPlaceQuery)
                    launch(Dispatchers.Main) {
                        userAgent.lookAtPlacesList(places)
                    }
                }
            }

            userAgent.onPlaceSelected = { place ->
                when (selectRouteScreenAgent.data.selectedFieldType) {
                    SelectRouteScreenData.FieldType.Start,
                    SelectRouteScreenData.FieldType.None -> {
                        selectRouteScreenAgent.data.route.startPlace = place
                        userAgent.lookAtStartPlaceButton(place)
                    }
                    SelectRouteScreenData.FieldType.Finish -> {
                        selectRouteScreenAgent.data.route.finishPlace = place
                        userAgent.lookAtFinishPlaceButton(place)
                    }
                }

                selectRouteScreenAgent.data.startPlaceQuery = place.name
                val areAllPlacesSelected = selectRouteScreenAgent.data.route.finishPlace != null
                        && selectRouteScreenAgent.data.route.startPlace != null
                if (areAllPlacesSelected) {
                    mainScreenAgent.showScreen(selectRouteScreenAgent.data.route)
                }
            }
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
        var startPlaceQuery: String = "",
        var finishPlaceQuery: String = "",
        var selectedFieldType: FieldType = when {
            route.startPlace != null && route.finishPlace == null -> FieldType.Start
            route.startPlace == null && route.finishPlace != null -> FieldType.Finish
            else -> FieldType.None
        }
    ) : Serializable {
        enum class FieldType { None, Start, Finish }
    }

    // NOTE: агент для актора экрана Select Route
    inner class SelectRouteScreenAgent {
        lateinit var data: SelectRouteScreenData
    }

    // NOTE: агент для актора экрана Main
    inner class MainScreenAgent {
        fun getInitialData(): Route {
            return requireArguments().getSerializable(KEY_INITIAL_DATA) as Route
        }

        fun showScreen(route: Route) {
            setNavigationResult(KEY_RESULT_DATA, route)
            findNavController().popBackStack()
        }
    }

    // NOTE: агент для актора реального пользователя
    inner class UserAgent {
        lateinit var onPlaceSelected: (place: Place) -> Unit

        lateinit var onStartPlaceClick: () -> Unit
        lateinit var onStartPlaceFieldSelected: () -> Unit
        lateinit var onStartPlaceQueryChanged: (query: String) -> Unit

        lateinit var onFinishPlaceClick: () -> Unit
        lateinit var onFinishPlaceFieldSelected: () -> Unit
        lateinit var onFinishPlaceQueryChanged: (query: String) -> Unit

        fun lookAtStartPlaceField(name: String?) {
            binding.startPlaceButton.root.isVisible = false
            binding.startPlaceField.isVisible = true
            binding.startPlaceField.onFocusChangeListener =
                View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        onStartPlaceFieldSelected.invoke()
                    }
                }
            binding.startPlaceField.doOnTextChanged { text, start, before, count ->
                onStartPlaceQueryChanged.invoke(text.toString())
            }
            binding.startPlaceField.setText(name)
        }

        fun lookAtFinishPlaceField(name: String?) {
            binding.finishPlaceButton.root.isVisible = false
            binding.finishPlaceField.isVisible = true
            binding.finishPlaceField.onFocusChangeListener =
                View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        onFinishPlaceFieldSelected.invoke()
                    }
                }
            binding.finishPlaceField.doOnTextChanged { text, start, before, count ->
                onFinishPlaceQueryChanged.invoke(text.toString())
            }
            binding.finishPlaceField.setText(name)
        }

        fun lookAtFinishPlaceButton(place: Place) {
            binding.finishPlaceField.isVisible = false
            binding.finishPlaceButton.root.isVisible = true
            binding.finishPlaceButton.nameLabel.text = place.name
            binding.finishPlaceButton.additionLabel.text = place.addition
            binding.finishPlaceButton.root.setOnClickListener {
                onFinishPlaceClick.invoke()
            }
        }

        fun lookAtStartPlaceButton(place: Place) {
            binding.startPlaceField.isVisible = false
            binding.startPlaceButton.root.isVisible = true
            binding.startPlaceButton.nameLabel.text = place.name
            binding.startPlaceButton.additionLabel.text = place.addition
            binding.startPlaceButton.root.setOnClickListener {
                onStartPlaceClick.invoke()
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
            outState.putSerializable(dataKey, selectRouteScreenAgent.data)
        }
    }
}
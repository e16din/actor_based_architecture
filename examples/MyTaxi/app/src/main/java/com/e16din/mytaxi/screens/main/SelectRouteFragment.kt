package com.e16din.mytaxi.screens.main

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.e16din.mytaxi.MyTaxiApp
import com.e16din.mytaxi.databinding.FragmentSelectRouteBinding
import com.e16din.mytaxi.server.HttpClient
import com.e16din.mytaxi.server.Place
import com.e16din.mytaxi.support.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.Serializable


class SelectRouteFragment : Fragment(), DataKey {

    companion object {
        const val INITIAL_DATA = "INITIAL_DATA"
        const val RESULT_DATA = "RESULT_DATA"
    }

    // NOTE: агент для актора работающего приожения
    inner class AppAgent {
    }

    class SelectRouteScreenData(
        var route: Route,
    ) : Serializable

    // NOTE: агент для актора экрана
    inner class SelectRouteScreenAgent {
        lateinit var data: SelectRouteScreenData

        fun saveData(outState: Bundle) {
            outState.putSerializable(dataKey, screenAgent.data)
        }

        var currentPlaceQuery = ""
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
            val url = "${HttpClient.GET_PLACES}/?${HttpClient.PARAM_QUERY}=${currentPlaceQuery}"
            return httpClient.get(url) {
                accept(ContentType.Application.Json)
            }
        }
    }

    // NOTE: агент для актора операционной системы телефона
    inner class SystemAgent {
        lateinit var onCreateView: () -> Unit
        lateinit var onViewCreated: (savedInstanceState: Bundle?) -> Unit
        lateinit var onSaveInstanceState: (outState: Bundle) -> Unit
    }

    private val appAgent = AppAgent()
    private val screenAgent = SelectRouteScreenAgent()
    private val userAgent = UserAgent()
    private val systemAgent = SystemAgent()
    private val serverAgent = ServerAgent()

    private lateinit var binding: FragmentSelectRouteBinding

    init {
        systemAgent.onCreateView = {
            // do nothing
        }
        systemAgent.onViewCreated = { savedInstanceState ->
            screenAgent.data = savedInstanceState?.getParcelable(dataKey)
                ?: requireArguments().getParcelable(KEY_INITIAL_DATA)!!

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
                val places = serverAgent.getPlaces(screenAgent.currentPlaceQuery)
                userAgent.lookAtPlacesList(places)
            }
        }
        systemAgent.onSaveInstanceState = { outState ->
            screenAgent.saveData(outState)
        }
    }

    private fun onAnyPlaceChanged(placeName: String) {
        screenAgent.currentPlaceQuery = placeName

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
        systemAgent.onCreateView.invoke()
        binding = FragmentSelectRouteBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        systemAgent.onViewCreated.invoke(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        systemAgent.onSaveInstanceState.invoke(outState)
        super.onSaveInstanceState(outState)
    }
}
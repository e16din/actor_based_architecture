package com.e16din.mytaxi.screens.main

import com.e16din.mytaxi.support.AndroidLocationSerializer
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.e16din.mytaxi.MyTaxiApp
import com.e16din.mytaxi.R
import com.e16din.mytaxi.databinding.FragmentMainBinding
import com.e16din.mytaxi.server.*
import com.e16din.mytaxi.support.DataKey
import com.e16din.mytaxi.support.getApplication
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json


//todo: check on memory leaks with LeakCanary
class MainFragment : Fragment(), DataKey {

    private val appAgent = AppAgent()
    private val screenAgent = MainScreenAgent()
    private val selectRouteScreenAgent = SelectRouteScreenAgent()
    private val userAgent = UserAgent()
    private val deviceAgent = DeviceAgent()
    private val serverAgent = ServerAgent()

    private lateinit var binding: FragmentMainBinding

    init {
        deviceAgent.onCreateView = {
            deviceAgent.trackLocation { location ->
                appAgent.lastLocation = location
            }
        }

        deviceAgent.onViewCreated = { savedInstanceState ->
            screenAgent.data = deviceAgent.restoreData(savedInstanceState)
                ?: MainScreenData()

            when (screenAgent.data.screenState) {
                MainScreenData.ScreenState.ToSelectRoute -> {
                    switchToSelectRouteState()
                }
                MainScreenData.ScreenState.ToOrderService -> {
                    switchToOrderServiceState(screenAgent.data.selectedRoute)
                }
                MainScreenData.ScreenState.ToSearchCar -> {
                    screenAgent.data.selectedService?.let {
                        switchToSearchCarState(it)
                    }
                }
                MainScreenData.ScreenState.ToWaitForCar -> {
                    switchToWaitForCarState()
                }
                MainScreenData.ScreenState.ToTrackTrip -> {
                    // todo:
                }
                MainScreenData.ScreenState.ToRateService -> {
                    // todo:
                }
            }

            userAgent.lookAtLeftSideBar(
                enabled = screenAgent.data.isSideBarOpened,
                onSideBarStateChanged = { opened ->
                    screenAgent.data.isSideBarOpened = opened
                }
            )

            lifecycleScope.launch {
                val lastPlaces = serverAgent.getLastPlaces(2)
                    ?: screenAgent.data.lastPlaces

                fun onLastPlaceClick(position: Int) {
                    screenAgent.data.selectedRoute.finishPlace = lastPlaces[position]
                    switchToOrderServiceState(screenAgent.data.selectedRoute)
                }

                userAgent.lookAtLastPlacesList(
                    lastPlaces = lastPlaces,
                    onLastPlace1Click = { onLastPlaceClick(0) },
                    onLastPlace2Click = { onLastPlaceClick(1) }
                )

                screenAgent.data.bonusesCount = serverAgent.getBonusesCount()
                    ?: screenAgent.data.bonusesCount
                userAgent.lookAtBonusesCount(screenAgent.data.bonusesCount)
            }

            deviceAgent.onSaveInstanceState = { outState ->
                deviceAgent.saveData(outState)
            }
        }
    }

    private fun switchToWaitForCarState() {
        screenAgent.data.screenState = MainScreenData.ScreenState.ToWaitForCar
        userAgent.lookAtWaitForCarArea()
        lifecycleScope.launch {
            serverAgent.listenCarDataChanged { carData ->
                userAgent.lookAtWaitingTimeLabel(carData.waitingTimeMinutes)
                val orderedCar = screenAgent.data.orderedCar
                val characteristics =
                    "${orderedCar?.carColor} ${orderedCar?.carModel} [${orderedCar?.carNumber}]"
                userAgent.lookAtCarCharacteristics(characteristics)
                userAgent.lookAtCarLocation(carData.carLocation)

                val routeToMe = Route(
                    startPlace = Place("carLocation", carData.carLocation),
                    finishPlace = screenAgent.data.selectedRoute.startPlace
                )
                userAgent.lookAtRouteLine(routeToMe)
            }
        }
    }

    private fun switchToSearchCarState(service: Service) {
        screenAgent.data.screenState = MainScreenData.ScreenState.ToSearchCar
        var job: Job? = null
        userAgent.lookAtSearchCarArea(onCancelClick = {
            if (job?.isActive == true) {
                job?.cancel()
            }
            switchToOrderServiceState(screenAgent.data.selectedRoute)
        })
        job = lifecycleScope.launch {
            val response = serverAgent.postOrder(service)
            if (response.success) {
                screenAgent.data.orderedCar = response.car
                switchToWaitForCarState()
            } else {
                userAgent.lookAtSearchResultMessage(response.message)
            }
        }
    }

    private fun switchToOrderServiceState(route: Route) {
        screenAgent.data.screenState = MainScreenData.ScreenState.ToOrderService
        userAgent.lookAtRouteLine(screenAgent.data.selectedRoute)
        userAgent.lookAtOrderArea()
        userAgent.lookAtOrderRouteFields(route)

        lifecycleScope.launch {
            screenAgent.data.services = serverAgent.getServices()
            userAgent.lookAtServices(screenAgent.data.services)
            userAgent.lookAtOrderButton(
                service = screenAgent.data.selectedService,
                onOrderClick = { service ->
                    switchToSearchCarState(service)
                }
            )
        }
    }

    private fun switchToSelectRouteState() {
        screenAgent.data.screenState = MainScreenData.ScreenState.ToSelectRoute

        appAgent.lastLocation?.let { lastLocation ->
            userAgent.lookAtYourLocation(lastLocation)

        } ?: run {
            userAgent.lookAtLocationDoNotAvailableMessage()
        }

        val placeName = screenAgent.data.selectedRoute.startPlace?.name
            ?: "???"

        val onSelectPlaceClick = {
            selectRouteScreenAgent.startScreen(
                route = screenAgent.data.selectedRoute,
                onTheSelectRouteScreenResponse = { route ->
                    if (route.startPlace != null && route.finishPlace != null) {
                        switchToOrderServiceState(route)
                    }
                }
            )
        }
        userAgent.lookAtStartPlaceLabel(
            placeName = placeName,
            onSelectPlaceClick = onSelectPlaceClick
        )
        userAgent.lookAtSelectRouteArea(onSelectPlaceClick)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        deviceAgent.onCreateView.invoke()
        binding = FragmentMainBinding.inflate(inflater)
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
        var isAuthorized: Boolean = false // todo: use it on order the car
            get() {
                return sharedPreferences()
                    .getBoolean(MyTaxiApp.KEY_IS_AUTHORIZED, field)
            }
            set(value) {
                field = value
                sharedPreferences()
                    .edit().putBoolean(MyTaxiApp.KEY_IS_AUTHORIZED, value)
                    .apply()
            }

        var lastLocation: Location? = null
            get() {
                val locationJson = sharedPreferences()
                    .getString(MyTaxiApp.KEY_LAST_LOCATION, null)
                return locationJson?.let {
                    Json.decodeFromString(AndroidLocationSerializer(), it)
                }
            }
            set(value) {
                field = value
                val locationJson = value?.let {
                    Json.encodeToString(AndroidLocationSerializer(), value)
                }
                sharedPreferences()
                    .edit().putString(MyTaxiApp.KEY_LAST_LOCATION, locationJson)
                    .apply()
            }

        private fun sharedPreferences() = getApplication()
            .sharedPreferences()
    }

    @Serializable
    class MainScreenData(
        var bonusesCount: Int = 0,
        var isSideBarOpened: Boolean = false,
        var selectedRoute: Route = Route(),
        var lastPlaces: List<Place> = emptyList(),
        var services: List<Service> = emptyList(),
        var selectedService: Service? = null,
        var screenState: ScreenState = ScreenState.ToSelectRoute,
        var orderedCar: OrderResult.Car? = null
    ) : java.io.Serializable {

        enum class ScreenState {
            ToSelectRoute,
            ToOrderService,
            ToSearchCar, //todo: InProgress, Success, Fail
            ToWaitForCar, //todo: Await, Done, Canceled
            ToTrackTrip, //todo: Await, Done, Canceled
            ToRateService //todo: add rate screen
        }
    }

    // NOTE: агент для актора главного экрана
    inner class MainScreenAgent {
        lateinit var data: MainScreenData
    }

    // NOTE: агент для актора экрана выбора маршрута
    inner class SelectRouteScreenAgent {

        fun startScreen(
            route: Route,
            onTheSelectRouteScreenResponse: (route: Route) -> Unit
        ) {
            val navController = findNavController()
            val savedStateLiveData =
                navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Route>(
                    SelectRouteFragment.KEY_RESULT_DATA
                )
            savedStateLiveData?.observe(viewLifecycleOwner) { newRoute ->
                onTheSelectRouteScreenResponse.invoke(newRoute)
            }
            val bundle = bundleOf(SelectRouteFragment.KEY_INITIAL_DATA to route)
            navController.navigate(R.id.action_select_route_fragment, bundle)
        }
    }

    // NOTE: агент для актора реального пользователя
    inner class UserAgent {

        fun lookAtYourLocation(location: Location) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment?
            mapFragment?.getMapAsync { googleMap ->
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(getString(R.string.you_are_here))
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }

        fun lookAtCarLocation(location: Place.Location) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment?
            mapFragment?.getMapAsync { googleMap ->
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap.addMarker(
                    MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car_light))
                        .position(latLng)
                        .title(getString(R.string.car_is_here))
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }

        fun lookAtRouteLine(route: Route) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment?
            mapFragment?.getMapAsync { googleMap ->
                val options = PolylineOptions()

                options.color(Color.parseColor("#CC0000FF"))
                options.width(5f)
                options.visible(true)

                val startLocation = route.startPlace?.location
                val startLatLng =
                    LatLng(startLocation?.latitude ?: 0.0, startLocation?.longitude ?: 0.0)
                options.add(startLatLng)

                val finishLocation = route.finishPlace?.location
                val finishLatLng =
                    LatLng(finishLocation?.latitude ?: 0.0, finishLocation?.longitude ?: 0.0)
                options.add(finishLatLng)

                googleMap.addPolyline(options)

                googleMap.moveCamera(CameraUpdateFactory.newLatLng(startLatLng))
            }
        }

        fun lookAtStartPlaceLabel(placeName: String, onSelectPlaceClick: () -> Unit) {
            binding.startPlaceLabel.text = placeName
            binding.selectStartPlaceButton.setOnClickListener {
                onSelectPlaceClick.invoke()
            }
        }

        fun lookAtSelectRouteArea(onSelectPlaceClick: () -> Unit) {
            binding.selectRouteContainer.root.isVisible = true
            binding.selectRouteContainer.selectFinishPlaceButton.setOnClickListener {
                onSelectPlaceClick.invoke()
            }

            binding.waitForCarContainer.root.isVisible = false
            binding.orderContainer.root.isVisible = false
        }

        fun lookAtLastPlacesList(
            lastPlaces: List<Place>,
            onLastPlace1Click: () -> Unit,
            onLastPlace2Click: () -> Unit
        ) {
            binding.selectRouteContainer.lastPlacesContainer.isInvisible =
                lastPlaces.isEmpty()

            lastPlaces.forEachIndexed { index, place ->
                val lastPlaceButton = when (index) {
                    1 -> binding.selectRouteContainer.lastPlace1Button.apply {
                        root.setOnClickListener { onLastPlace1Click.invoke() }
                    }
                    2 -> binding.selectRouteContainer.lastPlace2Button.apply {
                        root.setOnClickListener { onLastPlace2Click.invoke() }
                    }
                    else -> null
                }
                lastPlaceButton?.placeLabel?.text = place.name
                lastPlaceButton?.timeLabel?.text =
                    "15 минут" // todo: replace this stub with real data
            }
        }

        fun lookAtBonusesCount(bonusesCount: Int) {
            binding.bonusesLabel.text = "$bonusesCount"
        }

        fun lookAtLeftSideBar(
            enabled: Boolean,
            onSideBarStateChanged: (opened: Boolean) -> Unit
        ) {
            binding.drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                }

                override fun onDrawerOpened(drawerView: View) {
                    onSideBarStateChanged.invoke(true)
                }

                override fun onDrawerClosed(drawerView: View) {
                    onSideBarStateChanged.invoke(false)
                }

                override fun onDrawerStateChanged(newState: Int) {
                }
            })

            if (enabled) {
                binding.drawer.openDrawer(Gravity.LEFT)

            } else {
                binding.drawer.close()
            }
        }

        fun lookAtLocationDoNotAvailableMessage(thenDoIt: (() -> Unit)? = null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.location_do_not_available),
                Toast.LENGTH_LONG
            ).show()
            thenDoIt?.invoke()
        }

        fun lookAtOrderArea() {
            binding.orderContainer.root.isVisible = true

            binding.orderContainer.root.isVisible = false
            binding.selectRouteContainer.root.isVisible = false
            binding.selectStartPlaceButton.isVisible = false
        }

        fun lookAtOrderRouteFields(route: Route) {
            binding.orderContainer.startPlaceButton.text = route.startPlace?.name
            binding.orderContainer.finishPlaceButton.text = route.finishPlace?.name
        }

        fun lookAtServices(serviceTypes: List<Service>) {
            binding.orderContainer.servicesList.adapter = ServicesAdapter(serviceTypes)
        }

        fun lookAtSearchCarArea(onCancelClick: () -> Unit) {
            val newInstance = SearchCarDialogFragment.newInstance()
            newInstance.show(
                childFragmentManager,
                SearchCarDialogFragment::class.simpleName
            )
            newInstance.onCancelClick = onCancelClick
        }

        fun lookAtWaitForCarArea() {
            binding.waitForCarContainer.root.isVisible = true

            binding.selectRouteContainer.root.isVisible = false
            binding.orderContainer.root.isVisible = false
        }

        fun lookAtWaitingTimeLabel(waitingTimeMinutes: Int) { //todo:
//            binding.waitForCarContainer.waitingTimeLabel.text =
//                "Осталось ждать $waitingTimeMinutes минут"
        }

        fun lookAtOrderButton(
            service: Service?,
            onOrderClick: (service: Service) -> Unit
        ) {
            binding.orderContainer.orderButton.isEnabled = service != null
            binding.orderContainer.orderButton.setOnClickListener {
                service?.let { service ->
                    onOrderClick.invoke(service)
                }
            }
        }

        fun lookAtSearchResultMessage(message: String) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG)
                .show()
        }

        fun lookAtCarCharacteristics(characteristics: String) { //todo:
//            binding.waitForCarContainer.carCharacteristicsLabel.text = characteristics
        }
    }

    // NOTE: агент для актора реального сервера
    inner class ServerAgent {
        private val httpClient by lazy { getApplication().httpClient() }

        suspend fun getBonusesCount(): Int? {
            return httpClient.get(HttpClient.GET_BONUSES_COUNT) {
                accept(ContentType.Application.Json)
            }
        }

        suspend fun getLastPlaces(count: Int): List<Place>? {
            val url = HttpClient.GET_LAST_PLACES + "?${HttpClient.PARAM_COUNT}=${count}"
            val result = httpClient.get<List<Place>>(url) {
                accept(ContentType.Application.Json)
            }
            return if (result.isEmpty()) null else result
        }

        suspend fun getServices(): List<Service> {
            return httpClient.request {
                method = HttpMethod.Get
                headers {
                    append("Accept", "application/json")
                }
                url(HttpClient.GET_SERVICES)
            }
        }

        suspend fun postOrder(service: Service): OrderResult {
            return httpClient.post(HttpClient.POST_ORDER) {
                contentType(ContentType.Application.Json)
                body = service
            }
        }

        suspend fun getCarData(service: Service): OrderResult {
            return httpClient.post(HttpClient.POST_ORDER) {
                contentType(ContentType.Application.Json)
                body = service
            }
        }

        suspend fun listenCarDataChanged(listener: (carData: CarDataResult) -> Unit) {
            //todo: replace with webSockets mock
            var waitingTimeMinutes = 15

            while (waitingTimeMinutes > 0) {
                waitingTimeMinutes--
                listener.invoke(
                    CarDataResult(
                        waitingTimeMinutes = waitingTimeMinutes,
                        carLocation = Place.Location(0.0, 0.0), //todo: add random position
                        hasCarArrived = waitingTimeMinutes == 0
                    )
                )
                delay(60 * 1000)
            }
        }
    }

    // NOTE: агент для актора устройства
    inner class DeviceAgent {
        lateinit var onCreateView: () -> Unit
        lateinit var onViewCreated: (savedInstanceState: Bundle?) -> Unit
        lateinit var onSaveInstanceState: (outState: Bundle) -> Unit

        fun trackLocation(onLocationChanged: (Location?) -> Unit) {
            val locationClient =
                LocationServices.getFusedLocationProviderClient(requireContext())

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            locationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    onLocationChanged.invoke(location)
                }
            }
        }

        fun restoreData(bundle: Bundle?): MainScreenData? {
            return bundle?.getSerializable(dataKey) as MainScreenData?
        }

        fun saveData(bundle: Bundle) {
            bundle.putSerializable(dataKey, screenAgent.data)
        }
    }
}
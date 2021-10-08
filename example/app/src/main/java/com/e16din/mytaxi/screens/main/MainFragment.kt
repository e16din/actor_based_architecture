package com.e16din.mytaxi.screens.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.e16din.mytaxi.MyTaxiApp
import com.e16din.mytaxi.R
import com.e16din.mytaxi.databinding.FragmentMainBinding
import com.e16din.mytaxi.screens.Screen
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlin.random.Random


class MainFragment : Fragment(), Screen {

    inner class AppAgent {
        private val gson = Gson()

        var isAuthorized: Boolean = false
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
                return field
                    ?: gson.fromJson(locationJson, Location::class.java)
            }
            set(value) {
                field = value
                sharedPreferences()
                    .edit().putString(MyTaxiApp.KEY_LAST_LOCATION, gson.toJson(value))
                    .apply()
            }

        private fun sharedPreferences() = requireActivity()
            .getSharedPreferences(MyTaxiApp::class.java.simpleName, Context.MODE_PRIVATE)
    }

    inner class UserAgent {
        fun lookAtMap(location: Location) {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
            mapFragment?.getMapAsync { googleMap ->
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap.addMarker(
                    MarkerOptions().position(latLng).title(getString(R.string.you_are_here))
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }

        fun lookAtActionsBottomSheet() {
            binding.actionsBottomSheetContainer.isVisible = true
        }

        fun lookAtBonusesCount(bonusesCount: Int) {
            binding.bonusesLabel.text = "$bonusesCount"
        }

        fun lookAtSideBar(sidebarOpened: Boolean) {
            // TODO("Not yet implemented")
        }

        fun lookAtLocationDoNotAvailableMessage() {
            Toast.makeText(
                requireContext(),
                getString(R.string.location_do_not_available),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    inner class ServerAgent {
        suspend fun getBonusesCount(): Int? {
            // emulate request to API

            delay(1000)
            return if (System.currentTimeMillis() % 2L == 0L) {
                // success
                Random.nextInt(1, 500)

            } else {
                // fail
                null
            }
            // todo: use Retrofit or Ktor
        }
    }

    inner class SystemAgent {
        var lastLocation: Location? = null

        fun trackLocation() {
            val fusedLocationClient =
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
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    lastLocation = location
                    appAgent.lastLocation = location
                }
            }
        }
    }

    @Parcelize
    class MainScreenData(
        var bonusesCount: Int = 0,
        var isSideBarOpened: Boolean = false
    ) : Parcelable

    private val appAgent = AppAgent()
    private val userAgent = UserAgent()
    private val systemAgent = SystemAgent()
    private val serverAgent = ServerAgent()

    private val screenDataKey = MainScreenData::class.java.simpleName
    private var screenData = MainScreenData()

    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        systemAgent.trackLocation()

        binding = FragmentMainBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.getParcelable<MainScreenData>(screenDataKey)?.let {
            screenData = it
        }

        val lastLocation = systemAgent.lastLocation
            ?: appAgent.lastLocation
        lastLocation?.let {
            userAgent.lookAtMap(lastLocation)
        } ?: run {
            userAgent.lookAtLocationDoNotAvailableMessage()
        }

        userAgent.lookAtActionsBottomSheet()
        userAgent.lookAtSideBar(screenData.isSideBarOpened)

        lifecycleScope.launch {
            screenData.bonusesCount = serverAgent.getBonusesCount()
                ?: screenData.bonusesCount
            userAgent.lookAtBonusesCount(screenData.bonusesCount)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(screenDataKey, screenData)
        super.onSaveInstanceState(outState)
    }
}
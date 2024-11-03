package com.cs407.pixelated

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cs407.pixelated.BuildConfig.MAPS_API_KEY
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApiService {
    @GET("nearbysearch/json")
    suspend fun getNearbyArcades(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("keyword") keyword: String,
        @Query("key") apiKey: String
    ): PlacesResponse
}
data class PlacesResponse(
    val results: List<PlaceResult>
)

data class PlaceResult(
    val name: String,
    val geometry: Geometry
)

data class Geometry(val location: Location)

data class Location(val lat: Double, val lng: Double)

class ArcadeMap : AppCompatActivity() {
    private lateinit var mMap: GoogleMap
    private lateinit var currentLatLng: LatLng
    private var currentPolyline: Polyline? = null
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    private val placesApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/place/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlacesApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_physical_arcade)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        mapFragment?.getMapAsync(){googleMap: GoogleMap ->
            mMap = googleMap

            checkLocationPermission()
            mMap.setOnMarkerClickListener { marker ->
                drawLineToMarker(marker)
                true // Return true to indicate that we have consumed the event
            }
        }

    }

    fun setLocationMarker(googleMap: GoogleMap,
                          destination: LatLng,
                          destinationName: String){
        googleMap.addMarker(
            MarkerOptions()
                .title(destinationName)
                .position(destination)
        )
    }

    private fun checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),1)
        } else {
            //get current location

            mFusedLocationProviderClient.getLastLocation().addOnCompleteListener { task ->
                val currentLocation = task.result
                currentLatLng = LatLng(
                    currentLocation.latitude,
                    currentLocation.longitude
                )
                //set current location marker
                setLocationMarker(mMap, currentLatLng, "Current Location")
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,10f))

                searchNearbyArcades(currentLatLng)
            }
        }
    }

    private fun searchNearbyArcades(location: LatLng) {
        val locationString = "${location.latitude},${location.longitude}"
        val radius = 50000 // Adjust radius as needed
        val keyword = "arcade"

        lifecycleScope.launch {
            try {
                val response = placesApiService.getNearbyArcades(
                    locationString,
                    radius,
                    keyword,
                    "${MAPS_API_KEY}")

                if(response.results.isEmpty()){
                    showDialog()
                } else {
                    response.results.forEach { place ->
                        val placeLatLng = LatLng(place.geometry.location.lat, place.geometry.location.lng)
                        mMap.addMarker(MarkerOptions().position(placeLatLng).title(place.name))
                    }
                }

            } catch (e: Exception) {
                Log.e("MapsError", "Error fetching nearby arcades: ${e.localizedMessage}")
            }
        }
    }
    private fun showDialog(){
        AlertDialog.Builder(this)
            .setTitle("No locations")
            .setMessage("We're sorry, there don't appear to be any arcades nearby.")
            .setPositiveButton("OK"){dialog, _ ->dialog.dismiss()}
            .create()
            .show()
    }

    private fun drawLineToMarker(marker: Marker) {
        currentPolyline?.remove()

        currentPolyline = mMap.addPolyline(
            PolylineOptions()
                .add(
                    marker.position,
                    currentLatLng
                )
        )
    }
}

package com.cs407.pixelated

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class ArcadeMap : AppCompatActivity() {
    private lateinit var mMap: GoogleMap
    private lateinit var mDestinationLatLng: LatLng //FIXME: array of destinations?
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_physical_arcade)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        mapFragment?.getMapAsync(){googleMap: GoogleMap ->
            mMap = googleMap

            //code to display markers for nearby arcades


            //display current location and draw lines to arcades FIXME: or just one arcade?
            checkLocationPermissionAndDrawPolyline()
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

    private fun checkLocationPermissionAndDrawPolyline() {
        if(ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),1)
        } else {
            //get current location

            mFusedLocationProviderClient.getLastLocation().addOnCompleteListener { task ->
                val currentLocation = task.result
                val currentLatLng = LatLng(
                    currentLocation.latitude,
                    currentLocation.longitude
                )
                //set current location marker
                setLocationMarker(mMap, currentLatLng, "Current Location")
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,15f))
                //TODO: add code for destinations
                /*mMap.addPolyline(
                    PolylineOptions()
                        .add(
                            mDestinationLatLng,
                            currentLatLng
                        )
                )*/
            }



        }
    }
}

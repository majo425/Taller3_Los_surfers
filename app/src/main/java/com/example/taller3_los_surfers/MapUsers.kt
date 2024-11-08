package com.example.taller3_los_surfers

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import data.MyUser

class MapUsers : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var selectedUser: MyUser
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var distanceTextView: TextView
    private var currentUserMarker: Marker? = null
    private var selectedUserMarker: Marker? = null
    private var polyline: Polyline? = null

    private val handler = Handler(Looper.getMainLooper())
    private val tiempoActualizacion: Long = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_users)

        selectedUser = intent.getSerializableExtra("selectedUser") as MyUser //Usuario seleccionado de la lista
        distanceTextView = findViewById(R.id.distanceTextView)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        handler.postDelayed(object : Runnable {
            override fun run() {
                updateSelectedUserLocation()
                handler.postDelayed(this, tiempoActualizacion)
            }
        }, tiempoActualizacion)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        mMap.isMyLocationEnabled = true
        updateLocationInRealTime()
    }

    //Actualizar ubicación del usuario seleccionado
    private fun updateSelectedUserLocation() {
        val email = selectedUser.email

        Firebase.database.reference.child("users")
            .orderByChild("email")
            .equalTo(email)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val userMap = snapshot.children.first().key
                    if (userMap != null) {
                        Firebase.database.reference.child("users").child(userMap)
                            .get()
                            .addOnSuccessListener { userSnapshot ->
                                val updatedUser = userSnapshot.getValue(MyUser::class.java)
                                updatedUser?.let {
                                    if (selectedUser.latitude != it.latitude || selectedUser.longitude != it.longitude) {
                                        selectedUser.latitude = it.latitude
                                        selectedUser.longitude = it.longitude
                                        updateLocationInRealTime()
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("MapUsers", "Error al obtener datos del usuario: ${exception.message}")
                            }
                    }
                } else {
                    Log.e("MapUsers", "Usuario no encontrado en Firebase")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MapUsers", "Error al obtener el UID del usuario: ${exception.message}")
            }
    }

    //Actualizar marcador en el mapa y linea
    private fun updateLocationInRealTime() {
        val selectedUserLatLng = LatLng(selectedUser.latitude, selectedUser.longitude)
        Log.d("MapUsers", "Ubicación actualizada: Lat: ${selectedUser.latitude}, Long: ${selectedUser.longitude}")

        selectedUserMarker?.remove()
        selectedUserMarker = mMap.addMarker(
            MarkerOptions().position(selectedUserLatLng)
                .title("${selectedUser.name} ${selectedUser.lastName}")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)

                currentUserMarker?.remove()
                currentUserMarker = mMap.addMarker(
                    MarkerOptions().position(currentLatLng)
                        .title("Mi Ubicación")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 10f))

                val distance = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    selectedUser.latitude, selectedUser.longitude, distance
                )

                distanceTextView.text = "Distancia: ${distance[0].toInt()} m"
                Log.d("MapUsers", "Distancia entre mi ubicación y la del contacto: ${distance[0].toInt()} m")
                polyline?.remove()

                polyline = mMap.addPolyline(
                    PolylineOptions()
                        .add(currentLatLng)
                        .add(selectedUserLatLng)
                        .color(Color.RED)
                        .width(5f)
                )

                val bounds = LatLngBounds.builder()
                    .include(currentLatLng)
                    .include(selectedUserLatLng)
                    .build()
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
                mMap.animateCamera(cameraUpdate)
            } else {
                Log.e("MapUsers", "No se pudo obtener la ubicación actual.")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
    }
}
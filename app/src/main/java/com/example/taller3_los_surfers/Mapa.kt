package com.example.taller3_los_surfers

import android.Manifest
import android.content.ClipData.Item
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import data.Localizacion


class Mapa : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var item: Item
    private var ubicacionActualMarker: Marker? = null
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        val intent = Intent(this, ServicioCambiosUsuario::class.java)
        startService(intent)

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        val toolbar: Toolbar = findViewById(R.id.barra)
        setSupportActionBar(toolbar)

        // Configurar el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        supportActionBar?.show()
    }

    //Cargar mapa
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        verificarPermisosUbicacion()
    }
    //Cargar el menú
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        Log.d("Mapa", "Menú cargado correctamente")

        if (menu.size() == 0) {
            Log.e("Mapa", "Error al cargar el menú")
        }
        return true
    }

    //Manejar opciones del menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuLogOut -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.dispo -> {
                alternarEstadoUsuario(item)
                true
            }
            R.id.usuarios -> {
                val intent = Intent(this, UsuariosDisponibles::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //Cambiar estado del usuario
    private fun alternarEstadoUsuario(item: MenuItem) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid

            database.child(userId).child("status").get().addOnSuccessListener { snapshot ->
                val estadoActual = snapshot.getValue(String::class.java) ?: "no disponible"

                Log.d("EstadoUsuario", "Estado actual del usuario: $estadoActual")
                val nuevoEstado = if (estadoActual == "disponible") "no disponible" else "disponible"

                // Actualizar el estado en Firebase
                Log.d("EstadoUsuario", "Actualizando estado a: $nuevoEstado")
                database.child(userId).child("status").setValue(nuevoEstado)
                    .addOnSuccessListener {
                        if (nuevoEstado == "disponible") {
                            item.setIcon(R.drawable.ic_disponible) // Ícono verde-disponible
                        } else {
                            item.setIcon(R.drawable.ic_no_disponible) // Ícono rojo-no disponible
                        }
                        Toast.makeText(this, "Estado actualizado a: $nuevoEstado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al actualizar el estado: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener el estado actual: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No se encontró un usuario autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    //Verificar permisos de ubicación
    private fun verificarPermisosUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Solicitar permisos si no están otorgados
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        } else {
            //Cargar ubicación y datos del mapa
            mostrarUbicacionActual()
            loadMapData()
        }

    }

    //Manejo de respuesta de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mostrarUbicacionActual()
            loadMapData()
        } else {
            Log.d("Permisos", "Permisos de ubicación no otorgados.")
        }
    }

    //Mostrar ubicación actual y actualizar en firebase
    private fun mostrarUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L
        ).setMinUpdateIntervalMillis(5000L).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.i("UbicacionActual", "Latitud: ${location.latitude}, Longitud: ${location.longitude}")

                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    if (ubicacionActualMarker == null) {
                        ubicacionActualMarker = mMap.addMarker(
                            MarkerOptions()
                                .position(currentLatLng)
                                .title("Ubicación Actual")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )
                    } else {
                        ubicacionActualMarker?.position = currentLatLng
                    }

                    val user = auth.currentUser
                    user?.let {
                        val userId = it.uid
                        val updates = mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude
                        )
                        database.child(userId).updateChildren(updates)
                            .addOnSuccessListener {
                                Log.d("UbicacionFirebase", "Ubicación actualizada en Firebase: $updates")
                            }
                            .addOnFailureListener { e ->
                                Log.e("UbicacionFirebase", "Error al actualizar ubicación en Firebase: ${e.message}")
                            }
                    }
                }
            }
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }


    override fun onStop() {
        super.onStop()
        fusedLocationClient.removeLocationUpdates(object : LocationCallback() {})
    }

    //Cargar ubicaciones d lugares representativos de Bogotá al mapa
    private fun loadMapData() {
        Log.d("Mapa", "loadMapData() fue llamada")
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("locations")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val boundsBuilder = LatLngBounds.Builder()
                    Log.d("Firebase", "Datos recibidos: ${snapshot.childrenCount} lugares encontrados.")

                    for (locationSnapshot in snapshot.children) {
                        val localizacion = locationSnapshot.getValue(Localizacion::class.java)

                        if (localizacion != null) {
                            val latLng = LatLng(localizacion.latitude, localizacion.longitude)
                            mMap.addMarker(
                                MarkerOptions().position(latLng).title(localizacion.name)
                            )
                            boundsBuilder.include(latLng)
                        }
                    }
                    val bounds = boundsBuilder.build()
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                } else {
                    Log.d("Firebase", "No hay datos disponibles.")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al obtener datos", error.toException())
            }
        })
    }
}

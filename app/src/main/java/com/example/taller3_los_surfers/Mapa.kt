package com.example.taller3_los_surfers

import android.Manifest
import android.content.ClipData.Item
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

data class Localizacion(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val name: String = ""
)

class Mapa : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var item: Item

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

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

        // Asegurarse de que el ActionBar está visible
        supportActionBar?.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.d("Mapa", "Mapa listo. Verificando permisos...")

        // Verificar permisos de ubicación
        verificarPermisosUbicacion()
    }
    // Inflar el menú
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)  // Asegúrate de que el archivo menu.xml esté correctamente configurado
        Log.d("Mapa", "Menú inflado correctamente")

        // Verificar si el menú se infló correctamente
        if (menu.size() == 0) {
            Log.e("Mapa", "Error al inflar el menú")
            // Manejar el error, por ejemplo, mostrar un mensaje al usuario
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Manejo de la selección de los items del menú
        return when (item.itemId) {
            R.id.menuLogOut -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                //finish()*/
                true
            }
            R.id.dispo -> {
                //actualizarEstadoUsuario("disponible") // Cambia el estado a "disponible"
                alternarEstadoUsuario(item)
                true
            }
            R.id.noDispo -> {
                // Acción para abrir configuración o lo que necesites
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun alternarEstadoUsuario(item: MenuItem) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid

            Log.d("EstadoUsuario", "Iniciando actualización para el usuario con ID: $userId")

            // Obtén el estado actual desde Firebase
            database.child(userId).child("status").get().addOnSuccessListener { snapshot ->
                val estadoActual = snapshot.getValue(String::class.java) ?: "no disponible"

                Log.d("EstadoUsuario", "Estado actual del usuario: $estadoActual")

                // Determina el nuevo estado en función del actual
                val nuevoEstado = if (estadoActual == "disponible") "no disponible" else "disponible"

                // Actualiza el estado en Firebase
                Log.d("EstadoUsuario", "Actualizando estado a: $nuevoEstado")
                database.child(userId).child("status").setValue(nuevoEstado)
                    .addOnSuccessListener {
                        // Cambia el ícono del item del menú según el nuevo estado
                        Log.d("EstadoUsuario", "Estado actualizado con éxito. Cambiando ícono.")
                        if (nuevoEstado == "disponible") {
                            item.setIcon(R.drawable.ic_disponible) // Ícono verde
                        } else {
                            item.setIcon(R.drawable.ic_no_disponible) // Ícono rojo
                        }
                        Toast.makeText(this, "Estado actualizado a: $nuevoEstado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("EstadoUsuario", "Error al actualizar el estado: ${e.message}")
                        Toast.makeText(this, "Error al actualizar el estado: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener { e ->
                Log.e("EstadoUsuario", "Error al obtener el estado actual: ${e.message}")
                Toast.makeText(this, "Error al obtener el estado actual: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("EstadoUsuario", "No se encontró un usuario autenticado.")
            Toast.makeText(this, "No se encontró un usuario autenticado", Toast.LENGTH_SHORT).show()
        }
    }


    /*private fun actualizarEstadoUsuario(nuevoEstado: String) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid

            // Actualiza el estado del usuario en Firebase
            database.child(userId).child("status").setValue(nuevoEstado)
                .addOnSuccessListener {
                    Toast.makeText(this, "Estado actualizado a: $nuevoEstado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar el estado: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "No se encontró un usuario autenticado", Toast.LENGTH_SHORT).show()
        }
    }*/

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
            // Los permisos ya están otorgados, cargar ubicación y datos del mapa
            mostrarUbicacionActual()
            loadMapData()
        }

    }

    // Método para manejar la respuesta de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permisos otorgados, mostrar ubicación y cargar datos
            mostrarUbicacionActual()
            loadMapData()
        } else {
            Log.d("Permisos", "Permisos de ubicación no otorgados.")
        }
    }

    private fun mostrarUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                // Cambia el color del marcador usando BitmapDescriptorFactory
                val markerOptions = MarkerOptions()
                    .position(currentLatLng)
                    .title("Ubicación Actual")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) // Cambia el color aquí

                mMap.addMarker(markerOptions)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun loadMapData() {
        Log.d("Mapa", "loadMapData() fue llamada")
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("locations")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var firstLocation: LatLng? = null
                    Log.d("Firebase", "Datos recibidos: ${snapshot.childrenCount} lugares encontrados.")

                    // Recorrer los puntos de interés y agregar marcadores
                    for (locationSnapshot in snapshot.children) {
                        val localizacion = locationSnapshot.getValue(Localizacion::class.java)

                        if (localizacion != null) {
                            val latLng = LatLng(localizacion.latitude, localizacion.longitude)
                            mMap.addMarker(
                                MarkerOptions().position(latLng).title(localizacion.name)
                            )

                            // Guardar la primera ubicación para mover la cámara
                            if (firstLocation == null) {
                                firstLocation = latLng
                            }
                        }
                    }

                    // Si tenemos al menos un marcador, mover la cámara
                    firstLocation?.let {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 12f))
                    }
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
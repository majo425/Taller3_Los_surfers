package com.example.taller3_los_surfers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.taller3_los_surfers.databinding.ActivitySingupBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class SignUp : AppCompatActivity() {

    private lateinit var binding: ActivitySingupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        verificarPermisosUbicacion()

        // Configura el botón de registro
        binding.registrarse.setOnClickListener {
            registrarUsuario()
        }

    }
    private fun registrarUsuario() {
        val email = binding.emailNuevo.text.toString()
        val password = binding.contrasenaNuevo.text.toString()
        val name = binding.nombre.text.toString()
        val lastName = binding.apellido.text.toString()
        val identificationNumber = binding.numId.text.toString()
        val latitude = binding.latitud.text.toString().toDoubleOrNull() ?: 0.0
        val longitude = binding.longitud.text.toString().toDoubleOrNull() ?: 0.0

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName("$name $lastName")
                            .build()
                        user.updateProfile(profileUpdates)

                        // Crear un objeto de MyUser y guardarlo en Realtime Database con el estado inicial "disponible"
                        val myUser = MyUser(
                            name = name,
                            lastName = lastName,
                            email = email,
                            identificationNumber = identificationNumber,
                            latitude = latitude,
                            longitude = longitude,
                            status = "disponible"
                        )
                        database.child(user.uid).setValue(myUser)

                        Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, Mapa::class.java)
                        intent.putExtra("user", auth.currentUser?.email)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Error al registrar usuario: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FirebaseAuth", task.exception?.message ?: "Error desconocido")
                }
            }
    }

    private fun verificarPermisosUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // Solicitar permisos si no están otorgados
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        } else {
            // Los permisos ya están otorgados, obtener ubicación actual
            mostrarUbicacionActual()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permisos otorgados, mostrar ubicación actual
            mostrarUbicacionActual()
        } else {
            Log.d("Permisos", "Permisos de ubicación no otorgados.")
        }
    }

    private fun mostrarUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // Asignar la latitud y longitud a los EditText correspondientes
                    binding.latitud.setText(latitude.toString())
                    binding.longitud.setText(longitude.toString())

                } else {
                    Log.d("Ubicación", "No se pudo obtener la ubicación.")
                }
            }
        }
    }
}

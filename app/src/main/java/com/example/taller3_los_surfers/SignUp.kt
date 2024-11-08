package com.example.taller3_los_surfers

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller3_los_surfers.databinding.ActivitySingupBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import data.MyUser
import java.io.File


class SignUp : AppCompatActivity() {

    private lateinit var binding: ActivitySingupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var imageUrl: Uri
    private lateinit var storage: FirebaseStorage

    private val FILE_NAME = "profile_photo.jpg"

    // Contrato para tomar foto
    private val cameraContract = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.foto.setImageURI(null)
            binding.foto.setImageURI(imageUrl)
            saveImageToGallery(imageUrl)
        } else {
            Toast.makeText(this, "No se pudo tomar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    // Contrato para abrir galería
    private val galleryContract = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.foto.setImageURI(it)
            imageUrl = it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        storage = FirebaseStorage.getInstance()
        verificarPermisosUbicacion()

        imageUrl = createImageUri()

        // Configuración de botones
        binding.camara.setOnClickListener {
            if (checkAndRequestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)) {
                imageUrl = createImageUri()
                cameraContract.launch(imageUrl)
            }
        }

        binding.galeria.setOnClickListener {
            openGallery()
        }

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

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.let {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName("$name $lastName")
                        .build()
                    user.updateProfile(profileUpdates)

                    // Subir imagen de perfil a Firebase Storage
                    uploadProfileImage { downloadUrl ->
                        val myUser = MyUser(
                            name = name,
                            lastName = lastName,
                            email = email,
                            identificationNumber = identificationNumber,
                            latitude = latitude,
                            longitude = longitude,
                            status = "disponible",
                            imageUrl = downloadUrl.toString()
                        )
                        // Guardar datos de usuario en la base de datos
                        database.child(user.uid).setValue(myUser)
                        Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, Mapa::class.java).putExtra("user", auth.currentUser?.email))
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Error al registrar usuario: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Guardar imagen de perfil
    private fun uploadProfileImage(onSuccess: (Uri) -> Unit) {
        val storageRef = storage.reference.child("profile_images/${auth.currentUser?.uid}.jpg")
        storageRef.putFile(imageUrl)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir la imagen: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //Verificar permisos de ubicación
    private fun verificarPermisosUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        } else {
            mostrarUbicacionActual()
        }
    }

    //Mostrar ubicación actual
    private fun mostrarUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    binding.latitud.setText(it.latitude.toString())
                    binding.longitud.setText(it.longitude.toString())
                }
            }
        }
    }

    //Validar permisos
    private fun checkAndRequestPermissions(permissions: Array<String>, requestCode: Int): Boolean {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        return if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, requestCode)
            false
        } else {
            true
        }
    }

    //Abrir galeria
    private fun openGallery() {
        if (checkAndRequestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), GALLERY_REQUEST_CODE)) {
            galleryContract.launch("image/*")
        }
    }

    //Crear uri de la imagen
    private fun createImageUri(): Uri {
        val image = File(filesDir, FILE_NAME)
        return FileProvider.getUriForFile(this, "com.example.taller3_los_surfers.fileprovider", image)
    }

    //Guardar imagen en la galeria
    private fun saveImageToGallery(uri: Uri) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { imageUri ->
            contentResolver.openOutputStream(imageUri).use { outputStream ->
                contentResolver.openInputStream(uri)?.copyTo(outputStream!!)
            }
            Toast.makeText(this, "Imagen guardada en la galería", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraContract.launch(imageUrl)
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
            GALLERY_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    galleryContract.launch("image/*")
                } else {
                    Toast.makeText(this, "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 10
        private const val GALLERY_REQUEST_CODE = 11
    }
}

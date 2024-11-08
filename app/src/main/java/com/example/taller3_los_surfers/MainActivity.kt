package com.example.taller3_los_surfers

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3_los_surfers.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.registro.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }
        binding.login.setOnClickListener {
            val email = binding.emailAddress.text.toString()
            val password = binding.contrasena.text.toString()
            signInUser(email, password)
        }
    }
    override fun onStart() {
        super.onStart()
        //Verificar si ya hay un usuario autenticado
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    //Iniciar sesión
    private fun signInUser(email: String, password: String) {
        if (validateForm()) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, Mapa::class.java)
                        intent.putExtra("user", auth.currentUser?.email)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Fallo en la autenticación.", Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val intent = Intent(this, Mapa::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
            finish()
        } else {
            binding.emailAddress.setText("")
            binding.contrasena.setText("")
        }
    }

    //Validar formulario de registro
    private fun validateForm(): Boolean {
        val email = binding.emailAddress.text.toString()
        val password = binding.contrasena.text.toString()

        var valid = true
        if (TextUtils.isEmpty(email)) {
            binding.emailAddress.error = "Campo requerido."
            valid = false
        }
        if (TextUtils.isEmpty(password)) {
            binding.contrasena.error = "Campo requerido."
            valid = false
        }

        return valid
    }
}
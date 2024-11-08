package com.example.taller3_los_surfers

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import data.MyUser

class ServicioCambiosUsuario : Service() {

    private lateinit var database: DatabaseReference
    private lateinit var userListener: ValueEventListener

    override fun onCreate() {
        super.onCreate()
        database = Firebase.database.reference.child("users")

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(MyUser::class.java)
                    user?.let {
                        if (it.status == "disponible") {
                            Toast.makeText(applicationContext, "${it.name} está disponible", Toast.LENGTH_SHORT).show()
                            addUserToAvailableList(it)
                        } else if (it.status == "no disponible") {
                            removeUserFromAvailableList(it)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserStatusService", "Error al escuchar los cambios: ${error.message}")
            }
        }
        database.addValueEventListener(userListener)
    }

    private fun addUserToAvailableList(user: MyUser) {
        Log.d("UserStatusService", "Usuario agregado a la lista de disponibles: ${user.name}")
    }

    private fun removeUserFromAvailableList(user: MyUser) {
        Log.d("UserStatusService", "Usuario eliminado de la lista de disponibles: ${user.name}")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        database.removeEventListener(userListener)
    }
}

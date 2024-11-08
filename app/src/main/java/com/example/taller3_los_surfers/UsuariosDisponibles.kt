package com.example.taller3_los_surfers

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import data.MyUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database

class UsuariosDisponibles : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UsuariosDisponiblesAdapter
    private val users = mutableListOf<MyUser>()
    private lateinit var currentUserEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usuarios_disponibles)

        recyclerView = findViewById(R.id.recycler_view_usuarios)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val user = FirebaseAuth.getInstance().currentUser
        currentUserEmail = user?.email ?: ""

        adapter = UsuariosDisponiblesAdapter(users, currentUserEmail) { selectedUser ->
            val intent = Intent(this, MapUsers::class.java)
            intent.putExtra("selectedUser", selectedUser)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        fetchUsersFromFirebase()
    }

    private fun fetchUsersFromFirebase() {
        val database = Firebase.database.reference.child("users")
        database.get().addOnSuccessListener { snapshot ->
            users.clear()
            for (child in snapshot.children) {
                val user = child.getValue(MyUser::class.java)

                if (user?.status == "disponible" && user.email != currentUserEmail) {
                    users.add(user)
                }
            }
            adapter.notifyDataSetChanged()
        }
    }
}
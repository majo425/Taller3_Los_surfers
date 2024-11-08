package com.example.taller3_los_surfers

import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import data.MyUser
import java.io.File

class UsuariosDisponiblesAdapter(
    private val contacts: List<MyUser>,
    private val currentUserEmail: String,
    private val onLocationClick: (MyUser) -> Unit
) : RecyclerView.Adapter<UsuariosDisponiblesAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewContact)
        val nameTextView: TextView = view.findViewById(R.id.textViewContactName)
        val locationButton: Button = view.findViewById(R.id.buttonViewLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_adapter_usuarios_disponibles, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]

        if (contact.email == currentUserEmail) {
            return
        }

        holder.nameTextView.text = "${contact.name} ${contact.lastName}"

        if (contact.imageUrl.isNotEmpty()) {
            try {
                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(contact.imageUrl)
                val localFile = File.createTempFile("profile", "jpg")

                storageReference.getFile(localFile).addOnSuccessListener {
                    val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                    holder.imageView.setImageBitmap(bitmap)
                }.addOnFailureListener {
                    holder.imageView.setImageResource(R.drawable.ic_launcher_background)
                }
            } catch (e: IllegalArgumentException) {
                // Manejar URLs inválidas
                holder.imageView.setImageResource(R.drawable.ic_launcher_background)
            }
        } else {
            //Imagen predeterminada si hay un null
            holder.imageView.setImageResource(R.drawable.ic_launcher_background)
        }

        holder.locationButton.setOnClickListener {
            onLocationClick(contact)
        }
    }

    override fun getItemCount(): Int = contacts.size
}

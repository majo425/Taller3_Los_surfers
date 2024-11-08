package data

import java.io.Serializable


data class MyUser(
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val identificationNumber: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    val status: String = "",
    val imageUrl: String = ""
): Serializable



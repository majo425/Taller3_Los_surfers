package data

data class MyUser(
    val name: String,
    val lastName: String,
    val email: String,
    val identificationNumber: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val imageUrl: String
)
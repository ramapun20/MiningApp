package com.example.miningapp.domain.model

data class Product(
    val id: String,
    val title: String,
    val detail: String,
    val price: String,
    val category: ProductCategory,
    val imageName: String,
    val specifications: List<String> = emptyList()
)

enum class ProductCategory(val label: String) {
    ELECTRONIC("Electronic"),
    ELECTRIC("Electric"),
    DRILLING("Drilling"),
    CAR_RENTALS("Car rentals"),
    JOBS("Jobs")
}

enum class MapPlaceType(val label: String) {
    MINE("Mines"),
    MARKET("Markets")
}

data class MapPlace(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val type: MapPlaceType
)

data class ChatMessage(
    val id: Long,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isOutgoing: Boolean
)

data class ChatContact(
    val id: String,
    val name: String,
    val description: String,
    val lastMessage: String
)

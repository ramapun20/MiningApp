package com.example.miningapp.domain.usecase

import com.example.miningapp.domain.model.MapPlace
import com.example.miningapp.domain.model.Product
import com.example.miningapp.domain.repository.KanzRepository

class GetProducts(private val repository: KanzRepository) {
    operator fun invoke(): List<Product> = repository.products()
}

class GetMapPlaces(private val repository: KanzRepository) {
    operator fun invoke(): List<MapPlace> = repository.mapPlaces()
}

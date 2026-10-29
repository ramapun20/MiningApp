package com.example.miningapp.domain.repository

import com.example.miningapp.domain.model.MapPlace
import com.example.miningapp.domain.model.Product

interface KanzRepository {
    fun products(): List<Product>
    fun mapPlaces(): List<MapPlace>
}

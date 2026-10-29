package com.example.miningapp.data.repository

import com.example.miningapp.domain.model.MapPlace
import com.example.miningapp.domain.model.MapPlaceType
import com.example.miningapp.domain.model.Product
import com.example.miningapp.domain.model.ProductCategory
import com.example.miningapp.domain.repository.KanzRepository

class DemoKanzRepository : KanzRepository {
    override fun products() = listOf(
        Product(
            id = "ctx3030",
            title = "Gold detection machine",
            detail = "CTX3030",
            price = "5,000 USD",
            category = ProductCategory.ELECTRONIC,
            imageName = "metal_detector",
            specifications = listOf(
                "Manufacturer: Loop Techno Systems, India",
                "Year: 2018"
            )
        ),
        Product(
            id = "jackhammer",
            title = "Electric Jack Hammer",
            detail = "1700 Watt Light Weight",
            price = "3,000 USD",
            category = ProductCategory.ELECTRIC,
            imageName = "jack_hammer"
        ),
        Product(
            id = "washing-plant",
            title = "Gold Washing Plant",
            detail = "Gold Wash Process & Equipment",
            price = "50,000 USD",
            category = ProductCategory.DRILLING,
            imageName = "gold_wash_plant"
        ),
        Product(
            id = "pickup-rental",
            title = "Mining Pickup Rental",
            detail = "4x4 vehicle for field transport",
            price = "Price on request",
            category = ProductCategory.CAR_RENTALS,
            imageName = "car_icon"
        ),
        Product(
            id = "mining-jobs",
            title = "Mining field opportunities",
            detail = "Jobs and services in Mauritania",
            price = "View opportunities",
            category = ProductCategory.JOBS,
            imageName = "jobs_icon"
        )
    )

    override fun mapPlaces() = listOf(
        MapPlace("nouakchott-mining", "Nouakchott Prospecting Area", "Mining and prospecting zone near Nouakchott", 18.0735, -15.9582, MapPlaceType.MINE),
        MapPlace("atar-mining", "Atar Mining Zone", "Mining and prospecting zone near Atar", 20.5169, -13.0499, MapPlaceType.MINE),
        MapPlace("akjoujt-mine", "Guelb Moghrein Mine", "Copper and gold mine near Akjoujt", 19.7465, -14.3856, MapPlaceType.MINE),
        MapPlace("nouakchott-market", "Nouakchott Market Area", "Equipment and services market area", 18.0864, -15.9753, MapPlaceType.MARKET),
        MapPlace("atar-market", "Atar Market Area", "Local supplies and services market area", 20.5177, -13.0487, MapPlaceType.MARKET),
        MapPlace("akjoujt-market", "Akjoujt Market Area", "Local supplies and services market area", 19.7462, -14.3918, MapPlaceType.MARKET)
    )

}

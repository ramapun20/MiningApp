package com.example.miningapp

import com.example.miningapp.data.repository.DemoKanzRepository
import com.example.miningapp.domain.model.MapPlaceType
import com.example.miningapp.domain.model.ProductCategory
import com.example.miningapp.domain.usecase.SendChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KanzRepositoryTest {
    private val repository = DemoKanzRepository()

    @Test
    fun demoProductsCoverRequestedMarketplaceCategories() {
        val categories = repository.products().map { it.category }.toSet()

        assertEquals(
            setOf(
                ProductCategory.ELECTRONIC,
                ProductCategory.ELECTRIC,
                ProductCategory.DRILLING,
                ProductCategory.CAR_RENTALS,
                ProductCategory.JOBS
            ),
            categories
        )
    }

    @Test
    fun mapDataContainsMineAndMarketZones() {
        val places = repository.mapPlaces()

        assertTrue(places.any { it.name.contains("Atar") && it.type == MapPlaceType.MINE })
        assertTrue(places.any { it.name.contains("Akjoujt") && it.type == MapPlaceType.MARKET })
        assertTrue(places.any { it.name.contains("Nouakchott") })
    }

    @Test
    fun sendingMessageAddsOutgoingMessageToConversation() {
        val messages = SendChatMessage(repository)("ali-ibrahim", "Tester", "  Is this available?  ")
        val sent = messages.last()

        assertEquals("Is this available?", sent.text)
        assertEquals("Tester", sent.sender)
        assertTrue(sent.isOutgoing)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankChatMessageIsRejected() {
        SendChatMessage(repository)("ali-ibrahim", "Tester", "  ")
    }
}

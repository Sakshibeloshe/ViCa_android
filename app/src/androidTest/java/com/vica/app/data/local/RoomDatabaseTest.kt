package com.vica.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vica.app.data.local.dao.CardDao
import com.vica.app.data.local.entities.CardEntity
import com.vica.app.data.local.entities.FieldEntity
import com.vica.app.data.model.CardTheme
import com.vica.app.data.model.CardType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {
    private lateinit var cardDao: CardDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        cardDao = db.cardDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() { db.close() }

    @Test
    fun writeCardAndReadInList() = runBlocking {
        val cardId = UUID.randomUUID()
        val card = CardEntity(
            id = cardId,
            typeRaw = CardType.PERSONAL.rawValue,
            createdAt = Date(), updatedAt = Date(),
            themeHex = CardTheme.PINK.rawValue,
            displayName = "Sakshi", subtitle = "Android Developer",
            org = "ViCa", bio = "Loving Kotlin"
        )
        val field = FieldEntity(
            id = UUID.randomUUID(), cardId = cardId,
            key = "instagram", label = "Instagram", value = "@sakshi",
            kindRaw = "handle", orderIndex = 0
        )
        cardDao.insertCardWithFields(card, listOf(field))
        val allCards = cardDao.getAllCardsWithFields().first()
        assertEquals(1, allCards.size)
        assertEquals("Sakshi", allCards[0].card.displayName)
        assertEquals(1, allCards[0].fields.size)
        assertEquals("@sakshi", allCards[0].fields[0].value)
    }

    @Test
    fun testCascadeDelete() = runBlocking {
        val cardId = UUID.randomUUID()
        val card = CardEntity(
            id = cardId, typeRaw = CardType.PERSONAL.rawValue,
            createdAt = Date(), updatedAt = Date(),
            themeHex = CardTheme.PINK.rawValue,
            displayName = "Sakshi", subtitle = "Android Developer",
            org = "ViCa", bio = "Loving Kotlin"
        )
        val field = FieldEntity(
            id = UUID.randomUUID(), cardId = cardId,
            key = "instagram", label = "Instagram", value = "@sakshi",
            kindRaw = "handle", orderIndex = 0
        )
        cardDao.insertCardWithFields(card, listOf(field))
        assertEquals(1, cardDao.getAllCardsWithFields().first().size)
        cardDao.deleteCard(card)
        assertEquals(0, cardDao.getAllCardsWithFields().first().size)
        assertEquals(null, cardDao.getCardWithFieldsById(cardId))
    }
}

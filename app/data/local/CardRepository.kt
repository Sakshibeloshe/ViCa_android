package com.vica.app.data.local

import com.vica.app.data.fields.FieldCatalog
import com.vica.app.data.local.dao.CardDao
import com.vica.app.data.local.entities.CardEntity
import com.vica.app.data.local.entities.FieldEntity
import com.vica.app.data.model.CardTheme
import com.vica.app.data.model.CardType
import java.util.Date
import java.util.UUID

class CardRepository(private val cardDao: CardDao) {

    suspend fun createCard(
        type: CardType,
        values: Map<String, String>,
        photoBytes: ByteArray?,
        theme: CardTheme = CardTheme.PINK,
        usesProfileName: Boolean = true,
        usesProfileTitle: Boolean = true,
        usesProfileCompany: Boolean = true,
        usesProfilePhoto: Boolean = true
    ) {
        val cardId = UUID.randomUUID()
        
        // Resolve display name, subtitle, org, and bio matching iOS CardRepository.swift logic
        val displayName = values["fullName"] ?: values["nickname"] ?: values["displayName"] ?: values["title"] ?: "Untitled"
        val subtitle = values["title"] ?: values["eventBadge"] ?: values["roleAtEvent"] ?: values["description"]
        val org = values["company"] ?: values["eventName"]
        val bio = values["bio"]

        val card = CardEntity(
            id = cardId,
            typeRaw = type.rawValue,
            createdAt = Date(),
            updatedAt = Date(),
            themeHex = theme.rawValue,
            photoData = photoBytes,
            displayName = displayName,
            subtitle = subtitle,
            org = org,
            bio = bio,
            isFavorite = false,
            isReceived = false,
            usesProfileName = usesProfileName,
            usesProfileTitle = usesProfileTitle,
            usesProfileCompany = usesProfileCompany,
            usesProfilePhoto = usesProfilePhoto
        )

        // Save all other fields dynamically
        val definitions = FieldCatalog.fields(type)
        val fields = mutableListOf<FieldEntity>()

        definitions.forEachIndexed { index, def ->
            val rawValue = values[def.key]?.trim() ?: ""
            if (rawValue.isNotEmpty()) {
                val skipFields = listOf("fullName", "nickname", "displayName", "title", "company", "eventBadge", "eventName", "bio", "roleAtEvent")
                if (!skipFields.contains(def.key)) {
                    fields.add(
                        FieldEntity(
                            id = UUID.randomUUID(),
                            cardId = cardId,
                            key = def.key,
                            label = def.label,
                            value = rawValue,
                            kindRaw = def.kind.rawValue,
                            orderIndex = index
                        )
                    )
                }
            }
        }

        cardDao.insertCardWithFields(card, fields)
    }
}

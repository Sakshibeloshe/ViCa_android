package com.vica.app.data.model

import com.vica.app.data.local.entities.CardEntity
import com.vica.app.data.local.entities.CardWithFields
import com.vica.app.data.local.entities.FieldEntity
import java.util.Date
import java.util.UUID

/**
 * Pure Kotlin domain model — used by ViewModels and UI.
 * Mapped from Room's CardWithFields relation.
 */
data class CardModel(
    val id: UUID,
    val type: CardType,
    val displayName: String,
    val subtitle: String?,
    val org: String?,
    val bio: String?,
    val themeHex: String,
    val theme: CardTheme,
    val photoData: ByteArray?,
    val fields: List<CardField>,
    val isFavorite: Boolean,
    val isReceived: Boolean,
    val note: String?,
    val createdAt: Date
) {
    companion object {
        fun from(cardWithFields: CardWithFields): CardModel {
            val entity = cardWithFields.card
            return CardModel(
                id          = entity.id,
                type        = CardType.fromRaw(entity.typeRaw),
                displayName = entity.displayName,
                subtitle    = entity.subtitle,
                org         = entity.org,
                bio         = entity.bio,
                themeHex    = entity.themeHex,
                theme       = CardTheme.fromRaw(entity.themeHex),
                photoData   = entity.photoData,
                fields      = cardWithFields.fields.map { CardField.from(it) },
                isFavorite  = entity.isFavorite,
                isReceived  = entity.isReceived,
                note        = entity.note,
                createdAt   = entity.createdAt
            )
        }
    }
}

data class CardField(
    val id: UUID,
    val key: String,
    val label: String,
    val value: String,
    val kind: String
) {
    companion object {
        fun from(entity: FieldEntity) = CardField(
            id    = entity.id,
            key   = entity.key,
            label = entity.label,
            value = entity.value,
            kind  = entity.kindRaw
        )
    }
}

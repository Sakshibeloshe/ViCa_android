package com.vica.app.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class CardWithFields(
    @Embedded val card: CardEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "cardId"
    )
    val fields: List<FieldEntity>
)

package com.vica.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "card_fields",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class FieldEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val cardId: UUID,
    val key: String,
    val label: String,
    val value: String,
    val kindRaw: String,
    val orderIndex: Int
)

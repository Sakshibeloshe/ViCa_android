package com.vica.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val typeRaw: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val themeHex: String,
    val photoData: ByteArray? = null,
    
    // Core Fields
    val displayName: String,
    val subtitle: String?,
    val org: String?,
    val bio: String?,
    
    // Inbox & State
    val isFavorite: Boolean = false,
    val isReceived: Boolean = false,
    val folderId: UUID? = null,
    val note: String? = null,
    val tagsRaw: String? = null, // Comma-separated tags
    
    // Profile sync flags
    val usesProfileName: Boolean = true,
    val usesProfileTitle: Boolean = true,
    val usesProfileCompany: Boolean = true,
    val usesProfilePhoto: Boolean = true
)

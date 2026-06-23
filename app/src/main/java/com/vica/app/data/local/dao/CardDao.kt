package com.vica.app.data.local.dao

import androidx.room.*
import com.vica.app.data.local.entities.CardEntity
import com.vica.app.data.local.entities.FieldEntity
import com.vica.app.data.local.entities.CardWithFields
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CardDao {
    @Transaction
    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun getAllCardsWithFields(): Flow<List<CardWithFields>>

    @Transaction
    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardWithFieldsById(cardId: UUID): CardWithFields?

    @Transaction
    @Query("SELECT * FROM cards WHERE isReceived = 1 ORDER BY createdAt DESC")
    fun getReceivedCardsWithFields(): Flow<List<CardWithFields>>

    @Transaction
    @Query("SELECT * FROM cards WHERE isReceived = 0 ORDER BY createdAt DESC")
    fun getMyCardsWithFields(): Flow<List<CardWithFields>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFields(fields: List<FieldEntity>)

    @Transaction
    suspend fun insertCardWithFields(card: CardEntity, fields: List<FieldEntity>) {
        deleteFieldsByCardId(card.id)
        insertCard(card)
        insertFields(fields)
    }

    @Query("DELETE FROM card_fields WHERE cardId = :cardId")
    suspend fun deleteFieldsByCardId(cardId: UUID)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: UUID)
}

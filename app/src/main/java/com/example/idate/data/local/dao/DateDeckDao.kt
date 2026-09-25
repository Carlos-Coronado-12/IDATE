package com.example.idate.data.local.dao

import androidx.room.*
import com.example.idate.data.local.entity.DateDeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DateDeckDao {
    @Query("SELECT * FROM date_decks WHERE targetGroupId IS NULL ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<DateDeckEntity>>

    @Query("SELECT * FROM date_decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: String): DateDeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DateDeckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<DateDeckEntity>)

    @Query("DELETE FROM date_decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: String)

    @Query("DELETE FROM date_decks")
    suspend fun deleteAllDecks()
}

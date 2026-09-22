package com.example.idate.data.local.dao

import androidx.room.*
import com.example.idate.data.local.entity.MatchSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchSessionDao {
    @Query("SELECT * FROM match_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<MatchSessionEntity>>

    @Query("SELECT * FROM match_sessions WHERE roomCode = :roomCode LIMIT 1")
    suspend fun getSessionByCode(roomCode: String): MatchSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MatchSessionEntity)

    @Update
    suspend fun updateSession(session: MatchSessionEntity)

    @Query("DELETE FROM match_sessions WHERE roomCode = :roomCode")
    suspend fun deleteSession(roomCode: String)
}

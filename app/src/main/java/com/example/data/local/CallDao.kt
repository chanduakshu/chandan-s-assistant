package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE id = :id")
    suspend fun getCallById(id: Long): CallEntity?

    @Query("SELECT * FROM calls WHERE category = :category ORDER BY timestamp DESC")
    fun getCallsByCategory(category: String): Flow<List<CallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity): Long

    @Update
    suspend fun updateCall(call: CallEntity)

    @Delete
    suspend fun deleteCall(call: CallEntity)

    @Query("DELETE FROM calls WHERE id = :id")
    suspend fun deleteCallById(id: Long)

    @Query("UPDATE calls SET isTrusted = :isTrusted WHERE id = :id")
    suspend fun updateTrusted(id: Long, isTrusted: Boolean)

    @Query("UPDATE calls SET isSpam = :isSpam, category = 'Spam / Telemarketing' WHERE id = :id")
    suspend fun markAsSpam(id: Long, isSpam: Boolean)

    @Query("UPDATE calls SET transcriptJson = '[]' WHERE id = :id")
    suspend fun deleteTranscript(id: Long)

    @Query("UPDATE calls SET hasRecording = 0 WHERE id = :id")
    suspend fun deleteRecording(id: Long)
}

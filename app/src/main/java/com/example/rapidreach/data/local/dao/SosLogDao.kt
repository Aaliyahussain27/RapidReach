package com.example.rapidreach.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rapidreach.data.local.entity.SosLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SosLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SosLogEntity): Long

    @Query("SELECT * FROM sos_logs WHERE synced = 0")
    suspend fun getUnsynced(): List<SosLogEntity>

    @Query("UPDATE sos_logs SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM sos_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SosLogEntity>>

    @Query("UPDATE sos_logs SET audioFilePath = :path, synced = 0 WHERE id = :id")
    suspend fun updateAudioPath(id: Long, path: String)
}

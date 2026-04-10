package com.example.rapidreach.data.local.dao

import androidx.room.*
import com.example.rapidreach.data.local.entity.CustomHelplineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomHelplineDao {
    @Query("SELECT * FROM custom_helplines ORDER BY name ASC")
    fun getAllCustomHelplines(): Flow<List<CustomHelplineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(helpline: CustomHelplineEntity)

    @Delete
    suspend fun delete(helpline: CustomHelplineEntity)
}

package com.example.rapidreach.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.rapidreach.data.local.dao.SosLogDao
import com.example.rapidreach.data.local.entity.SosLogEntity

@Database(entities = [SosLogEntity::class], version = 1, exportSchema = false)
abstract class RapidReachDatabase : RoomDatabase() {
    abstract fun sosLogDao(): SosLogDao

    companion object {
        @Volatile
        private var INSTANCE: RapidReachDatabase? = null

        fun getInstance(context: Context): RapidReachDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    RapidReachDatabase::class.java,
                    "rapidreach_db"
                ).build().also { INSTANCE = it }
            }
    }
}

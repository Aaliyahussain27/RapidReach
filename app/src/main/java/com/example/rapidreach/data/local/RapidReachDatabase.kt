package com.example.rapidreach.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.rapidreach.data.local.dao.SosLogDao
import com.example.rapidreach.data.local.dao.CustomHelplineDao
import com.example.rapidreach.data.local.entity.SosLogEntity
import com.example.rapidreach.data.local.entity.CustomHelplineEntity

@Database(entities = [SosLogEntity::class, CustomHelplineEntity::class], version = 2, exportSchema = false)
abstract class RapidReachDatabase : RoomDatabase() {
    abstract fun sosLogDao(): SosLogDao
    abstract fun customHelplineDao(): CustomHelplineDao

    companion object {
        @Volatile
        private var INSTANCE: RapidReachDatabase? = null

        fun getInstance(context: Context): RapidReachDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    RapidReachDatabase::class.java,
                    "rapidreach_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}

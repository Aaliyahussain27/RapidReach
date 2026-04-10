package com.example.rapidreach.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_helplines")
data class CustomHelplineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val number: String,
    val category: String = "PERSONAL"
)

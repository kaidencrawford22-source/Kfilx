package com.kflix.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconRes: String,
    val age: Int? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

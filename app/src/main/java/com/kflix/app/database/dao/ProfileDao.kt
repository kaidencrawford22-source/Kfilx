package com.kflix.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kflix.app.models.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAll(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAllSync(): List<Profile>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): Profile?

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile)

    @Query("UPDATE profiles SET name = :name, iconRes = :iconRes, age = :age WHERE id = :id")
    suspend fun update(id: String, name: String, iconRes: String, age: Int?)

    @Query("UPDATE profiles SET isDefault = 0 WHERE isDefault = 1")
    suspend fun clearDefault()

    @Query("UPDATE profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: String)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getCount(): Int
}

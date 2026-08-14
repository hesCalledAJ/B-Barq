package com.aliJafari.bbarq.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aliJafari.bbarq.data.model.Place

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY id ASC")
    fun getAll(): List<Place>

    @Insert
    fun insert(place: Place): Long

    @Update
    fun update(place: Place)

    @Delete
    fun delete(place: Place)
}

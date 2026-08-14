package com.aliJafari.bbarq.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val billId: String,
    val colorKey: String,
    val iconKey: String,
    val reminderOffsetsMask: Int = 0 // bitwise OR of ReminderOffset.bit, 0 = reminders off
) {
    val remindersEnabled: Boolean get() = reminderOffsetsMask != 0
}

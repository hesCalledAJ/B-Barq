package com.aliJafari.bbarq.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outages")
data class Outage(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "reason") val reason: String?,
    @ColumnInfo(name = "date") val date: String?,
    @ColumnInfo(name = "outageStartTime") val startTime: String?,
    @ColumnInfo(name = "outageEndTime") val endTime: String?,
    @ColumnInfo(name = "billId") val billId: String?,
    @ColumnInfo(name = "address") val address: String?,

    )
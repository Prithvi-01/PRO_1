package com.sensor.heatlh

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SymptomsData(
    @ColumnInfo(index = true)
    @PrimaryKey(autoGenerate = true)
    var id:Int = 0,
    var patientId: Long = 0,
    var symptoms: String? = "0",
    var symptomsStarRate: String? = ""

)

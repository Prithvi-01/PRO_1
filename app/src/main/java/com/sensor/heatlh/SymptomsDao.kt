package com.sensor.heatlh

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
abstract class SymptomsDao {

   /* @Insert(onConflict = OnConflictStrategy.REPLACE)
     abstract fun insert(symptomsData: List<SymptomsData>)*/

}
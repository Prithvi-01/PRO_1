package com.sensor.heatlh

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PatientData::class/*, SymptomsData::class*/ ],version = 1, exportSchema = false)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun symptomsDao(): SymptomsDao

    companion object{
        private lateinit var DB_INSTANCE: HealthDatabase
        fun createDbInstance(context: Context) : HealthDatabase{
//            if(DB_INSTANCE == null) {
                DB_INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "Health_DB"
                ).allowMainThreadQueries().build()
                return DB_INSTANCE
           /* } else {
                return DB_INSTANCE
            }*/
        }

        fun getDBInstance(context: Context): HealthDatabase{
            return createDbInstance(context)
        }
    }
}
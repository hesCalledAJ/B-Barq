package com.aliJafari.bbarq.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aliJafari.bbarq.data.local.dao.OutageDao
import com.aliJafari.bbarq.data.local.dao.PlaceDao
import com.aliJafari.bbarq.data.model.Outage
import com.aliJafari.bbarq.data.model.Place

@Database(entities = [Outage::class, Place::class], version = 2, exportSchema = false)
abstract class ADatabase : RoomDatabase() {
    abstract fun OutageDao(): OutageDao
    abstract fun PlaceDao(): PlaceDao

    companion object {
        @Volatile
        private var instance: ADatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS places (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        billId TEXT NOT NULL,
                        colorKey TEXT NOT NULL,
                        iconKey TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): ADatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ADatabase::class.java,
                    "bbarq.db"
                )
                    .addMigrations(migration1To2)
                    .build()
                    .also { instance = it }
            }
    }
}

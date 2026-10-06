package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ExportHistoryEntity::class], version = 1, exportSchema = false)
abstract class ApolloDatabase : RoomDatabase() {
    abstract fun exportHistoryDao(): ExportHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ApolloDatabase? = null

        fun getInstance(context: Context): ApolloDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApolloDatabase::class.java,
                    "apollo_photo_export.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

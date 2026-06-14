package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlayerEntity::class,
        TeamEntity::class,
        MatchEntity::class,
        DeliveryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CricketDatabase : RoomDatabase() {
    abstract val playerDao: PlayerDao
    abstract val teamDao: TeamDao
    abstract val matchDao: MatchDao
    abstract val deliveryDao: DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: CricketDatabase? = null

        fun getInstance(context: Context): CricketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketDatabase::class.java,
                    "cricket_scorer_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

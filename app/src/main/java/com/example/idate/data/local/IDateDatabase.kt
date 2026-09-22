package com.example.idate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.idate.data.local.dao.LikedPlanDao
import com.example.idate.data.local.dao.MatchSessionDao
import com.example.idate.data.local.dao.PlanDao
import com.example.idate.data.local.entity.LikedPlanEntity
import com.example.idate.data.local.entity.MatchSessionEntity
import com.example.idate.data.local.entity.PlanEntity

@Database(
    entities = [
        PlanEntity::class,
        LikedPlanEntity::class,
        MatchSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class IDateDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun likedPlanDao(): LikedPlanDao
    abstract fun matchSessionDao(): MatchSessionDao

    companion object {
        @Volatile
        private var INSTANCE: IDateDatabase? = null

        fun getDatabase(context: Context): IDateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IDateDatabase::class.java,
                    "idate_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}

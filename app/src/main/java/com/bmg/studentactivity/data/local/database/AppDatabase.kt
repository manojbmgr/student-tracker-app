package com.bmg.studentactivity.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bmg.studentactivity.data.local.dao.ActivityDao
import com.bmg.studentactivity.data.local.dao.TimetableDao
import com.bmg.studentactivity.data.local.entities.ActivityEntity
import com.bmg.studentactivity.data.local.entities.TimetableEntity

@Database(
    entities = [ActivityEntity::class, TimetableEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun timetableDao(): TimetableDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_activity_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


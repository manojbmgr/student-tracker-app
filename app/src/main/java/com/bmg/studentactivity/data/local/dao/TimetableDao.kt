package com.bmg.studentactivity.data.local.dao

import androidx.room.*
import com.bmg.studentactivity.data.local.entities.TimetableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable")
    fun getAllTimetableEntries(): Flow<List<TimetableEntity>>
    
    @Query("SELECT * FROM timetable WHERE day = :day")
    fun getTimetableByDay(day: String): Flow<List<TimetableEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableEntry(entry: TimetableEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableEntries(entries: List<TimetableEntity>)
    
    @Update
    suspend fun updateTimetableEntry(entry: TimetableEntity)
    
    @Delete
    suspend fun deleteTimetableEntry(entry: TimetableEntity)
    
    @Query("DELETE FROM timetable WHERE id = :id")
    suspend fun deleteTimetableEntryById(id: String)
    
    @Query("DELETE FROM timetable")
    suspend fun deleteAll()
}


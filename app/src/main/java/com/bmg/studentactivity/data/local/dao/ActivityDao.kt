package com.bmg.studentactivity.data.local.dao

import androidx.room.*
import com.bmg.studentactivity.data.local.entities.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities")
    fun getAllActivities(): Flow<List<ActivityEntity>>
    
    @Query("SELECT * FROM activities WHERE status = :status")
    fun getActivitiesByStatus(status: String): Flow<List<ActivityEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)
    
    @Update
    suspend fun updateActivity(activity: ActivityEntity)
    
    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)
    
    @Query("DELETE FROM activities")
    suspend fun deleteAll()
}


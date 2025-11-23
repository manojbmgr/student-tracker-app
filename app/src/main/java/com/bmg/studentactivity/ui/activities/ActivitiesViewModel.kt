package com.bmg.studentactivity.ui.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmg.studentactivity.data.models.ActivitiesData
import com.bmg.studentactivity.data.models.StudentActivities
import com.bmg.studentactivity.data.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    private val activityRepository: ActivityRepository
) : ViewModel() {
    
    private val _studentsData = MutableLiveData<List<StudentActivities>>()
    val studentsData: LiveData<List<StudentActivities>> = _studentsData
    
    private val _filteredStudentsData = MutableLiveData<List<StudentActivities>>()
    val filteredStudentsData: LiveData<List<StudentActivities>> = _filteredStudentsData
    
    private val _activitiesData = MutableLiveData<ActivitiesData?>()
    val activitiesData: LiveData<ActivitiesData?> = _activitiesData
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private var currentFilter: ActivityFilter = ActivityFilter.ALL
    private var lastStudentEmail: String? = null
    private var lastDay: String? = null
    private var lastStatus: String? = null
    
    enum class ActivityFilter {
        ALL, COMPLETED, PENDING, OVERDUE
    }
    
    fun loadActivities(
        studentEmail: String? = null,
        day: String? = null,
        status: String? = null
    ) {
        // Store filter parameters for refresh
        lastStudentEmail = studentEmail
        lastDay = day
        lastStatus = status
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                android.util.Log.d("ActivitiesViewModel", "loadActivities called: studentEmail=$studentEmail, day=$day, status=$status")
                val result = activityRepository.getActivities(studentEmail, day, status)
                android.util.Log.d("ActivitiesViewModel", "Repository call completed, result: ${result.isSuccess}")
                result.onSuccess { response ->
                    android.util.Log.d("ActivitiesViewModel", "Response success: ${response.success}, data: ${response.data != null}")
                    if (response.success && response.data != null) {
                        _activitiesData.value = response.data
                        // If students array exists, use it; otherwise use activities list
                        if (response.data.students != null && response.data.students.isNotEmpty()) {
                            android.util.Log.d("ActivitiesViewModel", "Found ${response.data.students.size} students")
                            _studentsData.value = response.data.students
                            applyFilter(currentFilter)
                        } else if (response.data.activities != null) {
                            android.util.Log.d("ActivitiesViewModel", "Found ${response.data.activities.size} activities")
                            // Convert activities list to student activities format
                            // Try to get studentEmail from first activity if available
                            val firstActivityEmail = response.data.activities.firstOrNull()?.studentEmail
                            val studentActivities = StudentActivities(
                                studentEmail = firstActivityEmail ?: studentEmail ?: "Unknown",
                                studentName = response.data.activities.firstOrNull()?.studentName,
                                activities = response.data.activities,
                                statistics = response.data.statistics ?: com.bmg.studentactivity.data.models.ActivityStatistics()
                            )
                            _studentsData.value = listOf(studentActivities)
                            applyFilter(currentFilter)
                        } else {
                            android.util.Log.d("ActivitiesViewModel", "No students or activities found in response")
                            _studentsData.value = emptyList()
                            _filteredStudentsData.value = emptyList()
                        }
                    } else {
                        val errorMsg = response.message ?: "Failed to load activities"
                        android.util.Log.e("ActivitiesViewModel", "Response not successful: $errorMsg")
                        _error.value = errorMsg
                    }
                }.onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to load activities"
                    android.util.Log.e("ActivitiesViewModel", "Repository call failed: $errorMsg", exception)
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Not authenticated"
                android.util.Log.e("ActivitiesViewModel", "Exception in loadActivities: $errorMsg", e)
                _error.value = errorMsg
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun filterActivities(filter: ActivityFilter) {
        currentFilter = filter
        applyFilter(filter)
    }
    
    fun refreshActivities() {
        // Reload activities with last used filter parameters
        loadActivities(lastStudentEmail, lastDay, lastStatus)
    }
    
    private fun applyFilter(filter: ActivityFilter) {
        val allStudents = _studentsData.value ?: emptyList()
        
        val filtered = when (filter) {
            ActivityFilter.ALL -> allStudents.map { student ->
                student.copy(
                    activities = student.activities,
                    statistics = calculateStatistics(student.activities)
                )
            }
            
            ActivityFilter.COMPLETED -> allStudents.map { student ->
                val completedActivities = student.activities.filter { 
                    it.isCompleted == true || it.isCompletedToday == true 
                }
                student.copy(
                    activities = completedActivities,
                    statistics = calculateStatistics(completedActivities)
                )
            }
            
            ActivityFilter.PENDING -> allStudents.map { student ->
                val pendingActivities = student.activities.filter { activity ->
                    val isCompleted = activity.isCompleted == true || activity.isCompletedToday == true
                    val isOverdue = activity.isOverdue == true
                    !isCompleted && !isOverdue
                }
                student.copy(
                    activities = pendingActivities,
                    statistics = calculateStatistics(pendingActivities)
                )
            }
            
            ActivityFilter.OVERDUE -> allStudents.map { student ->
                val overdueActivities = student.activities.filter { 
                    it.isOverdue == true 
                }
                student.copy(
                    activities = overdueActivities,
                    statistics = calculateStatistics(overdueActivities)
                )
            }
        }
        
        _filteredStudentsData.value = filtered
    }
    
    private fun calculateStatistics(activities: List<com.bmg.studentactivity.data.models.Activity>): com.bmg.studentactivity.data.models.ActivityStatistics {
        val total = activities.size
        val completed = activities.count { activity ->
            activity.isCompleted == true || activity.isCompletedToday == true
        }
        val overdue = activities.count { activity ->
            activity.isOverdue == true
        }
        val pending = activities.count { activity ->
            val isCompleted = activity.isCompleted == true || activity.isCompletedToday == true
            val isOverdue = activity.isOverdue == true
            !isCompleted && !isOverdue
        }
        val completionPercentage = if (total > 0) (completed.toDouble() / total * 100) else 0.0
        
        return com.bmg.studentactivity.data.models.ActivityStatistics(
            total = total,
            completed = completed,
            pending = pending,
            overdue = overdue,
            completionPercentage = completionPercentage
        )
    }
    
    /**
     * Get all overdue activities with alarm URLs from all students
     */
    fun getOverdueActivitiesWithAlarms(): List<com.bmg.studentactivity.data.models.Activity> {
        val allStudents = _studentsData.value ?: emptyList()
        return allStudents.flatMap { student ->
            student.activities.filter { activity ->
                val isOverdue = activity.isOverdue == true
                val isCompleted = activity.isCompleted == true || activity.isCompletedToday == true
                val hasAlarmUrl = !activity.alarmAudioUrl.isNullOrEmpty()
                isOverdue && !isCompleted && hasAlarmUrl
            }
        }
    }
    
}


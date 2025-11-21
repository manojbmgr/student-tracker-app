package com.bmg.studentactivity.ui.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmg.studentactivity.data.models.ActivitiesData
import com.bmg.studentactivity.data.models.StudentActivities
import com.bmg.studentactivity.data.repository.ActivityRepository
import com.bmg.studentactivity.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _studentsData = MutableLiveData<List<StudentActivities>>()
    val studentsData: LiveData<List<StudentActivities>> = _studentsData
    
    private val _activitiesData = MutableLiveData<ActivitiesData?>()
    val activitiesData: LiveData<ActivitiesData?> = _activitiesData
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadActivities(
        studentEmail: String? = null,
        day: String? = null,
        status: String? = null
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val result = activityRepository.getActivities(studentEmail, day, status)
                result.onSuccess { response ->
                    if (response.success && response.data != null) {
                        _activitiesData.value = response.data
                        // If students array exists, use it; otherwise use activities list
                        if (response.data.students != null && response.data.students.isNotEmpty()) {
                            _studentsData.value = response.data.students
                        } else if (response.data.activities != null) {
                            // Convert activities list to student activities format
                            val studentActivities = StudentActivities(
                                studentEmail = "",
                                studentName = null,
                                activities = response.data.activities,
                                statistics = response.data.statistics ?: com.bmg.studentactivity.data.models.ActivityStatistics()
                            )
                            _studentsData.value = listOf(studentActivities)
                        }
                    } else {
                        _error.value = response.message ?: "Failed to load activities"
                    }
                }.onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load activities"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Not authenticated"
            }
            _loading.value = false
        }
    }
    
    fun markActivityComplete(activityId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val result = activityRepository.markActivityComplete(activityId)
                result.onSuccess { response ->
                    if (response.success) {
                        loadActivities() // Reload activities
                    } else {
                        _error.value = response.message ?: "Failed to mark activity complete"
                    }
                }.onFailure { exception ->
                    _error.value = exception.message ?: "Failed to mark activity complete"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to mark activity complete"
            }
            _loading.value = false
        }
    }
}


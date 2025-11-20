package com.bmg.studentactivity.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmg.studentactivity.data.models.Activity
import com.bmg.studentactivity.data.repository.ActivityRepository
import com.bmg.studentactivity.utils.Constants
import com.bmg.studentactivity.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _activities = MutableLiveData<List<Activity>>()
    val activities: LiveData<List<Activity>> = _activities
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadTodayActivities(studentEmail: String? = null, day: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null // Clear previous errors
            
            val userType = tokenManager.getUserType()
            val token = tokenManager.getToken()
            
            // Check if we have authentication
            if (token == null) {
                _error.value = "Not authenticated. Please login again."
                _loading.value = false
                return@launch
            }
            
            if (userType == null) {
                _error.value = "User type not set. Please login again."
                _loading.value = false
                return@launch
            }
            
            try {
                if (userType == Constants.USER_TYPE_PARENT) {
                    // Parent dashboard API call
                    android.util.Log.d("DashboardViewModel", "Loading dashboard for parent, userType=$userType, token exists=${token != null}")
                    val result = activityRepository.getParentDashboard(studentEmail, day)
                    result.onSuccess { response ->
                        android.util.Log.d("DashboardViewModel", "Dashboard API call successful")
                        if (response.success && response.data != null) {
                            // Flatten activities from all students
                            val allActivities = mutableListOf<Activity>()
                            response.data.students?.forEach { studentData ->
                                studentData.activities?.list?.let { list ->
                                    allActivities.addAll(list)
                                }
                            }
                            _activities.value = allActivities
                            // If no activities, set empty list (not null)
                            if (allActivities.isEmpty()) {
                                _activities.value = emptyList()
                            }
                        } else {
                            _error.value = response.message ?: "Failed to load dashboard"
                        }
                    }.onFailure { exception ->
                        _error.value = exception.message ?: "Failed to load dashboard"
                    }
                } else {
                    // Student activities API call
                    val result = activityRepository.getActivities(studentEmail, day)
                    result.onSuccess { response ->
                        if (response.success && response.data != null) {
                            _activities.value = response.data
                        } else {
                            _error.value = response.message
                        }
                    }.onFailure { exception ->
                        _error.value = exception.message ?: "Failed to load activities"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while loading data"
            } finally {
                _loading.value = false
            }
        }
    }

    fun markActivityComplete(activity: Activity) {
        viewModelScope.launch {
            try {
                val result = activityRepository.markActivityComplete(activity.id)
                result.onSuccess { response ->
                    if (response.success) {
                        loadTodayActivities() // Reload to update list
                    } else {
                        _error.value = response.message
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to mark activity complete"
            }
        }
    }
}

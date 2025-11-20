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
            val userType = tokenManager.getUserType()
            
            try {
                if (userType == Constants.USER_TYPE_PARENT) {
                    val result = activityRepository.getParentDashboard(studentEmail, day)
                    result.onSuccess { response ->
                        if (response.success && response.data != null) {
                            // Flatten activities from all students
                            val allActivities = mutableListOf<Activity>()
                            response.data.students?.forEach { studentData ->
                                studentData.activities?.list?.let { list ->
                                    allActivities.addAll(list)
                                }
                            }
                            _activities.value = allActivities
                        } else {
                            _error.value = "Failed to load dashboard" 
                        }
                    }.onFailure { exception ->
                        _error.value = exception.message
                    }
                } else {
                    // Student logic
                    val result = activityRepository.getActivities(studentEmail, day)
                    result.onSuccess { response ->
                        if (response.success && response.data != null) {
                            _activities.value = response.data
                        } else {
                            _error.value = response.message
                        }
                    }.onFailure { exception ->
                        _error.value = exception.message
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Not authenticated"
            }
            _loading.value = false
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

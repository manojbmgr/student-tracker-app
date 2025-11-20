package com.bmg.studentactivity.ui.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmg.studentactivity.data.models.Activity
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
    
    private val _activities = MutableLiveData<List<Activity>>()
    val activities: LiveData<List<Activity>> = _activities
    
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
            try {
                val result = activityRepository.getActivities(studentEmail, day, status)
                result.onSuccess { response ->
                    if (response.success && response.data != null) {
                        _activities.value = response.data
                    } else {
                        _error.value = response.message
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Not authenticated"
            }
            _loading.value = false
        }
    }
    
    fun markActivityComplete(activityId: String) {
        viewModelScope.launch {
            try {
                val result = activityRepository.markActivityComplete(activityId)
                result.onSuccess { response ->
                    if (response.success) {
                        loadActivities() // Reload activities
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


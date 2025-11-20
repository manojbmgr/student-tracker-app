package com.bmg.studentactivity.ui.timetable

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmg.studentactivity.data.models.TimetableEntry
import com.bmg.studentactivity.data.repository.TimetableRepository
import com.bmg.studentactivity.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _timetableEntries = MutableLiveData<List<TimetableEntry>>()
    val timetableEntries: LiveData<List<TimetableEntry>> = _timetableEntries
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadTimetable(day: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            val token = tokenManager.getToken()
            if (token != null) {
                val result = timetableRepository.getTimetable(token, day)
                result.onSuccess { response ->
                    if (response.success && response.data != null) {
                        _timetableEntries.value = response.data
                    } else {
                        _error.value = response.message
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } else {
                _error.value = "Not authenticated"
            }
            _loading.value = false
        }
    }
    
    fun createEntry(day: String, time: String, subject: String, description: String?, audioUrl: String?) {
        viewModelScope.launch {
            val token = tokenManager.getToken()
            if (token != null) {
                val result = timetableRepository.createTimetableEntry(token, day, time, subject, description, audioUrl)
                result.onSuccess { response ->
                    if (response.success) {
                        loadTimetable()
                    } else {
                        _error.value = response.message
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            }
        }
    }
    
    fun updateEntry(id: String, day: String, time: String, subject: String, description: String?, audioUrl: String?) {
        viewModelScope.launch {
            val token = tokenManager.getToken()
            if (token != null) {
                val result = timetableRepository.updateTimetableEntry(token, id, day, time, subject, description, audioUrl)
                result.onSuccess { response ->
                    if (response.success) {
                        loadTimetable()
                    } else {
                        _error.value = response.message
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            }
        }
    }
    
    fun deleteEntry(id: String) {
        viewModelScope.launch {
            val token = tokenManager.getToken()
            if (token != null) {
                val result = timetableRepository.deleteTimetableEntry(token, id)
                result.onSuccess { response ->
                    if (response.success) {
                        loadTimetable()
                    } else {
                        _error.value = response.message
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            }
        }
    }
}


package com.bmg.studentactivity.ui.timetable

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmg.studentactivity.data.models.TimetableEntry
import com.bmg.studentactivity.data.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository
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
            try {
                val result = timetableRepository.getTimetable(day)
                result.onSuccess { response ->
                    if (response.success && response.data != null) {
                        _timetableEntries.value = response.data
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
    
    fun createEntry(day: String, time: String, subject: String, description: String?, audioUrl: String?) {
        viewModelScope.launch {
            try {
                val result = timetableRepository.createTimetableEntry(day, time, subject, description, audioUrl)
                result.onSuccess { response ->
                    if (response.success) {
                        loadTimetable()
                    } else {
                        _error.value = response.message
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create timetable entry"
            }
        }
    }
    
    fun updateEntry(id: String, day: String, time: String, subject: String, description: String?, audioUrl: String?) {
        viewModelScope.launch {
            try {
                val result = timetableRepository.updateTimetableEntry(id, day, time, subject, description, audioUrl)
                result.onSuccess { response ->
                    if (response.success) {
                        loadTimetable()
                    } else {
                        _error.value = response.message
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update timetable entry"
            }
        }
    }
    
    fun deleteEntry(id: String) {
        viewModelScope.launch {
            try {
                val result = timetableRepository.deleteTimetableEntry(id)
                result.onSuccess { response ->
                    if (response.success) {
                        loadTimetable()
                    } else {
                        _error.value = response.message
                    }
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete timetable entry"
            }
        }
    }
}


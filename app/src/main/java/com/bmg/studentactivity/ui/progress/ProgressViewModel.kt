package com.bmg.studentactivity.ui.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmg.studentactivity.data.models.ProgressData
import com.bmg.studentactivity.data.repository.ProgressRepository
import com.bmg.studentactivity.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _progressData = MutableLiveData<ProgressData?>()
    val progressData: LiveData<ProgressData?> = _progressData
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadProgress(studentEmail: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = progressRepository.getProgress(studentEmail)
                result.onSuccess { response ->
                    if (response.success && response.data != null) {
                        _progressData.value = response.data
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
}


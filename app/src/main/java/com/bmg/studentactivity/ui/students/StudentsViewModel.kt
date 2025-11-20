package com.bmg.studentactivity.ui.students

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmg.studentactivity.data.models.Student
import com.bmg.studentactivity.data.repository.StudentsRepository
import com.bmg.studentactivity.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val studentsRepository: StudentsRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadStudents() {
        viewModelScope.launch {
            _loading.value = true
            val token = tokenManager.getToken()
            if (token != null) {
                val result = studentsRepository.getStudents(token)
                result.onSuccess { response ->
                    if (response.success && response.data != null) {
                        _students.value = response.data
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
}


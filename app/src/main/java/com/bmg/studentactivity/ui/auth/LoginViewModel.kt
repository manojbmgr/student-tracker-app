package com.bmg.studentactivity.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmg.studentactivity.data.repository.AuthRepository
import com.bmg.studentactivity.utils.Constants
import com.bmg.studentactivity.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState
    
    fun login(email: String, password: String, isParent: Boolean) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                if (isParent) {
                    val result = authRepository.parentLogin(email, password)
                    result.onSuccess { response ->
                        if (response.success && response.data != null) {
                            val parentData = response.data
                            tokenManager.saveToken(parentData.token)
                            tokenManager.saveUserType(Constants.USER_TYPE_PARENT)
                            tokenManager.saveParentId(parentData.parentId)
                            _loginState.value = LoginState.Success(isParent = true, parentId = parentData.parentId)
                        } else {
                            _loginState.value = LoginState.Error(response.message)
                        }
                    }.onFailure { exception ->
                        _loginState.value = LoginState.Error(exception.message ?: "Login failed")
                    }
                } else {
                    val result = authRepository.login(email, password)
                    result.onSuccess { response ->
                        if (response.success && response.data != null) {
                            val studentData = response.data
                            tokenManager.saveToken(studentData.token)
                            tokenManager.saveUserType(Constants.USER_TYPE_STUDENT)
                            tokenManager.saveStudentId(studentData.studentId)
                            _loginState.value = LoginState.Success(isParent = false, studentId = studentData.studentId)
                        } else {
                            _loginState.value = LoginState.Error(response.message)
                        }
                    }.onFailure { exception ->
                        _loginState.value = LoginState.Error(exception.message ?: "Login failed")
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "An error occurred")
            }
        }
    }
}

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val isParent: Boolean, val studentId: String? = null, val parentId: String? = null) : LoginState()
    data class Error(val message: String) : LoginState()
}


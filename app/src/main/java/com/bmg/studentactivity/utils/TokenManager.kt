package com.bmg.studentactivity.utils

import android.content.Context

class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    
    fun saveToken(token: String) {
        prefs.edit().putString(Constants.KEY_TOKEN, token).apply()
    }
    
    fun getToken(): String? = prefs.getString(Constants.KEY_TOKEN, null)
    
    fun clearToken() {
        prefs.edit().remove(Constants.KEY_TOKEN).apply()
    }
    
    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(Constants.KEY_API_KEY, apiKey).apply()
    }
    
    fun getApiKey(): String? = prefs.getString(Constants.KEY_API_KEY, null)
    
    fun clearApiKey() {
        prefs.edit().remove(Constants.KEY_API_KEY).apply()
    }
    
    fun saveUserType(userType: String) {
        prefs.edit().putString(Constants.KEY_USER_TYPE, userType).apply()
    }
    
    fun getUserType(): String? = prefs.getString(Constants.KEY_USER_TYPE, null)
    
    fun saveStudentId(studentId: String) {
        prefs.edit().putString(Constants.KEY_STUDENT_ID, studentId).apply()
    }
    
    fun getStudentId(): String? = prefs.getString(Constants.KEY_STUDENT_ID, null)
    
    fun saveParentId(parentId: String) {
        prefs.edit().putString(Constants.KEY_PARENT_ID, parentId).apply()
    }
    
    fun getParentId(): String? = prefs.getString(Constants.KEY_PARENT_ID, null)
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}


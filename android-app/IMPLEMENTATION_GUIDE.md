# Android App Implementation Guide

## Step-by-Step Implementation

### Phase 1: Project Setup

1. **Create Android Studio Project**
   - Open Android Studio
   - New Project → Empty Activity
   - Language: Kotlin
   - Minimum SDK: 24 (Android 7.0)
   - Target SDK: 34 (Android 14)

2. **Configure Gradle Files**
   - Add dependencies (see README.md)
   - Enable data binding/view binding
   - Configure Hilt/Koin for DI

3. **Set Up Project Structure**
   - Create package: `com.bmgdigital.studentprogresstracker`
   - Create directories following the structure in README.md

### Phase 2: Core Setup

1. **Constants & Configuration**
   ```kotlin
   // utils/Constants.kt
   object Constants {
       const val BASE_URL = "https://student.bmgdigital.in/api/v1"
       const val PREF_NAME = "StudentProgressPrefs"
       const val KEY_TOKEN = "auth_token"
       const val KEY_API_KEY = "api_key"
       const val KEY_USER_TYPE = "user_type"
       const val KEY_STUDENT_ID = "student_id"
       const val KEY_PARENT_ID = "parent_id"
   }
   ```

2. **Token Manager**
   ```kotlin
   // utils/TokenManager.kt
   class TokenManager(context: Context) {
       private val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
       
       fun saveToken(token: String) {
           prefs.edit().putString(Constants.KEY_TOKEN, token).apply()
       }
       
       fun getToken(): String? = prefs.getString(Constants.KEY_TOKEN, null)
       
       fun clearToken() {
           prefs.edit().remove(Constants.KEY_TOKEN).apply()
       }
   }
   ```

3. **API Service Interface**
   ```kotlin
   // data/api/ApiService.kt
   interface ApiService {
       @POST("auth/login")
       suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
       
       @POST("auth/parent-login")
       suspend fun parentLogin(@Body request: LoginRequest): Response<ParentLoginResponse>
       
       @GET("activities")
       suspend fun getActivities(
           @Header("Authorization") token: String,
           @Query("status") status: String? = null
       ): Response<ActivitiesResponse>
       
       @POST("activities/complete")
       suspend fun markActivityComplete(
           @Header("Authorization") token: String,
           @Body request: CompleteRequest
       ): Response<CompleteResponse>
       
       @GET("timetable")
       suspend fun getTimetable(
           @Header("Authorization") token: String,
           @Query("day") day: String? = null
       ): Response<TimetableResponse>
       
       // Add more endpoints...
   }
   ```

### Phase 3: Data Layer

1. **Data Models**
   - Create data classes matching API responses
   - Use Gson annotations for JSON parsing

2. **Repository Pattern**
   - Implement repositories for each feature
   - Handle API calls and local caching
   - Return Flow/LiveData for reactive updates

3. **Room Database**
   - Create entities for offline caching
   - Implement DAOs for database operations
   - Set up database with migrations

### Phase 4: UI Layer

1. **Authentication**
   - Login screen (Student/Parent tabs)
   - Handle token storage
   - Navigate to dashboard on success

2. **Dashboard**
   - Show today's activities
   - Display statistics
   - Quick actions

3. **Activities**
   - List view with RecyclerView
   - Mark complete/incomplete
   - Filter by status

4. **Timetable**
   - Weekly view
   - Add/Edit/Delete entries
   - Audio playback for alarms

5. **Progress**
   - Charts and statistics
   - Subject-wise breakdown
   - Chapter progress

### Phase 5: Advanced Features

1. **Offline Support**
   - Cache data in Room
   - Sync when online
   - Show offline indicator

2. **Push Notifications**
   - Firebase Cloud Messaging
   - Activity reminders
   - Progress updates

3. **Biometric Authentication**
   - Fingerprint/Face unlock
   - Secure token storage

## API Integration Example

```kotlin
// Example: Login and get activities
class AuthRepository(private val apiService: ApiService) {
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Testing Checklist

- [ ] Unit tests for ViewModels
- [ ] Unit tests for Repositories
- [ ] UI tests for critical flows
- [ ] API integration tests
- [ ] Offline functionality tests

## Security Best Practices

1. Use EncryptedSharedPreferences for token storage
2. Implement certificate pinning
3. Validate all user inputs
4. Use ProGuard/R8 for code obfuscation
5. Never log sensitive data


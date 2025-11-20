# Student Progress Tracker - Android App

## Project Structure

This Android app integrates with the Student Progress Tracker API.

## Technology Stack

- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **Networking:** Retrofit + OkHttp
- **JSON Parsing:** Gson
- **Local Database:** Room
- **Dependency Injection:** Hilt or Koin
- **Coroutines:** For async operations
- **Navigation:** Navigation Component

## Directory Structure

```
android-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/bmgdigital/studentprogresstracker/
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/
│   │   │   │   │   │   ├── ApiService.kt
│   │   │   │   │   │   ├── ApiClient.kt
│   │   │   │   │   │   └── interceptors/
│   │   │   │   │   │       └── AuthInterceptor.kt
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── database/
│   │   │   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   │   │   └── dao/
│   │   │   │   │   │   │       ├── StudentDao.kt
│   │   │   │   │   │   │       ├── ActivityDao.kt
│   │   │   │   │   │   │       └── TimetableDao.kt
│   │   │   │   │   │   └── entities/
│   │   │   │   │   │       ├── Student.kt
│   │   │   │   │   │       ├── Activity.kt
│   │   │   │   │   │       └── Timetable.kt
│   │   │   │   │   ├── models/
│   │   │   │   │   │   ├── LoginRequest.kt
│   │   │   │   │   │   ├── LoginResponse.kt
│   │   │   │   │   │   ├── Activity.kt
│   │   │   │   │   │   ├── Timetable.kt
│   │   │   │   │   │   └── Progress.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── AuthRepository.kt
│   │   │   │   │       ├── ActivityRepository.kt
│   │   │   │   │       ├── TimetableRepository.kt
│   │   │   │   │       └── ProgressRepository.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── auth/
│   │   │   │   │   │   ├── LoginActivity.kt
│   │   │   │   │   │   ├── LoginViewModel.kt
│   │   │   │   │   │   └── fragments/
│   │   │   │   │   │       ├── StudentLoginFragment.kt
│   │   │   │   │   │       └── ParentLoginFragment.kt
│   │   │   │   │   ├── dashboard/
│   │   │   │   │   │   ├── DashboardActivity.kt
│   │   │   │   │   │   ├── DashboardViewModel.kt
│   │   │   │   │   │   └── fragments/
│   │   │   │   │   │       ├── StudentDashboardFragment.kt
│   │   │   │   │   │       └── ParentDashboardFragment.kt
│   │   │   │   │   ├── activities/
│   │   │   │   │   │   ├── ActivitiesActivity.kt
│   │   │   │   │   │   ├── ActivitiesViewModel.kt
│   │   │   │   │   │   └── fragments/
│   │   │   │   │   │       ├── TodayActivitiesFragment.kt
│   │   │   │   │   │       └── ActivityDetailFragment.kt
│   │   │   │   │   ├── timetable/
│   │   │   │   │   │   ├── TimetableActivity.kt
│   │   │   │   │   │   ├── TimetableViewModel.kt
│   │   │   │   │   │   └── fragments/
│   │   │   │   │   │       ├── TimetableListFragment.kt
│   │   │   │   │   │       └── AddEditTimetableFragment.kt
│   │   │   │   │   ├── progress/
│   │   │   │   │   │   ├── ProgressActivity.kt
│   │   │   │   │   │   ├── ProgressViewModel.kt
│   │   │   │   │   │   └── fragments/
│   │   │   │   │   │       ├── OverallProgressFragment.kt
│   │   │   │   │   │       └── SubjectProgressFragment.kt
│   │   │   │   │   └── students/
│   │   │   │   │       ├── StudentsActivity.kt
│   │   │   │   │       ├── StudentsViewModel.kt
│   │   │   │   │       └── fragments/
│   │   │   │   │           └── StudentsListFragment.kt
│   │   │   │   ├── utils/
│   │   │   │   │   ├── Constants.kt
│   │   │   │   │   ├── PrefsManager.kt
│   │   │   │   │   ├── TokenManager.kt
│   │   │   │   │   └── Extensions.kt
│   │   │   │   └── di/
│   │   │   │       ├── AppModule.kt
│   │   │   │       ├── NetworkModule.kt
│   │   │   │       └── DatabaseModule.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_login.xml
│   │   │   │   │   ├── activity_dashboard.xml
│   │   │   │   │   ├── fragment_today_activities.xml
│   │   │   │   │   ├── fragment_timetable.xml
│   │   │   │   │   ├── item_activity.xml
│   │   │   │   │   └── item_timetable.xml
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── drawable/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## Key Features

1. **Authentication**
   - Student login
   - Parent login
   - Token management
   - Secure storage

2. **Activities**
   - View today's activities
   - Mark activities as complete
   - Filter by status (completed/pending)

3. **Timetable**
   - View weekly timetable
   - Add/edit timetable entries
   - Delete entries
   - Audio alarm support

4. **Progress**
   - Overall progress dashboard
   - Subject-wise progress
   - Chapter-wise progress
   - Statistics and charts

5. **Students (Parent)**
   - View all students
   - Switch between students
   - View individual student data

## API Integration

Base URL: `https://student.bmgdigital.in/api/v1`

### Endpoints Used

- `POST /auth/login` - Student login
- `POST /auth/parent-login` - Parent login
- `GET /activities` - Get activities
- `POST /activities/complete` - Mark activity complete
- `GET /timetable` - Get timetable
- `POST /timetable` - Create timetable entry
- `PUT /timetable` - Update timetable entry
- `DELETE /timetable` - Delete timetable entry
- `GET /progress` - Get progress
- `GET /students` - Get students (parent)

## Setup Instructions

1. Open Android Studio
2. Create new project
3. Copy this directory structure
4. Add dependencies (see build.gradle.kts)
5. Configure API base URL in Constants.kt
6. Implement features following MVVM pattern

## Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Secure Storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
```

## Next Steps

1. Create Android Studio project
2. Set up Gradle files
3. Implement API service interfaces
4. Create data models
5. Implement repositories
6. Create ViewModels
7. Build UI components
8. Add navigation
9. Implement authentication flow
10. Add offline support with Room


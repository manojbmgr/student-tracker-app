# Student Progress Tracker - Student API Documentation

## Table of Contents
1. [Overview](#overview)
2. [Base URL](#base-url)
3. [Authentication](#authentication)
4. [Getting Started](#getting-started)
5. [Response Format](#response-format)
6. [Endpoints](#endpoints)
7. [Error Handling](#error-handling)
8. [Best Practices](#best-practices)
9. [Code Examples](#code-examples)

---

## Overview

This API provides a RESTful interface for students to access their own progress data, timetable, and activities. Students can authenticate using JWT tokens (recommended for mobile/web apps) or API keys (for external integrations).

**Key Features:**
- ✅ Access your own timetable and activities
- ✅ Track your progress across subjects and chapters
- ✅ Mark activities as complete
- ✅ Secure authentication with JWT tokens or API keys

All endpoints return JSON responses and follow RESTful conventions.

---

## Base URL

```
https://student.bmgdigital.in/api/v1
```

---

## Authentication

### Method 1: JWT Token Authentication (Recommended for Apps)

**When to use:** Mobile apps, web applications, or any scenario where users can log in.

**How it works:**
1. Student logs in via `/auth/login`
2. Server returns a JWT token
3. Client stores the token securely
4. Client includes token in subsequent API requests
5. Token expires after 7 days (client should refresh by logging in again)

**Advantages:**
- ✅ Secure - tokens expire automatically
- ✅ User-specific - each student has their own token
- ✅ No need to store API keys
- ✅ Better for user-facing applications

**How to use:**

**Step 1: Login**
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "student@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "status": "success",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJkYXRhIjp7InN0dWRlbnRJZCI6MX0sImlhdCI6MTc2MzIxNzU1NSwiZXhwIjoxNzYzODIyMzU1fQ.TQPO4sZ1tY6P2wEV3rwyhOBjuC2w-LltwUZO_fPnu4c",
    "expiresIn": 604800,
    "studentId": 1
  },
  "message": "Login successful"
}
```

**Step 2: Use Token in Requests**

**Option A: Authorization Header (Recommended)**
```bash
GET /api/v1/activities
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

**Option B: Query Parameter**
```bash
GET /api/v1/activities?token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

---

### Method 2: API Key Authentication (For External Access)

**When to use:** External integrations, scripts, third-party services, or scenarios where user login is not possible.

**How it works:**
1. Generate API key from Student Profile → Edit Profile → Generate API Key
2. Include API key in every request
3. API key doesn't expire (but can be regenerated)

**Advantages:**
- ✅ No login required
- ✅ Good for automated scripts
- ✅ Suitable for server-to-server communication
- ✅ Can be used indefinitely

**Disadvantages:**
- ⚠️ API key must be kept secure (never expose in client-side code)
- ⚠️ Doesn't expire automatically
- ⚠️ Less secure if compromised

**How to use:**

**Step 1: Generate API Key**
- Go to Student Profile → Edit Profile → Generate API Key

**Step 2: Use API Key in Requests**

**Option A: X-API-Key Header (Recommended)**
```bash
GET /api/v1/activities
X-API-Key: 4bf291b277662a13328825cb13dbaf72fa44485eea8387fbe9d6091a2486c9ab
```

**Option B: Query Parameter**
```bash
GET /api/v1/activities?api_key=4bf291b277662a13328825cb13dbaf72fa44485eea8387fbe9d6091a2486c9ab
```

---

## Getting Started

### Quick Start with JWT Token (Mobile/Web App)

```javascript
// 1. Login
const loginResponse = await fetch('https://student.bmgdigital.in/api/v1/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'student@example.com',
    password: 'password123'
  })
});

const loginData = await loginResponse.json();
const token = loginData.data.token;

// 2. Store token securely (e.g., SecureStorage, Keychain, etc.)
// await SecureStorage.setItem('auth_token', token);

// 3. Use token in subsequent requests
const activitiesResponse = await fetch('https://student.bmgdigital.in/api/v1/activities', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const activities = await activitiesResponse.json();
```

### Quick Start with API Key (External Script)

```bash
# Using curl with API key
curl -X GET "https://student.bmgdigital.in/api/v1/activities" \
  -H "X-API-Key: your-api-key-here"
```

```python
# Using Python requests
import requests

headers = {
    'X-API-Key': 'your-api-key-here'
}

response = requests.get('https://student.bmgdigital.in/api/v1/activities', headers=headers)
data = response.json()
```

---

## Response Format

### Success Response
```json
{
  "status": "success",
  "timestamp": "2024-01-01 12:00:00",
  "data": { ... },
  "message": "Optional message"
}
```

### Error Response
```json
{
  "status": "error",
  "timestamp": "2024-01-01 12:00:00",
  "message": "Error description",
  "errors": {
    "field": "Field-specific error"
  }
}
```

---

## Endpoints

### Authentication

#### Student Login
**POST** `/auth/login`

Authenticates a student and returns a JWT token.

**Request:**
```json
{
  "email": "student@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "status": "success",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 604800,
    "studentId": 1
  },
  "message": "Login successful"
}
```

**cURL Example:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"student@example.com","password":"password123"}'
```

---

### Timetable

#### Get Timetable
**GET** `/timetable`

Returns all timetable entries for the authenticated student.

**Authentication:** JWT Token or API Key (Student only)

**Query Parameters:**
- `day` (optional): Filter by day of week

**Note:** Timetable endpoint still uses GET method as it doesn't expose sensitive information. (Monday, Tuesday, etc.)

**Response:**
```json
{
  "status": "success",
  "data": [
    {
      "timetableId": 1,
      "studentId": 1,
      "dayOfWeek": "Monday",
      "startTime": "09:00:00",
      "endTime": "10:00:00",
      "subject": "Mathematics",
      "location": "Room 101",
      "teacher": "Mr. Smith",
      "notes": "Bring calculator",
      "alarmAudio": "alarm_123.mp3",
      "alarmAudioUrl": "https://student.bmgdigital.in/uploads/alarms/alarm_123.mp3"
    }
  ]
}
```

**cURL Examples:**

**Step 1: Login to get JWT Token (REQUIRED FIRST)**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@example.com",
    "password": "your-password"
  }'
```

**Step 2: Use the token to access timetable**
```bash
# Replace YOUR_TOKEN_HERE with the token from Step 1
curl -X GET "https://student.bmgdigital.in/api/v1/timetable" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json"
```

**Complete Example (with token extraction):**
```bash
# Step 1: Login and save token
TOKEN=$(curl -s -X POST "https://student.bmgdigital.in/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"student@example.com","password":"your-password"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# Step 2: Use token to get timetable
curl -X GET "https://student.bmgdigital.in/api/v1/timetable" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

# Filter by day
curl -X GET "https://student.bmgdigital.in/api/v1/timetable?day=Monday" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**With API Key (Alternative Method):**
```bash
curl -X GET "https://student.bmgdigital.in/api/v1/timetable" \
  -H "X-API-Key: your-api-key-here" \
  -H "Content-Type: application/json"
```

**Important Notes:**
- The timetable endpoint requires **student authentication only**
- Token must be obtained from `/api/v1/auth/login` endpoint first
- Token expires after 7 days - you'll need to login again
- Make sure there's a space after "Bearer" in the Authorization header

---

#### Create Timetable Entry
**POST** `/timetable`

Creates a new timetable entry.

**Authentication:** JWT Token or API Key (Student only)

**Request Body (JSON):**
```json
{
  "dayOfWeek": "Monday",
  "startTime": "09:00:00",
  "endTime": "10:00:00",
  "subject": "Mathematics",
  "location": "Room 101",
  "teacher": "Mr. Smith",
  "notes": "Bring calculator"
}
```

**Request Body (Multipart - with audio upload):**
```
Content-Type: multipart/form-data

dayOfWeek: Monday
startTime: 09:00:00
endTime: 10:00:00
subject: Mathematics
location: Room 101
teacher: Mr. Smith
notes: Bring calculator
alarmAudio: [file]
```

**Response:**
```json
{
  "status": "success",
  "data": {
    "timetableId": 1
  },
  "message": "Timetable entry added successfully"
}
```

**cURL Examples:**

**With JWT Token (JSON):**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/timetable" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{
    "dayOfWeek": "Monday",
    "startTime": "09:00:00",
    "endTime": "10:00:00",
    "subject": "Mathematics",
    "location": "Room 101",
    "teacher": "Mr. Smith",
    "notes": "Bring calculator"
  }'
```

**With API Key (Multipart with audio):**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/timetable" \
  -H "X-API-Key: your-api-key-here" \
  -F "dayOfWeek=Monday" \
  -F "startTime=09:00:00" \
  -F "endTime=10:00:00" \
  -F "subject=Mathematics" \
  -F "location=Room 101" \
  -F "teacher=Mr. Smith" \
  -F "notes=Bring calculator" \
  -F "alarmAudio=@/path/to/audio.mp3"
```

---

#### Update Timetable Entry
**PUT** `/timetable`

Updates an existing timetable entry.

**Authentication:** JWT Token or API Key (Student only)

**Request Body:**
```json
{
  "timetableId": 1,
  "dayOfWeek": "Monday",
  "startTime": "09:00:00",
  "endTime": "10:00:00",
  "subject": "Mathematics",
  "location": "Room 102",
  "teacher": "Mrs. Johnson",
  "notes": "Updated notes",
  "removeAudio": false
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Timetable entry updated successfully"
}
```

**cURL Example:**
```bash
curl -X PUT "https://student.bmgdigital.in/api/v1/timetable" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{
    "timetableId": 1,
    "dayOfWeek": "Monday",
    "startTime": "09:00:00",
    "endTime": "10:00:00",
    "subject": "Mathematics",
    "location": "Room 102",
    "teacher": "Mrs. Johnson",
    "notes": "Updated notes"
  }'
```

---

#### Delete Timetable Entry
**DELETE** `/timetable?timetableId=1`

Deletes a timetable entry.

**Authentication:** JWT Token or API Key (Student only)

**Response:**
```json
{
  "status": "success",
  "message": "Timetable entry deleted successfully"
}
```

**cURL Example:**
```bash
curl -X DELETE "https://student.bmgdigital.in/api/v1/timetable?timetableId=1" \
  -H "Authorization: Bearer your-jwt-token-here"
```

---

### Activities

#### Get Activities
**POST** `/activities`

Returns today's activities for the authenticated student.

**Authentication:** JWT Token or API Key

**Request Body:**
```json
{
  "day": "Monday",
  "status": "pending"
}
```

**Body Parameters:**
- `day` (optional): Filter by day (Monday, Tuesday, etc.). Default: current day
- `status` (optional): Filter by status (`completed`, `pending`, `all`). Default: `all`
- Body can be empty `{}` or omitted for default behavior

**Response:**
```json
{
  "status": "success",
  "data": {
    "activities": [
      {
        "timetableId": 1,
        "studentId": 1,
        "dayOfWeek": "Monday",
        "startTime": "09:00:00",
        "endTime": "10:00:00",
        "subject": "Mathematics",
        "location": "Room 101",
        "teacher": "Mr. Smith",
        "notes": "Bring calculator",
        "alarmAudio": "alarm_123.mp3",
        "alarmAudioUrl": "https://student.bmgdigital.in/uploads/alarms/alarm_123.mp3",
        "isCompleted": false,
        "isCompletedToday": false,
        "isOverdue": false,
        "activityName": "Mathematics",
        "activityType": "study"
      }
    ],
    "statistics": {
      "total": 5,
      "completed": 2,
      "pending": 3,
      "overdue": 1,
      "completionPercentage": 40.0
    },
    "currentTime": "2024-01-01 14:30:00",
    "currentDay": "Monday",
    "date": "2024-01-01"
  }
}
```

**cURL Examples:**

**With JWT Token:**
```bash
# Get all activities
curl -X POST "https://student.bmgdigital.in/api/v1/activities" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{}'

# Get pending activities only
curl -X POST "https://student.bmgdigital.in/api/v1/activities" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "pending"
  }'
```

**With API Key:**
```bash
# Get all activities
curl -X POST "https://student.bmgdigital.in/api/v1/activities" \
  -H "X-API-Key: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{}'

# Get completed activities
curl -X POST "https://student.bmgdigital.in/api/v1/activities" \
  -H "X-API-Key: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "completed"
  }'
```

---

#### Mark Activity Complete
**POST** `/activities/complete`

Marks an activity as complete or incomplete.

**Authentication:** JWT Token or API Key

**Request Body:**
```json
{
  "timetableId": 1,
  "isCompleted": true
}
```

**OR**

```json
{
  "activityId": 5,
  "isCompleted": true
}
```

**Response:**
```json
{
  "status": "success",
  "data": {
    "timetableId": 1,
    "activityId": 10,
    "isCompleted": true,
    "completedAt": "2024-01-01 10:15:00",
    "subject": "Mathematics"
  },
  "message": "Activity marked as completed"
}
```

**cURL Examples:**

**With JWT Token:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/activities/complete" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{"timetableId":1,"isCompleted":true}'
```

**With API Key:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/activities/complete" \
  -H "X-API-Key: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{"timetableId":1,"isCompleted":true}'
```

---

### Dashboard

#### Get Dashboard Data
**POST** `/dashboard`

Returns comprehensive dashboard data including progress, activities, and statistics in a single response. This endpoint aggregates data from multiple sources for easy dashboard display.

**Authentication:** JWT Token or API Key (Student only)

**Request Body:**
```json
{
  "day": "Monday"
}
```

**Body Parameters:**
- `day` (optional): Filter activities by day (Monday, Tuesday, etc.). Default: current day
- Body can be empty `{}` or omitted for default behavior

**Response:**
```json
{
  "status": "success",
  "data": {
    "student": {
      "studentId": 1,
      "studentName": "Jane Doe",
      "email": "jane@example.com",
      "phoneNo": "1234567890",
      "class": 10,
      "profileImg": "profile.jpg",
      "profileImgUrl": "https://student.bmgdigital.in/uploads/profiles/profile.jpg",
      "status": "active"
    },
    "progress": {
      "overall": {
        "totalSubjects": 5,
        "totalChapters": 20,
        "totalExercises": 100,
        "totalSubmissions": 75,
        "approvedCount": 60,
        "pendingCount": 10,
        "rejectedCount": 5,
        "approvedPercentage": 60.0,
        "pendingPercentage": 10.0,
        "rejectedPercentage": 5.0,
        "completionPercentage": 75.0
      },
      "subjects": [ ... ],
      "completedSubjects": [ ... ],
      "completedChapters": [ ... ],
      "chapterProgress": { ... }
    },
    "activities": {
      "list": [
        {
          "timetableId": 1,
          "studentId": 1,
          "dayOfWeek": "Monday",
          "startTime": "09:00:00",
          "endTime": "10:00:00",
          "subject": "Mathematics",
          "location": "Room 101",
          "teacher": "Mr. Smith",
          "notes": "Bring calculator",
          "isCompleted": false,
          "isCompletedToday": false,
          "isOverdue": false,
          "completedAt": null
        }
      ],
      "statistics": {
        "total": 5,
        "completed": 2,
        "pending": 3,
        "overdue": 1,
        "completionPercentage": 40.0
      },
      "currentDay": "Monday",
      "currentDate": "2024-01-01",
      "currentTime": "2024-01-01 14:30:00"
    },
    "summary": {
      "totalSubjects": 5,
      "totalChapters": 20,
      "totalExercises": 100,
      "completionPercentage": 75.0,
      "approvedPercentage": 60.0,
      "todayActivitiesTotal": 5,
      "todayActivitiesCompleted": 2,
      "todayActivitiesPending": 3
    }
  }
}
```

**cURL Examples:**

**With JWT Token:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/dashboard" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**With API Key:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/dashboard" \
  -H "X-API-Key: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Filter by day:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/dashboard" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{
    "day": "Tuesday"
  }'
```

**Benefits:**
- ✅ Single API call instead of multiple calls to `/progress`, `/activities`, and `/students`
- ✅ Optimized for dashboard display
- ✅ Includes all necessary statistics and summaries
- ✅ Reduces network requests and improves app performance

---

### Progress

#### Get Student Progress
**POST** `/progress`

Returns comprehensive progress data for the authenticated student.

**Authentication:** JWT Token or API Key

**Request Body:**
- Body can be empty `{}` or omitted - returns own progress

**Response:**
```json
{
  "status": "success",
  "data": {
    "overall": {
      "totalSubjects": 5,
      "totalChapters": 20,
      "totalExercises": 100,
      "totalSubmissions": 75,
      "approvedCount": 60,
      "pendingCount": 10,
      "rejectedCount": 5,
      "approvedPercentage": 60.0,
      "pendingPercentage": 10.0,
      "rejectedPercentage": 5.0,
      "completionPercentage": 75.0
    },
    "subjects": [
      {
        "subjectId": 1,
        "subjectName": "Mathematics",
        "totalChapter": 5,
        "completedChapters": 3,
        "totalSubmissions": 20,
        "approvedCount": 15,
        "pendingCount": 3,
        "rejectedCount": 2,
        "completionPercentage": 60.0,
        "approvedPercentage": 75.0
      }
    ],
    "completedSubjects": [],
    "completedChapters": [],
    "chapterProgress": {
      "1": [
        {
          "chapterId": 1,
          "chapterName": "Algebra",
          "chapterNo": 1,
          "totalExercises": 10,
          "completedExercises": 8,
          "approvedCount": 7,
          "pendingCount": 1,
          "rejectedCount": 0,
          "completionPercentage": 80.0,
          "approvedPercentage": 70.0
        }
      ]
    }
  }
}
```

**cURL Examples:**

**With JWT Token:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/progress" \
  -H "Authorization: Bearer student-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**With API Key:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/progress" \
  -H "X-API-Key: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{}'
```

---

### Students

#### Get Student Information
**GET** `/students`

Returns the authenticated student's own information.

**Authentication:** JWT Token or API Key

**Response:**
```json
{
  "status": "success",
  "data": {
    "students": [
      {
        "studentId": 1,
        "studentName": "Jane Doe",
        "email": "jane@example.com",
        "phoneNo": "1234567890",
        "class": 10,
        "profileImg": "profile.jpg",
        "profileImgUrl": "https://student.bmgdigital.in/uploads/profiles/profile.jpg",
        "status": "active"
      }
    ],
    "total": 1
  }
}
```

**cURL Examples:**

**With JWT Token:**
```bash
curl -X GET "https://student.bmgdigital.in/api/v1/students" \
  -H "Authorization: Bearer your-jwt-token-here"
```

**With API Key:**
```bash
curl -X GET "https://student.bmgdigital.in/api/v1/students" \
  -H "X-API-Key: your-api-key-here"
```

---

## Error Handling

### HTTP Status Codes

- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized (Invalid or missing authentication)
- `403` - Forbidden (Valid authentication but insufficient permissions)
- `404` - Not Found
- `405` - Method Not Allowed
- `422` - Validation Error
- `500` - Internal Server Error

### Error Response Format

```json
{
  "status": "error",
  "timestamp": "2024-01-01 12:00:00",
  "message": "Error description",
  "errors": {
    "field": "Field-specific error message"
  }
}
```

### Common Errors

**401 Unauthorized:**
```json
{
  "status": "error",
  "message": "Authentication required."
}
```

**403 Forbidden:**
```json
{
  "status": "error",
  "message": "Student authentication required"
}
```

**422 Validation Error:**
```json
{
  "status": "error",
  "message": "Validation failed",
  "errors": {
    "email": "Email is required",
    "password": "Password is required"
  }
}
```

---

## Best Practices

### Security

1. **JWT Tokens:**
   - Store tokens securely (never in localStorage for web apps)
   - Use SecureStorage/Keychain for mobile apps
   - Implement token refresh before expiration
   - Never log or expose tokens

2. **API Keys:**
   - Keep API keys secret (never commit to version control)
   - Use environment variables for API keys
   - Regenerate keys if compromised
   - Never use API keys in client-side code

3. **HTTPS:**
   - Always use HTTPS in production
   - Never send credentials over HTTP

### Performance

1. **Caching:**
   - Cache responses when appropriate
   - Use ETags for conditional requests
   - Implement offline support for mobile apps

2. **Rate Limiting:**
   - Implement rate limiting on client side
   - Handle 429 (Too Many Requests) responses gracefully

### Error Handling

1. **Always check response status:**
   ```javascript
   if (response.status === 401) {
     // Token expired, redirect to login
   }
   ```

2. **Handle network errors:**
   ```javascript
   try {
     const response = await fetch(url);
   } catch (error) {
     // Handle network error
   }
   ```

3. **Validate responses:**
   ```javascript
   const data = await response.json();
   if (data.status === 'error') {
     // Handle error
   }
   ```

---

## Code Examples

### JavaScript/TypeScript (Fetch API)

```javascript
class StudentAPI {
  constructor(baseURL, token = null, apiKey = null) {
    this.baseURL = baseURL;
    this.token = token;
    this.apiKey = apiKey;
  }

  async request(endpoint, options = {}) {
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    };

    // Add authentication
    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    } else if (this.apiKey) {
      headers['X-API-Key'] = this.apiKey;
    }

    const response = await fetch(`${this.baseURL}${endpoint}`, {
      ...options,
      headers
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Request failed');
    }

    return await response.json();
  }

  async login(email, password) {
    const response = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
    this.token = response.data.token;
    return response;
  }

  async getTimetable(day = null) {
    const params = day ? `?day=${day}` : '';
    return await this.request(`/timetable${params}`);
  }

  async createTimetableEntry(entry) {
    return await this.request('/timetable', {
      method: 'POST',
      body: JSON.stringify(entry)
    });
  }

  async updateTimetableEntry(timetableId, entry) {
    return await this.request('/timetable', {
      method: 'PUT',
      body: JSON.stringify({ timetableId, ...entry })
    });
  }

  async deleteTimetableEntry(timetableId) {
    return await this.request(`/timetable?timetableId=${timetableId}`, {
      method: 'DELETE'
    });
  }

  async getActivities(status = null, day = null) {
    const body = {};
    if (status) body.status = status;
    if (day) body.day = day;
    return await this.request('/activities', {
      method: 'POST',
      body: JSON.stringify(body)
    });
  }

  async markActivityComplete(timetableId, isCompleted = true) {
    return await this.request('/activities/complete', {
      method: 'POST',
      body: JSON.stringify({ timetableId, isCompleted })
    });
  }

  async getProgress() {
    return await this.request('/progress', {
      method: 'POST',
      body: JSON.stringify({})
    });
  }

  async getStudentInfo() {
    return await this.request('/students');
  }
}

// Usage
const api = new StudentAPI('https://student.bmgdigital.in/api/v1');
await api.login('student@example.com', 'password123');
const timetable = await api.getTimetable();
const activities = await api.getActivities();
const progress = await api.getProgress();
```

### Python (Requests)

```python
import requests

class StudentAPI:
    def __init__(self, base_url, token=None, api_key=None):
        self.base_url = base_url
        self.token = token
        self.api_key = api_key
        self.session = requests.Session()
        
        # Set default headers
        self.session.headers.update({
            'Content-Type': 'application/json'
        })
        
        # Set authentication
        if self.token:
            self.session.headers.update({
                'Authorization': f'Bearer {self.token}'
            })
        elif self.api_key:
            self.session.headers.update({
                'X-API-Key': self.api_key
            })
    
    def login(self, email, password):
        response = self.session.post(
            f'{self.base_url}/auth/login',
            json={'email': email, 'password': password}
        )
        response.raise_for_status()
        data = response.json()
        self.token = data['data']['token']
        self.session.headers.update({
            'Authorization': f'Bearer {self.token}'
        })
        return data
    
    def get_timetable(self, day=None):
        params = {'day': day} if day else {}
        response = self.session.get(
            f'{self.base_url}/timetable',
            params=params
        )
        response.raise_for_status()
        return response.json()
    
    def create_timetable_entry(self, entry):
        response = self.session.post(
            f'{self.base_url}/timetable',
            json=entry
        )
        response.raise_for_status()
        return response.json()
    
    def update_timetable_entry(self, timetable_id, entry):
        response = self.session.put(
            f'{self.base_url}/timetable',
            json={'timetableId': timetable_id, **entry}
        )
        response.raise_for_status()
        return response.json()
    
    def delete_timetable_entry(self, timetable_id):
        response = self.session.delete(
            f'{self.base_url}/timetable',
            params={'timetableId': timetable_id}
        )
        response.raise_for_status()
        return response.json()
    
    def get_activities(self, status=None, day=None):
        data = {}
        if status:
            data['status'] = status
        if day:
            data['day'] = day
        response = self.session.post(
            f'{self.base_url}/activities',
            json=data
        )
        response.raise_for_status()
        return response.json()
    
    def mark_activity_complete(self, timetable_id, is_completed=True):
        response = self.session.post(
            f'{self.base_url}/activities/complete',
            json={'timetableId': timetable_id, 'isCompleted': is_completed}
        )
        response.raise_for_status()
        return response.json()
    
    def get_progress(self):
        response = self.session.post(
            f'{self.base_url}/progress',
            json={}
        )
        response.raise_for_status()
        return response.json()
    
    def get_student_info(self):
        response = self.session.get(f'{self.base_url}/students')
        response.raise_for_status()
        return response.json()

# Usage
api = StudentAPI('https://student.bmgdigital.in/api/v1')
api.login('student@example.com', 'password123')
timetable = api.get_timetable()
activities = api.get_activities()
progress = api.get_progress()
```

---

## Support

For issues, questions, or feature requests, please contact the development team.

**API Version:** 1.0.0  
**Last Updated:** 2024-01-01


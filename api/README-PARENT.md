# Student Progress Tracker - Parent API Documentation

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

This API provides a RESTful interface for parents to access their children's progress data, activities, and information. Parents can view data for all their students or filter by specific student. Parents can authenticate using JWT tokens (recommended for mobile/web apps) or API keys (for external integrations).

**Key Features:**
- ✅ Access all your children's data in one place
- ✅ View activities and progress for each student
- ✅ Filter by specific student when needed
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
1. Parent logs in via `/auth/parent-login`
2. Server returns a JWT token
3. Client stores the token securely
4. Client includes token in subsequent API requests
5. Token expires after 7 days (client should refresh by logging in again)

**Advantages:**
- ✅ Secure - tokens expire automatically
- ✅ User-specific - each parent has their own token
- ✅ No need to store API keys
- ✅ Better for user-facing applications

**How to use:**

**Step 1: Login**
```bash
POST /api/v1/auth/parent-login
Content-Type: application/json

{
  "email": "parent@example.com",
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
    "apiKey": "4bf291b277662a13328825cb13dbaf72fa44485eea8387fbe9d6091a2486c9ab",
    "parentId": 1,
    "userName": "John Doe",
    "email": "parent@example.com",
    "userType": "parent",
    "students": [
      {
        "studentId": 1,
        "studentName": "Jane Doe",
        "class": 10
      },
      {
        "studentId": 2,
        "studentName": "Bob Doe",
        "class": 8
      }
    ],
    "totalStudents": 2,
    "authentication": {
      "jwtToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
      "apiKey": "4bf291b277662a13328825cb13dbaf72fa44485eea8387fbe9d6091a2486c9ab",
      "note": "Both JWT token and API key are available. Use JWT for apps, API key for external integrations."
    }
  },
  "message": "Login successful"
}
```

**Note:** 
- `apiKey` will be `null` if the parent hasn't generated an API key yet. Generate one from the parent profile page.
- Both JWT token and API key are returned, allowing you to use either authentication method.
- Use JWT token for mobile/web apps where users log in.
- Use API key for external integrations, scripts, or server-to-server communication.

**Step 2: Use Token in Requests**

**Option A: Authorization Header (Recommended)**
```bash
GET /api/v1/students
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

**Option B: Query Parameter**
```bash
GET /api/v1/students?token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

**Important:** When using a parent token:
- Access all students' data by omitting `studentEmail` (returns grouped data)
- Access specific student's data by including `studentEmail` parameter
- Use `/api/v1/students` to get list of all students

---

### Method 2: API Key Authentication (For External Access)

**When to use:** External integrations, scripts, third-party services, or scenarios where user login is not possible.

**How it works:**
1. Generate API key from Parent Profile → My Profile → Generate API Key
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
- Go to Parent Profile → My Profile → Generate API Key

**Step 2: Use API Key in Requests**

**Option A: X-API-Key Header (Recommended)**
```bash
GET /api/v1/students
X-API-Key: 4bf291b277662a13328825cb13dbaf72fa44485eea8387fbe9d6091a2486c9ab
```

**Option B: Query Parameter**
```bash
GET /api/v1/students?api_key=4bf291b277662a13328825cb13dbaf72fa44485eea8387fbe9d6091a2486c9ab
```

---

## Getting Started

### Quick Start with JWT Token (Mobile/Web App)

```javascript
// 1. Login
const loginResponse = await fetch('https://student.bmgdigital.in/api/v1/auth/parent-login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'parent@example.com',
    password: 'password123'
  })
});

const loginData = await loginResponse.json();
const token = loginData.data.token;
const students = loginData.data.students; // List of all students

// 2. Store token securely (e.g., SecureStorage, Keychain, etc.)
// await SecureStorage.setItem('auth_token', token);

// 3. Use token in subsequent requests
// Get all students' activities
const activitiesResponse = await fetch('https://student.bmgdigital.in/api/v1/activities', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

// Get specific student's activities
const studentActivitiesResponse = await fetch('https://student.bmgdigital.in/api/v1/activities?studentId=1', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

### Quick Start with API Key (External Script)

```bash
# Using curl with API key
curl -X GET "https://student.bmgdigital.in/api/v1/students" \
  -H "X-API-Key: your-api-key-here"
```

```python
# Using Python requests
import requests

headers = {
    'X-API-Key': 'your-api-key-here'
}

# Get all students
response = requests.get('https://student.bmgdigital.in/api/v1/students', headers=headers)
students = response.json()

# Get activities for specific student
response = requests.get(
    'https://student.bmgdigital.in/api/v1/activities',
    headers=headers,
    params={'studentId': 1}
)
activities = response.json()
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

#### Parent Login
**POST** `/auth/parent-login`

Authenticates a parent and returns a JWT token. Parents can access data for all their students.

**Request:**
```json
{
  "email": "parent@example.com",
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
    "apiKey": "4bf291b277662a13328825cb13dbaf72fa44485eea8387fbe9d6091a2486c9ab",
    "parentId": 1,
    "userName": "John Doe",
    "email": "parent@example.com",
    "userType": "parent",
    "students": [
      {
        "studentId": 1,
        "studentName": "Jane Doe",
        "class": 10
      },
      {
        "studentId": 2,
        "studentName": "Bob Doe",
        "class": 8
      }
    ],
    "totalStudents": 2,
    "authentication": {
      "jwtToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
      "apiKey": "4bf291b277662a13328825cb13dbaf72fa44485eea8387fbe9d6091a2486c9ab",
      "note": "Both JWT token and API key are available. Use JWT for apps, API key for external integrations."
    }
  },
  "message": "Login successful"
}
```

**cURL Example:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/auth/parent-login" \
  -H "Content-Type: application/json" \
  -d '{"email":"parent@example.com","password":"password123"}'
```

---

### Students

#### Get All Students
**GET** `/students`

Returns list of all students under the authenticated parent.

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
      },
      {
        "studentId": 2,
        "studentName": "Bob Doe",
        "email": "bob@example.com",
        "phoneNo": "0987654321",
        "class": 8,
        "profileImg": null,
        "profileImgUrl": null,
        "status": "active"
      }
    ],
    "total": 2
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

### Activities

#### Get Activities
**POST** `/activities`

Returns activities for the authenticated parent's students. 

**Authentication:** JWT Token or API Key

**Request Body:**
```json
{
  "studentEmail": "student@example.com",
  "day": "Monday",
  "status": "pending"
}
```

**Body Parameters:**
- `studentEmail` (optional): Filter by specific student email. If omitted, returns all students' activities grouped
- `day` (optional): Filter by day (Monday, Tuesday, etc.). Default: current day
- `status` (optional): Filter by status (`completed`, `pending`, `all`). Default: `all`

**Response (All Students - Grouped):**
```json
{
  "status": "success",
  "data": {
    "students": [
      {
        "studentId": 1,
        "studentName": "Jane Doe",
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
            "isCompleted": false,
            "isCompletedToday": false,
            "isOverdue": false
          }
        ],
        "statistics": {
          "total": 5,
          "completed": 2,
          "pending": 3,
          "overdue": 1,
          "completionPercentage": 40.0
        }
      },
      {
        "studentId": 2,
        "studentName": "Bob Doe",
        "activities": [ ... ],
        "statistics": {
          "total": 3,
          "completed": 3,
          "pending": 0,
          "overdue": 0,
          "completionPercentage": 100.0
        }
      }
    ],
    "overallStatistics": {
      "total": 8,
      "completed": 5,
      "pending": 3,
      "overdue": 1,
      "completionPercentage": 62.5
    },
    "currentTime": "2024-01-01 14:30:00",
    "currentDay": "Monday"
  }
}
```

**Response (Specific Student):**
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
        "isCompleted": false,
        "isCompletedToday": false,
        "isOverdue": false
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

**With JWT Token (All Students):**
```bash
# Get all students' activities (grouped)
curl -X POST "https://student.bmgdigital.in/api/v1/activities" \
  -H "Authorization: Bearer parent-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{}'

# Get all students' activities for specific day
curl -X POST "https://student.bmgdigital.in/api/v1/activities" \
  -H "Authorization: Bearer parent-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{
    "day": "Monday"
  }'
```

**With JWT Token (Filter by Student):**
```bash
# Get specific student's activities
curl -X POST "https://student.bmgdigital.in/api/v1/activities" \
  -H "Authorization: Bearer parent-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{
    "studentEmail": "student@example.com"
  }'

# Get pending activities for specific student
curl -X POST "https://student.bmgdigital.in/api/v1/activities" \
  -H "Authorization: Bearer parent-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{
    "studentEmail": "student@example.com",
    "status": "pending",
    "day": "Monday"
  }'
```

**With API Key:**
```bash
# Get all students' activities
curl -X GET "https://student.bmgdigital.in/api/v1/activities" \
  -H "X-API-Key: your-api-key-here"

# Get specific student's activities
curl -X GET "https://student.bmgdigital.in/api/v1/activities?studentId=1" \
  -H "X-API-Key: your-api-key-here"
```

---

### Dashboard

#### Get Dashboard Data
**POST** `/dashboard`

Returns comprehensive dashboard data for all children (or filtered by specific student). This endpoint aggregates progress, activities, and statistics in a single response for easy dashboard display.

**Authentication:** JWT Token or API Key

**Request Body:**
```json
{
  "studentEmail": "student@example.com",
  "day": "Monday"
}
```

**Body Parameters:**
- `studentEmail` (optional): Filter by specific student email. If omitted, returns all students' data
- `day` (optional): Filter activities by day (Monday, Tuesday, etc.). Default: current day
- For student authentication: Body can be empty `{}` or omitted - returns own dashboard data

**Response (All Students):**
```json
{
  "status": "success",
  "data": {
    "students": [
      {
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
            "completionPercentage": 75.0,
            "approvedPercentage": 60.0
          },
          "subjects": [ ... ],
          "completedSubjects": [ ... ],
          "completedChapters": [ ... ]
        },
        "activities": {
          "list": [ ... ],
          "statistics": {
            "total": 5,
            "completed": 2,
            "pending": 3,
            "overdue": 1,
            "completionPercentage": 40.0
          }
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
    ],
    "overallStatistics": {
      "totalStudents": 2,
      "totalActivities": 8,
      "completedActivities": 5,
      "pendingActivities": 3,
      "overdueActivities": 1,
      "completionPercentage": 62.5
    },
    "currentDay": "Monday",
    "currentDate": "2024-01-01",
    "currentTime": "2024-01-01 14:30:00"
  }
}
```

**Response (Single Student):**
When `studentEmail` is provided, returns data for that specific student only (similar structure but with single student object).

**cURL Examples:**

**With JWT Token (All Students):**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/dashboard" \
  -H "Authorization: Bearer parent-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**With JWT Token (Specific Student):**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/dashboard" \
  -H "Authorization: Bearer parent-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{
    "studentEmail": "student@example.com"
  }'
```

**With API Key (All Students):**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/dashboard" \
  -H "X-API-Key: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**With API Key (Specific Student):**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/dashboard" \
  -H "X-API-Key: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{
    "studentEmail": "student@example.com"
  }'
```

**Filter by day:**
```bash
curl -X GET "https://student.bmgdigital.in/api/v1/dashboard?day=Tuesday" \
  -H "Authorization: Bearer parent-jwt-token-here"
```

**Benefits:**
- ✅ Single API call instead of multiple calls to `/students`, `/progress`, and `/activities`
- ✅ Optimized for dashboard display
- ✅ Includes overall statistics across all children
- ✅ Can filter by specific student when needed
- ✅ Reduces network requests and improves app performance

---

### Progress

#### Get Student Progress
**POST** `/progress`

Returns comprehensive progress data for a specific student.

**Authentication:** JWT Token or API Key

**Request Body:**
```json
{
  "studentEmail": "student@example.com"
}
```

**Body Parameters:**
- `studentEmail` (required for parent authentication): Specify student email
- For student authentication: Body can be empty `{}` or omitted - returns own progress

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
# Get progress for specific student
curl -X GET "https://student.bmgdigital.in/api/v1/progress?studentId=1" \
  -H "Authorization: Bearer parent-jwt-token-here"
```

**With API Key:**
```bash
# Get progress for specific student
curl -X GET "https://student.bmgdigital.in/api/v1/progress?studentId=1" \
  -H "X-API-Key: parent-api-key-here"
```

**Important Notes:**
- All endpoints use **POST method only**
- For parent authentication, include `studentEmail` in request body when filtering by specific student
- The student email must belong to a student under the authenticated parent's account
- If you try to access a student that doesn't belong to you, you'll get a 403 Forbidden error
- Using email instead of ID provides better security and privacy
- POST method prevents email addresses from appearing in URL logs and browser history

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
  "message": "The requested student does not belong to this parent account"
}
```

**422 Validation Error:**
```json
{
  "status": "error",
  "message": "Student email is required",
  "errors": {
    "studentEmail": "Student email is required for parent authentication"
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
class ParentAPI {
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
    const response = await this.request('/auth/parent-login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
    this.token = response.data.token;
    return response;
  }

  async getStudents() {
    return await this.request('/students');
  }

  async getActivities(studentEmail = null, status = null, day = null) {
    const body = {};
    if (studentEmail) body.studentEmail = studentEmail;
    if (status) body.status = status;
    if (day) body.day = day;
    return await this.request('/activities', {
      method: 'POST',
      body: JSON.stringify(body)
    });
  }

  async getProgress(studentEmail) {
    if (!studentEmail) {
      throw new Error('studentEmail is required for parent authentication');
    }
    return await this.request('/progress', {
      method: 'POST',
      body: JSON.stringify({ studentEmail })
    });
  }
}

// Usage
const api = new ParentAPI('https://student.bmgdigital.in/api/v1');
const loginResponse = await api.login('parent@example.com', 'password123');
const students = loginResponse.data.students;

// Get all students' activities
const allActivities = await api.getActivities();

// Get specific student's activities
const studentActivities = await api.getActivities(students[0].email);

// Get progress for specific student
const progress = await api.getProgress(students[0].email);
```

### Python (Requests)

```python
import requests

class ParentAPI:
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
            f'{self.base_url}/auth/parent-login',
            json={'email': email, 'password': password}
        )
        response.raise_for_status()
        data = response.json()
        self.token = data['data']['token']
        self.session.headers.update({
            'Authorization': f'Bearer {self.token}'
        })
        return data
    
    def get_students(self):
        response = self.session.get(f'{self.base_url}/students')
        response.raise_for_status()
        return response.json()
    
    def get_activities(self, student_email=None, status=None, day=None):
        data = {}
        if student_email:
            data['studentEmail'] = student_email
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
    
    def get_progress(self, student_email):
        if not student_email:
            raise ValueError('student_email is required for parent authentication')
        response = self.session.post(
            f'{self.base_url}/progress',
            json={'studentEmail': student_email}
        )
        response.raise_for_status()
        return response.json()

# Usage
api = ParentAPI('https://student.bmgdigital.in/api/v1')
login_data = api.login('parent@example.com', 'password123')
students = login_data['data']['students']

# Get all students' activities
all_activities = api.get_activities()

# Get specific student's activities
student_activities = api.get_activities(student_email=students[0]['email'])

# Get progress for specific student
progress = api.get_progress(students[0]['email'])
```

---

## Support

For issues, questions, or feature requests, please contact the development team.

**API Version:** 1.0.0  
**Last Updated:** 2024-01-01


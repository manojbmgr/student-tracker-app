# Student Progress Tracker - API Documentation

## Overview

This API provides a RESTful interface for accessing Student Progress Tracker data. The API supports two user types with different access levels:

- **Students** - Can access their own timetable, activities, and progress
- **Parents** - Can access all their children's data, with ability to filter by specific student

All endpoints return JSON responses and follow RESTful conventions.

---

## Documentation by User Type

### 📚 [Student API Documentation](./README-STUDENT.md)
Complete documentation for student endpoints, including:
- Student login and authentication
- Timetable management (CRUD operations)
- Activities tracking
- Progress monitoring
- Student profile information

**👉 [View Student Documentation](./README-STUDENT.md)**

---

### 👨‍👩‍👧‍👦 [Parent API Documentation](./README-PARENT.md)
Complete documentation for parent endpoints, including:
- Parent login and authentication
- Viewing all children's data
- Filtering by specific student
- Activities and progress monitoring for multiple students

**👉 [View Parent Documentation](./README-PARENT.md)**

---

## Base URL

```
https://student.bmgdigital.in/api/v1
```

---

## Authentication Methods

The API supports two authentication methods:

### 1. JWT Token Authentication (Recommended for Apps)
- **When to use:** Mobile apps, web applications, or any scenario where users can log in
- **How it works:** User logs in, receives a JWT token, uses token in subsequent requests
- **Token expires:** After 7 days (user must login again)
- **Advantages:** Secure, user-specific, automatic expiration

### 2. API Key Authentication (For External Access)
- **When to use:** External integrations, scripts, third-party services
- **How it works:** Generate API key from profile, include in every request
- **API key expires:** Never (but can be regenerated)
- **Advantages:** No login required, good for automation

For detailed authentication instructions, see:
- [Student Authentication Guide](./README-STUDENT.md#authentication)
- [Parent Authentication Guide](./README-PARENT.md#authentication)

---

## Quick Start

### For Students

1. **Login to get JWT token:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"student@example.com","password":"password123"}'
```

2. **Use token to access endpoints:**
```bash
curl -X GET "https://student.bmgdigital.in/api/v1/timetable" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**👉 See [Student Documentation](./README-STUDENT.md) for complete examples**

---

### For Parents

1. **Login to get JWT token:**
```bash
curl -X POST "https://student.bmgdigital.in/api/v1/auth/parent-login" \
  -H "Content-Type: application/json" \
  -d '{"email":"parent@example.com","password":"password123"}'
```

2. **Use token to access endpoints:**
```bash
# Get all students
curl -X GET "https://student.bmgdigital.in/api/v1/students" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Get activities for specific student
curl -X GET "https://student.bmgdigital.in/api/v1/activities?studentId=1" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**👉 See [Parent Documentation](./README-PARENT.md) for complete examples**

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

## HTTP Status Codes

- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized (Invalid or missing authentication)
- `403` - Forbidden (Valid authentication but insufficient permissions)
- `404` - Not Found
- `405` - Method Not Allowed
- `422` - Validation Error
- `500` - Internal Server Error

---

## Key Differences: Student vs Parent

| Feature | Student | Parent |
|---------|---------|--------|
| **Login Endpoint** | `/auth/login` | `/auth/parent-login` |
| **Timetable Access** | ✅ Full CRUD access | ❌ Not available |
| **Activities** | Own activities only | All children's activities (can filter by studentId) |
| **Dashboard** | Own dashboard data | All children's dashboard data (POST to filter by studentEmail) |
| **Progress** | Own progress | POST with `studentEmail` in body (required for parents) |
| **Students List** | Own information only | All children's information |
| **Data Scope** | Single student | Multiple students |

---

## Endpoint Summary

### Authentication
- `POST /auth/login` - Student login
- `POST /auth/parent-login` - Parent login

### Students
- `GET /students` - Get student(s) information

### Timetable (Student Only)
- `GET /timetable` - Get timetable entries
- `POST /timetable` - Create timetable entry
- `PUT /timetable` - Update timetable entry
- `DELETE /timetable` - Delete timetable entry

### Dashboard
- `GET /dashboard` - Get comprehensive dashboard data (progress, activities, statistics)

### Activities
- `GET /activities` - Get activities
- `POST /activities/complete` - Mark activity complete

### Progress
- `GET /progress` - Get student progress

For detailed endpoint documentation, see:
- [Student Endpoints](./README-STUDENT.md#endpoints)
- [Parent Endpoints](./README-PARENT.md#endpoints)

---

## Best Practices

### Security
- ✅ Always use HTTPS in production
- ✅ Store JWT tokens securely (never in localStorage for web apps)
- ✅ Use SecureStorage/Keychain for mobile apps
- ✅ Keep API keys secret (never commit to version control)
- ✅ Regenerate API keys if compromised

### Performance
- ✅ Cache responses when appropriate
- ✅ Implement offline support for mobile apps
- ✅ Handle rate limiting gracefully

### Error Handling
- ✅ Always check response status codes
- ✅ Handle network errors gracefully
- ✅ Validate API responses before use

For detailed best practices, see:
- [Student Best Practices](./README-STUDENT.md#best-practices)
- [Parent Best Practices](./README-PARENT.md#best-practices)

---

## Code Examples

### JavaScript/TypeScript
See complete examples in:
- [Student Code Examples](./README-STUDENT.md#code-examples)
- [Parent Code Examples](./README-PARENT.md#code-examples)

### Python
See complete examples in:
- [Student Code Examples](./README-STUDENT.md#code-examples)
- [Parent Code Examples](./README-PARENT.md#code-examples)

---

## Support

For issues, questions, or feature requests, please contact the development team.

**API Version:** 1.0.0  
**Last Updated:** 2024-01-01

---

## Quick Links

- 📚 [Student API Documentation](./README-STUDENT.md)
- 👨‍👩‍👧‍👦 [Parent API Documentation](./README-PARENT.md)

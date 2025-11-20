<?php
/**
 * API Endpoint: Parent Login
 * POST /api/v1/auth/parent-login
 * 
 * Authenticates parent and returns JWT token
 * Parents can access data for all their students
 */

require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/config.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/response.php';

use Firebase\JWT\JWT;

// Only allow POST method
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    ApiResponse::error('Method not allowed', 405);
}

// Get request data
$input = json_decode(file_get_contents('php://input'), true);
$email = isset($input['email']) ? trim($input['email']) : '';
$password = isset($input['password']) ? trim($input['password']) : '';

// Validate input
if (empty($email) || empty($password)) {
    ApiResponse::validationError([
        'email' => 'Email is required',
        'password' => 'Password is required'
    ], 'Email and password are required');
}

// Verify credentials from users table (parent/admin)
// Note: userType column may not exist in all databases, so we default to 'parent'
$stmt = $conn->prepare("SELECT userId, userEmail, password, userName, api_key FROM users WHERE userEmail = ?");
if (!$stmt) {
    ApiResponse::error('Database error', 500);
}

$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    ApiResponse::error('Invalid email or password', 401);
}

$user = $result->fetch_assoc();
$stmt->close();

// Verify password
if (!password_verify($password, $user['password'])) {
    ApiResponse::error('Invalid email or password', 401);
}

// Get API key if it exists
$apiKey = !empty($user['api_key']) ? $user['api_key'] : null;

// Get all students for this parent
$studentsStmt = $conn->prepare("SELECT studentId, studentName, class FROM students WHERE parentId = ?");
$studentsStmt->bind_param("i", $user['userId']);
$studentsStmt->execute();
$studentsResult = $studentsStmt->get_result();
$students = [];
while ($studentRow = $studentsResult->fetch_assoc()) {
    $students[] = [
        'studentId' => $studentRow['studentId'],
        'studentName' => $studentRow['studentName'],
        'class' => $studentRow['class']
    ];
}
$studentsStmt->close();

// Generate JWT token
function generateToken($data) {
    global $encryptKey;
    $issuedAt = time();
    $expire = $issuedAt + (7 * 24 * 60 * 60); // Valid for 7 days
    $payload = ['data' => $data, 'iat' => $issuedAt, 'exp' => $expire];
    return JWT::encode($payload, $encryptKey, 'HS256');
}

$token = generateToken([
    'parentId' => $user['userId'],
    'email' => $user['userEmail'],
    'userName' => $user['userName'],
    'userType' => 'parent'
]);

// Return success with token, API key, and student list
ApiResponse::success([
    'token' => $token,
    'expiresIn' => 7 * 24 * 60 * 60, // 7 days in seconds
    'apiKey' => $apiKey, // API key for external integrations (null if not generated)
    'parentId' => $user['userId'],
    'userName' => $user['userName'],
    'email' => $user['userEmail'],
    'userType' => 'parent',
    'students' => $students,
    'totalStudents' => count($students),
    'authentication' => [
        'jwtToken' => $token,
        'apiKey' => $apiKey,
        'note' => $apiKey ? 'Both JWT token and API key are available. Use JWT for apps, API key for external integrations.' : 'API key not set. Generate one from your profile page for external integrations.'
    ]
], 'Login successful');


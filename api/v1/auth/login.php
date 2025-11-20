<?php
/**
 * API Endpoint: Login
 * POST /api/v1/auth/login
 * 
 * Authenticates user and returns JWT token
 */

require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/config.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/response.php';

use Firebase\JWT\JWT;
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/auth.php';

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

// Verify credentials
$stmt = $conn->prepare("SELECT studentId, email, password FROM students WHERE email = ?");
if (!$stmt) {
    ApiResponse::error('Database error', 500);
}

$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    ApiResponse::error('Invalid email or password', 401);
}

$student = $result->fetch_assoc();
$stmt->close();

// Verify password
if (!password_verify($password, $student['password'])) {
    ApiResponse::error('Invalid email or password', 401);
}

// Generate JWT token
function generateToken($data) {
    global $encryptKey;
    $issuedAt = time();
    $expire = $issuedAt + (7 * 24 * 60 * 60); // Valid for 7 days
    $payload = ['data' => $data, 'iat' => $issuedAt, 'exp' => $expire];
    return JWT::encode($payload, $encryptKey, 'HS256');
}

$token = generateToken([
    'studentId' => $student['studentId'],
    'email' => $student['email']
]);

// Return success with token
ApiResponse::success([
    'token' => $token,
    'expiresIn' => 7 * 24 * 60 * 60, // 7 days in seconds
    'studentId' => $student['studentId']
], 'Login successful');


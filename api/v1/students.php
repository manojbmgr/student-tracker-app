<?php
/**
 * API Endpoint: Get Students
 * GET /api/v1/students
 * 
 * Returns list of students for authenticated parent
 * For student authentication, returns their own information
 */

require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/config.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/response.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/auth.php';

// Only allow GET method
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    ApiResponse::error('Method not allowed', 405);
}

// Authenticate request
if (!ApiAuth::authenticate()) {
    ApiResponse::unauthorized();
}

$students = [];

// If authenticated as student, return their own info
if (ApiAuth::getStudentId()) {
    $studentId = ApiAuth::getStudentId();
    $stmt = $conn->prepare("SELECT studentId, studentName, email, phoneNo, class, profileImg, status FROM students WHERE studentId = ?");
    $stmt->bind_param("i", $studentId);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        $student = $result->fetch_assoc();
        // Add profile image URL
        if (!empty($student['profileImg'])) {
            $student['profileImgUrl'] = BASE_URL . 'uploads/profiles/' . $student['profileImg'];
        }
        $students[] = $student;
    }
    $stmt->close();
} 
// If authenticated as parent, return all their students
elseif (ApiAuth::getParentId() || ApiAuth::isParentKey()) {
    $parentId = ApiAuth::getParentId();
    if (!$parentId) {
        // If parent ID not set but is parent key, get it from token or API key
        $parentStudentIds = ApiAuth::getParentStudentIds();
        if (empty($parentStudentIds)) {
            ApiResponse::error('No students found for this parent account', 404);
        }
        // Get parent ID from first student
        $stmt = $conn->prepare("SELECT parentId FROM students WHERE studentId = ?");
        $stmt->bind_param("i", $parentStudentIds[0]);
        $stmt->execute();
        $result = $stmt->get_result();
        if ($result->num_rows > 0) {
            $row = $result->fetch_assoc();
            $parentId = $row['parentId'];
        }
        $stmt->close();
    }
    
    if ($parentId) {
        $stmt = $conn->prepare("SELECT studentId, studentName, email, phoneNo, class, profileImg, status FROM students WHERE parentId = ? ORDER BY studentName");
        $stmt->bind_param("i", $parentId);
        $stmt->execute();
        $result = $stmt->get_result();
        
        while ($row = $result->fetch_assoc()) {
            // Add profile image URL
            if (!empty($row['profileImg'])) {
                $row['profileImgUrl'] = BASE_URL . 'uploads/profiles/' . $row['profileImg'];
            }
            $students[] = $row;
        }
        $stmt->close();
    }
}

if (empty($students)) {
    ApiResponse::error('No students found', 404);
}

ApiResponse::success([
    'students' => $students,
    'total' => count($students)
]);


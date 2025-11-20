<?php
/**
 * API Endpoint: Student Progress
 * POST /api/v1/progress
 * 
 * Returns student progress data including subjects, chapters, and exercises
 * For students: Returns own progress (no body required)
 * For parents: Requires studentEmail in request body
 */

require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/config.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/response.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/auth.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/action/index.php';

// Only allow POST method
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    ApiResponse::error('Method not allowed. Use POST method.', 405);
}

// Authenticate request
if (!ApiAuth::authenticate()) {
    ApiResponse::unauthorized();
}

$studentId = ApiAuth::getStudentId();
if (!$studentId) {
    // For parent authentication (JWT token or API key), check if studentEmail is provided
    if (ApiAuth::getParentId() || ApiAuth::isParentKey()) {
        // Get studentEmail from POST body
        $input = json_decode(file_get_contents('php://input'), true);
        if ($input === null) {
            $input = [];
        }
        
        $requestedStudentEmail = isset($input['studentEmail']) ? trim($input['studentEmail']) : '';
        
        if (!empty($requestedStudentEmail)) {
            $parentId = ApiAuth::getParentId();
            if (!$parentId) {
                // For API key, get parent ID from first student
                $parentStudentIds = ApiAuth::getParentStudentIds();
                if (!empty($parentStudentIds)) {
                    $tempStmt = $conn->prepare("SELECT parentId FROM students WHERE studentId = ?");
                    $tempStmt->bind_param("i", $parentStudentIds[0]);
                    $tempStmt->execute();
                    $tempResult = $tempStmt->get_result();
                    if ($tempResult->num_rows > 0) {
                        $tempRow = $tempResult->fetch_assoc();
                        $parentId = $tempRow['parentId'];
                    }
                    $tempStmt->close();
                }
            }
            
            // Get student ID from email and verify it belongs to this parent
            $stmt = $conn->prepare("SELECT studentId FROM students WHERE email = ? AND parentId = ?");
            $stmt->bind_param("si", $requestedStudentEmail, $parentId);
            $stmt->execute();
            $result = $stmt->get_result();
            
            if ($result->num_rows === 0) {
                ApiResponse::forbidden('The requested student email does not belong to this parent account or does not exist');
            }
            
            $row = $result->fetch_assoc();
            $requestedStudentId = $row['studentId'];
            $parentStudentIds = ApiAuth::getParentStudentIds();
            
            if (in_array($requestedStudentId, $parentStudentIds)) {
                $studentId = $requestedStudentId;
            } else {
                ApiResponse::forbidden('The requested student does not belong to this parent account');
            }
            $stmt->close();
        } else {
            ApiResponse::validationError(['studentEmail' => 'Student email is required for parent authentication'], 'Student email is required');
        }
    } else {
        ApiResponse::forbidden('Student authentication required');
    }
}

// Get progress data
$progress = getStudentProgressWithPercentages($studentId);

ApiResponse::success($progress);


<?php
/**
 * API Endpoint: Mark Activity Complete
 * POST /api/v1/activities/complete
 * 
 * Marks an activity as complete or incomplete
 */

require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/config.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/response.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/auth.php';

// Only allow POST method
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    ApiResponse::error('Method not allowed', 405);
}

// Authenticate request
if (!ApiAuth::authenticate()) {
    ApiResponse::unauthorized();
}

$studentId = ApiAuth::getStudentId();
if (!$studentId) {
    ApiResponse::forbidden('Student authentication required');
}

// Get request data
$input = json_decode(file_get_contents('php://input'), true);
$activityId = isset($input['activityId']) ? intval($input['activityId']) : 0;
$timetableId = isset($input['timetableId']) ? intval($input['timetableId']) : 0;
$isCompleted = isset($input['isCompleted']) ? 
    ($input['isCompleted'] === true || $input['isCompleted'] === '1' || $input['isCompleted'] === 'true') : true;

if ($activityId <= 0 && $timetableId <= 0) {
    ApiResponse::validationError([
        'activityId' => 'Activity ID or Timetable ID is required'
    ], 'Activity ID or Timetable ID is required');
}

try {
    $completedAt = $isCompleted ? date('Y-m-d H:i:s') : null;
    
    if ($timetableId > 0) {
        // Handle timetable entry - get timetable data and create/update in timetable_activities
        $timetableSql = "SELECT * FROM timetable WHERE timetableId = ? AND studentId = ?";
        $timetableStmt = $conn->prepare($timetableSql);
        $timetableStmt->bind_param("ii", $timetableId, $studentId);
        $timetableStmt->execute();
        $timetableResult = $timetableStmt->get_result();
        
        if ($timetableResult->num_rows === 0) {
            ApiResponse::notFound('Timetable entry not found or access denied');
        }
        
        $timetable = $timetableResult->fetch_assoc();
        
        // Check if entry exists in timetable_activities for today
        $currentDate = date('Y-m-d');
        $checkSql = "SELECT * FROM timetable_activities WHERE studentId = ? AND dayOfWeek = ? AND startTime = ? AND endTime = ? AND activityName = ? AND DATE(createdAt) = ?";
        $checkStmt = $conn->prepare($checkSql);
        $checkStmt->bind_param("isssss", $studentId, $timetable['dayOfWeek'], $timetable['startTime'], $timetable['endTime'], $timetable['subject'], $currentDate);
        $checkStmt->execute();
        $checkResult = $checkStmt->get_result();
        
        if ($checkResult->num_rows > 0) {
            // Update existing entry for today
            $existing = $checkResult->fetch_assoc();
            $updateSql = "UPDATE timetable_activities SET isCompleted = ?, completedAt = ? WHERE activityId = ?";
            $updateStmt = $conn->prepare($updateSql);
            $isCompletedInt = $isCompleted ? 1 : 0;
            $updateStmt->bind_param("isi", $isCompletedInt, $completedAt, $existing['activityId']);
            
            if ($updateStmt->execute()) {
                ApiResponse::success([
                    'timetableId' => $timetableId,
                    'activityId' => $existing['activityId'],
                    'isCompleted' => $isCompleted,
                    'completedAt' => $completedAt,
                    'subject' => $timetable['subject']
                ], $isCompleted ? 'Activity marked as completed' : 'Activity marked as incomplete');
            } else {
                ApiResponse::error('Failed to update activity: ' . $updateStmt->error, 500);
            }
        } else {
            // Create new entry in timetable_activities
            $insertSql = "INSERT INTO timetable_activities (studentId, dayOfWeek, startTime, endTime, activityType, activityName, location, notes, isCompleted, completedAt) 
                         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            $insertStmt = $conn->prepare($insertSql);
            $activityType = 'study';
            $isCompletedInt = $isCompleted ? 1 : 0;
            $insertStmt->bind_param("isssssssis", $studentId, $timetable['dayOfWeek'], $timetable['startTime'], $timetable['endTime'], 
                                   $activityType, $timetable['subject'], $timetable['location'], $timetable['notes'], $isCompletedInt, $completedAt);
            
            if ($insertStmt->execute()) {
                ApiResponse::success([
                    'timetableId' => $timetableId,
                    'activityId' => $conn->insert_id,
                    'isCompleted' => $isCompleted,
                    'completedAt' => $completedAt,
                    'subject' => $timetable['subject']
                ], $isCompleted ? 'Activity marked as completed' : 'Activity marked as incomplete');
            } else {
                ApiResponse::error('Failed to create activity entry: ' . $insertStmt->error, 500);
            }
        }
    } else {
        // Handle existing timetable_activities entry
        $checkSql = "SELECT * FROM timetable_activities WHERE activityId = ? AND studentId = ?";
        $checkStmt = $conn->prepare($checkSql);
        $checkStmt->bind_param("ii", $activityId, $studentId);
        $checkStmt->execute();
        $checkResult = $checkStmt->get_result();
        
        if ($checkResult->num_rows === 0) {
            ApiResponse::notFound('Activity not found or access denied');
        }
        
        $activity = $checkResult->fetch_assoc();
        
        // Update completion status
        $updateSql = "UPDATE timetable_activities SET isCompleted = ?, completedAt = ? WHERE activityId = ?";
        $updateStmt = $conn->prepare($updateSql);
        $isCompletedInt = $isCompleted ? 1 : 0;
        $updateStmt->bind_param("isi", $isCompletedInt, $completedAt, $activityId);
        
        if ($updateStmt->execute()) {
            ApiResponse::success([
                'activityId' => $activityId,
                'isCompleted' => $isCompleted,
                'completedAt' => $completedAt,
                'activityName' => $activity['activityName'],
                'activityType' => $activity['activityType']
            ], $isCompleted ? 'Activity marked as completed' : 'Activity marked as incomplete');
        } else {
            ApiResponse::error('Failed to update activity: ' . $updateStmt->error, 500);
        }
    }
} catch (Exception $e) {
    ApiResponse::error('Error: ' . $e->getMessage(), 500);
}


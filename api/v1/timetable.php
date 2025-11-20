<?php
/**
 * API Endpoint: Timetable Management
 * GET, POST, PUT, DELETE /api/v1/timetable
 * 
 * CRUD operations for timetable entries
 */

require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/config.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/response.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/auth.php';

// Authenticate request
if (!ApiAuth::authenticate()) {
    ApiResponse::unauthorized();
}

$studentId = ApiAuth::getStudentId();
if (!$studentId) {
    ApiResponse::forbidden('Student authentication required');
}

$method = $_SERVER['REQUEST_METHOD'];

// Ensure timetable table exists
try {
    $createTable = "CREATE TABLE IF NOT EXISTS timetable (
        timetableId INT AUTO_INCREMENT PRIMARY KEY,
        studentId INT NOT NULL,
        dayOfWeek ENUM('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday') NOT NULL,
        startTime TIME NOT NULL,
        endTime TIME NOT NULL,
        subject VARCHAR(255) NOT NULL,
        location VARCHAR(255) DEFAULT NULL,
        teacher VARCHAR(255) DEFAULT NULL,
        notes TEXT DEFAULT NULL,
        alarmAudio VARCHAR(255) DEFAULT NULL,
        createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (studentId) REFERENCES students(studentId) ON DELETE CASCADE,
        INDEX idx_student_day (studentId, dayOfWeek)
    )";
    $conn->query($createTable);
    
    // Check if alarmAudio column exists, add if not
    $checkColumn = $conn->query("SHOW COLUMNS FROM timetable LIKE 'alarmAudio'");
    if (!$checkColumn || $checkColumn->num_rows === 0) {
        $conn->query("ALTER TABLE timetable ADD COLUMN alarmAudio VARCHAR(255) DEFAULT NULL AFTER notes");
    }
} catch (Exception $e) {
    // Table might already exist
}

// Handle different HTTP methods
switch ($method) {
    case 'GET':
        $day = isset($_GET['day']) ? $_GET['day'] : null;
        
        $sql = "SELECT * FROM timetable WHERE studentId = ?";
        $params = [$studentId];
        $types = "i";
        
        if ($day) {
            $sql .= " AND dayOfWeek = ?";
            $params[] = $day;
            $types .= "s";
        }
        
        $sql .= " ORDER BY dayOfWeek, startTime";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param($types, ...$params);
        $stmt->execute();
        $result = $stmt->get_result();
        $entries = [];
        
        while ($row = $result->fetch_assoc()) {
            // Add alarm audio URL if present
            if (!empty($row['alarmAudio'])) {
                $row['alarmAudioUrl'] = BASE_URL . 'uploads/alarms/' . $row['alarmAudio'];
            }
            $entries[] = $row;
        }
        
        ApiResponse::success($entries);
        break;
        
    case 'POST':
        // Create new timetable entry
        $input = json_decode(file_get_contents('php://input'), true);
        
        $dayOfWeek = isset($input['dayOfWeek']) ? $input['dayOfWeek'] : '';
        $startTime = isset($input['startTime']) ? $input['startTime'] : '';
        $endTime = isset($input['endTime']) ? $input['endTime'] : '';
        $subject = isset($input['subject']) ? trim($input['subject']) : '';
        $location = isset($input['location']) ? trim($input['location']) : '';
        $teacher = isset($input['teacher']) ? trim($input['teacher']) : '';
        $notes = isset($input['notes']) ? trim($input['notes']) : '';
        
        if (empty($dayOfWeek) || empty($startTime) || empty($endTime) || empty($subject)) {
            ApiResponse::validationError([
                'dayOfWeek' => 'Day of week is required',
                'startTime' => 'Start time is required',
                'endTime' => 'End time is required',
                'subject' => 'Subject is required'
            ], 'Required fields are missing');
        }
        
        // Handle audio file upload (if multipart/form-data)
        $alarmAudio = null;
        if (isset($_FILES['alarmAudio']) && $_FILES['alarmAudio']['error'] === UPLOAD_ERR_OK) {
            $uploadDir = $_SERVER['DOCUMENT_ROOT'] . '/uploads/alarms/';
            if (!is_dir($uploadDir)) {
                mkdir($uploadDir, 0755, true);
            }
            
            $fileExtension = strtolower(pathinfo($_FILES['alarmAudio']['name'], PATHINFO_EXTENSION));
            $allowedExtensions = ['mp3', 'wav', 'ogg', 'm4a', 'aac'];
            
            if (in_array($fileExtension, $allowedExtensions)) {
                $fileName = uniqid('alarm_', true) . '.' . $fileExtension;
                $filePath = $uploadDir . $fileName;
                
                if (move_uploaded_file($_FILES['alarmAudio']['tmp_name'], $filePath)) {
                    $alarmAudio = $fileName;
                }
            }
        }
        
        $sql = "INSERT INTO timetable (studentId, dayOfWeek, startTime, endTime, subject, location, teacher, notes, alarmAudio) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("issssssss", $studentId, $dayOfWeek, $startTime, $endTime, $subject, $location, $teacher, $notes, $alarmAudio);
        
        if ($stmt->execute()) {
            ApiResponse::success([
                'timetableId' => $conn->insert_id
            ], 'Timetable entry added successfully', 201);
        } else {
            ApiResponse::error('Failed to add timetable entry: ' . $stmt->error, 500);
        }
        break;
        
    case 'PUT':
        // Update timetable entry
        $input = json_decode(file_get_contents('php://input'), true);
        $timetableId = isset($input['timetableId']) ? intval($input['timetableId']) : 0;
        
        if ($timetableId <= 0) {
            ApiResponse::validationError(['timetableId' => 'Invalid timetable ID'], 'Invalid timetable ID');
        }
        
        // Verify ownership
        $checkSql = "SELECT studentId, alarmAudio FROM timetable WHERE timetableId = ?";
        $checkStmt = $conn->prepare($checkSql);
        $checkStmt->bind_param("i", $timetableId);
        $checkStmt->execute();
        $checkResult = $checkStmt->get_result();
        
        if ($checkResult->num_rows === 0) {
            ApiResponse::notFound('Timetable entry not found');
        }
        
        $entry = $checkResult->fetch_assoc();
        if ($entry['studentId'] != $studentId) {
            ApiResponse::forbidden();
        }
        
        $dayOfWeek = isset($input['dayOfWeek']) ? $input['dayOfWeek'] : '';
        $startTime = isset($input['startTime']) ? $input['startTime'] : '';
        $endTime = isset($input['endTime']) ? $input['endTime'] : '';
        $subject = isset($input['subject']) ? trim($input['subject']) : '';
        $location = isset($input['location']) ? trim($input['location']) : '';
        $teacher = isset($input['teacher']) ? trim($input['teacher']) : '';
        $notes = isset($input['notes']) ? trim($input['notes']) : '';
        $currentAudio = $entry['alarmAudio'];
        $alarmAudio = $currentAudio;
        
        // Handle audio removal
        if (isset($input['removeAudio']) && $input['removeAudio'] == true) {
            if ($currentAudio) {
                $oldFilePath = $_SERVER['DOCUMENT_ROOT'] . '/uploads/alarms/' . $currentAudio;
                if (file_exists($oldFilePath)) {
                    @unlink($oldFilePath);
                }
            }
            $alarmAudio = null;
        }
        
        // Handle new audio file upload
        if (isset($_FILES['alarmAudio']) && $_FILES['alarmAudio']['error'] === UPLOAD_ERR_OK) {
            // Delete old audio file if exists
            if ($currentAudio) {
                $oldFilePath = $_SERVER['DOCUMENT_ROOT'] . '/uploads/alarms/' . $currentAudio;
                if (file_exists($oldFilePath)) {
                    @unlink($oldFilePath);
                }
            }
            
            $uploadDir = $_SERVER['DOCUMENT_ROOT'] . '/uploads/alarms/';
            if (!is_dir($uploadDir)) {
                mkdir($uploadDir, 0755, true);
            }
            
            $fileExtension = strtolower(pathinfo($_FILES['alarmAudio']['name'], PATHINFO_EXTENSION));
            $allowedExtensions = ['mp3', 'wav', 'ogg', 'm4a', 'aac'];
            
            if (in_array($fileExtension, $allowedExtensions)) {
                $fileName = uniqid('alarm_', true) . '.' . $fileExtension;
                $filePath = $uploadDir . $fileName;
                
                if (move_uploaded_file($_FILES['alarmAudio']['tmp_name'], $filePath)) {
                    $alarmAudio = $fileName;
                }
            }
        }
        
        $sql = "UPDATE timetable SET dayOfWeek = ?, startTime = ?, endTime = ?, subject = ?, location = ?, teacher = ?, notes = ?, alarmAudio = ? WHERE timetableId = ?";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("ssssssssi", $dayOfWeek, $startTime, $endTime, $subject, $location, $teacher, $notes, $alarmAudio, $timetableId);
        
        if ($stmt->execute()) {
            ApiResponse::success(null, 'Timetable entry updated successfully');
        } else {
            ApiResponse::error('Failed to update timetable entry: ' . $stmt->error, 500);
        }
        break;
        
    case 'DELETE':
        $timetableId = isset($_GET['timetableId']) ? intval($_GET['timetableId']) : 0;
        
        if ($timetableId <= 0) {
            ApiResponse::validationError(['timetableId' => 'Invalid timetable ID'], 'Invalid timetable ID');
        }
        
        // Verify ownership
        $checkSql = "SELECT studentId, alarmAudio FROM timetable WHERE timetableId = ?";
        $checkStmt = $conn->prepare($checkSql);
        $checkStmt->bind_param("i", $timetableId);
        $checkStmt->execute();
        $checkResult = $checkStmt->get_result();
        
        if ($checkResult->num_rows === 0) {
            ApiResponse::notFound('Timetable entry not found');
        }
        
        $entry = $checkResult->fetch_assoc();
        if ($entry['studentId'] != $studentId) {
            ApiResponse::forbidden();
        }
        
        // Delete associated audio file if exists
        if (!empty($entry['alarmAudio'])) {
            $audioPath = $_SERVER['DOCUMENT_ROOT'] . '/uploads/alarms/' . $entry['alarmAudio'];
            if (file_exists($audioPath)) {
                @unlink($audioPath);
            }
        }
        
        $sql = "DELETE FROM timetable WHERE timetableId = ?";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("i", $timetableId);
        
        if ($stmt->execute()) {
            ApiResponse::success(null, 'Timetable entry deleted successfully');
        } else {
            ApiResponse::error('Failed to delete timetable entry: ' . $stmt->error, 500);
        }
        break;
        
    default:
        ApiResponse::error('Method not allowed', 405);
}


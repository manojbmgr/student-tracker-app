<?php
/**
 * API Endpoint: Get Activities
 * POST /api/v1/activities
 * 
 * Returns today's activities for authenticated student(s)
 * For students: Returns own activities (no body required)
 * For parents: Returns all students' activities, or filter by studentEmail in body
 */

require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/config.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/response.php';
require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/auth.php';

// Only allow POST method
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    ApiResponse::error('Method not allowed. Use POST method.', 405);
}

// Authenticate request
if (!ApiAuth::authenticate()) {
    ApiResponse::unauthorized();
}

// Get student IDs
$studentIds = [];
if (ApiAuth::getStudentId()) {
    $studentIds[] = ApiAuth::getStudentId();
} elseif (ApiAuth::getParentId() || ApiAuth::isParentKey()) {
    // Get request body
    $input = json_decode(file_get_contents('php://input'), true);
    if ($input === null) {
        $input = [];
    }
    
    // For parent authentication (JWT token or API key), get all students or filter by studentEmail
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
            $studentIds = [$requestedStudentId];
        } else {
            ApiResponse::forbidden('The requested student does not belong to this parent account');
        }
        $stmt->close();
    } else {
        $studentIds = ApiAuth::getParentStudentIds();
    }
}

if (empty($studentIds)) {
    ApiResponse::error('No students found', 404);
}

// Get optional filters from POST body
if (!isset($input)) {
    $input = json_decode(file_get_contents('php://input'), true);
    if ($input === null) {
        $input = [];
    }
}
$day = isset($input['day']) ? $input['day'] : date('l'); // Default to current day
$status = isset($input['status']) ? $input['status'] : null; // 'completed', 'pending', or 'all'

// Ensure timetable_activities table exists
try {
    $createTable = "CREATE TABLE IF NOT EXISTS timetable_activities (
        activityId INT AUTO_INCREMENT PRIMARY KEY,
        studentId INT NOT NULL,
        dayOfWeek ENUM('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday') NOT NULL,
        startTime TIME NOT NULL,
        endTime TIME NOT NULL,
        activityType VARCHAR(100) NOT NULL,
        activityName VARCHAR(255) NOT NULL,
        description TEXT DEFAULT NULL,
        alarmAudio VARCHAR(255) DEFAULT NULL,
        isCompleted BOOLEAN DEFAULT FALSE,
        completedAt DATETIME DEFAULT NULL,
        location VARCHAR(255) DEFAULT NULL,
        notes TEXT DEFAULT NULL,
        createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (studentId) REFERENCES students(studentId) ON DELETE CASCADE,
        INDEX idx_student_day (studentId, dayOfWeek),
        INDEX idx_student_completed (studentId, isCompleted),
        INDEX idx_activity_type (activityType)
    )";
    $conn->query($createTable);
} catch (Exception $e) {
    // Table might already exist
}

// Build query - read from timetable table and check timetable_activities for completion
$currentDate = date('Y-m-d');
$placeholders = implode(',', array_fill(0, count($studentIds), '?'));
    $sql = "SELECT t.*, s.studentName, s.email as studentEmail,
            ta.activityId, ta.isCompleted, ta.completedAt,
            CASE 
                WHEN ta.completedAt IS NOT NULL AND DATE(ta.completedAt) = ? THEN 1 
                ELSE 0 
            END AS isCompletedToday
            FROM timetable t
            LEFT JOIN students s ON t.studentId = s.studentId
        LEFT JOIN timetable_activities ta ON t.studentId = ta.studentId 
            AND t.dayOfWeek = ta.dayOfWeek 
            AND t.startTime = ta.startTime 
            AND t.endTime = ta.endTime
            AND t.subject = ta.activityName
            AND DATE(ta.createdAt) = ?
        WHERE t.studentId IN ($placeholders) AND t.dayOfWeek = ?";

$params = [$currentDate, $currentDate];
$params = array_merge($params, $studentIds);
$params[] = $day;
$types = "ss" . str_repeat('i', count($studentIds)) . "s";

// Filter by completion status
if ($status === 'completed') {
    $sql .= " AND ta.completedAt IS NOT NULL AND DATE(ta.completedAt) = ?";
    $params[] = $currentDate;
    $types .= "s";
} else if ($status === 'pending') {
    $sql .= " AND (ta.completedAt IS NULL OR DATE(ta.completedAt) != ?)";
    $params[] = $currentDate;
    $types .= "s";
}

$sql .= " ORDER BY t.studentId, t.startTime";

$stmt = $conn->prepare($sql);
if (!$stmt) {
    ApiResponse::error('Database error: ' . $conn->error, 500);
}

$stmt->bind_param($types, ...$params);
$stmt->execute();
$result = $stmt->get_result();

$activities = [];
$stats = [
    'total' => 0,
    'completed' => 0,
    'pending' => 0,
    'overdue' => 0,
    'completionPercentage' => 0
];

$currentTime = date('H:i:s');
// Build student email map for grouping
$studentEmailMap = [];
if (!empty($studentIds)) {
    $placeholders = implode(',', array_fill(0, count($studentIds), '?'));
    $emailStmt = $conn->prepare("SELECT studentId, email FROM students WHERE studentId IN ($placeholders)");
    $emailTypes = str_repeat('i', count($studentIds));
    $emailStmt->bind_param($emailTypes, ...$studentIds);
    $emailStmt->execute();
    $emailResult = $emailStmt->get_result();
    while ($emailRow = $emailResult->fetch_assoc()) {
        $studentEmailMap[$emailRow['studentId']] = $emailRow['email'];
    }
    $emailStmt->close();
}

while ($row = $result->fetch_assoc()) {
    $row['activityName'] = $row['subject'];
    $row['activityType'] = 'study';
    $row['isCompleted'] = (bool)$row['isCompletedToday'];
    $row['isCompletedToday'] = (bool)$row['isCompletedToday'];
    
    // Replace studentId with studentEmail (email is already in row from SQL query)
    $studentEmail = $row['studentEmail'] ?? $studentEmailMap[$row['studentId']] ?? null;
    unset($row['studentId']);
    $row['studentEmail'] = $studentEmail;
    
    // Add alarm audio URL if present
    $alarmAudio = isset($row['alarmAudio']) && !empty($row['alarmAudio']) ? $row['alarmAudio'] : null;
    $row['alarmAudioUrl'] = $alarmAudio ? BASE_URL . 'uploads/alarms/' . $alarmAudio : null;
    $row['alarmAudio'] = $alarmAudio;
    
    // Check if overdue
    $isOverdue = false;
    if (!$row['isCompletedToday'] && $currentTime > $row['endTime']) {
        $isOverdue = true;
    }
    $row['isOverdue'] = $isOverdue;
    
    $activities[] = $row;
    
    // Update statistics
    $stats['total']++;
    if ($row['isCompletedToday']) {
        $stats['completed']++;
    } else {
        $stats['pending']++;
        if ($isOverdue) {
            $stats['overdue']++;
        }
    }
}

$stats['completionPercentage'] = $stats['total'] > 0 ? round(($stats['completed'] / $stats['total']) * 100, 2) : 0;

// Group by student if parent authentication with multiple students
$responseData = [
    'activities' => $activities,
    'statistics' => $stats,
    'currentTime' => date('Y-m-d H:i:s'),
    'currentDay' => $day,
    'date' => date('Y-m-d')
];

if ((ApiAuth::getParentId() || ApiAuth::isParentKey()) && count($studentIds) > 1) {
    // Group activities by student
    $groupedActivities = [];
    foreach ($studentIds as $sid) {
        $studentEmail = $studentEmailMap[$sid] ?? null;
        $studentActivities = array_filter($activities, function($a) use ($studentEmail) {
            return $a['studentEmail'] == $studentEmail;
        });
        
        $studentStats = [
            'total' => count($studentActivities),
            'completed' => count(array_filter($studentActivities, fn($a) => $a['isCompletedToday'])),
            'pending' => count(array_filter($studentActivities, fn($a) => !$a['isCompletedToday'])),
            'overdue' => count(array_filter($studentActivities, fn($a) => $a['isOverdue']))
        ];
        $studentStats['completionPercentage'] = $studentStats['total'] > 0 ? 
            round(($studentStats['completed'] / $studentStats['total']) * 100, 2) : 0;
        
        $groupedActivities[] = [
            'studentEmail' => $studentEmail,
            'studentName' => !empty($studentActivities) ? reset($studentActivities)['studentName'] : '',
            'activities' => array_values($studentActivities),
            'statistics' => $studentStats
        ];
    }
    
    $responseData = [
        'students' => $groupedActivities,
        'overallStatistics' => $stats,
        'currentTime' => date('Y-m-d H:i:s'),
        'currentDay' => $day
    ];
}

ApiResponse::success($responseData);


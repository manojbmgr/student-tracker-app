<?php
/**
 * API Endpoint: Dashboard
 * POST /api/v1/dashboard
 * 
 * Returns comprehensive dashboard data including progress, activities, and statistics
 * For students: Returns own dashboard data (no body required)
 * For parents: Returns all children's dashboard data, or filter by studentEmail in body
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

// Check if authenticated as student
$studentId = ApiAuth::getStudentId();
$parentId = ApiAuth::getParentId();
$isParentKey = ApiAuth::isParentKey();

// Handle student dashboard
if ($studentId) {
    // Get student progress
    $progress = getStudentProgressWithPercentages($studentId);
    
    // Get request body
    $input = json_decode(file_get_contents('php://input'), true);
    if ($input === null) {
        $input = [];
    }
    
    // Get today's activities
    $day = isset($input['day']) ? $input['day'] : date('l'); // Default to current day
    $currentDate = date('Y-m-d');
    
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
            INDEX idx_student_completed (studentId, isCompleted)
        )";
        $conn->query($createTable);
    } catch (Exception $e) {
        // Table might already exist
    }
    
    // Get activities for today
    $sql = "SELECT t.*, 
            ta.activityId, ta.isCompleted, ta.completedAt,
            CASE 
                WHEN ta.completedAt IS NOT NULL AND DATE(ta.completedAt) = ? THEN 1 
                ELSE 0 
            END AS isCompletedToday
            FROM timetable t
            LEFT JOIN timetable_activities ta ON t.studentId = ta.studentId 
                AND t.dayOfWeek = ta.dayOfWeek 
                AND t.startTime = ta.startTime 
                AND t.endTime = ta.endTime
                AND t.subject = ta.activityName
                AND DATE(ta.createdAt) = ?
            WHERE t.studentId = ? AND t.dayOfWeek = ?
            ORDER BY t.startTime";
    
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ssis", $currentDate, $currentDate, $studentId, $day);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $activities = [];
    $totalActivities = 0;
    $completedActivities = 0;
    $pendingActivities = 0;
    $overdueActivities = 0;
    
    while ($row = $result->fetch_assoc()) {
        $isCompleted = isset($row['isCompletedToday']) ? (bool)$row['isCompletedToday'] : false;
        $isOverdue = false;
        
        // Check if overdue (past end time and not completed)
        if (!$isCompleted) {
            $currentTime = date('H:i:s');
            if ($day === date('l') && $row['endTime'] < $currentTime) {
                $isOverdue = true;
                $overdueActivities++;
            }
        }
        
        if ($isCompleted) {
            $completedActivities++;
        } else {
            $pendingActivities++;
        }
        
        $activity = [
            'timetableId' => $row['timetableId'],
            'studentEmail' => $studentInfo['email'] ?? null,
            'dayOfWeek' => $row['dayOfWeek'],
            'startTime' => $row['startTime'],
            'endTime' => $row['endTime'],
            'subject' => $row['subject'],
            'location' => $row['location'],
            'teacher' => $row['teacher'],
            'notes' => $row['notes'],
            'isCompleted' => $isCompleted,
            'isCompletedToday' => $isCompleted,
            'isOverdue' => $isOverdue,
            'completedAt' => $row['completedAt']
        ];
        
        if (!empty($row['alarmAudio'])) {
            $activity['alarmAudio'] = $row['alarmAudio'];
            $activity['alarmAudioUrl'] = BASE_URL . 'uploads/alarms/' . $row['alarmAudio'];
        }
        
        $activities[] = $activity;
        $totalActivities++;
    }
    $stmt->close();
    
    // Calculate completion percentage
    $completionPercentage = $totalActivities > 0 ? round(($completedActivities / $totalActivities) * 100, 2) : 0;
    
    // Get student info
    $studentStmt = $conn->prepare("SELECT studentId, studentName, email, phoneNo, class, profileImg, status FROM students WHERE studentId = ?");
    $studentStmt->bind_param("i", $studentId);
    $studentStmt->execute();
    $studentResult = $studentStmt->get_result();
    $studentInfo = $studentResult->fetch_assoc();
    $studentStmt->close();
    
    if ($studentInfo) {
        // Remove studentId from response, keep only email
        unset($studentInfo['studentId']);
        
        if (!empty($studentInfo['profileImg'])) {
            $studentInfo['profileImgUrl'] = BASE_URL . 'uploads/profiles/' . $studentInfo['profileImg'];
        }
    }
    
    // Build dashboard response
    $dashboard = [
        'student' => $studentInfo,
        'progress' => $progress,
        'activities' => [
            'list' => $activities,
            'statistics' => [
                'total' => $totalActivities,
                'completed' => $completedActivities,
                'pending' => $pendingActivities,
                'overdue' => $overdueActivities,
                'completionPercentage' => $completionPercentage
            ],
            'currentDay' => $day,
            'currentDate' => $currentDate,
            'currentTime' => date('Y-m-d H:i:s')
        ],
        'summary' => [
            'totalSubjects' => $progress['overall']['totalSubjects'] ?? 0,
            'totalChapters' => $progress['overall']['totalChapters'] ?? 0,
            'totalExercises' => $progress['overall']['totalExercises'] ?? 0,
            'completionPercentage' => $progress['overall']['completionPercentage'] ?? 0,
            'approvedPercentage' => $progress['overall']['approvedPercentage'] ?? 0,
            'todayActivitiesTotal' => $totalActivities,
            'todayActivitiesCompleted' => $completedActivities,
            'todayActivitiesPending' => $pendingActivities
        ]
    ];
    
    ApiResponse::success($dashboard);
    
} 
// Handle parent dashboard
elseif ($parentId || $isParentKey) {
    // Get request body
    $input = json_decode(file_get_contents('php://input'), true);
    if ($input === null) {
        $input = [];
    }
    
    // Check if filtering by specific student email
    $requestedStudentEmail = isset($input['studentEmail']) ? trim($input['studentEmail']) : '';
    $parentStudentIds = ApiAuth::getParentStudentIds();
    
    if (empty($parentStudentIds)) {
        ApiResponse::error('No students found for this parent account', 404);
    }
    
    // If specific student email requested, validate and use it
    if (!empty($requestedStudentEmail)) {
        // Get student ID from email and verify it belongs to this parent
        $stmt = $conn->prepare("SELECT studentId FROM students WHERE email = ? AND parentId = ?");
        if ($parentId) {
            $stmt->bind_param("si", $requestedStudentEmail, $parentId);
        } else {
            // For API key, get parent ID from first student
            $tempStmt = $conn->prepare("SELECT parentId FROM students WHERE studentId = ?");
            $tempStmt->bind_param("i", $parentStudentIds[0]);
            $tempStmt->execute();
            $tempResult = $tempStmt->get_result();
            if ($tempResult->num_rows > 0) {
                $tempRow = $tempResult->fetch_assoc();
                $parentId = $tempRow['parentId'];
            }
            $tempStmt->close();
            $stmt->bind_param("si", $requestedStudentEmail, $parentId);
        }
        $stmt->execute();
        $result = $stmt->get_result();
        
        if ($result->num_rows === 0) {
            ApiResponse::forbidden('The requested student email does not belong to this parent account or does not exist');
        }
        
        $row = $result->fetch_assoc();
        $requestedStudentId = $row['studentId'];
        
        if (!in_array($requestedStudentId, $parentStudentIds)) {
            ApiResponse::forbidden('The requested student does not belong to this parent account');
        }
        $studentIds = [$requestedStudentId];
        $stmt->close();
    } else {
        $studentIds = $parentStudentIds;
    }
    
    // Get parent dashboard stats
    if (!$parentId) {
        // For API key, get parent ID from first student
        $stmt = $conn->prepare("SELECT parentId FROM students WHERE studentId = ?");
        $stmt->bind_param("i", $studentIds[0]);
        $stmt->execute();
        $result = $stmt->get_result();
        if ($result->num_rows > 0) {
            $row = $result->fetch_assoc();
            $parentId = $row['parentId'];
        } else {
            ApiResponse::error('Unable to determine parent account', 500);
        }
        $stmt->close();
    }
    
    $parentStats = getParentDashboardStats($parentId);
    
    // Get activities for all students
    $day = isset($input['day']) ? $input['day'] : date('l');
    $currentDate = date('Y-m-d');
    
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
            INDEX idx_student_completed (studentId, isCompleted)
        )";
        $conn->query($createTable);
    } catch (Exception $e) {
        // Table might already exist
    }
    
    // Build dashboard data for each student
    $studentsDashboard = [];
    $overallStats = [
        'totalStudents' => count($studentIds),
        'totalActivities' => 0,
        'completedActivities' => 0,
        'pendingActivities' => 0,
        'overdueActivities' => 0
    ];
    
    foreach ($studentIds as $sid) {
        // Get student info
        $studentStmt = $conn->prepare("SELECT studentId, studentName, email, phoneNo, class, profileImg, status FROM students WHERE studentId = ?");
        $studentStmt->bind_param("i", $sid);
        $studentStmt->execute();
        $studentResult = $studentStmt->get_result();
        $studentInfo = $studentResult->fetch_assoc();
        $studentStmt->close();
        
        if (!$studentInfo) {
            continue;
        }
        
        // Remove studentId from response, keep only email
        unset($studentInfo['studentId']);
        
        if (!empty($studentInfo['profileImg'])) {
            $studentInfo['profileImgUrl'] = BASE_URL . 'uploads/profiles/' . $studentInfo['profileImg'];
        }
        
        // Get progress
        $progress = getStudentProgressWithPercentages($sid);
        
        // Get activities
        $sql = "SELECT t.*, 
                ta.activityId, ta.isCompleted, ta.completedAt,
                CASE 
                    WHEN ta.completedAt IS NOT NULL AND DATE(ta.completedAt) = ? THEN 1 
                    ELSE 0 
                END AS isCompletedToday
                FROM timetable t
                LEFT JOIN timetable_activities ta ON t.studentId = ta.studentId 
                    AND t.dayOfWeek = ta.dayOfWeek 
                    AND t.startTime = ta.startTime 
                    AND t.endTime = ta.endTime
                    AND t.subject = ta.activityName
                    AND DATE(ta.createdAt) = ?
                WHERE t.studentId = ? AND t.dayOfWeek = ?
                ORDER BY t.startTime";
        
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("ssis", $currentDate, $currentDate, $sid, $day);
        $stmt->execute();
        $result = $stmt->get_result();
        
        $activities = [];
        $totalActivities = 0;
        $completedActivities = 0;
        $pendingActivities = 0;
        $overdueActivities = 0;
        
        while ($row = $result->fetch_assoc()) {
            $isCompleted = isset($row['isCompletedToday']) ? (bool)$row['isCompletedToday'] : false;
            $isOverdue = false;
            
            if (!$isCompleted) {
                $currentTime = date('H:i:s');
                if ($day === date('l') && $row['endTime'] < $currentTime) {
                    $isOverdue = true;
                    $overdueActivities++;
                }
            }
            
            if ($isCompleted) {
                $completedActivities++;
            } else {
                $pendingActivities++;
            }
            
            $activity = [
                'timetableId' => $row['timetableId'],
                'studentEmail' => $studentInfo['email'] ?? null,
                'dayOfWeek' => $row['dayOfWeek'],
                'startTime' => $row['startTime'],
                'endTime' => $row['endTime'],
                'subject' => $row['subject'],
                'location' => $row['location'],
                'teacher' => $row['teacher'],
                'notes' => $row['notes'],
                'isCompleted' => $isCompleted,
                'isCompletedToday' => $isCompleted,
                'isOverdue' => $isOverdue,
                'completedAt' => $row['completedAt']
            ];
            
            if (!empty($row['alarmAudio'])) {
                $activity['alarmAudio'] = $row['alarmAudio'];
                $activity['alarmAudioUrl'] = BASE_URL . 'uploads/alarms/' . $row['alarmAudio'];
            }
            
            $activities[] = $activity;
            $totalActivities++;
        }
        $stmt->close();
        
        $completionPercentage = $totalActivities > 0 ? round(($completedActivities / $totalActivities) * 100, 2) : 0;
        
        // Update overall stats
        $overallStats['totalActivities'] += $totalActivities;
        $overallStats['completedActivities'] += $completedActivities;
        $overallStats['pendingActivities'] += $pendingActivities;
        $overallStats['overdueActivities'] += $overdueActivities;
        
        // Find matching student in parentStats
        $studentProgressData = null;
        foreach ($parentStats['students'] as $statStudent) {
            if ($statStudent['studentId'] == $sid) {
                $studentProgressData = $statStudent;
                break;
            }
        }
        
        $studentsDashboard[] = [
            'student' => $studentInfo,
            'progress' => $progress,
            'activities' => [
                'list' => $activities,
                'statistics' => [
                    'total' => $totalActivities,
                    'completed' => $completedActivities,
                    'pending' => $pendingActivities,
                    'overdue' => $overdueActivities,
                    'completionPercentage' => $completionPercentage
                ]
            ],
            'summary' => [
                'totalSubjects' => $progress['overall']['totalSubjects'] ?? 0,
                'totalChapters' => $progress['overall']['totalChapters'] ?? 0,
                'totalExercises' => $progress['overall']['totalExercises'] ?? 0,
                'completionPercentage' => $progress['overall']['completionPercentage'] ?? 0,
                'approvedPercentage' => $progress['overall']['approvedPercentage'] ?? 0,
                'todayActivitiesTotal' => $totalActivities,
                'todayActivitiesCompleted' => $completedActivities,
                'todayActivitiesPending' => $pendingActivities
            ]
        ];
    }
    
    // Calculate overall completion percentage
    $overallCompletionPercentage = $overallStats['totalActivities'] > 0 ? 
        round(($overallStats['completedActivities'] / $overallStats['totalActivities']) * 100, 2) : 0;
    
    $dashboard = [
        'students' => $studentsDashboard,
        'overallStatistics' => [
            'totalStudents' => $overallStats['totalStudents'],
            'totalActivities' => $overallStats['totalActivities'],
            'completedActivities' => $overallStats['completedActivities'],
            'pendingActivities' => $overallStats['pendingActivities'],
            'overdueActivities' => $overallStats['overdueActivities'],
            'completionPercentage' => $overallCompletionPercentage
        ],
        'currentDay' => $day,
        'currentDate' => $currentDate,
        'currentTime' => date('Y-m-d H:i:s')
    ];
    
    ApiResponse::success($dashboard);
    
} else {
    ApiResponse::forbidden('Authentication required');
}


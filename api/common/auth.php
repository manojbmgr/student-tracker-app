<?php
/**
 * API Authentication Middleware
 * Handles JWT token and API key authentication
 */

require_once $_SERVER['DOCUMENT_ROOT'] . '/api/common/config.php';

use Firebase\JWT\JWT;
use Firebase\JWT\Key;

class ApiAuth {
    private static $authenticatedStudentId = null;
    private static $authenticatedParentId = null;
    private static $authMethod = null;
    private static $isParentKey = false;
    
    /**
     * Get API key from request (header, GET, or POST)
     * 
     * @return string|null API key or null if not found
     */
    private static function getApiKey() {
        // Check X-API-Key header first (most common for API calls)
        if (isset($_SERVER['HTTP_X_API_KEY']) && !empty($_SERVER['HTTP_X_API_KEY'])) {
            return trim($_SERVER['HTTP_X_API_KEY']);
        }
        
        // Try getallheaders() for some server configurations
        if (function_exists('getallheaders')) {
            $headers = getallheaders();
            if ($headers) {
                foreach ($headers as $name => $value) {
                    if (strtolower($name) === 'x-api-key') {
                        return trim($value);
                    }
                }
            }
        }
        
        // Check GET parameter
        if (isset($_GET['api_key']) && !empty($_GET['api_key'])) {
            return trim($_GET['api_key']);
        }
        
        // Check POST parameter
        if (isset($_POST['api_key']) && !empty($_POST['api_key'])) {
            return trim($_POST['api_key']);
        }
        
        return null;
    }
    
    /**
     * Get JWT token from request (header or GET)
     * 
     * @return string|null Token or null if not found
     */
    private static function getToken() {
        // Check Authorization header
        if (isset($_SERVER['HTTP_AUTHORIZATION'])) {
            $authHeader = $_SERVER['HTTP_AUTHORIZATION'];
            if (preg_match('/Bearer\s+(.*)$/i', $authHeader, $matches)) {
                return trim($matches[1]);
            }
        }
        
        // Check GET parameter
        if (isset($_GET['token']) && !empty($_GET['token'])) {
            return trim($_GET['token']);
        }
        
        return null;
    }
    
    /**
     * Decode JWT token
     * 
     * @param string $token JWT token
     * @return array Decoded token data
     * @throws Exception If token is invalid
     */
    private static function decodeToken($token) {
        global $encryptKey;
        
        if (empty($token)) {
            throw new Exception("No token provided");
        }
        
        try {
            $decoded = JWT::decode($token, new Key($encryptKey, 'HS256'));
            return (array) $decoded->data;
        } catch (Exception $e) {
            throw new Exception("Token error: " . $e->getMessage());
        }
    }
    
    /**
     * Verify API key and get student/parent ID
     * 
     * @param string $apiKey API key
     * @return array ['studentId' => int|null, 'parentId' => int|null, 'isParentKey' => bool]
     */
    private static function verifyApiKey($apiKey) {
        global $conn;
        
        $result = [
            'studentId' => null,
            'parentId' => null,
            'isParentKey' => false
        ];
        
        // Check if api_key column exists in students table
        $checkColumn = $conn->query("SHOW COLUMNS FROM students LIKE 'api_key'");
        $columnExists = $checkColumn && $checkColumn->num_rows > 0;
        
        if (!$columnExists) {
            // Try to add the column
            $alterSql = "ALTER TABLE students ADD COLUMN api_key VARCHAR(64) NULL";
            @$conn->query($alterSql);
        }
        
        $apiKey = trim($apiKey);
        
        // First, try to find in students table (student API key)
        $stmt = $conn->prepare("SELECT studentId FROM students WHERE api_key = ?");
        if ($stmt) {
            $stmt->bind_param("s", $apiKey);
            $stmt->execute();
            $resultSet = $stmt->get_result();
            
            if ($resultSet->num_rows > 0) {
                $row = $resultSet->fetch_assoc();
                $result['studentId'] = $row['studentId'];
                $stmt->close();
                return $result;
            }
            $stmt->close();
        }
        
        // Try with trimmed database values
        $trimStmt = $conn->prepare("SELECT studentId FROM students WHERE TRIM(api_key) = ?");
        if ($trimStmt) {
            $trimStmt->bind_param("s", $apiKey);
            $trimStmt->execute();
            $trimResult = $trimStmt->get_result();
            if ($trimResult->num_rows > 0) {
                $row = $trimResult->fetch_assoc();
                $result['studentId'] = $row['studentId'];
                $trimStmt->close();
                return $result;
            }
            $trimStmt->close();
        }
        
        // Check if it's a parent API key
        $parentStmt = $conn->prepare("SELECT userId FROM users WHERE api_key = ? OR TRIM(api_key) = ?");
        if ($parentStmt) {
            $parentStmt->bind_param("ss", $apiKey, $apiKey);
            $parentStmt->execute();
            $parentResult = $parentStmt->get_result();
            if ($parentResult->num_rows > 0) {
                $parentRow = $parentResult->fetch_assoc();
                $result['parentId'] = $parentRow['userId'];
                $result['isParentKey'] = true;
                $parentStmt->close();
                return $result;
            }
            $parentStmt->close();
        }
        
        return null;
    }
    
    /**
     * Authenticate request
     * Checks for JWT token first, then API key
     * 
     * @param bool $requireAuth Whether authentication is required (default: true)
     * @return bool True if authenticated, false otherwise
     */
    public static function authenticate($requireAuth = true) {
        // JWT token authentication is stateless - no session needed  
        // 1. Check for JWT token first
        $token = self::getToken();
        if ($token) {
            try {
                $data = self::decodeToken($token);
                // Check for student token
                $studentId = isset($data['studentId']) ? intval($data['studentId']) : 0;
                if ($studentId > 0) {
                    self::$authenticatedStudentId = $studentId;
                    self::$authMethod = 'jwt';
                    return true;
                }
                // Check for parent token
                $parentId = isset($data['parentId']) ? intval($data['parentId']) : 0;
                if ($parentId > 0) {
                    self::$authenticatedParentId = $parentId;
                    self::$isParentKey = true; // Mark as parent for consistency
                    self::$authMethod = 'jwt';
                    return true;
                }
            } catch (Exception $e) {
                // Token invalid, continue to check API key
            }
        }
        
        // 2. Check for API key
        $apiKey = self::getApiKey();
        if ($apiKey) {
            $apiResult = self::verifyApiKey($apiKey);
            if ($apiResult) {
                if ($apiResult['studentId']) {
                    self::$authenticatedStudentId = $apiResult['studentId'];
                    self::$authMethod = 'api_key';
                    return true;
                } elseif ($apiResult['parentId']) {
                    self::$authenticatedParentId = $apiResult['parentId'];
                    self::$isParentKey = $apiResult['isParentKey'];
                    self::$authMethod = 'api_key';
                    return true;
                }
            }
        }
        
        // If authentication is required and we haven't authenticated, return false
        if ($requireAuth) {
            return false;
        }
        
        return false;
    }
    
    /**
     * Get authenticated student ID
     * 
     * @return int|null Student ID or null if not authenticated as student
     */
    public static function getStudentId() {
        return self::$authenticatedStudentId;
    }
    
    /**
     * Get authenticated parent ID
     * 
     * @return int|null Parent ID or null if not authenticated as parent
     */
    public static function getParentId() {
        return self::$authenticatedParentId;
    }
    
    /**
     * Get authentication method used
     * 
     * @return string|null 'jwt', 'api_key', or null
     */
    public static function getAuthMethod() {
        return self::$authMethod;
    }
    
    /**
     * Check if using parent API key
     * 
     * @return bool True if parent API key is being used
     */
    public static function isParentKey() {
        return self::$isParentKey;
    }
    
    /**
     * Get all student IDs for parent (if authenticated as parent)
     * 
     * @return array Array of student IDs
     */
    public static function getParentStudentIds() {
        global $conn;
        
        if (!self::$authenticatedParentId) {
            return [];
        }
        
        $stmt = $conn->prepare("SELECT studentId FROM students WHERE parentId = ?");
        if ($stmt) {
            $stmt->bind_param("i", self::$authenticatedParentId);
            $stmt->execute();
            $result = $stmt->get_result();
            $studentIds = [];
            while ($row = $result->fetch_assoc()) {
                $studentIds[] = $row['studentId'];
            }
            $stmt->close();
            return $studentIds;
        }
        
        return [];
    }
}


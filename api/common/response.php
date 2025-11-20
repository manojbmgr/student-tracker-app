<?php
/**
 * Standardized API Response Helper
 * Provides consistent JSON response format across all API endpoints
 */

class ApiResponse {
    /**
     * Send success response
     * 
     * @param mixed $data Response data
     * @param string $message Optional message
     * @param int $httpCode HTTP status code
     */
    public static function success($data = null, $message = null, $httpCode = 200) {
        http_response_code($httpCode);
        $response = [
            'status' => 'success',
            'timestamp' => date('Y-m-d H:i:s'),
            'data' => $data
        ];
        
        if ($message !== null) {
            $response['message'] = $message;
        }
        
        echo json_encode($response, JSON_PRETTY_PRINT);
        exit;
    }
    
    /**
     * Send error response
     * 
     * @param string $message Error message
     * @param int $httpCode HTTP status code
     * @param mixed $errors Optional error details
     */
    public static function error($message = 'An error occurred', $httpCode = 400, $errors = null) {
        http_response_code($httpCode);
        $response = [
            'status' => 'error',
            'timestamp' => date('Y-m-d H:i:s'),
            'message' => $message
        ];
        
        if ($errors !== null) {
            $response['errors'] = $errors;
        }
        
        echo json_encode($response, JSON_PRETTY_PRINT);
        exit;
    }
    
    /**
     * Send unauthorized response
     * 
     * @param string $message Optional message
     */
    public static function unauthorized($message = 'Authentication required') {
        self::error($message, 401);
    }
    
    /**
     * Send forbidden response
     * 
     * @param string $message Optional message
     */
    public static function forbidden($message = 'Access denied') {
        self::error($message, 403);
    }
    
    /**
     * Send not found response
     * 
     * @param string $message Optional message
     */
    public static function notFound($message = 'Resource not found') {
        self::error($message, 404);
    }
    
    /**
     * Send validation error response
     * 
     * @param array $errors Validation errors
     * @param string $message Optional message
     */
    public static function validationError($errors, $message = 'Validation failed') {
        self::error($message, 422, $errors);
    }
}


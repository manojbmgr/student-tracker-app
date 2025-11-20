<?php
/**
 * API Configuration
 * Central configuration for API endpoints
 */

// Define API context to prevent session redirects
define('API_CONTEXT', true);

// Set JSON header
header('Content-Type: application/json');

// CORS headers (if needed for web apps)
if (isset($_SERVER['HTTP_ORIGIN'])) {
    header("Access-Control-Allow-Origin: {$_SERVER['HTTP_ORIGIN']}");
    header('Access-Control-Allow-Credentials: true');
    header('Access-Control-Max-Age: 86400');
}

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    if (isset($_SERVER['HTTP_ACCESS_CONTROL_REQUEST_METHOD'])) {
        header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS");
    }
    if (isset($_SERVER['HTTP_ACCESS_CONTROL_REQUEST_HEADERS'])) {
        header("Access-Control-Allow-Headers: {$_SERVER['HTTP_ACCESS_CONTROL_REQUEST_HEADERS']}");
    }
    exit(0);
}

// API Version
define('API_VERSION', 'v1');

// Base path
define('API_BASE_PATH', $_SERVER['DOCUMENT_ROOT']);

// Include required files
require_once API_BASE_PATH . '/lib/conn.php';
require_once API_BASE_PATH . '/vendor/autoload.php';

use Firebase\JWT\JWT;
use Firebase\JWT\Key;


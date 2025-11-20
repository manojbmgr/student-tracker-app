<?php
/**
 * API Index
 * Provides API information and version details
 */

header('Content-Type: application/json');

$response = [
    'name' => 'Student Progress Tracker API',
    'version' => '1.0.0',
    'description' => 'RESTful API for Student Progress Tracker',
    'endpoints' => [
        'v1' => [
            'base_url' => '/api/v1',
            'authentication' => [
                'student_login' => 'POST /api/v1/auth/login',
                'parent_login' => 'POST /api/v1/auth/parent-login'
            ],
            'dashboard' => [
                'get' => 'GET /api/v1/dashboard'
            ],
            'activities' => [
                'list' => 'GET /api/v1/activities',
                'complete' => 'POST /api/v1/activities/complete'
            ],
            'timetable' => [
                'list' => 'GET /api/v1/timetable',
                'create' => 'POST /api/v1/timetable',
                'update' => 'PUT /api/v1/timetable',
                'delete' => 'DELETE /api/v1/timetable'
            ],
            'progress' => [
                'get' => 'GET /api/v1/progress'
            ]
        ]
    ],
    'authentication' => [
        'methods' => ['JWT Token', 'API Key'],
        'jwt_header' => 'Authorization: Bearer <token>',
        'api_key_header' => 'X-API-Key: <key>'
    ],
    'documentation' => '/api/README.md'
];

echo json_encode($response, JSON_PRETTY_PRINT);


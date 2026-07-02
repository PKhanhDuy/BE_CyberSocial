package com.cybersocial.demo.dto;

public record DemoUserGenerationResponse(
        int requestedCount,
        int totalDemoUsers,
        int usersCreated
) {
}

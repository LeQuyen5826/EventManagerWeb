package com.cmc.eventmanagement.dto.response;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}

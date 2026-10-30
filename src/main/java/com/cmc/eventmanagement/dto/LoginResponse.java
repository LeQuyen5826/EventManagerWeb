package com.cmc.eventmanagement.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}

package com.cmc.eventmanagement.security;

import com.cmc.eventmanagement.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Lỗi phát sinh trong filter không đi qua @RestControllerAdvice nên phải tự ghi JSON ApiError. */
@Component
@RequiredArgsConstructor
class SecurityErrorWriter {
    private final ObjectMapper objectMapper;

    void write(HttpServletRequest req, HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), ApiError.of(status, message, req.getRequestURI()));
    }
}

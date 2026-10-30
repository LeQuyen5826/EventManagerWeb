package com.cmc.eventmanagement.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Xử lý lỗi tập trung (mục 3.4b). Mọi lỗi trả về cùng cấu trúc ApiError. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Dữ liệu không hợp lệ", req.getRequestURI(), errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Nội dung request không đọc được (sai định dạng JSON hoặc kiểu dữ liệu)", req.getRequestURI()));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> business(BusinessException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ApiError.of(400, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(404, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(409, ex.getMessage(), req.getRequestURI()));
    }

    /** Vi phạm ràng buộc UNIQUE do hai request đồng thời: coi như xung đột. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Vi phạm ràng buộc dữ liệu tại {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Dữ liệu bị trùng hoặc xung đột, vui lòng thử lại", req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)   // phải khai báo riêng, nếu không sẽ rơi vào handler 500
    ResponseEntity<ApiError> denied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "Bạn không có quyền thực hiện thao tác này", req.getRequestURI()));
    }

    /** Đăng nhập sai (AuthenticationManager ném ra). Thông báo chung để không lộ tài khoản có tồn tại hay không. */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(AuthenticationException ex, HttpServletRequest req) {
        String msg = ex instanceof BadCredentialsException
                ? "Email hoặc mật khẩu không đúng"
                : "Không đăng nhập được hoặc tài khoản đã bị khóa";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(401, msg, req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Lỗi không lường trước tại {}", req.getRequestURI(), ex);   // chi tiết chỉ ghi log
        return ResponseEntity.internalServerError()
                .body(ApiError.of(500, "Đã xảy ra lỗi, vui lòng thử lại sau", req.getRequestURI()));
    }
}

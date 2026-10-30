package com.cmc.eventmanagement.exception;

/** 400: vi phạm quy tắc nghiệp vụ (BR01, BR02, ngoài thời gian đăng ký/check-in...). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}

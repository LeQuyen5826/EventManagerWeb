package com.cmc.eventmanagement.exception;

/** Vi phạm quy tắc nghiệp vụ (BR01, BR02, BR05, BR06...) -> 400 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}

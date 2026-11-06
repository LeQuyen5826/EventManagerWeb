package com.cmc.eventmanagement.exception;

/** Đăng ký trùng, hết chỗ, QR đã check-in -> 409 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}

package com.cmc.eventmanagement.exception;

/** 409: đăng ký trùng, hết chỗ, mã QR đã check-in. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}

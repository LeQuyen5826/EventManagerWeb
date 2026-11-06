package com.cmc.eventmanagement.util;

import java.security.SecureRandom;
import java.util.HexFormat;

/** Token QR: 32 byte ngẫu nhiên an toàn -> 64 ký tự hex, không chứa thông tin cá nhân (FR37). */
public final class QrTokenGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private QrTokenGenerator() {}

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}

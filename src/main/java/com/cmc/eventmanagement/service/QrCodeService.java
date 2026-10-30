package com.cmc.eventmanagement.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
public class QrCodeService {

    /** Token ngẫu nhiên, không đoán được; đây là nội dung được mã hóa vào QR. */
    public String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** Sinh ảnh PNG của mã QR chứa token. */
    public byte[] generatePng(String content, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được mã QR", e);
        }
    }
}

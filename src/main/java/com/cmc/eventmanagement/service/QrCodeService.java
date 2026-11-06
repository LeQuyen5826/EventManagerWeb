package com.cmc.eventmanagement.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.cmc.eventmanagement.util.QrTokenGenerator;
import com.cmc.eventmanagement.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
public class QrCodeService {

    public String newToken() { return QrTokenGenerator.newToken(); }

    /** FR37-FR38: sinh ảnh PNG từ token. */
    public byte[] generatePng(String token, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.MARGIN, 1, EncodeHintType.CHARACTER_SET, "UTF-8"));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Không thể tạo mã QR");
        }
    }
}

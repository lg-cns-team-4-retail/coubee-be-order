package com.coubee.coubeebeorder.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
public class QrCodeService {
    
    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;

    public byte[] generateQrCodeImage(String text) {
        try {
            log.info("Generating QR code for text: {}", text);
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            byte[] qrCodeBytes = outputStream.toByteArray();
            log.info("QR code generated successfully, size: {} bytes", qrCodeBytes.length);
            
            return qrCodeBytes;
            
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code for text: {}", text, e);
            throw new RuntimeException("QR code generation failed", e);
        }
    }

    public byte[] generateOrderQrCode(String orderId, String baseUrl) {
        String orderUrl = baseUrl + "/orders/" + orderId;
        return generateQrCodeImage(orderUrl);
    }

    public byte[] generateOrderIdQrCode(String orderId) {
        return generateQrCodeImage(orderId);
    }
}
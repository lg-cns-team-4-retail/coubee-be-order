package com.coubee.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * QR Code Generation Service
 */
@Slf4j
@Service
public class QrCodeService {
    
    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;

    /**
     * Generate QR code image as byte array
     * 
     * @param text Text to encode in QR code
     * @return QR code image bytes (PNG format)
     * @throws RuntimeException if QR code generation fails
     */
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

    /**
     * Generate QR code for order lookup URL
     * 
     * @param orderId Order ID
     * @param baseUrl Base URL of the service
     * @return QR code image bytes
     */
    public byte[] generateOrderQrCode(String orderId, String baseUrl) {
        String orderUrl = baseUrl + "/orders/" + orderId;
        return generateQrCodeImage(orderUrl);
    }

    /**
     * Generate simple QR code with just the order ID
     * 
     * @param orderId Order ID
     * @return QR code image bytes
     */
    public byte[] generateOrderIdQrCode(String orderId) {
        return generateQrCodeImage(orderId);
    }
}
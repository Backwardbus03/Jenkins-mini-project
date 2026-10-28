package com.rentreminder.app.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class QrCodeService {

    /**
     * Builds standard Indian UPI payment URI:
     * upi://pay?pa=<upiId>&pn=<name>&am=<amount>&cu=INR&tn=<note>
     */
    public String buildUpiUri(String upiId, String payeeName, Double amount, String note) {
        String formattedAmount = String.format("%.2f", amount != null ? amount : 0.0);
        String encName = URLEncoder.encode(payeeName != null ? payeeName : "Owner", StandardCharsets.UTF_8).replace("+",
                "%20");
        String encNote = URLEncoder.encode(note != null ? note : "Rent Bill", StandardCharsets.UTF_8).replace("+",
                "%20");

        return String.format("upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s",
                upiId != null ? upiId : "sumedhhadkar@oksbi",
                encName,
                formattedAmount,
                encNote);
    }

    /**
     * Generates a PNG byte array representing the QR code for given content.
     */
    public byte[] generateQrPng(String content, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
}

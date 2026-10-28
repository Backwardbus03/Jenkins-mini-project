package com.rentreminder.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeServiceTest {

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService();
    }

    @Test
    void testBuildUpiUri_FormatsCorrectly() {
        String uri = qrCodeService.buildUpiUri("landlord@upi", "Landlord Name", 1450.50, "Electricity-Oct-101");

        assertNotNull(uri);
        assertTrue(uri.startsWith("upi://pay?"));
        assertTrue(uri.contains("pa=landlord@upi"));
        assertTrue(uri.contains("pn=Landlord%20Name"));
        assertTrue(uri.contains("am=1450.50"));
        assertTrue(uri.contains("cu=INR"));
        assertTrue(uri.contains("tn=Electricity-Oct-101"));
    }

    @Test
    void testGenerateQrPng_GeneratesValidPngBytes() {
        String uri = qrCodeService.buildUpiUri("test@upi", "Tester", 500.0, "Rent");
        byte[] pngBytes = qrCodeService.generateQrPng(uri, 200, 200);

        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > 100);

        // Verify PNG magic numbers: 0x89, 'P', 'N', 'G' (0x89, 0x50, 0x4E, 0x47)
        assertEquals((byte) 0x89, pngBytes[0]);
        assertEquals((byte) 0x50, pngBytes[1]);
        assertEquals((byte) 0x4E, pngBytes[2]);
        assertEquals((byte) 0x47, pngBytes[3]);
    }
}

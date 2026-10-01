package com.rentreminder.app.selenium.journeys;

import com.rentreminder.app.selenium.base.BaseSeleniumTest;
import com.rentreminder.app.selenium.pages.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * User Journey 5: Rent Payment Recording (Paid & Overdue), Security Deposit Adjustment, and Official Receipt Verification.
 */
public class Journey5_PaymentReceiptFlowTest extends BaseSeleniumTest {

    @Test
    @DisplayName("Journey 5.1: Record a Paid rent payment and verify printable receipt with PAID IN FULL stamp")
    void testRecordPaidPaymentAndVerifyReceipt() {
        long ts = System.currentTimeMillis();
        String tenantName = "Rajesh Khanna " + (ts % 10000);
        String tenantEmail = "rajesh." + ts + "@example.com";
        String aptNumber = "104";

        // 1. Owner logs in and onboards tenant
        LoginPage loginPage = new LoginPage(driver).open(getBaseUrl());
        loginPage.loginAs("owner@rentportal.com", "owner123");

        OwnerTenantsPage tenantsPage = new OwnerTenantsPage(driver);
        tenantsPage.clickAddTenant();

        AddTenantPage addTenantPage = new AddTenantPage(driver);
        addTenantPage.saveTenant(tenantName, tenantEmail, "+91 99000 11223", aptNumber);
        assertTrue(waitForUrlContains("/tenants"));

        // 2. Navigate to Payment Form and Record Rent Payment
        PaymentFormPage paymentPage = new PaymentFormPage(driver).open(getBaseUrl());
        assertTrue(paymentPage.isDisplayed(), "Payment form should be visible");

        double rentAmount = 14500.00;
        // Use default auto-populated valid date by passing null, and choose Paid
        paymentPage.recordPayment(tenantName, rentAmount, null, "Paid", 10, 2026);

        // 3. Verify Payment is Recorded in Summary Table
        assertTrue(waitForUrlContains("/payments/summary"), "Should redirect to payment summary");
        PaymentSummaryPage summaryPage = new PaymentSummaryPage(driver);
        assertTrue(summaryPage.isDisplayed(), "Payment summary page should load");
        assertTrue(summaryPage.hasPaymentForTenant(tenantName, "Paid"),
                "Payment row for " + tenantName + " with status 'Paid' must exist");

        // 4. View and Verify Official Printable Receipt
        summaryPage.clickReceiptForTenant(tenantName);

        assertTrue(waitForUrlContains("/payments/receipt/"), "Should navigate to receipt page");
        ReceiptPage receiptPage = new ReceiptPage(driver);
        assertTrue(receiptPage.isDisplayed(), "Printable receipt view should be displayed");
        assertTrue(receiptPage.getMainTitleText().toUpperCase().contains("RENT RECEIPT"), "Receipt title must be 'RENT RECEIPT'");
        assertTrue(receiptPage.isPaidStampDisplayed(), "Stamp '✓ PAID IN FULL' must be displayed");
        assertTrue(receiptPage.getGrandTotalText().contains("14500") || receiptPage.getGrandTotalText().contains("14,500"),
                "Receipt grand total must show ₹14500");
        assertTrue(receiptPage.getSignatureBlocksCount() >= 2,
                "Both Landlord and Tenant signature blocks must be present");
    }

    @Test
    @DisplayName("Journey 5.2: Record an Overdue payment and verify OVERDUE DEPOSIT DEDUCTED stamp on receipt")
    void testRecordOverduePaymentAndVerifyDeductionReceipt() {
        long ts = System.currentTimeMillis();
        String tenantName = "Ananya Roy " + (ts % 10000);
        String tenantEmail = "ananya." + ts + "@example.com";
        String aptNumber = "208";

        // 1. Owner logs in and onboards tenant
        LoginPage loginPage = new LoginPage(driver).open(getBaseUrl());
        loginPage.loginAs("owner@rentportal.com", "owner123");

        OwnerTenantsPage tenantsPage = new OwnerTenantsPage(driver);
        tenantsPage.clickAddTenant();

        AddTenantPage addTenantPage = new AddTenantPage(driver);
        addTenantPage.saveTenant(tenantName, tenantEmail, "+91 99111 22334", aptNumber);
        assertTrue(waitForUrlContains("/tenants"));

        // 2. Open Payment Form and record Overdue payment
        PaymentFormPage paymentPage = new PaymentFormPage(driver).open(getBaseUrl());
        double overdueAmount = 7000.00;
        paymentPage.recordPayment(tenantName, overdueAmount, null, "Overdue", 11, 2026);

        // 3. Verify Summary Table shows Overdue
        assertTrue(waitForUrlContains("/payments/summary"), "Should redirect to payment summary");
        PaymentSummaryPage summaryPage = new PaymentSummaryPage(driver);
        assertTrue(summaryPage.hasPaymentForTenant(tenantName, "Overdue"),
                "Summary table must show payment with status 'Overdue'");

        // 4. View and Verify Overdue Receipt with Stamp and Notice
        summaryPage.clickReceiptForTenant(tenantName);

        assertTrue(waitForUrlContains("/payments/receipt/"), "Should navigate to receipt page");
        ReceiptPage receiptPage = new ReceiptPage(driver);
        assertTrue(receiptPage.isDisplayed(), "Receipt should be visible");
        assertTrue(receiptPage.isOverdueStampDisplayed(),
                "Overdue receipt must display '⚠ OVERDUE (DEPOSIT DEDUCTED)' stamp");
        assertTrue(receiptPage.getGrandTotalText().contains("7000") || receiptPage.getGrandTotalText().contains("7,000"),
                "Receipt must indicate the overdue deduction amount of ₹7000");
    }
}

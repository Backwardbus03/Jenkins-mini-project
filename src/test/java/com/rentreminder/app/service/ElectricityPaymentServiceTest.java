package com.rentreminder.app.service;

import com.rentreminder.app.model.Payment;
import com.rentreminder.app.model.Tenant;
import com.rentreminder.app.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElectricityPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TenantInvitationService tenantInvitationService;

    @InjectMocks
    private PaymentService paymentService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant("Jane Doe", "jane@example.com", "9876543210", "102");
        tenant.setId(10L);
    }

    @Test
    void testSavePayment_CalculatesElectricityAmountCorrectly() {
        Payment payment = new Payment();
        payment.setTenant(tenant);
        payment.setType("ELECTRICITY");
        payment.setPreviousReading(1200.0);
        payment.setCurrentReading(1350.0); // 150 units consumed
        payment.setRatePerUnit(10.0);
        payment.setMonth(5);
        payment.setYear(2024);
        payment.setDate(LocalDate.of(2024, 5, 1));
        payment.setStatus("Pending");

        when(tenantInvitationService.isTenantLeaseExpired(tenant)).thenReturn(false);
        when(paymentRepository.findByTenantAndMonthAndYearAndType(tenant, 5, 2024, "ELECTRICITY"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment saved = paymentService.savePayment(payment);

        assertNotNull(saved);
        assertEquals(1500.0, saved.getAmount(), 0.001);
        assertEquals(150.0, saved.getUnitsConsumed(), 0.001);
        assertEquals("ELECTRICITY", saved.getType());
        assertNotNull(saved.getDueDate());
    }

    @Test
    void testSavePayment_RejectsCurrentReadingLessThanPrevious() {
        Payment payment = new Payment();
        payment.setTenant(tenant);
        payment.setType("ELECTRICITY");
        payment.setPreviousReading(1500.0);
        payment.setCurrentReading(1200.0); // Invalid reading
        payment.setRatePerUnit(10.0);
        payment.setMonth(5);
        payment.setYear(2024);

        when(tenantInvitationService.isTenantLeaseExpired(tenant)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.savePayment(payment));
        assertTrue(ex.getMessage().contains("cannot be less than previous"));
    }

    @Test
    void testSavePayment_OverdueElectricityDoesNotDeductFromDeposit() {
        Payment payment = new Payment();
        payment.setTenant(tenant);
        payment.setType("ELECTRICITY");
        payment.setAmount(1200.0);
        payment.setMonth(6);
        payment.setYear(2024);
        payment.setDate(LocalDate.of(2024, 6, 1));
        payment.setStatus("Overdue");

        when(tenantInvitationService.isTenantLeaseExpired(tenant)).thenReturn(false);
        when(paymentRepository.findByTenantAndMonthAndYearAndType(tenant, 6, 2024, "ELECTRICITY"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.savePayment(payment);

        // Verify deposit deduction was NEVER called for electricity bill
        verify(tenantInvitationService, never()).deductFromDeposit(any(), any());
    }

    @Test
    void testSavePayment_OverdueRentDeductsFromDeposit() {
        Payment payment = new Payment();
        payment.setTenant(tenant);
        payment.setType("RENT");
        payment.setAmount(15000.0);
        payment.setMonth(6);
        payment.setYear(2024);
        payment.setDate(LocalDate.of(2024, 6, 1));
        payment.setStatus("Overdue");

        when(tenantInvitationService.isTenantLeaseExpired(tenant)).thenReturn(false);
        when(paymentRepository.findByTenantAndMonthAndYearAndType(tenant, 6, 2024, "RENT"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.savePayment(payment);

        // Verify deposit deduction was called for rent
        verify(tenantInvitationService, times(1)).deductFromDeposit(tenant, 15000.0);
    }

    @Test
    void testMarkPaidAndConfirmFlow() {
        Payment payment = new Payment();
        payment.setId(99L);
        payment.setStatus("Pending");
        payment.setAmount(800.0);

        when(paymentRepository.findById(99L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Tenant marks paid
        Payment awaiting = paymentService.markPaymentPaidByTenant(99L, "UPI-REF-12345");
        assertEquals("Awaiting Confirmation", awaiting.getStatus());
        assertEquals("UPI-REF-12345", awaiting.getUpiReference());

        // Landlord confirms
        Payment confirmed = paymentService.confirmPayment(99L);
        assertEquals("Paid", confirmed.getStatus());
    }
}

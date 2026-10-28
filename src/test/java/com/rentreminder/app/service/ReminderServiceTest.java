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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TenantInvitationService tenantInvitationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private ReminderService reminderService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant("Alice Bob", "alice@example.com", "9999999999", "303");
        ReflectionTestUtils.setField(reminderService, "remindersEnabled", true);
        ReflectionTestUtils.setField(reminderService, "portalBaseUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(reminderService, "fromEmail", "noreply@rentportal.com");
    }

    @Test
    void testProcessDueReminders_WhenDisabled_DoesNotProcess() {
        ReflectionTestUtils.setField(reminderService, "remindersEnabled", false);

        int sent = reminderService.processDueReminders();
        assertEquals(0, sent);
        verify(notificationService, never()).notifyTenant(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testProcessDueReminders_SendsForDueSoonBills() {
        LocalDate today = LocalDate.now();

        Payment bill = new Payment();
        bill.setId(1L);
        bill.setTenant(tenant);
        bill.setType("ELECTRICITY");
        bill.setAmount(1200.0);
        bill.setStatus("Pending");
        bill.setDueDate(today.plusDays(2)); // Due in 2 days
        bill.setMonth(today.getMonthValue());
        bill.setYear(today.getYear());

        when(paymentRepository.findAll()).thenReturn(List.of(bill));

        int sent = reminderService.processDueReminders();

        assertEquals(1, sent);
        verify(notificationService, times(1)).notifyTenant(
                eq(tenant),
                contains("Electricity Bill Reminder"),
                any(),
                eq("WARNING"),
                eq("/portal/dashboard#payments"),
                eq(bill)
        );
        assertEquals(today, bill.getLastReminderSentOn());
        verify(paymentRepository, times(1)).save(bill);
    }

    @Test
    void testProcessDueReminders_SkipsIfAlreadyRemindedToday() {
        LocalDate today = LocalDate.now();

        Payment bill = new Payment();
        bill.setId(2L);
        bill.setTenant(tenant);
        bill.setType("RENT");
        bill.setAmount(15000.0);
        bill.setStatus("Pending");
        bill.setDueDate(today.plusDays(1));
        bill.setLastReminderSentOn(today); // Already notified today

        when(paymentRepository.findAll()).thenReturn(List.of(bill));

        int sent = reminderService.processDueReminders();

        assertEquals(0, sent);
        verify(notificationService, never()).notifyTenant(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testSendManualReminder_DispatchesUiNotificationAndUpdatesDate() {
        LocalDate today = LocalDate.now();

        Payment bill = new Payment();
        bill.setId(5L);
        bill.setTenant(tenant);
        bill.setType("RENT");
        bill.setAmount(18000.0);
        bill.setStatus("Pending");
        bill.setDueDate(today.plusDays(4));
        bill.setMonth(5);
        bill.setYear(2024);

        when(paymentRepository.findById(5L)).thenReturn(Optional.of(bill));

        boolean success = reminderService.sendManualReminder(5L);

        assertTrue(success);
        verify(notificationService, times(1)).notifyTenant(
                eq(tenant),
                contains("Rent Reminder"),
                any(),
                eq("WARNING"),
                eq("/portal/dashboard#payments"),
                eq(bill)
        );
        assertEquals(today, bill.getLastReminderSentOn());
        verify(paymentRepository, times(1)).save(bill);
    }
}

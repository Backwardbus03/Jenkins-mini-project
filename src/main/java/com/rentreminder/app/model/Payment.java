package com.rentreminder.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @NotNull(message = "Tenant is required")
    private Tenant tenant;

    @Min(value = 1, message = "Amount must be greater than 0")
    private Double amount;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private Integer month;
    private Integer year;
    private String status;

    private String type = "RENT"; // "RENT" or "ELECTRICITY"
    private LocalDate dueDate;
    private Double previousReading;
    private Double currentReading;
    private Double ratePerUnit;
    private String upiReference;
    private LocalDate lastReminderSentOn;

    public Payment() {
    }

    public Payment(Tenant tenant, Double amount, LocalDate date, Integer month, Integer year, String status) {
        this(tenant, amount, date, month, year, status, "RENT", null);
    }

    public Payment(Tenant tenant, Double amount, LocalDate date, Integer month, Integer year, String status, String type, LocalDate dueDate) {
        this.tenant = tenant;
        this.amount = amount;
        this.date = date;
        this.month = month;
        this.year = year;
        this.status = status;
        this.type = (type != null && !type.isBlank()) ? type : "RENT";
        this.dueDate = dueDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type != null ? type : "RENT";
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Double getPreviousReading() {
        return previousReading;
    }

    public void setPreviousReading(Double previousReading) {
        this.previousReading = previousReading;
    }

    public Double getCurrentReading() {
        return currentReading;
    }

    public void setCurrentReading(Double currentReading) {
        this.currentReading = currentReading;
    }

    public Double getRatePerUnit() {
        return ratePerUnit;
    }

    public void setRatePerUnit(Double ratePerUnit) {
        this.ratePerUnit = ratePerUnit;
    }

    public String getUpiReference() {
        return upiReference;
    }

    public void setUpiReference(String upiReference) {
        this.upiReference = upiReference;
    }

    public LocalDate getLastReminderSentOn() {
        return lastReminderSentOn;
    }

    public void setLastReminderSentOn(LocalDate lastReminderSentOn) {
        this.lastReminderSentOn = lastReminderSentOn;
    }

    public boolean isRent() {
        return "RENT".equalsIgnoreCase(this.type);
    }

    public boolean isElectricity() {
        return "ELECTRICITY".equalsIgnoreCase(this.type);
    }

    public Double getUnitsConsumed() {
        if (currentReading != null && previousReading != null) {
            return Math.max(0.0, currentReading - previousReading);
        }
        return null;
    }
}

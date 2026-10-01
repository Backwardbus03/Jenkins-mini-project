package com.rentreminder.app.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object representing the Record Payment form (/payments/new).
 */
public class PaymentFormPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By pageTitle     = By.cssSelector(".page-title");
    private final By tenantSelect  = By.id("pay-tenant");
    private final By amountInput   = By.id("pay-amount");
    private final By dateInput     = By.id("pay-date");
    private final By statusSelect  = By.id("pay-status");
    private final By monthInput    = By.id("pay-month");
    private final By yearInput     = By.id("pay-year");
    private final By saveButton    = By.id("btn-save-payment");
    private final By overdueNotice = By.id("overdue-notice");

    public PaymentFormPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public PaymentFormPage open(String baseUrl) {
        driver.get(baseUrl + "/payments/new");
        wait.until(ExpectedConditions.visibilityOfElementLocated(tenantSelect));
        return this;
    }

    public void selectTenantByPartialText(String partialText) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(tenantSelect));
        Select select = new Select(el);
        for (WebElement opt : select.getOptions()) {
            if (opt.getText().contains(partialText)) {
                opt.click();
                return;
            }
        }
        throw new IllegalArgumentException("No tenant matching: " + partialText);
    }

    public void setAmount(double amount) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(amountInput));
        el.clear();
        el.sendKeys(String.valueOf(amount));
    }

    public void setDate(String date) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(dateInput));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1]; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true })); " +
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            el, date
        );
    }

    public void setStatus(String status) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(statusSelect));
        Select select = new Select(el);
        try {
            select.selectByValue(status);
        } catch (Exception e) {
            for (WebElement opt : select.getOptions()) {
                if (opt.getText().toLowerCase().contains(status.toLowerCase())) {
                    opt.click();
                    return;
                }
            }
            select.selectByVisibleText(status);
        }
    }

    public void setMonth(int month) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(monthInput));
        el.clear();
        el.sendKeys(String.valueOf(month));
    }

    public void setYear(int year) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(yearInput));
        el.clear();
        el.sendKeys(String.valueOf(year));
    }

    public void clickSave() {
        wait.until(ExpectedConditions.elementToBeClickable(saveButton)).click();
    }

    public void recordPayment(String tenantPartialText, double amount, String date, String status, int month, int year) {
        selectTenantByPartialText(tenantPartialText);
        setAmount(amount);
        if (date != null && !date.isBlank()) {
            setDate(date);
        }
        setStatus(status);
        setMonth(month);
        setYear(year);
        clickSave();
    }

    public boolean isOverdueNoticeDisplayed() {
        return !driver.findElements(overdueNotice).isEmpty() && driver.findElement(overdueNotice).isDisplayed();
    }

    public boolean isDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(tenantSelect)).isDisplayed();
    }
}

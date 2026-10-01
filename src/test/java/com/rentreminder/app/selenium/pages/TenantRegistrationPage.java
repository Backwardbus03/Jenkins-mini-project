package com.rentreminder.app.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object representing the Tenant Self-Registration page (/portal/register).
 */
public class TenantRegistrationPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By nameInput            = By.id("reg-name");
    private final By emailInput           = By.id("reg-email");
    private final By phoneInput           = By.id("reg-phone");
    private final By passwordInput        = By.id("reg-password");
    private final By confirmPasswordInput = By.id("reg-confirm");
    private final By registerButton       = By.id("btn-register");
    private final By errorAlert           = By.cssSelector(".alert-error");
    private final By loginLink            = By.xpath("//a[contains(@href, '/login')]");

    public TenantRegistrationPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public TenantRegistrationPage open(String baseUrl) {
        driver.get(baseUrl + "/portal/register");
        wait.until(ExpectedConditions.visibilityOfElementLocated(nameInput));
        return this;
    }

    public void fillForm(String name, String email, String phone, String password, String confirmPassword) {
        WebElement nameEl = wait.until(ExpectedConditions.visibilityOfElementLocated(nameInput));
        nameEl.clear();
        nameEl.sendKeys(name);

        WebElement emailEl = wait.until(ExpectedConditions.visibilityOfElementLocated(emailInput));
        emailEl.clear();
        emailEl.sendKeys(email);

        if (phone != null && !phone.isBlank()) {
            WebElement phoneEl = wait.until(ExpectedConditions.visibilityOfElementLocated(phoneInput));
            phoneEl.clear();
            phoneEl.sendKeys(phone);
        }

        WebElement passEl = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordInput));
        passEl.clear();
        passEl.sendKeys(password);

        WebElement confirmEl = wait.until(ExpectedConditions.visibilityOfElementLocated(confirmPasswordInput));
        confirmEl.clear();
        confirmEl.sendKeys(confirmPassword);
    }

    public void clickRegister() {
        wait.until(ExpectedConditions.elementToBeClickable(registerButton)).click();
    }

    public void register(String name, String email, String phone, String password, String confirmPassword) {
        fillForm(name, email, phone, password, confirmPassword);
        clickRegister();
    }

    public String getErrorMessage() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(errorAlert)).getText();
    }

    public boolean isErrorAlertDisplayed() {
        return !driver.findElements(errorAlert).isEmpty() && driver.findElement(errorAlert).isDisplayed();
    }

    public boolean isDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(nameInput)).isDisplayed();
    }
}

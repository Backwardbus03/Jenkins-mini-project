package com.rentreminder.app.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object representing the Add New Tenant form (/tenants/new).
 */
public class AddTenantPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By pageTitle   = By.cssSelector(".page-title");
    private final By nameInput   = By.id("tenant-name");
    private final By emailInput  = By.id("tenant-email");
    private final By phoneInput  = By.id("tenant-phone");
    private final By aptInput    = By.id("tenant-apt");
    private final By saveButton  = By.id("btn-save-tenant");

    public AddTenantPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public AddTenantPage open(String baseUrl) {
        driver.get(baseUrl + "/tenants/new");
        wait.until(ExpectedConditions.visibilityOfElementLocated(nameInput));
        return this;
    }

    public void fillForm(String name, String email, String phone, String aptNumber) {
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

        WebElement aptEl = wait.until(ExpectedConditions.visibilityOfElementLocated(aptInput));
        aptEl.clear();
        aptEl.sendKeys(aptNumber);
    }

    public void clickSave() {
        wait.until(ExpectedConditions.elementToBeClickable(saveButton)).click();
    }

    public void saveTenant(String name, String email, String phone, String aptNumber) {
        fillForm(name, email, phone, aptNumber);
        clickSave();
    }

    public boolean isDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(nameInput)).isDisplayed();
    }
}

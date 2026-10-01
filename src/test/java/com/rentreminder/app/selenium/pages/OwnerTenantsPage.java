package com.rentreminder.app.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object representing the Owner Tenants Directory (/tenants).
 */
public class OwnerTenantsPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By pageTitle        = By.cssSelector(".page-title");
    private final By addTenantButton  = By.id("btn-add-tenant");
    private final By activeTabButton  = By.id("btn-tab-active");
    private final By expiredTabButton = By.id("btn-tab-expired");
    private final By activeTableRows  = By.cssSelector("#pane-active table tbody tr");
    private final By signOutButton    = By.cssSelector("form[action*='/logout'] button");
    private final By navbarBrand      = By.cssSelector(".navbar-brand");

    public OwnerTenantsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public OwnerTenantsPage open(String baseUrl) {
        driver.get(baseUrl + "/tenants");
        wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle));
        return this;
    }

    public boolean isDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle)).isDisplayed();
    }

    public String getPageTitleText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle)).getText();
    }

    public void clickAddTenant() {
        wait.until(ExpectedConditions.elementToBeClickable(addTenantButton)).click();
    }

    public void clickSignOut() {
        wait.until(ExpectedConditions.elementToBeClickable(signOutButton)).click();
    }

    public boolean hasTenant(String identifier) {
        List<WebElement> rows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(activeTableRows));
        return rows.stream().anyMatch(r -> r.getText().contains(identifier));
    }

    public WebElement findTenantRowByEmail(String email) {
        List<WebElement> rows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(activeTableRows));
        return rows.stream()
                .filter(r -> r.getText().contains(email))
                .findFirst()
                .orElse(null);
    }

    public String getTenantRowDetailsByEmail(String email) {
        WebElement row = findTenantRowByEmail(email);
        return row != null ? row.getText() : null;
    }

    public void clickAddPaymentForTenant(String email) {
        WebElement row = findTenantRowByEmail(email);
        if (row != null) {
            WebElement addPaymentLink = row.findElement(By.xpath(".//a[contains(@href, '/payments/new')]"));
            addPaymentLink.click();
        } else {
            throw new IllegalArgumentException("Tenant with email not found: " + email);
        }
    }

    public void switchTab(String tabName) {
        if ("expired".equalsIgnoreCase(tabName)) {
            wait.until(ExpectedConditions.elementToBeClickable(expiredTabButton)).click();
        } else {
            wait.until(ExpectedConditions.elementToBeClickable(activeTabButton)).click();
        }
    }
}

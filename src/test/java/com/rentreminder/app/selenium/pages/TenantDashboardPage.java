package com.rentreminder.app.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object representing the Tenant Dashboard (/portal/dashboard).
 */
public class TenantDashboardPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By pageTitle              = By.cssSelector(".page-title");
    private final By navbarWelcome          = By.cssSelector(".navbar-links");
    private final By successAlert           = By.cssSelector(".alert-success");
    private final By errorAlert             = By.cssSelector(".alert-error");
    private final By notificationCard       = By.cssSelector(".notification-card");
    private final By approveToggleBtn       = By.xpath("//button[contains(text(), 'Approve')]");
    private final By depositAmountInput     = By.id("deposit-amount");
    private final By startDateInput         = By.id("start-date");
    private final By endDateInput           = By.id("end-date");
    private final By confirmApproveButton   = By.id("btn-confirm-approve");
    private final By rejectButton           = By.id("btn-reject");
    private final By leaseCard              = By.cssSelector(".lease-card");
    private final By depositCard            = By.cssSelector(".deposit-card");
    private final By depositAmountValue     = By.cssSelector(".deposit-amount");
    private final By emptyStateHeading      = By.cssSelector(".empty-state h3");
    private final By signOutButton          = By.cssSelector("form[action*='/logout'] button");

    public TenantDashboardPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public TenantDashboardPage open(String baseUrl) {
        driver.get(baseUrl + "/portal/dashboard");
        wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle));
        return this;
    }

    public boolean isDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle)).isDisplayed();
    }

    public String getNavbarWelcomeText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(navbarWelcome)).getText();
    }

    public boolean hasSuccessAlert() {
        return !driver.findElements(successAlert).isEmpty() && driver.findElement(successAlert).isDisplayed();
    }

    public String getSuccessAlertText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(successAlert)).getText();
    }

    public boolean hasPendingInvitation() {
        return !driver.findElements(notificationCard).isEmpty();
    }

    public void approveInvitation(double depositAmount, String startDate, String endDate) {
        // Expand the approval form
        WebElement toggle = wait.until(ExpectedConditions.elementToBeClickable(approveToggleBtn));
        toggle.click();

        WebElement depositEl = wait.until(ExpectedConditions.visibilityOfElementLocated(depositAmountInput));
        depositEl.clear();
        depositEl.sendKeys(String.valueOf((int) depositAmount));

        WebElement startEl = wait.until(ExpectedConditions.presenceOfElementLocated(startDateInput));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1]; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true })); " +
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            startEl, startDate
        );

        WebElement endEl = wait.until(ExpectedConditions.presenceOfElementLocated(endDateInput));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1]; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true })); " +
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            endEl, endDate
        );

        wait.until(ExpectedConditions.elementToBeClickable(confirmApproveButton)).click();
    }

    public boolean hasActiveLease() {
        return !driver.findElements(leaseCard).isEmpty();
    }

    public String getLeaseCardText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(leaseCard)).getText();
    }

    public String getDepositBalanceText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(depositAmountValue)).getText();
    }

    public boolean hasEmptyState() {
        return !driver.findElements(emptyStateHeading).isEmpty();
    }

    public String getEmptyStateHeading() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(emptyStateHeading)).getText();
    }

    public void clickSignOut() {
        wait.until(ExpectedConditions.elementToBeClickable(signOutButton)).click();
    }
}

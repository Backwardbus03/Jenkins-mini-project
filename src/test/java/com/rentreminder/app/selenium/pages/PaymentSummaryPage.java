package com.rentreminder.app.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object representing the Payment Summary table (/payments/summary).
 */
public class PaymentSummaryPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By pageTitle       = By.cssSelector(".page-title");
    private final By addPaymentBtn   = By.xpath("//a[contains(@href, '/payments/new')]");
    private final By printAllBtn     = By.xpath("//a[contains(@href, '/payments/receipt') and not(contains(@href, '/payments/receipt/'))]");
    private final By tableRows       = By.cssSelector(".table-wrap table tbody tr");

    public PaymentSummaryPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public PaymentSummaryPage open(String baseUrl) {
        driver.get(baseUrl + "/payments/summary");
        wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle));
        return this;
    }

    public boolean isDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle)).isDisplayed();
    }

    public boolean hasPaymentForTenant(String tenantName, String status) {
        List<WebElement> rows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(tableRows));
        return rows.stream().anyMatch(row -> {
            String text = row.getText();
            return text.contains(tenantName) && text.toUpperCase().contains(status.toUpperCase());
        });
    }

    public WebElement findPaymentRow(String tenantName) {
        List<WebElement> rows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(tableRows));
        return rows.stream()
                .filter(r -> r.getText().contains(tenantName))
                .findFirst()
                .orElse(null);
    }

    public void clickReceiptForTenant(String tenantName) {
        WebElement row = findPaymentRow(tenantName);
        if (row != null) {
            WebElement receiptBtn = row.findElement(By.xpath(".//a[contains(@href, '/payments/receipt/')]"));
            receiptBtn.click();
        } else {
            throw new IllegalArgumentException("No payment record found for tenant: " + tenantName);
        }
    }
}

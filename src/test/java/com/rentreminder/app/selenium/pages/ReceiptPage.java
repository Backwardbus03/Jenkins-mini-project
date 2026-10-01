package com.rentreminder.app.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object representing the Printable Official Rent Receipt view (/payments/receipt/{id} or /portal/receipt/{id}).
 */
public class ReceiptPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By receiptMainTitle     = By.cssSelector(".receipt-main-title");
    private final By receiptBrandTitle    = By.cssSelector(".receipt-brand-title");
    private final By metaLines            = By.cssSelector(".receipt-meta-line");
    private final By statusStampPaid      = By.cssSelector(".receipt-stamp-paid");
    private final By statusStampOverdue   = By.cssSelector(".receipt-stamp-overdue");
    private final By receiptTable         = By.cssSelector(".receipt-table");
    private final By grandTotalRow        = By.cssSelector(".receipt-total-row.grand-total");
    private final By signatureBlocks      = By.cssSelector(".receipt-signature-block");
    private final By printButton          = By.xpath("//button[contains(., 'Print')]");

    public ReceiptPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public boolean isDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(receiptMainTitle)).isDisplayed();
    }

    public String getMainTitleText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(receiptMainTitle)).getText();
    }

    public String getBrandTitleText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(receiptBrandTitle)).getText();
    }

    public boolean isPaidStampDisplayed() {
        return !driver.findElements(statusStampPaid).isEmpty() && driver.findElement(statusStampPaid).isDisplayed();
    }

    public boolean isOverdueStampDisplayed() {
        return !driver.findElements(statusStampOverdue).isEmpty() && driver.findElement(statusStampOverdue).isDisplayed();
    }

    public String getGrandTotalText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(grandTotalRow)).getText();
    }

    public int getSignatureBlocksCount() {
        return driver.findElements(signatureBlocks).size();
    }
}

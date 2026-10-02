package com.rentreminder.app.selenium.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.time.Duration;

/**
 * Base test class for all Selenium WebDriver test journeys in the Rent Payment Reminder Portal.
 * Initializes Spring Boot with an embedded web server on a random port, configures headless Chrome,
 * provides explicit wait utilities, and attaches the ScreenshotWatcher extension for automatic failure screenshots.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(ScreenshotWatcher.class)
public abstract class BaseSeleniumTest {

    static {
        System.setProperty("webdriver.chrome.silentOutput", "true");
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.SEVERE);
    }

    @LocalServerPort
    protected int port;

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeEach
    public void setUpBase() {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        try {
            ChromeOptions chromeOptions = new ChromeOptions();
            if (headless) {
                chromeOptions.addArguments("--headless=new");
            }
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--window-size=1920,1080");
            chromeOptions.addArguments("--remote-allow-origins=*");
            driver = new ChromeDriver(chromeOptions);
        } catch (Exception e) {
            System.err.println("ChromeDriver initialization failed, attempting Edge fallback: " + e.getMessage());
            EdgeOptions edgeOptions = new EdgeOptions();
            if (headless) {
                edgeOptions.addArguments("--headless=new");
            }
            edgeOptions.addArguments("--no-sandbox");
            edgeOptions.addArguments("--disable-dev-shm-usage");
            edgeOptions.addArguments("--disable-gpu");
            edgeOptions.addArguments("--window-size=1920,1080");
            edgeOptions.addArguments("--remote-allow-origins=*");
            driver = new EdgeDriver(edgeOptions);
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDownBase() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error quitting driver: " + e.getMessage());
            }
        }
    }

    public WebDriver getDriver() {
        return driver;
    }

    public String getBaseUrl() {
        return System.getProperty("app.baseUrl", "http://localhost:" + port);
    }

    public void navigateTo(String path) {
        String url = getBaseUrl() + (path.startsWith("/") ? path : "/" + path);
        driver.get(url);
    }

    public WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public boolean waitForUrlContains(String fragment) {
        return wait.until(ExpectedConditions.urlContains(fragment));
    }

    public void click(By locator) {
        waitForClickable(locator).click();
    }

    public void type(By locator, String text) {
        WebElement el = waitForVisibility(locator);
        el.clear();
        el.sendKeys(text);
    }

    public String getText(By locator) {
        return waitForVisibility(locator).getText();
    }

    public String captureManualScreenshot(String label) {
        return ScreenshotWatcher.captureScreenshot(driver, getClass().getSimpleName(), label);
    }
}

package com.ivan.selenium.avitobuy.WebDriver.PaymentStrategy;

import com.ivan.selenium.avitobuy.WebDriver.PageActions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class VTBPaymentStrategy implements PaymentStrategy {
    private static final String SMS_CODE_INPUT_XPATH = "//input[@id='psw_id']";
    private static final String CONFIRM_BUTTON_XPATH = "//input[@id='btnSubmit']";
    @Override
    public void processPayment(WebDriver driver, String code) {
        PageActions actions = new PageActions(driver);
        actions.isElementAvailable(SMS_CODE_INPUT_XPATH);

        WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofMillis(500));
        WebElement smsCodeInput = quickWait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath(SMS_CODE_INPUT_XPATH)));
        smsCodeInput.sendKeys(code);

        // Подтверждаем оплату
        WebElement confirmButton = quickWait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(CONFIRM_BUTTON_XPATH)));
        try {
            confirmButton.click();
        } catch (Exception TimeoutException) {
            // Ошибка загрузки страницы ожидаема
        }
    }
}

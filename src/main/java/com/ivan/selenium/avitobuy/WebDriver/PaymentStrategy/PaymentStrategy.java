package com.ivan.selenium.avitobuy.WebDriver.PaymentStrategy;

import org.openqa.selenium.WebDriver;

public interface PaymentStrategy {
    void processPayment(WebDriver driver, String code);
}


package com.ivan.selenium.avitobuy.WebDriver.PaymentStrategy;

import org.openqa.selenium.WebDriver;

class SBERPaymentStrategy implements PaymentStrategy {
    @Override
    public void processPayment(WebDriver driver, String code) {
        System.out.println("Processing payment via SBER...");
    }
}

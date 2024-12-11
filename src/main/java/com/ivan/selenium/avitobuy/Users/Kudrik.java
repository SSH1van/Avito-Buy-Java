package com.ivan.selenium.avitobuy.Users;

import com.ivan.selenium.avitobuy.WebDriver.Main;
import com.ivan.selenium.avitobuy.WebDriver.PaymentStrategy.PaymentStrategy;
import com.ivan.selenium.avitobuy.WebDriver.PaymentStrategy.VTBPaymentStrategy;

public class Kudrik {
    public static void main(String[] args) {
        String url = "";
        String relativePath = "Data/User Data Kudrik";
        int maxPrice = 45000;
        long timeRefresh = 1800;
        long timeSleep = 0;
        boolean headless = true;
        PaymentStrategy paymentStrategy = new VTBPaymentStrategy();

        Main.startWebDriver(url, relativePath, maxPrice, timeRefresh, timeSleep, headless, paymentStrategy);
    }
}

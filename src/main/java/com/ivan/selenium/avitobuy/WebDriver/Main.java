package com.ivan.selenium.avitobuy.WebDriver;

import com.ivan.selenium.avitobuy.WebDriver.PaymentStrategy.PaymentStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final String BUY_BUTTON_XPATH = "//button[@data-marker='delivery-item-button-main']";
    private static final String PAYMENT_OR_ORDER_BUTTON_XPATH = "//button[@data-marker='sd/order-widget-payment-button' or @data-marker='sidebar/orderButton']";
    private static final String PAYMENT_OPTION_XPATH = "//li[@data-id='196765361|1']";
    private static final String GO_TO_PAYMENT_XPATH = "//button[@data-marker='payButton']";

    public static void startWebDriver(String url, String relativePath, int maxPrice, long timeRefresh, long timeSleep, boolean headless, PaymentStrategy paymentStrategy) {
        WebDriverManager driverManager = new WebDriverManager();
        ChromeOptions options = WebDriverManager.createOptions(relativePath, headless);
        WebDriver driver = driverManager.initDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(timeRefresh));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Программа завершена. Освобождаем ресурсы...");
            driverManager.cleanUp();
        }));

        try {
            PageActions actions = new PageActions(driver);

            // Открываем ссылку на товар
            actions.openUrl(url, timeSleep);

            // Проверка доступности кнопки "Купить с доставкой"
            actions.isElementAvailable(BUY_BUTTON_XPATH);

            // Проверка цены
            if (!actions.isPriceWithinLimit(maxPrice))
                return;

            //  Нажимаем кнопку "Купить с доставкой" и ждём появления кнопки "Оплатить" или "Перейти к оплате"
            actions.performActionWithRetry(BUY_BUTTON_XPATH, PAYMENT_OR_ORDER_BUTTON_XPATH);

            //  Нажимаем кнопку "Оплатить" или "Перейти к оплате" и ждём появления выбора способа оплаты
            actions.performActionWithRetry(PAYMENT_OR_ORDER_BUTTON_XPATH, PAYMENT_OPTION_XPATH);

            // Выбираем способ оплаты, увеличиваем время обновления страницы, переходим к оплате
            actions.selectPaymentMethod(PAYMENT_OPTION_XPATH, GO_TO_PAYMENT_XPATH, timeRefresh * 2);

            // Получаем код из SMS
            String code = actions.waitForCodeFile();

            // Осуществляем оплату способом, который был передан
            paymentStrategy.processPayment(driver, code);

            System.out.println("Купил");
        } catch (Exception e) {
            LOGGER.severe("An error occurred: " + e.getMessage());
        } finally {
            driverManager.cleanUp();
        }
    }
}
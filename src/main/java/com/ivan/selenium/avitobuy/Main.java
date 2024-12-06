package com.ivan.selenium.avitobuy;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static WebDriver driver;

    private static void cleanUpWebDriver() {
        if (driver != null) {
            driver.quit();
        }
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            int exitCode;

            if (osName.contains("win")) {
                // Завершение процесса для Windows
                exitCode = executeCommand("taskkill", "/F", "/IM", "chromedriver.exe", "/T");
            } else {
                // Завершение процесса для Linux/Mac
                exitCode = executeCommand("pkill", "-f", "chromedriver");
            }

            if (exitCode == 0) {
                LOGGER.info("Процесс chromedriver успешно завершен.");
            } else {
                LOGGER.warning("Не удалось завершить процесс chromedriver. Код выхода: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Произошла ошибка при завершении процесса chromedriver.", e);
            Thread.currentThread().interrupt(); // Восстановление флага прерывания
        }
    }

    private static int executeCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        return process.waitFor(); // Ждем завершения процесса и возвращаем код выхода
    }

    public static ChromeOptions createChromeOptions(String relativePath, boolean headless) {
        String absolutePath = Paths.get(relativePath).toAbsolutePath().toString();

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
        chromeOptions.addArguments("user-data-dir=" + absolutePath);
        chromeOptions.addArguments("profile-directory=Default");

        if (headless) {
            chromeOptions.addArguments("--headless=new");
            chromeOptions.addArguments("--disable-gpu");
        }

        return chromeOptions;
    }

    public static void isButtonAvailable(long timeSleep) {
        WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofMillis(500));
        boolean isButtonAvailable = false;
        Random random = new Random();

        try {
            TimeUnit.SECONDS.sleep(timeSleep);
            while (!isButtonAvailable) {
                try {
                    quickWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(BUY_BUTTON_XPATH)));
                    isButtonAvailable = true;
                } catch (Exception ex) {
                    TimeUnit.MILLISECONDS.sleep(100 + random.nextInt(250));
                    try {
                        driver.navigate().refresh();
                    } catch (Exception e) {
                        // Ошибка обновления страницы ожидаема
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean isPriceWithinLimit(int maxPrice) {
        String xpath = "//span[@data-marker='item-view/item-price']";
        try {
            WebElement priceElement = driver.findElement(By.xpath(xpath));
            String priceText = priceElement.getText();
            int price = Integer.parseInt(priceText.replaceAll("[^0-9]", ""));

            return price <= maxPrice;
        } catch (Exception e) {
            return false;
        }
    }

    private static final String BUY_BUTTON_XPATH = "//button[@data-marker='delivery-item-button-main']";
    private static final String PAY_BUTTON_XPATH = "//button[@data-marker='sd/order-widget-payment-button']";
    private static final String PAYMENT_OPTION_XPATH = "//li[@data-id='196765361|1']";

    public static void performActionWithRetry(String firstButtonXPath, String secondButtonXPath) {
        final AtomicBoolean firstButtonError = new AtomicBoolean(false);
        final AtomicBoolean secondButtonFound = new AtomicBoolean(false);
        long iterationStart = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            while (!(firstButtonError.get() && secondButtonFound.get())) {
                CountDownLatch latch = new CountDownLatch(2);

                executor.submit(() -> {
                    try {
                        WebElement firstButton = driver.findElement(By.xpath(firstButtonXPath));
                        firstButton.click();
                    } catch (Exception e) {
                        firstButtonError.set(true);
                    } finally {
                        latch.countDown();
                    }
                });

                executor.submit(() -> {
                    try {
                        secondButtonFound.set(!driver.findElements(By.xpath(secondButtonXPath)).isEmpty());
                    } catch (Exception e) {
                        // Ошибка поиска второй кнопки ожидаема
                    } finally {
                        latch.countDown();
                    }
                });

                try {
                    latch.await(); // Ждём завершения обеих задач
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Действия при сбое
                if (firstButtonError.get() && !secondButtonFound.get()) {
                    try {
                        driver.navigate().refresh();
                    } catch (Exception e) {
                        // Ошибка обновления страницы ожидаема
                    }
                    firstButtonError.set(false);
                    secondButtonFound.set(false);
                }
            }
        }

        long iterationEnd = System.currentTimeMillis();
        System.out.println("Время выполнения перехода: " + (iterationEnd - iterationStart) + " мс");
    }

    public static void startWebDriver(String url, String relativePath, int maxPrice, long timeWait, long timeSleep, boolean headless) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Программа завершена. Освобождаем ресурсы...");
            cleanUpWebDriver();
        }));

        ChromeOptions chromeOptions = createChromeOptions(relativePath, headless);
        driver = new ChromeDriver(chromeOptions);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(timeWait));

        try {
            try {
                driver.get(url);
            } catch (Exception TimeoutException) {
                // Ошибка загрузки страницы ожидаема
            }

            isButtonAvailable(timeSleep);
            if (!isPriceWithinLimit(maxPrice))
                return;

            //  Нажимаем кнопку "Купить с доставкой" и ждём появления кнопки "Оплатить"
            performActionWithRetry(BUY_BUTTON_XPATH, PAY_BUTTON_XPATH);

            //  Нажимаем кнопку "Оплатить" и ждём появления выбора способа оплаты
            performActionWithRetry(PAY_BUTTON_XPATH, PAYMENT_OPTION_XPATH);

            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofMillis(500));
            WebElement goToPaymentButton = quickWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(PAYMENT_OPTION_XPATH)));
            goToPaymentButton.click();

            // Нажимаем "Перейти к оплате"
//            WebElement goToPaymentButton = quickWait.until(ExpectedConditions.elementToBeClickable(
//                    By.xpath("//button[@data-marker='payButton']")));
//            goToPaymentButton.click();

//
//            // Вводим код из SMS
//            WebElement smsCodeInput = slowWait.until(ExpectedConditions.presenceOfElementLocated(
//                    By.xpath("//input[@id='psw_id']")));
//            smsCodeInput.sendKeys("1234"); // Здесь замените "1234" на актуальный код из SMS
//
//            // Подтверждаем оплату
//            WebElement confirmButton = slowWait.until(ExpectedConditions.elementToBeClickable(
//                    By.xpath("//input[@id='btnSubmit']")));
//            confirmButton.click();

            System.out.println("Купил");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Произошла ошибка.", e);
        } finally {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Пауза была прервана.", e);
            }
            driver.quit();
        }
    }
}
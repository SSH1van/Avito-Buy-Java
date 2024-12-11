package com.ivan.selenium.avitobuy;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

    // Сборщик мусора
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

    // Опции дял Chrome
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

    // Проверка доступности кнопки
    public static void isElementAvailable(String element, long timeSleep) {
        WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofMillis(500));
        boolean isElementAvailable = false;
        Random random = new Random();

        try {
            TimeUnit.SECONDS.sleep(timeSleep);
            while (!isElementAvailable) {
                if (isFirewallPresent()) return;

                try {
                    quickWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(element)));
                    isElementAvailable = true;
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

    // Проверка цены
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

    // Проверка на Firewall
    public static boolean isFirewallPresent() {
        try {
            driver.findElement(By.xpath("//h2[@class='firewall-title']"));
            System.out.println("Блокировка по IP. Решите капчу!");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static final String BUY_BUTTON_XPATH = "//button[@data-marker='delivery-item-button-main']";
    private static final String PAY_BUTTON_XPATH = "//button[@data-marker='sd/order-widget-payment-button']";
    private static final String PAYMENT_OPTION_XPATH = "//li[@data-id='196765361|1']";
    private static final String GO_TO_PAYMENT_XPATH = "//button[@data-marker='payButton']";
    private static final String SMS_CODE_INPUT_XPATH = "//input[@id='psw_id']";
    private static final String CONFIRM_BUTTON_XPATH = "//input[@id='btnSubmit']";

    // Попытка нажать на firstButton и найти на странице secondButton
    public static void performActionWithRetry(String firstButtonXPath, String secondButtonXPath) {
        final AtomicBoolean firstButtonError = new AtomicBoolean(false);
        final AtomicBoolean secondButtonFound = new AtomicBoolean(false);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            while (!(firstButtonError.get() && secondButtonFound.get())) {
                CountDownLatch latch = new CountDownLatch(2);

                executor.submit(() -> {
                    try {
                        WebElement firstButton = driver.findElement(By.xpath(firstButtonXPath));
                        firstButton.click();
                    } catch (TimeoutException e) {
                        // Ошибка загрузки страницы ожидаема
                    } catch (Exception e) {
                        // Ошибка нажатия первой кнопки ожидаема
                        firstButtonError.set(true);
                    } finally {
                        latch.countDown();
                    }
                });

                executor.submit(() -> {
                    try {
                        secondButtonFound.set(!driver.findElements(By.xpath(secondButtonXPath)).isEmpty());
                    } catch (TimeoutException e) {
                        // Ошибка загрузки страницы ожидаема
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
    }

    public static String waitForCodeFile() throws IOException {
        File file = new File("code.txt");

        while (!file.exists()) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                throw new IOException("Ожидание было прервано.", e);
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        } finally {
            if (file.delete()) {
                System.out.println("Файл " + file.getName() + " успешно удалён");
            } else {
                System.err.println("Не удалось удалить файл " + file.getName());
            }
        }
    }

    public static void startWebDriver(String url, String relativePath, int maxPrice, long timeRefresh, long timeSleep, boolean headless) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Программа завершена. Освобождаем ресурсы...");
            cleanUpWebDriver();
        }));

        ChromeOptions chromeOptions = createChromeOptions(relativePath, headless);
        driver = new ChromeDriver(chromeOptions);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(timeRefresh));

        try {
            try {
                driver.get(url);
            } catch (Exception TimeoutException) {
                // Ошибка загрузки страницы ожидаема
            }

            isElementAvailable(BUY_BUTTON_XPATH, timeSleep);
            if (!isPriceWithinLimit(maxPrice))
                return;

            //  Нажимаем кнопку "Купить с доставкой" и ждём появления кнопки "Оплатить"
            performActionWithRetry(BUY_BUTTON_XPATH, PAY_BUTTON_XPATH);

            //  Нажимаем кнопку "Оплатить" и ждём появления выбора способа оплаты
            performActionWithRetry(PAY_BUTTON_XPATH, PAYMENT_OPTION_XPATH);

            // Выбор способа оплаты
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofMillis(500));
            WebElement paymentOption = quickWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(PAYMENT_OPTION_XPATH)));
            paymentOption.click();


            // Увеличиваем время обновления, т.к. страница оплаты медленно загружается
            driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(timeRefresh * 2));
            // Нажимаем "Перейти к оплате"
            WebElement goToPaymentButton = quickWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(GO_TO_PAYMENT_XPATH)));
            try {
                goToPaymentButton.click();
            } catch (Exception TimeoutException) {
                // Ошибка загрузки страницы ожидаема
            }

            // Вводим код из SMS
            String code = waitForCodeFile();
            isElementAvailable(SMS_CODE_INPUT_XPATH, timeSleep);
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
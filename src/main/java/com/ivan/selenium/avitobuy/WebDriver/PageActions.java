package com.ivan.selenium.avitobuy.WebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PageActions {
    private final WebDriver driver;

    public PageActions(WebDriver driver) {
        this.driver = driver;
    }

    // Открытие ссылки
    public void openUrl(String url, long timeSleep) throws InterruptedException {
        try {
            driver.get(url);
        } catch (Exception TimeoutException) {
            // Ошибка загрузки страницы ожидаема
        }
        TimeUnit.SECONDS.sleep(timeSleep);
    }

    // Проверка доступности кнопки
    public void isElementAvailable(String xpath) {
        WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofMillis(500));
        boolean isElementAvailable = false;
        Random random = new Random();

        try {
            while (!isElementAvailable) {
                if (isFirewallPresent()) return;

                try {
                    quickWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
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

    // Проверка, что цена ниже заданного значения maxPrice
    private static final String PRICE_XPATH = "//span[@data-marker='item-view/item-price']";
    public boolean isPriceWithinLimit(int maxPrice) {
        try {
            WebElement priceElement = driver.findElement(By.xpath(PRICE_XPATH));
            String priceText = priceElement.getDomAttribute("content");
            assert priceText != null;
            int price = Integer.parseInt(priceText.replaceAll("[^0-9]", ""));
            return price <= maxPrice;
        } catch (Exception e) {
            return false;
        }
    }

    // Попытка нажать на firstButton и найти на странице secondButton
    public void performActionWithRetry(String firstButtonXPath, String secondButtonXPath) {
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

    // Выбираем способ оплаты, увеличиваем время обновления страницы, переходим к оплате
    public void selectPaymentMethod(String paymentOptionXpath, String goToPaymentXpath, long timeRefresh) {
        // Выбор способа оплаты
        WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofMillis(500));
        WebElement paymentOption = quickWait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(paymentOptionXpath)));
        paymentOption.click();

        // Увеличиваем время обновления, поскольку страница оплаты медленно загружается
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(timeRefresh));
        // Нажимаем "Перейти к оплате"
        WebElement goToPaymentButton = quickWait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(goToPaymentXpath)));
        try {
            goToPaymentButton.click();
        } catch (Exception TimeoutException) {
            // Ошибка загрузки страницы ожидаема
        }
    }

    // Ожидаем появления файла для получения кода из SMS
    public String waitForCodeFile() throws IOException {
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

    // Проверка на наличие капчи
    private static final String FIREWALL_XPATH = "//h2[@class='firewall-title']";
    public boolean isFirewallPresent() {
        try {
            driver.findElement(By.xpath(FIREWALL_XPATH));
            System.out.println("Блокировка по IP. Решите капчу!");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
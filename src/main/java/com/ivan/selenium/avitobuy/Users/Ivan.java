package com.ivan.selenium.avitobuy.Users;

import com.ivan.selenium.avitobuy.WebDriver.Main;
import com.ivan.selenium.avitobuy.WebDriver.PaymentStrategy.PaymentStrategy;
import com.ivan.selenium.avitobuy.WebDriver.PaymentStrategy.VTBPaymentStrategy;

public class Ivan {
    public static void main(String[] args) {
//        String url = "https://www.avito.ru/moskva/igry_pristavki_i_programmy/igrovaya_konsol_playstation_5_slim_4721689876?utm_campaign=native&utm_medium=item_page_ios&utm_source=soc_sharing";
        String url = "https://www.avito.ru/moskva/igry_pristavki_i_programmy/sony_playstation_5_digital_edition_4697920301";
//        String url = "https://www.avito.ru/moskva/tovary_dlya_detey_i_igrushki/nabor_kantselyarii_dlya_devochek_s_organayzerom_4785538351?slocation=621540";
        String relativePath = "Data/User Data Ivan";
        int maxPrice = 1000;
        long timeRefresh = 1800;
        long timeSleep = 0;
        boolean headless = true;
        PaymentStrategy paymentStrategy = new VTBPaymentStrategy();

        Main.startWebDriver(url, relativePath, maxPrice, timeRefresh, timeSleep, headless, paymentStrategy);
    }
}

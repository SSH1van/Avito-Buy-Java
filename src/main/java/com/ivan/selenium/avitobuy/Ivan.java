package com.ivan.selenium.avitobuy;

public class Ivan {
    public static void main(String[] args) {
        String url = "https://www.avito.ru/moskva/noutbuki/igrovoy_noutbuk_hp_gaming_i7_gtx_1060_32gb_4715976515";
        String relativePath = "Data/User Data Ivan";
        int maxPrice = 1000;
        int timeWait = 50000;
        boolean headless = false;

        Main.startWebDriver(url, relativePath, maxPrice, timeWait, headless);
    }
}

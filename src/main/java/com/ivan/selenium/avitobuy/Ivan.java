package com.ivan.selenium.avitobuy;

public class Ivan {
    public static void main(String[] args) {
        String url = "https://www.avito.ru/moskva/noutbuki/igrovoy_noutbuk_msi_i5_i7_rtx_4050_4060_3897526448";
//        String url = "https://www.avito.ru/moskva/tovary_dlya_detey_i_igrushki/nabor_kantselyarii_dlya_devochek_s_organayzerom_4785538351?slocation=621540";
        String relativePath = "Data/User Data Ivan";
        int maxPrice = 1000;
        long timeRefresh = 1800;
        long timeSleep = 0;
        boolean headless = true;

        Main.startWebDriver(url, relativePath, maxPrice, timeRefresh, timeSleep, headless);
    }
}

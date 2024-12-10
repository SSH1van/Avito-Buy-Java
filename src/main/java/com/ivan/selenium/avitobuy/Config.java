package com.ivan.selenium.avitobuy;
import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing() // Если файл отсутствует, приложение продолжит работать
            .load();

    public static final String AUTH_LOGIN = dotenv.get("LOGIN");
    public static final String AUTH_PASSWORD = dotenv.get("PASSWORD");
}

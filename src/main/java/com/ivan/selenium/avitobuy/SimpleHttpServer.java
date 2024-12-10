package com.ivan.selenium.avitobuy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleHttpServer {
    private static final String AUTH_LOGIN = Config.AUTH_LOGIN;
    private static final String AUTH_PASSWORD = Config.AUTH_PASSWORD;
    private static final String OUTPUT_FILE = "code.txt";

    public static void main(String[] args) throws IOException {
        // Создаем сервер на порту 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new PostHandler());
        server.setExecutor(null); // Используется стандартный исполнитель
        server.start();
        System.out.println("Сервер запущен на порту 8080");
    }

    static class PostHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Метод не поддерживается");
                return;
            }

            // Проверка авторизации
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Неверный логин или пароль");
                return;
            }

            // Считываем тело запроса
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            inputStream.close();

            // Извлекаем код из сообщения
            String code = getPartMessage(requestBody);

            if (code == null) {
                sendResponse(exchange, 400, "Не найден числовой код в сообщении");
                return;
            }

            // Сохраняем код в файл
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, StandardCharsets.UTF_8))) {
                writer.write(code);
            }

            sendResponse(exchange, 200, "Код сохранен в файл");
        }

        private boolean isAuthorized(HttpExchange exchange) {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                return false;
            }

            // Извлекаем закодированные данные
            String base64Credentials = authHeader.substring("Basic ".length());
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);

            // Разбиваем логин и пароль
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                return false;
            }

            String login = parts[0];
            String password = parts[1];

            return AUTH_LOGIN.equals(login) && AUTH_PASSWORD.equals(password);
        }

        private String getPartMessage(String data) {
            // Ищем 4-значный код в тексте
            Pattern pattern = Pattern.compile("(?<!\\S)\\d{4}(?!\\S)");
            Matcher matcher = pattern.matcher(data);
            return matcher.find() ? matcher.group() : null;
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }
}

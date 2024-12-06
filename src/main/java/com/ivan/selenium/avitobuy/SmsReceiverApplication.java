package com.ivan.selenium.avitobuy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
public class SmsReceiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsReceiverApplication.class, args);
    }
}

@RestController
@RequestMapping("/")
class SmsController {
    private final String USERNAME;
    private final String PASSWORD;

    public SmsController() {
        Dotenv dotenv = Dotenv.load();
        this.USERNAME = dotenv.get("USERNAME");
        this.PASSWORD = dotenv.get("PASSWORD");
    }

    @PostMapping
    public ResponseEntity<String> receiveSms(@RequestBody String data, @RequestHeader HttpHeaders headers) {
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (!checkAuth(authHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        String code = getPartMessage(data);
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input");
        }

        String FILE_PATH = "code.txt";
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            writer.write(code);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing to file");
        }

        return ResponseEntity.ok("{\"status\":\"success\"}");
    }

    private boolean checkAuth(String authHeader) {
        if (authHeader == null || !authHeader.toLowerCase().startsWith("basic ")) {
            return false;
        }
        try {
            String base64Credentials = authHeader.substring(6);
            String decodedCredentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] parts = decodedCredentials.split(":", 2);
            return parts[0].equals(USERNAME) && parts[1].equals(PASSWORD);
        } catch (Exception e) {
            return false;
        }
    }

    private String getPartMessage(String data) {
        Pattern pattern = Pattern.compile("(?<!\\S)\\d{4}(?!\\S)");
        Matcher matcher = pattern.matcher(data);
        return matcher.find() ? matcher.group() : null;
    }
}

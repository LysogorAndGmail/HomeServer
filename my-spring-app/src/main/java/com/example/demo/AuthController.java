package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*") // РАЗРЕШИТЬ ВХОД ДЛЯ VUE
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepository;
    private final String JWT_SECRET = "my-super-secret-key-that-is-at-least-32-chars";

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
    	String username = credentials.get("username");
    	String password = credentials.get("password");

    	// ЛОГ 1: Что прислал фронтенд
        System.out.println("FRONTEND DATA: user=" + username + ", pass=" + password);

    	Optional<User> userOpt = userRepository.findByUsername(username);

    	if (userOpt.isPresent()) {
        	User user = userOpt.get();
        	// ЛОГ 2: Что вытащили из БД
        	System.out.println("DATABASE DATA: user=" + user.getUsername() + ", pass=" + user.getPassword());

        	if (user.getPassword().equals(password)) {
            		String token = Jwts.builder()
                	    .setSubject(user.getUsername())
                	    .setIssuedAt(new Date())
                    	    .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
                    	    .compact();
            		return ResponseEntity.ok(Map.of("token", token));
        	} else {
            		System.out.println("ERROR: Passwords do not match!");
        	}
    	} else {
        	System.out.println("ERROR: User not found in DB!");
    	}
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @PostMapping("/updateIP")
    public ResponseEntity<?> updateIP(@RequestBody Map<String, String> credentials) {
    String currentIP = credentials.get("ip");
    
    // Регулярное выражение для проверки формата IPv4
    // Оно проверяет, что строка состоит из 4 групп цифр (от 0 до 255), разделенных точками
    String ipPattern = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";

    if (currentIP == null || !currentIP.matches(ipPattern)) {
        System.out.println("SECURITY ALERT: Invalid IP attempt: " + currentIP);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body("Invalid IP address format");
    }

    System.out.println("FRONTEND DATA: IP=" + currentIP + " (Validated)");

    int exitCode = -1;
    try {
        ProcessBuilder processBuilder = new ProcessBuilder("sh", "/home/lysogorand/updateIP.sh", currentIP);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        
        // Читаем лог скрипта
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("SCRIPT OUTPUT: " + line);
        }

        exitCode = process.waitFor();
        
        if (exitCode == 0) {
            return ResponseEntity.ok("IP " + currentIP + " updated successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Script failed with code: " + exitCode);
        }
    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
    }
    }


    @GetMapping("/currentIP")
public ResponseEntity<?> getCurrentIP() {
    Path path = Paths.get("/home/lysogorand/my-vue-test/src/api.js");
    try {
        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            if (line.contains("baseURL")) {
                // Если там ${currentHost}, значит IP динамический
                if (line.contains("${currentHost}")) {
                    return ResponseEntity.ok(Map.of("ip", "Dynamic (Detected by Browser)"));
                }
                // Иначе вырезаем статический IP
                String ip = line.replaceAll(".*http://|:8080.*", "").replace("`", "").replace("'", "").trim();
                return ResponseEntity.ok(Map.of("ip", ip));
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("IP not found");
    } catch (IOException e) {
        return ResponseEntity.status(500).body("Error reading file");
    }
}

}

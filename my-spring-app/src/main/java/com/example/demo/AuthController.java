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

@CrossOrigin(origins = "*") // РАЗРЕШИТЬ ВХОД ДЛЯ VUE
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
}

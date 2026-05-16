package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Map;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;

@RestController
public class HelloController {

    private final VisitRepository repository;

    // Переименуй AUTH_KEY в JWT_SECRET, чтобы совпадало с тем, что внизу
    private final String JWT_SECRET = "my-super-secret-key-that-is-at-least-32-chars";

    public HelloController(VisitRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/hello")
    public ResponseEntity<?> hello(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
             return ResponseEntity.status(401).body("Missing Token");
        }

        String token = bearerToken.substring(7);
        try {
            // Проверяем подпись токена
            Jwts.parserBuilder()
               .setSigningKey(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
               .build()
               .parseClaimsJws(token);

             return ResponseEntity.ok("Доступ разрешен по JWT! Записей: " + repository.count());
         } catch (Exception e) {
             return ResponseEntity.status(401).body("Invalid or Expired Token");
         }
    }
}

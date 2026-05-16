package com.example.demo; // Твой пакет

import org.springframework.stereotype.Component; // ЭТОГО НЕ ХВАТАЛО
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import java.util.Date;

@Component
public class JwtUtils {
    private String JWT_SECRET = "my-super-secret-key-that-is-at-least-32-chars";

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(JWT_SECRET.getBytes())).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

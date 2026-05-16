package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visits")
public class Visit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    private LocalDateTime timestamp;

    // Геттеры и сеттеры (Spring их любит)
    public void setMessage(String message) { this.message = message; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Long getId() { return id; }
}

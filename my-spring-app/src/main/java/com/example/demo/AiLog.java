package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_logs") // Таблица в PostgreSQL создастся автоматически
public class AiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "user_message", length = 2048) // С запасом под длинные промпты
    private String userMessage;

    @Column(name = "ai_response", length = 4096) // Ответы ИИ могут быть объемными
    private String aiResponse;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    // Конструктор по умолчанию (нужен для JPA)
    public AiLog() {}

    // Конструктор для удобного создания записей
    public AiLog(String sessionId, String userMessage, String aiResponse, LocalDateTime timestamp) {
        this.sessionId = sessionId;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.timestamp = timestamp;
    }

    // Геттеры и сеттеры (необходимы для сериализации в JSON и работы Hibernate)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

package com.example.demo;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiLogService {

    private final AiLogRepository aiLogRepository;

    // Внедряем репозиторий через конструктор
    public AiLogService(AiLogRepository aiLogRepository) {
        this.aiLogRepository = aiLogRepository;
    }

    // Сохраняем лог напрямую в базу PostgreSQL
    public void logAction(String sessionId, String userMessage, String aiResponse) {
        AiLog log = new AiLog(sessionId, userMessage, aiResponse, LocalDateTime.now());
        aiLogRepository.save(log);
    }

    // Получаем все логи из базы, отсортированные от новых к старым
    public List<AiLog> getAllLogs() {
        return aiLogRepository.findAllByOrderByIdDesc();
    }
}

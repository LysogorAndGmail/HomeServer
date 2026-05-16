package com.example.demo;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class DataController {

    // Внедряем репозиторий для работы с базой
    private final ApiLogRepository apiLogRepository;

    public DataController(ApiLogRepository apiLogRepository) {
        this.apiLogRepository = apiLogRepository;
    }

    // Старый тестовый метод
    @GetMapping("/records")
    public java.util.Map<String, Object> getProtectedData() {
        java.util.HashMap<String, Object> data = new java.util.HashMap<>();
        data.put("status", "success");
        data.put("message", "Это секретные данные из базы");
        data.put("records", new String[]{"Record 1", "Record 2", "Record 3"});
        return data;
    }

    // НОВЫЙ МЕТОД: Вывод всех логов из базы api_logs
    @GetMapping("/logs")
    public List<ApiLog> getAllLogs() {
        return apiLogRepository.findAll();
    }

    @PostMapping("/logs")
    public ApiLog createLog(@RequestBody ApiLog newLog) {
        // Поле id не указываем, благодаря автоинкременту Postgres сам назначит его.
        // Установим текущее время, если оно не передано с фронтенда
        if (newLog.getCreatedAt() == null) {
            newLog.setCreatedAt(java.time.LocalDateTime.now());
        }
        return apiLogRepository.save(newLog);
    }

}


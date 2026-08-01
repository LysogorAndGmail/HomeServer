package com.example.demo;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LogController {

    private final ApiLogRepository apiLogRepository;

    public LogController(ApiLogRepository apiLogRepository) {
        this.apiLogRepository = apiLogRepository;
    }

    @GetMapping("/logs")
    public List<ApiLog> getAllLogs() {
        return apiLogRepository.findAll();
    }

    @PostMapping("/logs")
    public ApiLog createLog(@RequestBody ApiLog newLog) {
        if (newLog.getCreatedAt() == null) {
            newLog.setCreatedAt(LocalDateTime.now());
        }
        return apiLogRepository.save(newLog);
    }

    @GetMapping("/records")
    public Map<String, Object> getProtectedData() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "success");
        data.put("message", "Это секретные данные из базы");
        data.put("records", new String[]{"Record 1", "Record 2", "Record 3"});
        return data;
    }
}
package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DataController {

    private final ApiLogRepository apiLogRepository;
    private final Model model;

    // Один конструктор для всего
    public DataController(ApiLogRepository apiLogRepository) throws Exception {
        this.apiLogRepository = apiLogRepository;
        // Путь к твоей модели на Mac Mini
        this.model = new Model("/home/lysogorand/my-spring-app/model-ru");
        System.out.println("VOSK: Модель готова!");
    }

    @PostMapping("audio/stream")
    public ResponseEntity<String> streamAudio(InputStream inputStream) {
        try (Recognizer recognizer = new Recognizer(model, 16000f)) {

            byte[] allAudioBytes = inputStream.readAllBytes();

            if (allAudioBytes.length == 0) {
                return ResponseEntity.badRequest().body("Пустой поток данных");
            }

            recognizer.acceptWaveForm(allAudioBytes, allAudioBytes.length);
            String finalJson = recognizer.getFinalResult();
            System.out.println("РЕЗУЛЬТАТ: " + finalJson);

            // ИСПРАВЛЕНО: вызываем у переменной (apiLogRepository), а не у класса
            ApiLog log = new ApiLog();
            log.setProjects("MSI-Final-Check");
            log.setLogUrl(finalJson);
            log.setCreatedAt(LocalDateTime.now());
            apiLogRepository.save(log); 

            return ResponseEntity.ok(finalJson);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
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
}

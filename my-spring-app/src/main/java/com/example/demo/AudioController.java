package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.InputStream;

@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = "*")
public class AudioController {

    private final AudioRecognitionService audioRecognitionService;

    public AudioController(AudioRecognitionService audioRecognitionService) {
        this.audioRecognitionService = audioRecognitionService;
    }

    @PostMapping("/stream")
    public ResponseEntity<String> streamAudio(InputStream inputStream) {
        System.out.println("=== [КОНТРОЛЛЕР] Входящий аудиопоток подключен. Передаем в listenLoop ===");
        try {
            audioRecognitionService.listenLoop(inputStream);
            System.out.println("=== [КОНТРОЛЛЕР] Поток завершен ===");
            return ResponseEntity.ok("{\"status\":\"success\"}");
        } catch (Exception e) {
            System.err.println("=== [КОНТРОЛЛЕР] Ошибка: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
    }
}
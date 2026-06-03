package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate; // ВОТ ЭТОТ ИМПОРТ МЫ ДОБАВИЛИ!
import org.vosk.Model;
import org.vosk.Recognizer;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    @GetMapping("/records")
    public java.util.Map<String, Object> getProtectedData() {
        java.util.HashMap<String, Object> data = new java.util.HashMap<>();
        data.put("status", "success");
        data.put("message", "Это секретные данные из базы");
        data.put("records", new String[]{"Record 1", "Record 2", "Record 3"});
        return data;
    }

    // --- УПРАВЛЕНИЕ ПЛАТОЙ MRIJA ЧЕРЕЗ БЭКЕНД ---

    @PostMapping("/mrija/on")
    public ResponseEntity<String> turnOnMrija() {
        String espUrl = "http://192.168.2.101/led/on";
        try {
            System.out.println("Java: Отправка команды ON на ESP32...");
            URL url = new URL(espUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3000); // 3 секунды таймаут
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                System.out.println("Java: Успешный ответ от ESP32: " + response.toString());
                return ResponseEntity.ok(response.toString());
            } else {
                System.out.println("Java: ESP32 вернула код ошибки: " + responseCode);
                return ResponseEntity.status(500).body("ESP32 вернула код: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Java ОШИБКА: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ошибка связи: " + e.getMessage());
        }
    }

    @PostMapping("/mrija/off")
    public ResponseEntity<String> turnOffMrija() {
        String espUrl = "http://192.168.2.101/led/off";
        try {
            System.out.println("Java: Отправка команды OFF на ESP32...");
            URL url = new URL(espUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                System.out.println("Java: Успешный ответ от ESP32: " + response.toString());
                return ResponseEntity.ok(response.toString());
            } else {
                return ResponseEntity.status(500).body("ESP32 вернула код: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Java ОШИБКА: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ошибка связи: " + e.getMessage());
        }
    }
}

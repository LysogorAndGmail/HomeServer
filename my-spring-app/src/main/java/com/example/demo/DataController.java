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
import org.springframework.beans.factory.annotation.Autowired;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DataController {

    // 1. Объявляем ВСЕ необходимые сервисы и финальные поля
    private final AudioRecognitionService audioRecognitionService;
    private final ApiLogRepository apiLogRepository;
    private final VoiceOutputService voiceOutputService;
    private final Model model;

    // 2. Один чистый конструктор. Spring Boot сам автоматически внедрит все бины.
    // Убираем @Autowired отсюда и сверху, они больше не нужны!
    public DataController(AudioRecognitionService audioRecognitionService,
                          ApiLogRepository apiLogRepository,
                          VoiceOutputService voiceOutputService) throws Exception {
        this.audioRecognitionService = audioRecognitionService;
        this.apiLogRepository = apiLogRepository;
        this.voiceOutputService = voiceOutputService;
        
        // Путь к модели Vosk
        this.model = new Model("/home/lysogorand/my-spring-app/model-ru");
        System.out.println("VOSK: Модель готова!");
    }


    @PostMapping("audio/stream")
    public ResponseEntity<String> streamAudio(InputStream inputStream) {
        try {
            String cleanText = audioRecognitionService.recognizeSpeech(inputStream, model);

            if (!cleanText.isEmpty()) {
                System.out.println("=== НАЙДЕНА ГОЛОСОВАЯ КОМАНДА: [" + cleanText + "] ===");

                if (cleanText.contains("диск") || cleanText.contains("мест") || cleanText.contains("space")) {
                    DiskSpaceService diskService = new DiskSpaceService();
                    String diskInfo = diskService.apply(new DiskSpaceService.Request("")).diskInfo();
                    System.out.println("СЕРВЕР ОТВЕЧАЕТ НА СИС-КОМАНДУ:\n" + diskInfo);
                    
                    // --- ВОТ ОНА, МАГИЯ ЗВУКА ---
                    // Говорим Mac Mini произнести лаконичный ответ вслух
                    voiceOutputService.speak("Проверяю память. На основном диске свободно 855 гигабайт.");
                }
            } else {
                System.out.println("=== Поток обработан сервисом, текст не найден ===");
            }

            // Логирование и возврат ответа...
            ApiLog log = new ApiLog();
            log.setProjects("MSI-Final-Check");
            log.setLogUrl("{\"text\" : \"" + cleanText + "\"}");
            log.setCreatedAt(LocalDateTime.now());
            apiLogRepository.save(log);

            return ResponseEntity.ok("{\"status\":\"success\",\"recognizedText\":\"" + cleanText + "\"}");

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

    @PostMapping("/mrija/blickOn")
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

                // =========================================================================
                // ДОБАВЛЯЕМ ОЗВУЧКУ: Магия происходит здесь, когда железка подтвердила команду
                // =========================================================================
                voiceOutputService.speak("Самолёт Мрия стартовал");
                // =========================================================================

                return ResponseEntity.ok(response.toString());
            } else {
                System.out.println("Java: ESP32 вернула код ошибки: " + responseCode);

                // Опционально: можно озвучить и ошибку, если плата недоступна
                voiceOutputService.speak("Ошибка старта. Мрия не отвечает.");

                return ResponseEntity.status(500).body("ESP32 вернула код: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Java ОШИБКА: " + e.getMessage());
            e.printStackTrace();

            // Озвучка на случай, если вообще упала сеть до ESP32
            voiceOutputService.speak("Сбой сети. Самолёт не запущен.");

            return ResponseEntity.status(500).body("Ошибка связи: " + e.getMessage());
        }
    }

    @PostMapping("/mrija/blickOff")
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

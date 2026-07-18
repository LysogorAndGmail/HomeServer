package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
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

    private final AudioRecognitionService audioRecognitionService;
    private final ApiLogRepository apiLogRepository;
    private final VoiceOutputService voiceOutputService;

    // Один чистый конструктор. Spring Boot сам внедряет все сервисы.
    // Больше никакой повторной инициализации New Model() здесь нет!
    public DataController(AudioRecognitionService audioRecognitionService,
    ApiLogRepository apiLogRepository,
    VoiceOutputService voiceOutputService) {
    this.audioRecognitionService = audioRecognitionService;
    this.apiLogRepository = apiLogRepository;
    this.voiceOutputService = voiceOutputService;
}

@PostMapping("audio/stream")
public ResponseEntity<String> streamAudio(InputStream inputStream) {
    System.out.println("=== [КОНТРОЛЛЕР] Входящий аудиопоток подключен. Передаем в listenLoop ===");
    try {
        // Передаем поток в сервис, у которого внутри уже есть своя рабочая модель Vosk
        audioRecognitionService.listenLoop(inputStream);

        System.out.println("=== [КОНТРОЛЛЕР] Поток завершен ===");
        return ResponseEntity.ok("{\"status\":\"success\"}");
    } catch (Exception e) {
        System.err.println("=== [КОНТРОЛЛЕР] Ошибка: " + e.getMessage());
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
public ResponseEntity<String> turnOnMrija(
    @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka,
@RequestParam(value = "chastota", required = false) Integer chastota,
@RequestParam(value = "interval", required = false) Integer interval
) {
    String espUrl = "http://192.168.2.101/led/on";

    if (chastota != null && interval != null) {
        espUrl += "?chastota=" + chastota + "&interval=" + interval;
        System.out.println("Java: Получены параметры огней -> chastota: " + chastota + ", interval: " + interval);
    }

    try {
        System.out.println("Java: Отправка команды ON на ESP32 по адресу: " + espUrl);
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

            if (ozvuchka != null && ozvuchka == 1) {
                voiceOutputService.speak("Самолёт Мрия стартовал");
            }

            return ResponseEntity.ok(response.toString());
        } else {
            System.out.println("Java: ESP32 вернула код ошибки: " + responseCode);
            voiceOutputService.speak("Ошибка старта. Мрия не отвечает.");
            return ResponseEntity.status(500).body("ESP32 вернула код: " + responseCode);
        }
    } catch (Exception e) {
        System.out.println("Java ОШИБКА: " + e.getMessage());
        e.printStackTrace();
        voiceOutputService.speak("Сбой сети. Самолёт не запущен.");
        return ResponseEntity.status(500).body("Ошибка связи: " + e.getMessage());
    }
}

@PostMapping("/mrija/blickOff")
public ResponseEntity<String> turnOffMrija(
    @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka
) {
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

            if (ozvuchka != null && ozvuchka == 1) {
                voiceOutputService.speak("Самолет завершил полёт");
            }

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

@PostMapping("/mrija/radioOn")
public ResponseEntity<String> turnOnMrijaRadio(
    @RequestParam(value = "volume", required = false) Integer volume,
@RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka
) {
    String espUrl = "http://192.168.2.101/radio/on";

    if (volume != null) {
        espUrl += "?volume=" + volume;
        System.out.println("Java: Получен параметр громкости: " + volume);
    }

    try {
        System.out.println("Java: Отправка команды ON на ESP32 по адресу: " + espUrl);
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

            if (ozvuchka != null && ozvuchka == 1) {
                voiceOutputService.speak("Радио включено");
            }

            return ResponseEntity.ok(response.toString());
        } else {
            System.out.println("Java: ESP32 вернула код ошибки: " + responseCode);
            voiceOutputService.speak("Ошибка старта. Мрия не отвечает.");
            return ResponseEntity.status(500).body("ESP32 вернула код: " + responseCode);
        }
    } catch (Exception e) {
        System.out.println("Java ОШИБКА: " + e.getMessage());
        e.printStackTrace();
        voiceOutputService.speak("Сбой сети. Самолёт не запущен.");
        return ResponseEntity.status(500).body("Ошибка связи: " + e.getMessage());
    }
}

@PostMapping("/mrija/radioOff")
public ResponseEntity<String> turnOffMrijaRadio(
    @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka
) {
    String espUrl = "http://192.168.2.101/radio/off";
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

            if (ozvuchka != null && ozvuchka == 1) {
                voiceOutputService.speak("Радио выключено");
            }

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

@PostMapping("/mrija/cabinOn")
public ResponseEntity<String> turnOnMrijaCabin(
    @RequestParam(value = "cabinBrightness", required = false) Integer cabinBrightness,
    @RequestParam(value = "cabinColor", required = false) String cabinColor,
    @RequestParam(value = "cabinDuration", required = false) Integer cabinDuration,
    @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka
) {
    String espUrl = "http://192.168.2.101/cabin/on";

    //if (cabinBrightness != null) {
        espUrl += "?brightness=" + cabinBrightness+"&color="+cabinColor+"&duration="+cabinDuration;
        System.out.println("Java: Получен параметр: " + cabinBrightness);
    //}

    try {
        System.out.println("Java: Отправка команды ONCabin на ESP32 по адресу: " + espUrl);
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

            if (ozvuchka != null && ozvuchka == 1) {
                voiceOutputService.speak("Cabin включено");
            }

            return ResponseEntity.ok(response.toString());
        } else {
            System.out.println("Java: ESP32 вернула код ошибки: " + responseCode);
            voiceOutputService.speak("Ошибка vkluchenija Cabin.");
            return ResponseEntity.status(500).body("ESP32 вернула код: " + responseCode);
        }
    } catch (Exception e) {
        System.out.println("Java ОШИБКА: " + e.getMessage());
        e.printStackTrace();
        voiceOutputService.speak("Сбой сети. Cabin не запущен.");
        return ResponseEntity.status(500).body("Ошибка связи: " + e.getMessage());
    }
}

@PostMapping("/mrija/cabinOff")
public ResponseEntity<String> turnOffMrijaRadio(
    @RequestParam(value = "cabinDuration", required = false) Integer cabinDuration,
    @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka
) {
    String espUrl = "http://192.168.2.101/cabin/off";

    if (cabinDuration != null) {
        espUrl += "?cabinDuration=" + cabinDuration;
        System.out.println("Java: Получен параметр: " + cabinDuration);
    }
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

            if (ozvuchka != null && ozvuchka == 1) {
                voiceOutputService.speak("Cabine выключено");
            }

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
package com.example.demo;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class AudioRecognitionService {

    private final VoiceOutputService voiceOutputService;
    private final DiskSpaceService diskSpaceService;
    private final WeatherService weatherService; // ДОБАВИЛИ СЕРВИС ПОГОДЫ
    private final Model model;
    private volatile boolean isSpeaking = false;

    // Внедряем WeatherService через конструктор
    public AudioRecognitionService(VoiceOutputService voiceOutputService,
    DiskSpaceService diskSpaceService,
    WeatherService weatherService) throws Exception {
    this.voiceOutputService = voiceOutputService;
    this.diskSpaceService = diskSpaceService;
    this.weatherService = weatherService;
    this.model = new Model("/home/lysogorand/my-spring-app/model-ru");
    System.out.println("VOSK: Модель готова!");
}

public void listenLoop(InputStream audioStream) {
    try (Recognizer recognizer = new Recognizer(this.model, 16000f)) {
        byte[] buffer = new byte[4096];
        int bytesRead;

        System.out.println("=== [VOSK] Бесконечный поток прослушивания запущен ===");

        while ((bytesRead = audioStream.read(buffer)) != -1) {
            if (isSpeaking) {
                continue;
            }

            if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                String resultJson = recognizer.getResult();
                String cleanText = parseVoskText(resultJson);

                if (!cleanText.isEmpty()) {
                    System.out.println("VOSK [Финальная фраза]: " + resultJson);
                    processCommand(cleanText, recognizer, audioStream);
                }
            }
        }
        System.out.println("=== [VOSK] Входной аудиопоток завершился ===");
    } catch (Exception e) {
        System.err.println("Ошибка в цикле прослушивания Vosk: " + e.getMessage());
        e.printStackTrace();
    }
}

private void processCommand(String cleanText, Recognizer recognizer, InputStream audioStream) {
    System.out.println("=== ОБРАБОТКА ТЕКСТА: [" + cleanText + "] ===");

    boolean commandExecuted = false;
    String answerText = "";

    // 1. КОМАНДА: ПРОВЕРКА ДИСКА
    if (cleanText.contains("сколько памяти")) {
        commandExecuted = true;
        try {
            String diskInfo = diskSpaceService.apply(new DiskSpaceService.Request("")).diskInfo();
            String readableSpace = extractAvailableSpace(diskInfo);
            answerText = "Проверяю память. На основном диске свободно " + readableSpace;
        } catch (Exception e) {
            answerText = "Не удалось проверить память.";
        }
    }
    // 2. КОМАНДА: ТЕКУЩЕЕ ВРЕМЯ
    else if (cleanText.contains("сколько время")) {
        commandExecuted = true;
        java.time.ZoneId zoneId = java.time.ZoneId.of("Europe/Berlin");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(zoneId);
        int hour = now.getHour();
        int minute = now.getMinute();
        answerText = "Сейчас " + hour + " " + getHourDeclension(hour) + " " + minute + " " + getMinuteDeclension(minute);
    }
    // 3. НОВАЯ КОМАНДА: ПОГОДА НА УЛИЦЕ
    else if (cleanText.contains("какая погода")) {
        commandExecuted = true;
        System.out.println("Java: Запрос погоды...");
        String tempText = weatherService.getCurrentTemperature();
        answerText = "На улице сейчас " + tempText;
    }

    // Блок выполнения команды и блокировки микрофона
    if (commandExecuted) {
        isSpeaking = true;
        recognizer.reset();

        try {
            voiceOutputService.speak(answerText);
            Thread.sleep(1500);

            int availableBytes = audioStream.available();
            if (availableBytes > 0) {
                byte[] wasteBuffer = new byte[availableBytes];
                audioStream.read(wasteBuffer);
            }

            byte[] silence = new byte[3200];
            recognizer.acceptWaveForm(silence, silence.length);
            recognizer.getResult();

        } catch (Exception e) {
            System.err.println("Ошибка при выполнении голосовой команды: " + e.getMessage());
        } finally {
            recognizer.reset();
            isSpeaking = false;
            System.out.println("=== Голосовой ответ завершен. Vosk снова слушает микрофон ===");
        }
    }
}

private String parseVoskText(String json) {
    if (json == null || !json.contains("\"text\" : \"")) {
        return "";
    }
    try {
        int start = json.indexOf("\"text\" : \"") + 10;
        int end = json.indexOf("\"", start);
        if (start > 9 && end > start) {
            return json.substring(start, end).trim();
        }
    } catch (Exception e) { }
    return "";
}

private String getHourDeclension(int hour) {
    int remainder = hour % 10;
    int remainder100 = hour % 100;
    if (remainder100 >= 11 && remainder100 <= 19) return "часов";
    if (remainder == 1) return "час";
    if (remainder >= 2 && remainder <= 4) return "часа";
    return "часов";
}

private String getMinuteDeclension(int minute) {
    int remainder = minute % 10;
    int remainder100 = minute % 100;
    if (remainder100 >= 11 && remainder100 <= 19) return "минут";
    if (remainder == 1) return "минута";
    if (remainder >= 2 && remainder <= 4) return "минуты";
    return "минут";
}

private String extractAvailableSpace(String dfLine) {
    if (dfLine == null || dfLine.isEmpty() || dfLine.startsWith("Ошибка")) {
        return "неизвестно сколько гигабайт.";
    }
    String[] parts = dfLine.split("\\s+");
    if (parts.length >= 4) {
        String rawSpace = parts[3];
        if (rawSpace.endsWith("G")) return rawSpace.replace("G", "") + " гигабайт.";
        if (rawSpace.endsWith("M")) return rawSpace.replace("M", "") + " мегабайт.";
        if (rawSpace.endsWith("T")) return rawSpace.replace("T", "") + " терабайт.";
        return rawSpace;
    }
    return "не удалось определить.";
}
}
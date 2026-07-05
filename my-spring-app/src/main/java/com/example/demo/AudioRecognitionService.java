package com.example.demo;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class AudioRecognitionService {

    private static final String TRIGGER_WORD = "сервер";
    private final VoiceOutputService voiceOutputService;
    private final DiskSpaceService diskSpaceService;
    private final Model model;
    private volatile boolean isSpeaking = false;

    public AudioRecognitionService(VoiceOutputService voiceOutputService,
    DiskSpaceService diskSpaceService) throws Exception {
    this.voiceOutputService = voiceOutputService;
    this.diskSpaceService = diskSpaceService;
    this.model = new Model("/home/lysogorand/my-spring-app/model-ru");
    System.out.println("VOSK: Модель готова!");
}

public void listenLoop(InputStream audioStream) {
    try (Recognizer recognizer = new Recognizer(this.model, 16000f)) {
        byte[] buffer = new byte[4096];
        int bytesRead;

        System.out.println("=== [VOSK] Бесконечный поток прослушивания запущен ===");

        while ((bytesRead = audioStream.read(buffer)) != -1) {
            // Если сервер говорит, просто сливаем входящий поток микрофона в пустоту
            if (isSpeaking) {
                continue;
            }

            if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                String resultJson = recognizer.getResult();
                String cleanText = parseVoskText(resultJson);

                if (!cleanText.isEmpty()) {
                    System.out.println("VOSK [Финальная фраза]: " + resultJson);
                    // ТЕПЕРЬ ПЕРЕДАЕМ НАШ RECOGNIZER внутрь обработки команды
                    processCommand(cleanText, recognizer);
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

    if (cleanText.contains("диsk") || cleanText.contains("диск") || cleanText.contains("мария")) {
        // 1. Блокируем обработку
        isSpeaking = true;
        recognizer.reset();

        try {
            String diskInfo = diskSpaceService.apply(new DiskSpaceService.Request("")).diskInfo();
            System.out.println("СЕРВЕР ОТВЕЧАЕТ НА СИС-КОМАНДУ:\n" + diskInfo);
            String readableSpace = extractAvailableSpace(diskInfo);

            // 2. Синхронно проговариваем фразу (Java честно ждет здесь окончания aplay)
            voiceOutputService.speak("Проверяю память. На основном диске свободно " + readableSpace);

            // Небольшая пауза, чтобы акустическое эхо в комнате физически затихло
            Thread.sleep(1000);

            // 3. УНИЧТОЖАЕМ ХВОСТЫ БУФЕРА
            // Читаем всё, что прямо сейчас доступно в потоке Java
            int availableBytes = audioStream.available();
            if (availableBytes > 0) {
                byte[] wasteBuffer = new byte[availableBytes];
                audioStream.read(wasteBuffer);
            }

            // 4. ДЕЛАЕМ СЛЕПУЮ ЗОНУ ДЛЯ VOSK
            // Скормим распознавателю пустую тишину (нули), чтобы принудительно
            // закрыть текущую фразу, если туда успел прорваться кусок звука динамика
            byte[] silence = new byte[3200]; // ~0.1 секунды чистой тишины для 16кГц
            recognizer.acceptWaveForm(silence, silence.length);

            // Забираем промежуточный результат, тем самым полностью очищая внутренний стек Vosk
            recognizer.getResult();

        } catch (Exception e) {
            System.err.println("Ошибка при озвучке: " + e.getMessage());
        } finally {
            // 5. Окончательный сброс и открытие микрофона
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
    } catch (Exception e) {
        // Игнорируем
    }
    return "";
}

private String extractAvailableSpace(String dfLine) {
    if (dfLine == null || dfLine.isEmpty() || dfLine.startsWith("Ошибка")) {
        return "неизвестно сколько гигабайт. Произошла ошибка.";
    }

    String[] parts = dfLine.split("\\s+");
    if (parts.length >= 4) {
        String rawSpace = parts[3];
        if (rawSpace.endsWith("G")) {
            return rawSpace.replace("G", "") + " гигабайт.";
        } else if (rawSpace.endsWith("M")) {
            return rawSpace.replace("M", "") + " мегабайт.";
        } else if (rawSpace.endsWith("T")) {
            return rawSpace.replace("T", "") + " терабайт.";
        }
        return rawSpace;
    }
    return "не удалось определить.";
}
}
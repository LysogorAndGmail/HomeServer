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

    // Флаг, определяющий, была ли найдена и выполнена команда
    boolean commandExecuted = false;
    String answerText = "";

    // 1. КОМАНДА: ПРОВЕРКА ДИСКА
    if (cleanText.contains("диск") || cleanText.contains("мария")) {
        commandExecuted = true;
        try {
            String diskInfo = diskSpaceService.apply(new DiskSpaceService.Request("")).diskInfo();
            System.out.println("СЕРВЕР ОТВЕЧАЕТ НА СИС-КОМАНДУ:\n" + diskInfo);
            String readableSpace = extractAvailableSpace(diskInfo);
            answerText = "Проверяю память. На основном диске свободно " + readableSpace;
        } catch (Exception e) {
            answerText = "Не удалось проверить память. Произошла ошибка.";
        }
    }
    // 2. КОМАНДА: ТЕКУЩЕЕ ВРЕМЯ
    // 2. КОМАНДА: ТЕКУЩЕЕ ВРЕМЯ
    else if (cleanText.contains("время") || cleanText.contains("времени") || cleanText.contains("час")) {
        commandExecuted = true;

        // Явно задаем часовой пояс Германии (Берлин/Тюбинген)
        java.time.ZoneId zoneId = java.time.ZoneId.of("Europe/Berlin");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(zoneId);

        int hour = now.getHour();
        int minute = now.getMinute();

        // Формируем человекочитаемый текст с правильными окончаниями
        answerText = "Сейчас " + hour + " " + getHourDeclension(hour) + " " + minute + " " + getMinuteDeclension(minute);
    }

    // Если какая-то команда сработала, запускаем блокировку и озвучку
    if (commandExecuted) {
        isSpeaking = true;
        recognizer.reset();

        try {
            // Озвучиваем сформированный текст
            voiceOutputService.speak(answerText);

            // Пауза, чтобы звук затих в комнате
            Thread.sleep(1500);

            // Очищаем сетевой буфер
            int availableBytes = audioStream.available();
            if (availableBytes > 0) {
                byte[] wasteBuffer = new byte[availableBytes];
                audioStream.read(wasteBuffer);
            }

            // Слепая зона для Vosk (тишина)
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

// Вспомогательный метод для склонения часов
private String getHourDeclension(int hour) {
    int remainder = hour % 10;
    int remainder100 = hour % 100;

    if (remainder100 >= 11 && remainder100 <= 19) {
        return "часов";
    }
    if (remainder == 1) {
        return "час";
    }
    if (remainder >= 2 && remainder <= 4) {
        return "часа";
    }
    return "часов";
}

// Вспомогательный метод для склонения минут
private String getMinuteDeclension(int minute) {
    int remainder = minute % 10;
    int remainder100 = minute % 100;

    if (remainder100 >= 11 && remainder100 <= 19) {
        return "минут";
    }
    if (remainder == 1) {
        return "минута";
    }
    if (remainder >= 2 && remainder <= 4) {
        return "минуты";
    }
    return "минут";
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
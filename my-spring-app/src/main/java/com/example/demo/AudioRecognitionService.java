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

    // --- СТАРЫЙ МЕТОД (ОСТАВЛЯЕМ КАК ЕСТЬ) ---
    public String recognizeSpeech(InputStream inputStream, Model model) throws Exception {
        try (Recognizer recognizer = new Recognizer(model, 16000f)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            StringBuilder resultSentence = new StringBuilder();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String partJson = recognizer.getResult();
                    String partText = parseVoskText(partJson);
                    if (!partText.isEmpty()) {
                        resultSentence.append(partText).append(" ");
                    }
                }
            }

            String finalJson = recognizer.getFinalResult();
            String finalLeftover = parseVoskText(finalJson);
            if (!finalLeftover.isEmpty()) {
                resultSentence.append(finalLeftover);
            }
            return resultSentence.toString().trim();
        }
    }

// 2. Обнови цикл прослушивания listenLoop, чтобы он пропускал обработку, пока мы говорим:
public void listenLoop(InputStream audioStream) {
    try (Recognizer recognizer = new Recognizer(this.model, 16000f)) {
        byte[] buffer = new byte[4096];
        int bytesRead;

        System.out.println("=== [VOSK] Бесконечный поток прослушивания запущен ===");

        while ((bytesRead = audioStream.read(buffer)) != -1) {
            // ЕСЛИ СЕРВЕР СЕЙЧАС ГОВОРЯЩИЙ, просто сливаем входящий поток микрофона в пустоту,
            // чтобы освободить аудиокарту для aplay
            if (isSpeaking) {
                continue;
            }

            if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                String resultJson = recognizer.getResult();
                String cleanText = parseVoskText(resultJson);

                if (!cleanText.isEmpty()) {
                    System.out.println("VOSK [Финальная фраза]: " + resultJson);
                    processCommand(cleanText);
                }
            }
        }
        System.out.println("=== [VOSK] Входной аудиопоток завершился ===");
    } catch (Exception e) {
        System.err.println("Ошибка в цикле прослушивания Vosk: " + e.getMessage());
        e.printStackTrace();
    }
}

// 3. Модифицируй processCommand, чтобы он управлял флагом и засыпал на время речи:
private void processCommand(String cleanText) {
    System.out.println("=== ОБРАБОТКА ТЕКСТА: [" + cleanText + "] ===");

    // Проверяем "диск" или "мария" (Vosk услышал "мария диск" вместо "сервер")
    if (cleanText.contains("диск") || cleanText.contains("мария")) {

        // Включаем режим блокировки микрофона
        isSpeaking = true;

        try {
            String diskInfo = diskSpaceService.apply(new DiskSpaceService.Request("")).diskInfo();
            System.out.println("СЕРВЕР ОТВЕЧАЕТ НА СИС-КОМАНДУ:\n" + diskInfo);
            String readableSpace = extractAvailableSpace(diskInfo);

            // Запускаем озвучку
            voiceOutputService.speak("Проверяю память. На основном диске свободно " + readableSpace);

            // Важно! Нам нужно дать физически договорить плееру aplay.
            // Засыпаем этот поток примерно на 4 секунды (пока играет фраза),
            // в это время listenLoop будет пропускать чтение карты и ALSA освободится!
            Thread.sleep(4000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Выключаем блокировку, Vosk снова начнет слушать
            isSpeaking = false;
            System.out.println("=== Голосовой ответ завершен. Vosk снова слушает микрофон ===");
        }
    }
}

    public interface CommandListener {
        void onCommandReceived(String command);
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
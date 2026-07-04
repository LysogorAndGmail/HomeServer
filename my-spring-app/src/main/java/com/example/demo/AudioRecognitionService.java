package com.example.demo;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class AudioRecognitionService {

    // Твой триггер
    private static final String TRIGGER_WORD = "мрия";

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

    // --- ИСПРАВЛЕННЫЙ МЕТОД ДЛЯ ПОСТОЯННОГО СТРИМА ---
    // Добавили Model model в параметры, чтобы создать Recognizer
    public void listenLoop(InputStream audioStream, Model model) {
        // Создаем рекогнайзер внутри try-with-resources
        try (Recognizer recognizer = new Recognizer(model, 16000f)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            System.out.println("=== [VOSK] Бесконечный поток прослушивания запущен ===");

            while ((bytesRead = audioStream.read(buffer)) != -1) {
                // Исправлено: acceptWaveForm (с большой буквы F)
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String resultJson = recognizer.getResult();
                    System.out.println("VOSK [Финальная фраза]: " + resultJson);

                    // Теперь метод существует и обрабатывает строку
                    processCommand(resultJson);
                } else {
                    String partialJson = recognizer.getPartialResult();
                    if (!partialJson.contains("\"partial\" : \"\"")) {
                        System.out.println("VOSK [Слышу прямо сейчас]: " + partialJson);
                    }
                }
            }
            System.out.println("=== [VOSK] Входной аудиопоток завершился (InputStream closed) ===");
        } catch (Exception e) {
            System.err.println("Ошибка в цикле прослушивания Vosk: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- ДОБАВЛЕННЫЙ МЕТОД ОБРАБОТКИ КОМАНДЫ ---
    private void processCommand(String json) {
        String cleanText = parseVoskText(json);
        if (cleanText.isEmpty()) {
            return;
        }

        System.out.println("=== ОБРАБОТКА ТЕКСТА: [" + cleanText + "] ===");

        // Проверяем наличие триггера "мрия"
        if (cleanText.contains(TRIGGER_WORD)) {
            System.out.println("!!! СРАБОТАЛ ТРИГГЕР МРИЯ !!!");
            // Твоя логика (например, если содержит "диск" -> вызвать DiskSpaceService и т.д.)
        }
    }

    // Вспомогательный интерфейс
    public interface CommandListener {
        void onCommandReceived(String command);
    }

    // Оригинальный парсер JSON
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
            // Игнорируем ошибки сдвига индексов
        }
        return "";
    }
}
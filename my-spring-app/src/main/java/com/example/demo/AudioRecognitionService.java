package com.example.demo;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class AudioRecognitionService {

    // Твой триггер (пиши в нижнем регистре, Vosk выдает текст маленькими буквами)
    private static final String TRIGGER_WORD = "мрия";

    // --- ОСТАВЛЯЕМ ТВОЙ СТАРЫЙ МЕТОД БЕЗ ИЗМЕНЕНИЙ ---
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

    // --- ДОБАВЛЯЕМ НОВЫЙ МЕТОД ДЛЯ ПОСТОЯННОГО СТРИМА ---
    public void listenLoop(InputStream audioStream) {
        try {
            // Убедись, что размер буфера достаточен (например, 4096 байт)
            byte[] buffer = new byte[4096];
            int bytesRead;

            System.out.println("=== [VOSK] Бесконечный поток прослушивания запущен ===");

            while ((bytesRead = audioStream.read(buffer)) != -1) {
                // Если Vosk распознал фразу (пауза в речи)
                if (recognizer.acceptWaveform(buffer, bytesRead)) {
                    String resultJson = recognizer.getResult();
                    System.out.println("VOSK [Финальная фраза]: " + resultJson);

                    // Твоя логика обработки команды (например, "мрия включи...")
                    processCommand(resultJson);
                } else {
                    // Это промежуточные данные, пока человек говорит.
                    // Выводим в лог, чтобы ОДНОЗНАЧНО видеть, что микрофон шлет данные
                    String partialJson = recognizer.getPartialResult();
                    if (!partialJson.contains("\"partial\" : \"\"")) { // не спамим пустыми
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

    // Вспомогательный интерфейс (лямбда-коллбэк)
    public interface CommandListener {
        void onCommandReceived(String command);
    }

    // Твой оригинальный парсер JSON (оставляем как есть)
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
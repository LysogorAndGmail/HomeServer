package com.example.demo;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class AudioRecognitionService {

    // Метод принимает поток и модель, возвращает чистый распознанный текст
    public String recognizeSpeech(InputStream inputStream, Model model) throws Exception {
        try (Recognizer recognizer = new Recognizer(model, 16000f)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            StringBuilder resultSentence = new StringBuilder();

            // 1. Читаем поток порциями
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String partJson = recognizer.getResult();
                    String partText = parseVoskText(partJson);
                    if (!partText.isEmpty()) {
                        resultSentence.append(partText).append(" ");
                    }
                }
            }

            // 2. Забираем остаток в конце потока
            String finalJson = recognizer.getFinalResult();
            System.out.println("Сырой Финальный JSON от Vosk (в сервисе): " + finalJson);
            
            String finalLeftover = parseVoskText(finalJson);
            if (!finalLeftover.isEmpty()) {
                resultSentence.append(finalLeftover);
            }

            return resultSentence.toString().trim();
        }
    }

    // Вспомогательный метод парсинга JSON
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

package com.example.demo;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class AudioRecognitionService {

    // Твой триггер
    private static final String TRIGGER_WORD = "сервер";

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

   // --- ЦИКЛ ПРОСЛУШИВАНИЯ БЕЗ ЛИШНИХ ЛОГОВ ---
       public void listenLoop(InputStream audioStream, Model model) {
           try (Recognizer recognizer = new Recognizer(model, 16000f)) {
               byte[] buffer = new byte[4096];
               int bytesRead;

               System.out.println("=== [VOSK] Бесконечный поток прослушивания запущен ===");

               while ((bytesRead = audioStream.read(buffer)) != -1) {
                   if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                       String resultJson = recognizer.getResult();
                       String cleanText = parseVoskText(resultJson);

                       // Защита от спама: если фраза пустая, игнорируем
                       if (cleanText.isEmpty()) {
                           continue;
                       }

                       System.out.println("VOSK [Финальная фраза]: " + resultJson);
                       processCommand(cleanText);
                   } else {
                       String partialJson = recognizer.getPartialResult();
                       // При желании можно раскомментировать для отладки partial результатов
                   }
               }
               System.out.println("=== [VOSK] Входной аудиопоток завершился (InputStream closed) ===");
           } catch (Exception e) {
               System.err.println("Ошибка в цикле прослушивания Vosk: " + e.getMessage());
               e.printStackTrace();
           }
       }

       // --- ОБРАБОТКА КОМАНДЫ (ПРИНИМАЕТ ЧИСТЫЙ ТЕКСТ) ---
       private void processCommand(String cleanText) {
           System.out.println("=== ОБРАБОТКА ТЕКСТА: [" + cleanText + "] ===");

           // Проверяем триггер "мак"
           if (cleanText.contains(TRIGGER_WORD) || cleanText.contains("mac")) {
                System.out.println("=== ТРИГГЕР [MAC] НАЙДЕН! Воспроизвожу звук Mac Chime... ===");
                playSystemBeep();
           }
       }

       // --- БЕЗОПАСНЫЙ СТРИМ ЗВУКА ЧЕРЕЗ DEFAULT МИКШЕР ---
       private void playSystemBeep() {
           new Thread(() -> {
               try {
                   // Воспроизводим скачанный звук Mac через дефолтный микшер, чтобы не блокировать карту
                   String[] command = {
                       "/bin/sh",
                       "-c",
                       "aplay -D default -q /home/lysogorand/my-spring-app/mac_chime.wav > /dev/null 2>&1"
                   };
                   Process process = Runtime.getRuntime().exec(command);
                   process.waitFor();
               } catch (Exception e) {
                   System.err.println("Не удалось воспроизвести системный звук: " + e.getMessage());
               }
           }).start();
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
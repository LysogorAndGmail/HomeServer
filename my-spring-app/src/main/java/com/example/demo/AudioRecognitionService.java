package com.example.demo;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class AudioRecognitionService {

    // Твой триггер
    private static final String TRIGGER_WORD = "сервер";
    private final VoiceOutputService voiceOutputService;
    private final DiskSpaceService diskSpaceService; // <-- Добавлено
    private final Model model;

    // Внедряем все зависимости через конструктор Spring
    public AudioRecognitionService(VoiceOutputService voiceOutputService,
                                   DiskSpaceService diskSpaceService) throws Exception {
        this.voiceOutputService = voiceOutputService;
        this.diskSpaceService = diskSpaceService; // <-- Инициализируем

        // Путь к модели Vosk
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

    // --- ЦИКЛ ПРОСЛУШИВАНИЯ БЕЗ ЛИШНИХ ЛОГОВ ---
    // Изменили сигнатуру: убрали Model model из параметров, используем поле класса
    public void listenLoop(InputStream audioStream) {
        // Используем здесь this.model
        try (Recognizer recognizer = new Recognizer(this.model, 16000f)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            System.out.println("=== [VOSK] Бесконечный поток прослушивания запущен ===");

            while ((bytesRead = audioStream.read(buffer)) != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String resultJson = recognizer.getResult();
                    String cleanText = parseVoskText(resultJson);

                    if (cleanText.isEmpty()) {
                        continue;
                    }

                    System.out.println("VOSK [Финальная фраза]: " + resultJson);
                    processCommand(cleanText);
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
        playSystemBeep();
        /*
        // Проверяем триггер "сервер"
        if (cleanText.contains(TRIGGER_WORD)) {
            System.out.println("=== ТРИГГЕР [" + TRIGGER_WORD.toUpperCase() + "] НАЙДЕН! Воспроизвожу звук... ===");
            playSystemBeep();

            if (cleanText.contains("диск") || cleanText.contains("мест") || cleanText.contains("space")) {
                // 1. Получаем сырую строку от сервиса
                String diskInfo = diskSpaceService.apply(new DiskSpaceService.Request("")).diskInfo();
                System.out.println("СЕРВЕР ОТВЕЧАЕТ НА СИС-КОМАНДУ:\n" + diskInfo);

                // 2. Выделяем доступное место для озвучки
                String readableSpace = extractAvailableSpace(diskInfo);

                // 3. Передаем реальные данные в голос
                voiceOutputService.speak("Проверяю память. На основном диске свободно " + readableSpace);
            }
        }
        *///test
         if (cleanText.contains("диск")) {
            // 1. Получаем сырую строку от сервиса
            String diskInfo = diskSpaceService.apply(new DiskSpaceService.Request("")).diskInfo();
            System.out.println("СЕРВЕР ОТВЕЧАЕТ НА СИС-КОМАНДУ:\n" + diskInfo);

            // 2. Выделяем доступное место для озвучки
            String readableSpace = extractAvailableSpace(diskInfo);

            // 3. Передаем реальные данные в голос
            voiceOutputService.speak("Проверяю память. На основном диске свободно " + readableSpace);
        }
        // end test
    }

    // --- БЕЗОПАСНЫЙ СТРИМ ЗВУКА ЧЕРЕЗ ВИРТУАЛЬНЫЙ МИКШЕР DMIX ---
        private void playSystemBeep() {
            new Thread(() -> {
                try {
                    // Меняем "-D default" на "-D plug:dmix", чтобы не было ошибки Device or resource busy
                    String[] command = {
                        "/bin/sh",
                        "-c",
                        "aplay -D plug:dmix -q /home/lysogorand/my-spring-app/mac_chime.wav > /dev/null 2>&1"
                    };
                    Process process = Runtime.getRuntime().exec(command);
                    process.waitFor();
                } catch (Exception e) {
                    System.err.println("Не удалось воспроизвести системный звук: " + e.getMessage());
                }
            }).start();
        }

    // Вспомогательный интерфейс (если используется где-то вовне)
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

    private String extractAvailableSpace(String dfLine) {
        if (dfLine == null || dfLine.isEmpty() || dfLine.startsWith("Ошибка")) {
            return "неизвестно сколько гигабайт. Произошла ошибка.";
        }

        // Разбиваем строку по любому количеству пробелов
        String[] parts = dfLine.split("\\s+");

        // В стандартном выводе df -h:
        // parts[0]=Filesystem, parts[1]=Size, parts[2]=Used, parts[3]=Avail
        if (parts.length >= 4) {
            String rawSpace = parts[3]; // Например, "33G", "855G" или "1.2T"

            // Красивое приведение к русской речи
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
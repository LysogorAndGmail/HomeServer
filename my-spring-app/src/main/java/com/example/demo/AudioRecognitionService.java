package com.example.demo;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.springframework.stereotype.Service;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AudioRecognitionService {

    private final VoiceOutputService voiceOutputService;
    private final VoiceOutputServiceEST voiceOutputServiceEST;
    private final DiskSpaceService diskSpaceService;
    private final WeatherService weatherService;

    // ДОБАВИЛИ ДЛЯ РАБОТЫ С AI
    private final OllamaChatClient chatClient;
    private final AiLogService aiLogService;
    private final Map<String, List<Message>> voiceChatHistoryMap = new ConcurrentHashMap<>();

    private final Model model;
    private volatile boolean isSpeaking = false;

    // Внедряем OllamaChatClient и AiLogService через конструктор
    public AudioRecognitionService(VoiceOutputService voiceOutputService,
        VoiceOutputServiceEST voiceOutputServiceEST,
        DiskSpaceService diskSpaceService,
        WeatherService weatherService,
        OllamaChatClient chatClient,
        AiLogService aiLogService) throws Exception {
        this.voiceOutputService = voiceOutputService;
        this.voiceOutputServiceEST = voiceOutputServiceEST;
        this.diskSpaceService = diskSpaceService;
        this.weatherService = weatherService;
        this.chatClient = chatClient;
        this.aiLogService = aiLogService;

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
        boolean EST = false;
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
        // 3. КОМАНДА: ПОГОДА НА УЛИЦЕ
        else if (cleanText.contains("какая погода")) {
            EST = true;
            commandExecuted = true;
            System.out.println("Java: Запрос погоды...");
            String tempText = weatherService.getCurrentTemperature();
            answerText = "На улице сейчас " + tempText;
        }
        // 4. ДЕФОЛТНЫЙ ВАРИАНТ: ЕСЛИ НИ ОДНА КОМАНДА НЕ СРАБОТАЛА — ОТПРАВЛЯЕМ В AI
        else if (cleanText.contains("баба")) {
            commandExecuted = true;
            String sessionId = "voice-user"; // Отдельная сессия для голосового общения

            System.out.println("Java: Передаю запрос нейросети Ollama...");
            try {
                // Подгружаем или создаем историю общения для голоса
                List<Message> history = voiceChatHistoryMap.computeIfAbsent(sessionId, k -> {
                    List<Message> newHistory = new ArrayList<>();
                    String systemInstructions =
                        "Ты — AI-агент домашнего сервера HomeServe на Ubuntu Server. Твой хозяин — lysogorand.\n" +
                        "Отвечай ОЧЕНЬ кратко (1-2 предложения максимум), вежливо и технически точно. Твой ответ будет озвучен голосом.";
                    newHistory.add(new SystemMessage(systemInstructions));
                    return newHistory;
                });

                history.add(new UserMessage(cleanText));

                // Ограничиваем историю (держим последние 5 реплик диалога, чтобы не перегружать контекст)
                if (history.size() > 11) {
                    history.remove(1);
                    history.remove(1);
                }

                Prompt prompt = new Prompt(history);
                String aiResponse = chatClient.call(prompt).getResult().getOutput().getContent().trim();

                // Очищаем ответ от лишних символов (например, звездочек форматирования, которые ИИ любит ставить)
                answerText = aiResponse.replaceAll("[\\*#`]", "");

                // Логируем в историю и в базу данных
                history.add(new AssistantMessage(aiResponse));
                aiLogService.logAction(sessionId, cleanText, aiResponse);

            } catch (Exception e) {
                System.err.println("Ошибка при обращении к Ollama: " + e.getMessage());
                answerText = "Извини, возникла ошибка при обращении к нейросети.";
            }
        }

        // Блок выполнения команды и блокировки микрофона (остался без изменений)
        if (commandExecuted) {
            isSpeaking = true;
            recognizer.reset();

            try {
                if(EST){
                    voiceOutputServiceEST.speak(answerText);
                    EST = false;
                }else{
                    voiceOutputService.speak(answerText);
                }

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
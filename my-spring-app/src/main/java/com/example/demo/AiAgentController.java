package com.example.demo;

import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@CrossOrigin(origins = "*")
public class AiAgentController {

    private final OllamaChatClient chatClient;
    private final AiLogService aiLogService;
    private final Map<String, List<Message>> chatHistoryMap = new ConcurrentHashMap<>();

    // Явный конструктор для внедрения зависимостей
    public AiAgentController(OllamaChatClient chatClient, AiLogService aiLogService) {
        this.chatClient = chatClient;
        this.aiLogService = aiLogService;
    }

    @GetMapping("/api/agent/chat")
    public Map<String, String> chatWithAgent(
            @RequestParam(value = "msg") String msg,
            @RequestParam(value = "id", defaultValue = "default-user") String sessionId) {
        
        // ПРЕД-ПЕРЕХВАТ НА СТОРОНЕ JAVA (Regex): проверяем КОРЕНЬ вопроса пользователя
        // Ищем слова: диск, диске, место, объема, df, space
        String userQuery = msg.toLowerCase();
        if (userQuery.contains("диск") || userQuery.contains("место") || 
            userQuery.contains("объем") || userQuery.contains("space") || userQuery.contains("df")) {
            
            DiskSpaceService diskService = new DiskSpaceService();
            String realDiskData = diskService.apply(new DiskSpaceService.Request("")).diskInfo();
            String clearResponse = "Сработало системное действие [getDiskSpace]. Данные сервера:\n" + realDiskData;
            
            // Записываем чистый системный лог в базу
            aiLogService.logAction(sessionId, msg, clearResponse);
            
            return Map.of("response", clearResponse);
        }

        // Если это обычный разговор — отдаем его модели, убрав из системного промпта упоминания маркеров
        List<Message> history = chatHistoryMap.computeIfAbsent(sessionId, k -> {
            List<Message> newHistory = new ArrayList<>();
            String systemInstructions = 
                "Ты — AI-агент домашнего сервера HomeServe на Ubuntu Server (Mac Mini 2014). Твой хозяин — lysogorand.\n" +
                "Отвечай кратко, вежливо и технически точно.";
            newHistory.add(new SystemMessage(systemInstructions));
            return newHistory;
        });

        history.add(new UserMessage(msg));

        if (history.size() > 11) {
            history.remove(1);
            history.remove(1);
        }

        Prompt prompt = new Prompt(history);
        String aiResponse = chatClient.call(prompt).getResult().getOutput().getContent().trim();

        // Логируем обычный ответ ИИ в базу
        history.add(new AssistantMessage(aiResponse));
        aiLogService.logAction(sessionId, msg, aiResponse);
        
        return Map.of("response", aiResponse);
    }

    // Проверяем этот эндпоинт
    @GetMapping("/api/agent/logs")
    public List<AiLog> getAiLogs() {
        return aiLogService.getAllLogs();
    }
}

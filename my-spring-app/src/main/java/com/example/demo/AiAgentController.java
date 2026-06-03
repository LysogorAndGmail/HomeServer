package com.example.demo;

import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class AiAgentController {

    private final OllamaChatClient chatClient;
    private final Map<String, List<Message>> chatHistoryMap = new ConcurrentHashMap<>();

    public AiAgentController(OllamaChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/api/agent/chat")
    public Map<String, String> chatWithAgent(
            @RequestParam(value = "msg") String msg,
            @RequestParam(value = "id", defaultValue = "default-user") String sessionId) {
        
        List<Message> history = chatHistoryMap.computeIfAbsent(sessionId, k -> {
            List<Message> newHistory = new ArrayList<>();
            String systemInstructions = 
                "Ты — AI-агент домашнего сервера HomeServe на Ubuntu Server (Mac Mini 2014). Твой хозяин — lysogorand.\n" +
                "Отвечай кратко и технически точно.\n" +
                "Если пользователь спрашивает про место на диске, память или характеристики сервера, " +
                "напиши ровно одно слово: ВЫЗОВ_ДИСКА";
            newHistory.add(new SystemMessage(systemInstructions));
            return newHistory;
        });

        history.add(new UserMessage(msg));

        if (history.size() > 11) {
            history.remove(1);
            history.remove(1);
        }

        // 1. Запрос к ИИ
        Prompt prompt = new Prompt(history);
        String aiResponse = chatClient.call(prompt).getResult().getOutput().getContent().trim();

        // 2. Перехват на стороне Java
        if (aiResponse.toUpperCase().contains("ДИСК")) {
            // Вызываем наш Java-сервис напрямую
            DiskSpaceService diskService = new DiskSpaceService();
            String realDiskData = diskService.apply(new DiskSpaceService.Request("")).diskInfo();

            // Вместо повторного обращения к капризной модели, сразу отдаем пользователю сырые системные данные
            String clearResponse = "Сработало системное действие [getDiskSpace]. Данные сервера:\n" + realDiskData;
            
            // Сохраняем это в историю, чтобы ИИ знал, что мы отдали пользователю данные диска
            history.add(new AssistantMessage(clearResponse));
            
            return Map.of("response", clearResponse);
        }

        // Если это был обычный разговор, просто возвращаем ответ ИИ
        history.add(new AssistantMessage(aiResponse));
        return Map.of("response", aiResponse);
    }
}

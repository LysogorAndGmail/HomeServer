package com.example.demo;

import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class VoiceOutputService {

    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        String[] command = {
            "/bin/sh", "-c", 
            "espeak-ng -v ru -s 130 -p 40 \"" + text + "\" --stdout | aplay -D plughw:0,0"
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            pb.inheritIO(); // <--- ВОТ ЭТА СТРОЧКА! Она перенаправит весь вывод и ошибки в консоль джавы
            Process process = pb.start();
            process.waitFor(); 
        } catch (IOException | InterruptedException e) {
            System.err.println("Ошибка голосового вывода: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}

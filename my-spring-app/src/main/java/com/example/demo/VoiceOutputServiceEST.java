package com.example.demo;

import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class VoiceOutputServiceEST {

    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        String[] command = {
            "/bin/sh", "-c",
            "espeak-ng -v ru -s 130 -p 40 -a 200 \"" + text + "\" --stdout | aplay -D plughw:1,0"
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Oshibka golosovogo vyvoda: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
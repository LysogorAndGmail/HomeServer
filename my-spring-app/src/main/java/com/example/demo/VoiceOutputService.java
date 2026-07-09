package com.example.demo;

import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.io.IOException;

@Service
public class VoiceOutputService {

    private static final String PIPER_PATH = "/opt/piper/piper";
    private static final String MODEL_PATH = "/opt/piper/voices/ru_RU-irina-medium.onnx";

    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        Process piperProcess = null;
        Process audioProcess = null;

        try {
            // Piper генерирует raw audio
            ProcessBuilder piperBuilder = new ProcessBuilder(
        		PIPER_PATH,
        		"--model",
        		MODEL_PATH,
        		"--length_scale",
        		"0.8",
        		"--output-raw"
            );

            piperProcess = piperBuilder.start();

            // aplay воспроизводит поток
            ProcessBuilder aplayBuilder = new ProcessBuilder(
                    "aplay",
                    "-r",
                    "22050",
                    "-f",
                    "S16_LE",
                    "-t",
                    "raw"
            );

            audioProcess = aplayBuilder.start();

            // Передаем текст в Piper
            try (OutputStream stdin = piperProcess.getOutputStream()) {
                stdin.write(text.getBytes("UTF-8"));
                stdin.write('\n');
            }

            // Копируем звук Piper -> aplay
            piperProcess.getInputStream()
                    .transferTo(audioProcess.getOutputStream());

            audioProcess.getOutputStream().close();

            piperProcess.waitFor();
            audioProcess.waitFor();

        } catch (IOException | InterruptedException e) {
            System.err.println("Ошибка Piper TTS: " + e.getMessage());
            Thread.currentThread().interrupt();

        } finally {
            if (piperProcess != null && piperProcess.isAlive()) {
                piperProcess.destroy();
            }

            if (audioProcess != null && audioProcess.isAlive()) {
                audioProcess.destroy();
            }
        }
    }
}

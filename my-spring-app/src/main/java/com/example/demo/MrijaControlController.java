package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mrija")
@CrossOrigin(origins = "*")
public class MrijaControlController {

    private final Esp32ClientService esp32Service;
    private final VoiceOutputService voiceOutputService;

    public MrijaControlController(Esp32ClientService esp32Service, VoiceOutputService voiceOutputService) {
        this.esp32Service = esp32Service;
        this.voiceOutputService = voiceOutputService;
    }

    // --- Маяк (Blick) ---
    @PostMapping("/blickOn")
    public ResponseEntity<String> turnOnBlick(
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka,
        @RequestParam(value = "chastota", required = false) Integer chastota,
        @RequestParam(value = "interval", required = false) Integer interval) {

        String path = "/led/on";
        if (chastota != null && interval != null) {
            path += "?chastota=" + chastota + "&interval=" + interval;
        }

        return executeCommand(path, "POST", "Самолёт Мрия стартовал", "Ошибка старта. Мрия не отвечает.", ozvuchka);
    }

    @PostMapping("/blickOff")
    public ResponseEntity<String> turnOffBlick(
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka) {
        return executeCommand("/led/off", "POST", "Самолет завершил полёт", null, ozvuchka);
    }

    // --- Krilija (Blick) ---
    @PostMapping("/krilijaOn")
    public ResponseEntity<String> turnOnKrilija(
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka,
        @RequestParam(value = "krilijaBrightness", required = false) Integer krilijaBrightness) {

        int brightness = (krilijaBrightness != null) ? krilijaBrightness : 150;
        String path = "/krilija/on";
        if (krilijaBrightness != null) {
            path += "?brightness=" + krilijaBrightness;
        }

        return executeCommand(path, "POST", "Krilija ne vkluchilis", "Ошибка старта. Мрия не отвечает.", ozvuchka);
    }

    @PostMapping("/krilijaOff")
    public ResponseEntity<String> turnOffKrilija(
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka) {
        return executeCommand("/krilija/off", "POST", "Krilija viklucheni", null, ozvuchka);
    }

    // --- fuzilash (Blick) ---
    @PostMapping("/fuzilashOn")
    public ResponseEntity<String> turnOnFuzilash(
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka,
        @RequestParam(value = "chastota", required = false) Integer chastota,
        @RequestParam(value = "interval", required = false) Integer interval) {

        String path = "/fuzilash/on";
        if (chastota != null && interval != null) {
            path += "?chastota=" + chastota + "&interval=" + interval;
        }

        return executeCommand(path, "POST", "fuzilash ne vkluchilsia", "Ошибка старта. Мрия не отвечает.", ozvuchka);
    }

    @PostMapping("/fuzilashOff")
    public ResponseEntity<String> turnOffFuzilash(
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka) {
        return executeCommand("/fuzilash/off", "POST", "Fuzilash otkluchen", null, ozvuchka);
    }

    // --- Радио ---
    @PostMapping("/radioOn")
    public ResponseEntity<String> turnOnRadio(
        @RequestParam(value = "volume", required = false) Integer volume,
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka) {

        String path = "/radio/on" + (volume != null ? "?volume=" + volume : "");
        return executeCommand(path, "POST", "Радио включено", "Ошибка включения радио.", ozvuchka);
    }

    @PostMapping("/radioOff")
    public ResponseEntity<String> turnOffRadio(
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka) {
        return executeCommand("/radio/off", "POST", "Радио выключено", null, ozvuchka);
    }

    // --- Кабина ---
    @PostMapping("/cabinOn")
    public ResponseEntity<String> turnOnCabin(
        @RequestParam(value = "cabinBrightness", required = false) Integer cabinBrightness,
        @RequestParam(value = "cabinColor", required = false) String cabinColor,
        @RequestParam(value = "cabinDuration", required = false) Integer cabinDuration,
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka) {

        int brightness = (cabinBrightness != null) ? cabinBrightness : 150;
        int duration = (cabinDuration != null) ? cabinDuration : 0;
        String cleanColor = sanitizeColor(cabinColor);

        String path = String.format("/cabin/on?brightness=%d&color=%s&duration=%d", brightness, cleanColor, duration);
        return executeCommand(path, "GET", "Кабина включена", "Ошибка включения кабины.", ozvuchka);
    }

    @PostMapping("/cabinOff")
    public ResponseEntity<String> turnOffCabin(
        @RequestParam(value = "cabinDuration", required = false) Integer cabinDuration,
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka) {

        String path = "/cabin/off" + (cabinDuration != null ? "?duration=" + cabinDuration : "");
        return executeCommand(path, "POST", "Кабина выключена", null, ozvuchka);
    }

    // --- ДХО ---
    @PostMapping("/dhoOn")
    public ResponseEntity<String> turnOnDHO(
        @RequestParam(value = "dhoBrightness", required = false) Integer dhoBrightness,
        @RequestParam(value = "dhoColor", required = false) String dhoColor,
        @RequestParam(value = "dhoDuration", required = false) Integer dhoDuration,
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka) {

        int brightness = (dhoBrightness != null) ? dhoBrightness : 150;
        int duration = (dhoDuration != null) ? dhoDuration : 0;
        String cleanColor = sanitizeColor(dhoColor);

        String path = String.format("/dho/on?brightness=%d&color=%s&duration=%d", brightness, cleanColor, duration);
        return executeCommand(path, "GET", "DHO включено", "Ошибка включения DHO.", ozvuchka);
    }

    @PostMapping("/dhoOff")
    public ResponseEntity<String> turnOffDHO(
        @RequestParam(value = "dhoDuration", required = false) Integer dhoDuration,
        @RequestParam(value = "ozvuchka", required = false, defaultValue = "1") Integer ozvuchka) {

        String path = "/dho/off" + (dhoDuration != null ? "?duration=" + dhoDuration : "");
        return executeCommand(path, "POST", "DHO выключено", null, ozvuchka);
    }

    // --- Вспомогательные методы ---
    private ResponseEntity<String> executeCommand(String path, String method, String successVoiceMsg, String errorVoiceMsg, Integer ozvuchka) {
        try {
            String response = esp32Service.sendCommand(path, method);
            if (ozvuchka != null && ozvuchka == 1 && successVoiceMsg != null) {
                voiceOutputService.speak(successVoiceMsg);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Java ОШИБКА: " + e.getMessage());
            if (ozvuchka != null && ozvuchka == 1 && errorVoiceMsg != null) {
                voiceOutputService.speak(errorVoiceMsg);
            }
            return ResponseEntity.status(500).body("Ошибка связи: " + e.getMessage());
        }
    }

    private String sanitizeColor(String color) {
        if (color != null && !color.isEmpty()) {
            return color.replace("#", "").replace("%23", "").trim();
        }
        return "FFF59D";
    }
}
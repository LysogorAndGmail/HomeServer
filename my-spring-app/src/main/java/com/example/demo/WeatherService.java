package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WeatherService {

    // Вставь сюда свой API-ключ от OpenWeatherMap
    private static final String API_KEY = "e08d0a28190ccabf76290259d3e6b05d";
    // Указываем твой город (можно написать "Tubingen,DE" или "Dettenhausen,DE")
    private static final String CITY = "Dettenhausen,DE";
    private static final String URL = "https://api.openweathermap.org/data/2.5/weather?q=" + CITY + "&appid=" + API_KEY + "&units=metric&lang=ru";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WeatherService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String getCurrentTemperature() {
        try {
            // Делаем GET-запрос к погодному API
            String response = restTemplate.getForObject(URL, String.class);

            // Парсим JSON ответ
            JsonNode root = objectMapper.readTree(response);

            // 1. Достаем температуру
            double temp = root.path("main").path("temp").asDouble();
            int roundedTemp = (int) Math.round(temp);

            // 2. Достаем описание погоды (пасмурно, дождь и т.д.)
            String description = "";
            JsonNode weatherNode = root.path("weather");
            if (weatherNode.isArray() && !weatherNode.isEmpty()) {
                description = weatherNode.get(0).path("description").asText();
            }

            // 3. Достаем скорость ветра и переводим в понятный текст
            double windSpeed = root.path("wind").path("speed").asDouble();
            String windText = getWindDescription(windSpeed);

            // Собираем всё в одну красивую фразу
            String result = roundedTemp + " " + getTemperatureDeclension(roundedTemp);
            if (!description.isEmpty()) {
                result += ", " + description;
            }
            result += ", " + windText;

            return result;

        } catch (Exception e) {
            System.err.println("Ошибка получения погоды: " + e.getMessage());
            return "неизвестно сколько градусов. Не удалось связаться с метеослужбой.";
        }
    }

    // Вспомогательный метод для правильного склонения слова "градус"
    private String getTemperatureDeclension(int temp) {
        int absTemp = Math.abs(temp);
        int remainder = absTemp % 10;
        int remainder100 = absTemp % 100;

        if (remainder100 >= 11 && remainder100 <= 19) {
        return "градусов";
        }
        if (remainder == 1) {
            return "градус";
        }
        if (remainder >= 2 && remainder <= 4) {
            return "градуса";
        }
        return "градусов";
    }

    private String getWindDescription(double speed) {
        if (speed < 0.3) {
            return "штиль";
        } else if (speed <= 3.3) {
            return "слабый ветер";
        } else if (speed <= 7.9) {
            return "умеренный ветер";
        } else if (speed <= 13.8) {
            return "сильный ветер";
        } else if (speed <= 20.7) {
            return "очень сильный ветер";
        } else {
            return "штормовой ветер";
        }
    }
}
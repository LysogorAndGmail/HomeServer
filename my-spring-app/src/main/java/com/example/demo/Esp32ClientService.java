package com.example.demo;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class Esp32ClientService {

    private final Esp32Properties properties;

    public Esp32ClientService(Esp32Properties properties) {
        this.properties = properties;
    }

    public String sendCommand(String path, String method) throws Exception {
        String fullUrl = properties.getBaseUrl() + path;

        URL url = new URL(fullUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(properties.getConnectTimeout());
        connection.setReadTimeout(properties.getReadTimeout());

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                return response.toString();
            }
        } else {
            throw new RuntimeException("ESP32 вернула код ошибки: " + responseCode);
        }
    }
}
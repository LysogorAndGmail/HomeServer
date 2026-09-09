package com.example.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*", allowedHeaders = "*") // Разрешает кросс-доменные запросы
public class Pm2StatusController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // ... остальной код контроллера без изменений

    // DTO для удобной передачи на Vue-фронтенд
    public record Pm2ProcessDto(
            int id,
            String name,
            String status,
            double cpu,
            String memory,
            long restarts,
            long uptime
    ) {}

    @GetMapping("/pm2-status")
    public ResponseEntity<?> getPm2Status() {
        try {
            // Выполняем команду 'pm2 jlist'
            ProcessBuilder processBuilder = new ProcessBuilder("pm2", "jlist");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Считываем JSON-вывод
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "PM2 process exited with code " + exitCode));
            }

            // Парсим вывод PM2
            List<JsonNode> rawList = objectMapper.readValue(
                    output.toString(), 
                    new TypeReference<List<JsonNode>>() {}
            );

            List<Pm2ProcessDto> resultList = new ArrayList<>();

            for (JsonNode node : rawList) {
                int id = node.path("pm_id").asInt();
                String name = node.path("name").asText("unknown");

                JsonNode pm2Env = node.path("pm2_env");
                String status = pm2Env.path("status").asText("unknown");
                long restarts = pm2Env.path("restart_time").asLong(0);
                long pmUptime = pm2Env.path("pm_uptime").asLong(0);

                // Расчет uptime в секундах
                long uptimeSec = 0;
                if (pmUptime > 0) {
                    uptimeSec = (System.currentTimeMillis() - pmUptime) / 1000;
                }

                JsonNode monit = node.path("monit");
                double cpu = monit.path("cpu").asDouble(0.0);
                long memoryBytes = monit.path("memory").asLong(0);

                // Преобразование байт в читаемый вид (MB)
                String memoryFormatted = String.format("%.1f MB", memoryBytes / (1024.0 * 1024.0));

                resultList.add(new Pm2ProcessDto(
                        id,
                        name,
                        status,
                        cpu,
                        memoryFormatted,
                        restarts,
                        uptimeSec
                ));
            }

            return ResponseEntity.ok(resultList);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve PM2 status: " + e.getMessage()));
        }
    }
}
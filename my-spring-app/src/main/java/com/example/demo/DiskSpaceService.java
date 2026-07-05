package com.example.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class DiskSpaceService implements Function<DiskSpaceService.Request, DiskSpaceService.Response> {

    // Описываем входные параметры для модели (даже если они пустые)
    public record Request(String path) {}
    // Описываем, что вернет функция модели
    public record Response(String diskInfo) {}

    @Override
    public Response apply(Request request) {
        try {
            // Выполняем стандартную команду Linux для проверки места на корневом диске
            String targetPath = (request.path() == null || request.path().isEmpty()) ? "/" : request.path();
            Process process = Runtime.getRuntime().exec("df -h " + targetPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            StringBuilder output = new StringBuilder();
            // Пропускаем заголовок и читаем данные
            reader.readLine(); 
            if ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            return new Response(output.toString().trim());
        } catch (Exception e) {
            return new Response("Ошибка при проверке диска: " + e.getMessage());
        }
    }
}

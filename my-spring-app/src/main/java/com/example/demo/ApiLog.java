package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_logs")
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String projects;

    @Column(name = "log_url", nullable = false)
    private String logUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Геттеры и сеттеры (обязательно для Jackson/Spring)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getProjects() { return projects; }
    public void setProjects(String projects) { this.projects = projects; }

    public String getLogUrl() { return logUrl; }
    public void setLogUrl(String logUrl) { this.logUrl = logUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

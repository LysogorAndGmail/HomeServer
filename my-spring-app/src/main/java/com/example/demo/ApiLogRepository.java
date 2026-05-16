package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiLogRepository extends JpaRepository<ApiLog, Integer> {
    // Здесь автоматически доступны методы: findAll(), save(), deleteById() и т.д.
}

package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiLogRepository extends JpaRepository<AiLog, Long> {
    // Метод для вывода последних логов (чтобы во фронте старые записи уходили вниз)
    List<AiLog> findAllByOrderByIdDesc();
}

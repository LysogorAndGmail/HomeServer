#!/bin/bash

echo "=== 1. Сборка проекта (Maven) ==="
mvn clean package -DskipTests --batch-mode

echo "=== 2. Перезапуск приложения в PM2 ==="
pm2 restart home-spring

echo "=== 3. Ожидание запуска (5 сек) ==="
sleep 5

echo "=== 4. Последние строки лога приложения ==="
tail -n 20 /root/.pm2/logs/home-spring-out.log

echo "=== Готово! ==="

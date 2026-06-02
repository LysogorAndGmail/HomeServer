#!/bin/bash

NEW_IP=$1
FILE_PATH="/home/lysogorand/my-vue-test/src/api.js"

if [ -z "$NEW_IP" ]; then
    echo "Error: No IP provided"
    exit 1
fi

# 1. Меняем IP в конфиге
sed -i "s|baseURL: 'http://[0-9.]*:8080'|baseURL: 'http://$NEW_IP:8080'|g" "$FILE_PATH"

if [ $? -eq 0 ]; then
    echo "IP updated in api.js. Starting full rebuild in background..."

    # Используем nohup и &, чтобы скрипт ушел в фон и не зависел от Java
    # > /dev/null 2>&1 полностью скрывает вывод фонового процесса
    nohup sh /home/lysogorand/my-spring-app/refresh.sh > /dev/null 2>&1 &

    # Даем небольшую паузу (0.5 сек), чтобы Java успела отправить HTTP-ответ
    sleep 1

    echo "ok"
    exit 0
else
    echo "Error: Failed to update api.js"
    exit 1
fi

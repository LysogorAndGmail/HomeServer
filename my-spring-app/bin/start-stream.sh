#!/bin/bash
# Зацикливаем сам ffmpeg внутри скрипта на случай сбоев аудиокарты
while true; do
  ffmpeg -loglevel quiet -nostdin -f alsa -i plughw:2,0 -acodec pcm_s16le -ac 1 -ar 16000 -f s16le - | \
  curl -N -s -H "Content-Type: application/octet-stream" -H "Transfer-Encoding: chunked" -X POST -T - http://127.0.0.1:8080/api/audio/stream

  # Если поток порвался, ждем 2 секунды перед повторной попыткой, чтобы не спамить бэкенд
  sleep 2
done

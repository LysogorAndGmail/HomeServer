import sys
import os
from vosk import Model, KaldiRecognizer
import wave

# Укажи путь к своей модели
model_path = "/home/lysogorand/my-spring-app/model-ru"

if not os.path.exists(model_path):
    print(f"Модель не найдена по адресу {model_path}")
    sys.exit(1)

model = Model(model_path)
# 16000 - частота, которую мы используем
rec = KaldiRecognizer(model, 16000)

# Попробуем скормить ему твой файл (закинь record_out.wav на Mac Mini)
wf = wave.open("perfect.wav", "rb")

while True:
    data = wf.readframes(4000)
    if len(data) == 0:
        break
    if rec.AcceptWaveform(data):
        print(rec.Result())
    else:
        print(rec.PartialResult())

print(rec.FinalResult())

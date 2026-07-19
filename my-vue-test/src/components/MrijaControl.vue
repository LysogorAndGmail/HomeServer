<script setup>
import { ref } from 'vue'
import api from '@/api'

// Пропсы, если нужно передавать состояние извне (например, общую озвучку)
const isOzvuchkaEnabled = ref(true)

// Состояния подсветки кабины
const cabinBrightness = ref(30)
const cabinColor = ref('#f5ce0a')
const cabinDuration = ref(5000)

// Состояния радио
const radioVolume = ref(12)

// Состояния огней
const chastota = ref(11)
const interval = ref(32)

const statusText = ref('')

const CabinOn = async () => {
  try {
    const response = await api.post('/api/mrija/cabinOn', null, {
      params: {
        cabinBrightness: cabinBrightness.value,
        cabinColor: cabinColor.value,
        cabinDuration: cabinDuration.value,
        ozvuchka: isOzvuchkaEnabled.value ? 1 : 0
      }
    })
    statusText.value = `Mrija Ответ: ${response.data}`
  } catch (e) {
    statusText.value = 'Ошибка бэкенда при включении Cabine Mrija'
  }
}

const CabinOff = async () => {
  try {
    statusText.value = 'Отправка команды на otkluchenie cabine...'
    const response = await api.post('/api/mrija/cabinOff', null, {
      params: {
        cabinDuration: cabinDuration.value,
        ozvuchka: isOzvuchkaEnabled.value ? 1 : 0
      }
    })
    statusText.value = `Mrija Ответ: ${response.data}`
  } catch (e) {
    statusText.value = 'Ошибка бэкенда при vikluchenii Cabine Mrija'
  }
}

const RadioOn = async () => {
  try {
    const response = await api.post('/api/mrija/radioOn', null, {
      params: {
        volume: radioVolume.value,
        ozvuchka: isOzvuchkaEnabled.value ? 1 : 0
      }
    })
    statusText.value = `Mrija Ответ: ${response.data}`
  } catch (e) {
    statusText.value = 'Ошибка бэкенда при включении Mrija'
  }
}

const RadioOnVolume = async () => {
  await RadioOn()
}

const RadioOff = async () => {
  try {
    statusText.value = 'Отправка команды на стоп...'
    const response = await api.post('/api/mrija/radioOff', null, {
      params: { ozvuchka: isOzvuchkaEnabled.value ? 1 : 0 }
    })
    statusText.value = `Mrija Ответ: ${response.data}`
  } catch (e) {
    statusText.value = 'Ошибка бэкенда при выключении Mrija'
  }
}

const BlickOn = async () => {
  try {
    const response = await api.post('/api/mrija/blickOn', null, {
      params: {
        ozvuchka: isOzvuchkaEnabled.value ? 1 : 0,
        chastota: chastota.value,
        interval: interval.value
      }
    })
    statusText.value = `Mrija Ответ: ${response.data}`
  } catch (e) {
    statusText.value = 'Ошибка бэкенда при включении Mrija'
  }
}

const BlickOff = async () => {
  try {
    statusText.value = 'Отправка команды на стоп...'
    const response = await api.post('/api/mrija/blickOff', null, {
      params: { ozvuchka: isOzvuchkaEnabled.value ? 1 : 0 }
    })
    statusText.value = `Mrija Ответ: ${response.data}`
  } catch (e) {
    statusText.value = 'Ошибка бэкенда при выключении Mrija'
  }
}
</script>

<template>
  <div class="mrija-control-block">
    <div class="d-flex justify-content-between align-items-center pt-3 pb-2 mb-3 border-bottom">
      <h2 class="h4 mb-0">Панель Mrija</h2>
      <div class="form-check form-switch">
        <input v-model="isOzvuchkaEnabled" class="form-check-input" type="checkbox" id="ozvuchkaSwitch">
        <label class="form-check-label" for="ozvuchkaSwitch">Ozvuchka</label>
      </div>
    </div>

    <!-- Cabin Light -->
    <div class="card p-3 mb-3 shadow-sm">
      <h5>Mrija Cabin Light</h5>
      <div class="d-flex flex-wrap align-items-center gap-3">
        <div class="d-flex align-items-center">
          <label class="me-2 text-nowrap">Яркость: {{ cabinBrightness }}</label>
          <input v-model.number="cabinBrightness" type="range" class="form-range" min="0" max="255" step="5" style="width: 120px;">
        </div>
        <div class="d-flex align-items-center">
          <label class="me-2 text-nowrap">Цвет:</label>
          <input v-model="cabinColor" type="color" class="form-control form-control-color form-control-sm">
        </div>
        <div class="d-flex align-items-center">
          <label class="me-2 text-nowrap">Плавность (мс):</label>
          <input v-model.number="cabinDuration" type="number" class="form-control form-control-sm" min="0" step="500" style="width: 90px;">
        </div>
        <button type="button" class="btn btn-sm btn-success" @click="CabinOn">Включить 💡</button>
        <button type="button" class="btn btn-sm btn-danger" @click="CabinOff">Выключить ✖</button>
      </div>
    </div>

    <!-- Radio -->
    <div class="card p-3 mb-3 shadow-sm">
      <h5>Mrija Radio</h5>
      <div class="d-flex align-items-center gap-3">
        <label class="text-nowrap">Gromkost: {{ radioVolume }}</label>
        <input v-model.number="radioVolume" @change="RadioOnVolume" type="range" class="form-range w-25" min="0" max="21" step="3">
        <button type="button" class="btn btn-sm btn-primary" @click="RadioOn">Start radio 🚀</button>
        <button type="button" class="btn btn-sm btn-info text-white" @click="RadioOff">Stop radio 🛑</button>
      </div>
    </div>

    <!-- Ogni -->
    <div class="card p-3 mb-3 shadow-sm">
      <h5>Mrija Ogni</h5>
      <div class="d-flex flex-wrap align-items-center gap-3">
        <label>Chastota: {{ chastota }}</label>
        <input v-model.number="chastota" @change="BlickOn" type="range" class="form-range" min="1" max="100" style="width: 100px;">

        <label>Interval: {{ interval }}</label>
        <input v-model.number="interval" @change="BlickOn" type="range" class="form-range" min="1" max="100" style="width: 100px;">

        <button type="button" class="btn btn-sm btn-success" @click="BlickOn">Start Blick 🚀</button>
        <button type="button" class="btn btn-sm btn-danger" @click="BlickOff">Stop Blick 🛑</button>
      </div>
    </div>

    <div class="alert alert-info py-2" v-if="statusText">
      {{ statusText }}
    </div>
  </div>
</template>
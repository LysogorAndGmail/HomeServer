<template>
  <div class="pm2-dashboard">
    <h2>Статус сервисов PM2</h2>
    
    <div class="services-grid">
      <div v-for="app in pm2Apps" :key="app.id" class="service-card">
        <div class="card-header">
          <span class="app-name">{{ app.name }}</span>
          <span :class="['status-badge', app.status]">{{ app.status }}</span>
        </div>
        
        <div class="card-body">
          <p><strong>ID:</strong> {{ app.id }}</p>
          <p><strong>CPU:</strong> {{ app.cpu }}%</p>
          <p><strong>RAM:</strong> {{ app.memory }}</p>
          <p><strong>Рестарты:</strong> {{ app.restarts }}</p>
          <p><strong>Uptime:</strong> {{ formatUptime(app.uptime) }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const pm2Apps = ref([]);
let timer = null;

const fetchStatus = async () => {
  try {
    const res = await fetch('/api/system/pm2-status');
    if (res.ok) {
      pm2Apps.value = await res.json();
    }
  } catch (err) {
    console.error('Ошибка загрузки PM2 статуса:', err);
  }
};

// Форматирование секунд в читаемый вид (дны/часы/минуты)
const formatUptime = (seconds) => {
  if (!seconds) return '0s';
  const d = Math.floor(seconds / (3600 * 24));
  const h = Math.floor((seconds % (3600 * 24)) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  
  if (d > 0) return `${d}д ${h}ч`;
  if (h > 0) return `${h}ч ${m}м`;
  return `${m}м`;
};

onMounted(() => {
  fetchStatus();
  timer = setInterval(fetchStatus, 5000); // автообновление каждые 5 сек
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped>
.services-grid { display: flex; gap: 16px; flex-wrap: wrap; }
.service-card { border: 1px solid #ddd; border-radius: 8px; padding: 16px; width: 220px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.app-name { font-weight: bold; }
.status-badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; }
.status-badge.online { background-color: #d4edda; color: #155724; }
.status-badge.stopped { background-color: #f8d7da; color: #721c24; }
</style>
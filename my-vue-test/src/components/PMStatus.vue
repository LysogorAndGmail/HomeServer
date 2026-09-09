<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '@/api'

const pm2Apps = ref([]);
const isLoading = ref(true); // Флаг состояния загрузки
let timer = null;

const fetchStatus = async () => {
  try {
    const res = await api.get('/api/system/pm2-status');
    pm2Apps.value = res.data;
  } catch (err) {
    console.error('Ошибка загрузки PM2 статуса:', err);
  } finally {
    // Выключаем loader после завершения первого запроса
    isLoading.value = false;
  }
};

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
  timer = setInterval(fetchStatus, 5000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<template>
  <div class="pm2-dashboard">
    <h2>Статус сервисов PM2</h2>
    
    <!-- Индикатор загрузки (показывается пока isLoading === true) -->
    <div v-if="isLoading" class="loader-container">
      <div class="spinner"></div>
      <p>Загрузка статуса процессов...</p>
    </div>

    <!-- Список карточек процессов (показывается после загрузки) -->
    <div v-else-if="pm2Apps.length > 0" class="services-grid">
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

    <!-- Запасной вариант, если список пуст -->
    <div v-else class="empty-state">
      Процессы PM2 не найдены или бэкенд недоступен.
    </div>
  </div>
</template>

<style scoped>
.pm2-dashboard {
  padding: 16px;
}

/* Стили для лоадера и спиннера */
.loader-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Стили карточек */
.services-grid { display: flex; gap: 16px; flex-wrap: wrap; }
.service-card { border: 1px solid #ddd; border-radius: 8px; padding: 16px; width: 220px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.app-name { font-weight: bold; }
.status-badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; }
.status-badge.online { background-color: #d4edda; color: #155724; }
.status-badge.stopped { background-color: #f8d7da; color: #721c24; }

.empty-state {
  color: #888;
  font-style: italic;
  padding: 20px 0;
}
</style>
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '@/api'

const pm2Apps = ref([]);
const isLoading = ref(true);
const restartingIds = ref(new Set()); // Набор ID процессов, которые сейчас перезапускаются
let timer = null;

const fetchStatus = async () => {
  try {
    const res = await api.get('/api/system/pm2-status');
    pm2Apps.value = res.data;
  } catch (err) {
    console.error('Ошибка загрузки PM2 статуса:', err);
  } finally {
    isLoading.value = false;
  }
};

// Функция перезапуска процесса по ID
const restartApp = async (id) => {
  if (restartingIds.value.has(id)) return;

  restartingIds.value.add(id);
  try {
    await api.post(`/api/system/pm2-restart/${id}`);
    // Сразу обновляем статус после перезапуска
    await fetchStatus();
  } catch (err) {
    console.error(`Ошибка при перезапуске процесса ${id}:`, err);
    alert(`Не удалось перезапустить процесс ID: ${id}`);
  } finally {
    restartingIds.value.delete(id);
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
    
    <div v-if="isLoading" class="loader-container">
      <div class="spinner"></div>
      <p>Загрузка статуса процессов...</p>
    </div>

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

        <div class="card-footer">
          <button 
            class="btn-restart" 
            :disabled="restartingIds.has(app.id)"
            @click="restartApp(app.id)"
          >
            <span v-if="restartingIds.has(app.id)">Перезапуск...</span>
            <span v-else>↻ Перезапустить</span>
          </button>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      Процессы PM2 не найдены или бэкенд недоступен.
    </div>
  </div>
</template>

<style scoped>
.services-grid { display: flex; gap: 16px; flex-wrap: wrap; }
.service-card { 
  border: 1px solid #ddd; 
  border-radius: 8px; 
  padding: 16px; 
  width: 220px; 
  display: flex; 
  flex-direction: column; 
  justify-content: space-between;
}
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.app-name { font-weight: bold; }
.status-badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; }
.status-badge.online { background-color: #d4edda; color: #155724; }
.status-badge.stopped { background-color: #f8d7da; color: #721c24; }

.card-footer { margin-top: 16px; }
.btn-restart {
  width: 100%;
  padding: 8px 12px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
}
.btn-restart:hover:not(:disabled) { background-color: #0056b3; }
.btn-restart:disabled { background-color: #a0c4ff; cursor: not-allowed; }

.loader-container { display: flex; flex-direction: column; align-items: center; padding: 40px; color: #666; }
.spinner { width: 40px; height: 40px; border: 4px solid #f3f3f3; border-top: 4px solid #3498db; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 12px; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
</style>
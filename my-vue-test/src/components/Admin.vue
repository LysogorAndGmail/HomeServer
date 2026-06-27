<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router' // 1. Импортируем хук роутера
import api from '@/api'
import axios from 'axios' // Импортируем чистый axios для прямых запросов к ESP32
import AdminLayout from './AdminLayout.vue' // Импортируем наш новый шаблон


const router = useRouter() // 2. Инициализируем роутер внутри setup
const recordsText = ref('Загрузка...')
const status = ref('')

const logs = ref([])
const isLogsLoading = ref(true)

const formProject = ref('')
const formUrl = ref('')
const isSubmitting = ref(false)

// Адрес нашей ESP32-S3 в основной сети
const espIp = 'http://192.168.2.101'

const getJavaData = async () => {
  try {
    const response = await api.get('/api/hello');
    recordsText.value = response.data;
  } catch (e) {
    recordsText.value = 'Ошибка авторизации или сервера';
  }
}

const loadData = async () => {
  try {
    const response = await api.get('/api/records');
    recordsText.value = response.data.message;
  } catch (error) {
    if (error.response && error.response.status === 403) {
      status.value = 'Доступ запрещен! Нужно войти заново.';
    }
  }
}

const loadApiLogs = async () => {
  isLogsLoading.value = true;
  try {
    const response = await api.get('/api/logs');
    logs.value = response.data;
  } catch (error) {
    console.error('Ошибка загрузки логов:', error);
  } finally {
    isLogsLoading.value = false;
  }
}

const saveLog = async () => {
  if (!formProject.value || !formUrl.value) {
    alert('Пожалуйста, заполните все поля!');
    return;
  }
  isSubmitting.value = true;
  try {
    await api.post('/api/logs', {
      projects: formProject.value,
      logUrl: formUrl.value
    });
    formProject.value = '';
    formUrl.value = '';
    await loadApiLogs();
  } catch (error) {
    console.error('Не удалось сохранить лог:', error);
    alert('Ошибка при сохранении данных в базу');
  } finally {
    isSubmitting.value = false;
  }
}

const formatDate = (dateString) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleString('ru-RU');
}

// Включение светодиода на ESP32
const BlickOn = async () => {
  try {
    // Шлём запрос на свой бэкенд, а не на плату напрямую
    const response = await api.post('/api/mrija/blickOn');
    recordsText.value = `Mrija Ответ: ${response.data}`;
  } catch (e) {
    recordsText.value = 'Ошибка бэкенда при включении Mrija';
  }
};

// Выключение светодиода на ESP32 (Добавили недостающую функцию)
// Выключение светодиода на ESP32 (ИСПРАВЛЕНО)
const BlickOff = async () => {
  try {
    recordsText.value = 'Отправка команды на стоп...';
    // Теперь шлем строго на Java бэкенд, без прямых IP платы!
    const response = await api.post('/api/mrija/blickOff');
    recordsText.value = `Mrija Ответ: ${response.data}`;
  } catch (e) {
    recordsText.value = 'Ошибка бэкенда при выключении Mrija';
    console.error(e);
  }
};

// Включение светодиода на ESP32
const radioVolume = ref(12)

// 2. Обновляем функцию включения радио
const RadioOn = async () => {
  try {
    // Передаем громкость в params, axios сам превратит это в /api/mrija/radioOn?volume=12
    const response = await api.post('/api/mrija/radioOn', null, {
      params: {
        volume: radioVolume.value
      }
    });
    recordsText.value = `Mrija Ответ: ${response.data}`;
  } catch (e) {
    recordsText.value = 'Ошибка бэкенда при включении Mrija';
  }
};

// Выключение светодиода на ESP32 (Добавили недостающую функцию)
// Выключение светодиода на ESP32 (ИСПРАВЛЕНО)
const RadioOff = async () => {
  try {
    recordsText.value = 'Отправка команды на стоп...';
    // Теперь шлем строго на Java бэкенд, без прямых IP платы!
    const response = await api.post('/api/mrija/radioOff');
    recordsText.value = `Mrija Ответ: ${response.data}`;
  } catch (e) {
    recordsText.value = 'Ошибка бэкенда при выключении Mrija';
    console.error(e);
  }
};



// Состояния для чата
const sessionAndId = ref('vue-dashboard')
const userMessage = ref('')
const lastResponse = ref('')
const loadingChat = ref(false)

// Состояния для таблицы логов
const logsAI = ref([])
const loadingLogs = ref(false)

// Базовый URL вашего Spring Boot бэкенда на Mac Mini
//const API_BASE = 'http://localhost:8080/api/agent'


// 1. Отправка сообщения в чат-агент через ваш api-плагин
const sendMessage = async () => {
  if (!userMessage.value.trim()) return
  
  loadingChat.value = true
  try {
    // Используем относительный путь, так как api сам подставит базовый URL (localhost:8080)
    const response = await api.get('/api/agent/chat', {
      params: {
        id: sessionAndId.value,
        msg: userMessage.value
      }
    });
    
    lastResponse.value = response.data.response;
    userMessage.value = ''; // Очищаем инпут
    await fetchLogs();      // Перезагружаем ИИ-логи
  } catch (err) {
    console.error('Ошибка при отправке запроса:', err)
    lastResponse.value = 'Не удалось связаться с сервером Spring Boot.'
  } finally {
    loadingChat.value = false
  }
}

// 2. Загрузка списка ИИ-логов из БД через Axios
const fetchLogs = async () => {
  loadingLogs.value = true
  try {
    const response = await api.get('/api/agent/logs');
    // В Axios данные лежат напрямую в .data
    logsAI.value = response.data; 
  } catch (err) {
    console.error('Ошибка получения логов:', err)
  } finally {
    loadingLogs.value = false
  }
}


// Форматирование даты ISO в нормальный вид
const formatTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  return date.toLocaleTimeString() + ' ' + date.toLocaleDateString()
}



onMounted(() => {
  //getJavaData(); for now 
  loadData();
  loadApiLogs();
  //AI logs
  fetchLogs()
})
</script>

<template>
  <!-- Оборачиваем всю страницу в шаблон структуры -->
    
    <!-- Передаем контент дашборда внутрь слота -->
    <main class="col-md-9 ms-sm-auto col-lg-10 px-md-4">
      <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
        <h1 class="h2">Dashboard</h1>
        <div class="btn-toolbar mb-2 mb-md-0">
          <button type="button" class="btn btn-sm btn-outline-secondary" @click="loadApiLogs">
            Обновить таблицу
          </button>
        </div>
      </div>
      <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
        <h1 class="h2">Mrija</h1>
        <div class="btn-toolbar mb-2 mb-md-0">
          <label for="customRange3" class="form-label">Gromkost: {{ radioVolume }}</label>
          <input
              v-model.number="radioVolume"
              @change="RadioOn"
              type="range"
              class="form-range radio_value"
              min="0"
              max="21"
              step="3"
              id="customRange3"
          >
          <p>&nbsp;</p>
          <button type="button" class="btn btn-sm ms-2 btn-primary" @click="RadioOn">
            Start radio 🚀
          </button>
          <button type="button" class="btn btn-sm ms-2 btn-info" @click="RadioOff">
            Stop radio 🛑
          </button>
          <button type="button" class="btn btn-sm ms-2 btn-success" @click="BlickOn">
            Start Blick 🚀
          </button>
          <button type="button" class="btn btn-sm ms-2 btn-danger" @click="BlickOff">
            Stop Blick 🛑
          </button>
        </div>
      </div>

      <div class="alert alert-info" v-if="recordsText || status">
        {{ status || recordsText }}
      </div>

      <!-- Форма создания нового лога -->
      <div class="card mb-4 shadow-sm">
        <div class="card-header bg-secondary text-white">
          <h5 class="card-title mb-0">Добавить тестовый лог</h5>
        </div>
        <div class="card-body">
          <form @submit.prevent="saveLog" class="row g-3">
            <div class="col-md-5">
              <label class="form-label">Название проекта</label>
              <input v-model="formProject" type="text" class="form-control" placeholder="Например: Mrija" required>
            </div>
            <div class="col-md-5">
              <label class="form-label">URL маршрута (Route URL)</label>
              <input v-model="formUrl" type="text" class="form-control" placeholder="Например: /api/v1/users" required>
            </div>
            <div class="col-md-2 d-flex align-items-end">
              <button type="submit" class="btn btn-primary w-100" :disabled="isSubmitting">
                {{ isSubmitting ? 'Сохранение...' : 'Отправить в БД' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Секция таблицы -->
      <h2>Логи из PostgreSQL (api_logs)</h2>
      <div class="table-responsive mt-3">
        <div v-if="isLogsLoading" class="text-center p-3">
          <div class="spinner-border text-primary" role="status"></div>
          <p>Загрузка логов...</p>
        </div>
        
        <table v-else class="table table-striped table-sm table-hover border">
          <thead class="table-dark">
            <tr>
              <th scope="col">ID</th>
              <th scope="col">Проект</th>
              <th scope="col">URL маршрута</th>
              <th scope="col">Дата создания</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td>{{ log.id }}</td>
              <td><strong>{{ log.projects }}</strong></td>
              <td><code>{{ log.logUrl }}</code></td>
              <td>{{ formatDate(log.createdAt) }}</td>
            </tr>
            <tr v-if="logs.length === 0">
              <td colspan="4" class="text-center text-muted p-3">Таблица api_logs пуста</td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>
    
    <div class="ai-manager">
    <h1>Панель управления AI-Агентом</h1>
    
    <div class="card chat-card">
      <h2><span class="icon">💬</span> Консоль чата с агентом</h2>
      <div class="chat-form">
        <div class="input-group">
          <input 
            v-model="sessionAndId" 
            type="text" 
            placeholder="ID сессии (например: vue-test)" 
            class="input-session"
          />
          <input 
            v-model="userMessage" 
            @keyup.enter="sendMessage"
            type="text" 
            placeholder="Введите запрос к серверу (например: привет или покажи инфу по диску)..." 
            class="input-msg"
            :disabled="loadingChat"
          />
          <button @click="sendMessage" :disabled="loadingChat" class="btn-send">
            {{ loadingChat ? 'Думает...' : 'Отправить' }}
          </button>
        </div>
      </div>
      
      <div v-if="lastResponse" class="chat-response">
        <strong>Ответ сервера:</strong>
        <pre>{{ lastResponse }}</pre>
      </div>
    </div>

    <div class="card logs-card">
      <div class="logs-header">
        <h2><span class="icon">📊</span> История логов из PostgreSQL</h2>
        <button @click="fetchLogs" :disabled="loadingLogs" class="btn-refresh">
          {{ loadingLogs ? 'Обновление...' : 'Обновить логи' }}
        </button>
      </div>

      <div v-if="logsAI.length === 0" class="no-data">
        Логов в базе данных пока нет. Отправьте первое сообщение выше!
      </div>

      <div v-else class="table-responsive">
        <table class="logs-table">
          <thead>
            <tr>
              <th style="width: 60px;">ID</th>
              <th style="width: 180px;">Время (Локальное)</th>
              <th style="width: 130px;">Сессия</th>
              <th>Запрос пользователя</th>
              <th>Ответ системы / ИИ</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logsAI" :key="log.id">
              <td><span class="id-badge">{{ log.id }}</span></td>
              <td class="time-cell">{{ formatTime(log.timestamp) }}</td>
              <td><span class="session-badge">{{ log.sessionId }}</span></td>
              <td class="text-bold">{{ log.userMessage }}</td>
              <td class="response-cell">
                <pre>{{ log.aiResponse }}</pre>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

</template>


<style scoped>
.ai-manager {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: #2c3e50;
  background-color: #f8f9fa;
  min-height: 100vh;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  font-weight: 600;
}

.card {
  background: #ffffff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06);
}

h2 {
  font-size: 1.25rem;
  margin-top: 0;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.icon {
  margin-right: 10px;
}

/* Стили формы чата */
.chat-form .input-group {
  display: flex;
  gap: 10px;
}

.input-session {
  width: 15%;
  padding: 10px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
}

.input-msg {
  flex-grow: 1;
  padding: 10px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
}

.btn-send, .btn-refresh {
  background-color: #42b983;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-send:hover, .btn-refresh:hover {
  background-color: #3aa876;
}

.btn-send:disabled, .btn-refresh:disabled {
  background-color: #a8ebd0;
  cursor: not-allowed;
}

.chat-response {
  margin-top: 20px;
  background: #f1f3f5;
  padding: 15px;
  border-radius: 6px;
  border-left: 4px solid #42b983;
}

.chat-response pre {
  margin: 10px 0 0 0;
  white-space: pre-wrap;
  font-family: monospace;
  font-size: 14px;
}

/* Стили таблицы логов */
.logs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.logs-header h2 {
  margin-bottom: 0;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.logs-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 14px;
}

.logs-table th {
  background-color: #f1f3f5;
  padding: 12px;
  font-weight: 600;
  border-bottom: 2px solid #dee2e6;
}

.logs-table td {
  padding: 12px;
  border-bottom: 1px solid #dee2e6;
  vertical-align: top;
}

.id-badge {
  background: #6c757d;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.session-badge {
  background: #e3fafc;
  color: #0b7285;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.time-cell {
  color: #495057;
  font-size: 13px;
  white-space: nowrap;
}

.text-bold {
  font-weight: 500;
}

.response-cell pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: 'Courier New', Courier, monospace;
  background: #f8f9fa;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #e9ecef;
  max-height: 200px;
  overflow-y: auto;
}

.no-data {
  text-align: center;
  color: #868e96;
  padding: 40px 0;
}
</style>

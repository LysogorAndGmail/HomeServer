<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router' // 1. Импортируем хук роутера
import api from '@/api'
import AdminLayout from './AdminLayout.vue' // Импортируем наш новый шаблон

const router = useRouter() // 2. Инициализируем роутер внутри setup
const recordsText = ref('Загрузка...')
const status = ref('')

const logs = ref([])
const isLogsLoading = ref(true)

const formProject = ref('')
const formUrl = ref('')
const isSubmitting = ref(false)

const getJavaData = async () => {
  try {
    const response = await api.get('/api/hello');
    recordsText.value = response.data;
  } catch (e) {
    recordsText.value = 'Ошибка авторизации или сервера';
  }
};

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

onMounted(() => {
  //getJavaData(); for now 
  loadData();
  loadApiLogs();
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
          <button type="button" class="btn btn-sm ms-2 btn-outline-danger" @click="loadApiLogs">
            Send request to ESP 32
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

</template>

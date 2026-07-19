<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api'

const recordsText = ref('Загрузка...')
const status = ref('')
const logs = ref([])
const isLogsLoading = ref(true)

const formProject = ref('')
const formUrl = ref('')
const isSubmitting = ref(false)

const loadData = async () => {
  try {
    const response = await api.get('/api/records')
    recordsText.value = response.data.message
  } catch (error) {
    if (error.response && error.response.status === 403) {
      status.value = 'Доступ запрещен! Нужно войти заново.'
    }
  }
}

const loadApiLogs = async () => {
  isLogsLoading.value = true
  try {
    const response = await api.get('/api/logs')
    logs.value = response.data
  } catch (error) {
    console.error('Ошибка загрузки логов:', error)
  } finally {
    isLogsLoading.value = false
  }
}

const saveLog = async () => {
  if (!formProject.value || !formUrl.value) {
    alert('Пожалуйста, заполните все поля!')
    return
  }
  isSubmitting.value = true
  try {
    await api.post('/api/logs', {
      projects: formProject.value,
      logUrl: formUrl.value
    })
    formProject.value = ''
    formUrl.value = ''
    await loadApiLogs()
  } catch (error) {
    console.error('Не удалось сохранить лог:', error)
    alert('Ошибка при сохранении данных в базу')
  } finally {
    isSubmitting.value = false
  }
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('ru-RU')
}

onMounted(() => {
  loadData()
  loadApiLogs()
})
</script>

<template>
  <div class="api-logs-manager mt-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h2>Логи из PostgreSQL (api_logs)</h2>
      <button type="button" class="btn btn-sm btn-outline-secondary" @click="loadApiLogs">
        Обновить таблицу
      </button>
    </div>

    <div class="alert alert-warning py-2" v-if="status">
      {{ status }}
    </div>

    <!-- Форма создания нового лога -->
    <div class="card mb-4 shadow-sm">
      <div class="card-header bg-secondary text-white py-2">
        <h5 class="card-title mb-0 fs-6">Добавить тестовый лог</h5>
      </div>
      <div class="card-body py-3">
        <form @submit.prevent="saveLog" class="row g-3">
          <div class="col-md-5">
            <label class="form-label small mb-1">Название проекта</label>
            <input v-model="formProject" type="text" class="form-control form-control-sm" placeholder="Mrija" required>
          </div>
          <div class="col-md-5">
            <label class="form-label small mb-1">URL маршрута (Route URL)</label>
            <input v-model="formUrl" type="text" class="form-control form-control-sm" placeholder="/api/v1/users" required>
          </div>
          <div class="col-md-2 d-flex align-items-end">
            <button type="submit" class="btn btn-primary btn-sm w-100" :disabled="isSubmitting">
              {{ isSubmitting ? 'Сохранение...' : 'В БД' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Таблица -->
    <div class="table-responsive">
      <div v-if="isLogsLoading" class="text-center p-3">
        <div class="spinner-border text-primary spinner-border-sm" role="status"></div>
        <p class="small mt-1">Загрузка логов...</p>
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
          <td colspan="4" class="text-center text-muted p-2">Таблица api_logs пуста</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
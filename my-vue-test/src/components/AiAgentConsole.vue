<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api'

const sessionAndId = ref('vue-dashboard')
const userMessage = ref('')
const lastResponse = ref('')
const loadingChat = ref(false)

const logsAI = ref([])
const loadingLogs = ref(false)

const sendMessage = async () => {
  if (!userMessage.value.trim()) return

  loadingChat.value = true
  try {
    const response = await api.get('/api/agent/chat', {
      params: {
        id: sessionAndId.value,
        msg: userMessage.value
      }
    })
    lastResponse.value = response.data.response
    userMessage.value = ''
    await fetchLogs()
  } catch (err) {
    console.error('Ошибка при отправке запроса:', err)
    lastResponse.value = 'Не удалось связаться с сервером Spring Boot.'
  } finally {
    loadingChat.value = false
  }
}

const fetchLogs = async () => {
  loadingLogs.value = true
  try {
    const response = await api.get('/api/agent/logs')
    logsAI.value = response.data
  } catch (err) {
    console.error('Ошибка получения логов:', err)
  } finally {
    loadingLogs.value = false
  }
}

const formatTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  return date.toLocaleTimeString() + ' ' + date.toLocaleDateString()
}

onMounted(() => {
  fetchLogs()
})
</script>

<template>
  <div class="ai-manager mt-4 border-top pt-4">
    <h3>Панель управления AI-Агентом</h3>

    <div class="card chat-card mb-4 shadow-sm p-3">
      <h4 class="fs-6"><span class="icon">💬</span> Консоль чата с агентом</h4>
      <div class="chat-form mt-2">
        <div class="d-flex gap-2">
          <input v-model="sessionAndId" type="text" placeholder="ID сессии" class="form-control form-control-sm w-25"/>
          <input v-model="userMessage" @keyup.enter="sendMessage" type="text" placeholder="Введите запрос к серверу..." class="form-control form-control-sm flex-grow-1" :disabled="loadingChat"/>
          <button @click="sendMessage" :disabled="loadingChat" class="btn btn-sm btn-success text-nowrap">
            {{ loadingChat ? 'Думает...' : 'Отправить' }}
          </button>
        </div>
      </div>

      <div v-if="lastResponse" class="chat-response mt-3 p-2 bg-light border-start border-4 border-success">
        <strong class="small">Ответ сервера:</strong>
        <pre class="mb-0 small bg-dark text-light p-2 rounded mt-1 overflow-auto">{{ lastResponse }}</pre>
      </div>
    </div>

    <div class="card logs-card shadow-sm p-3">
      <div class="d-flex justify-content-between align-items-center mb-2">
        <h4 class="fs-6 mb-0"><span class="icon">📊</span> История логов AI</h4>
        <button @click="fetchLogs" :disabled="loadingLogs" class="btn btn-sm btn-outline-primary">
          {{ loadingLogs ? '...' : 'Обновить' }}
        </button>
      </div>

      <div v-if="logsAI.length === 0" class="text-center text-muted py-3 small">
        Логов AI в базе данных пока нет.
      </div>

      <div v-else class="table-responsive">
        <table class="table table-sm table-hover table-bordered small align-middle">
          <thead class="table-light">
          <tr>
            <th style="width: 50px;">ID</th>
            <th style="width: 150px;">Время</th>
            <th style="width: 110px;">Сессия</th>
            <th>Запрос</th>
            <th>Ответ ИИ</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="log in logsAI" :key="log.id">
            <td><span class="badge bg-secondary">{{ log.id }}</span></td>
            <td class="text-muted">{{ formatTime(log.timestamp) }}</td>
            <td><span class="badge bg-info text-dark">{{ log.sessionId }}</span></td>
            <td class="fw-semibold">{{ log.userMessage }}</td>
            <td>
              <pre class="mb-0 p-1 bg-light border custom-pre text-wrap" style="max-height: 120px; font-size: 11px;">{{ log.aiResponse }}</pre>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-pre {
  white-space: pre-wrap;
  word-break: break-all;
  overflow-y: auto;
}
</style>
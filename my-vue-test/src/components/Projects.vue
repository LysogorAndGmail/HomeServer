<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api'

const projects = ref([])
const isLogsLoading = ref(true)

const formProject = ref('')
const isSubmitting = ref(false)

// Загрузка проектов (сейчас идет из /api/logs, при необходимости поменяйте урл)
const loadProjects = async () => {
  isLogsLoading.value = true
  try {
    const response = await api.get('/api/logs')
    projects.value = response.data
  } catch (error) {
    console.error('Ошибка загрузки проектов:', error)
  } finally {
    isLogsLoading.value = false
  }
}

// ДОБАВИЛИ НЕДОСТАЮЩУЮ ФУНКЦИЮ, ИЗ-ЗА КОТОРОЙ ВСЁ ПАДАЛО
const saveProject = async () => {
  if (!formProject.value.trim()) {
    alert('Пожалуйста, заполните название проекта!')
    return
  }
  isSubmitting.value = true
  try {
    // Шлём запрос на создание проекта (настройте объект под ваш бэкенд)
    await api.post('/api/logs', {
      projects: formProject.value,
      logUrl: '/project-auto-route' // Заглушка, если бэкенд требует это поле
    })
    formProject.value = ''
    await loadProjects() // Перезагружаем список
  } catch (error) {
    console.error('Не удалось сохранить проект:', error)
    alert('Ошибка при сохранении проекта')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  loadProjects()
})
</script>

<template>
  <!-- Убрали лишний <main class="col-md-9...">, оставили простую обертку -->
  <div class="projects-page">
    <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
      <h1 class="h2">Projects</h1>
    </div>

    <!-- Форма создания нового проекта (исправлен вызов на saveProject) -->
    <div class="card mb-4 shadow-sm">
      <div class="card-header bg-secondary text-white">
        <h5 class="card-title mb-0">Add new project</h5>
      </div>
      <div class="card-body">
        <form @submit.prevent="saveProject" class="row g-3">
          <div class="col-md-5">
            <label class="form-label">Name</label>
            <input v-model="formProject" type="text" class="form-control" placeholder="Например: Mrija" required>
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
    <h2>All projects</h2>
    <div class="table-responsive mt-3">
      <div v-if="isLogsLoading" class="text-center p-3">
        <div class="spinner-border text-primary" role="status"></div>
        <p>Load...</p>
      </div>

      <table v-else class="table table-striped table-sm table-hover border">
        <thead class="table-dark">
        <tr>
          <th scope="col">ID</th>
          <th scope="col">Проект</th>
          <th scope="col">Created</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="project in projects" :key="project.id">
          <td>{{ project.id }}</td>
          <!-- Исправил вывод: выводим имя проекта, а не три раза ID -->
          <td><strong>{{ project.projects || 'Без имени' }}</strong></td>
          <td>{{ project.createdAt ? new Date(project.createdAt).toLocaleDateString('ru-RU') : '-' }}</td>
        </tr>
        <tr v-if="projects.length === 0">
          <td colspan="3" class="text-center text-muted p-3">Таблица проектов пуста</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
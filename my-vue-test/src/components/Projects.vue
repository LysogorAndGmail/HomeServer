<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import AdminLayout from './AdminLayout.vue'

const router = useRouter()
const projects = ref([])

const recordsText = ref('Загрузка...')
const status = ref('')

const logs = ref([])
const isLogsLoading = ref(true)

const formProject = ref('')
const formUrl = ref('')
const isSubmitting = ref(false)


const loadProjects = async () => {
  isLogsLoading.value = true;
 console.error('start загрузки projects:')
  try {
    const response = await api.get('/api/logs');
    projects.value = response.data;
  } catch (error) {
    console.error('Ошибка загрузки логов:', error);
  } finally {
    isLogsLoading.value = false;
  }
}

onMounted(() => {
 loadProjects();
})
</script>

<template>
  <!-- Оборачиваем всю страницу в шаблон структуры -->
    
    <!-- Передаем контент дашборда внутрь слота -->
    <main class="col-md-9 ms-sm-auto col-lg-10 px-md-4">
      <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
        <h1 class="h2">Projects</h1>
        <div class="btn-toolbar mb-2 mb-md-0">
        </div>
      </div>

      <!-- Форма создания нового projects -->
      <div class="card mb-4 shadow-sm">
        <div class="card-header bg-secondary text-white">
          <h5 class="card-title mb-0">Add new project</h5>
        </div>
        <div class="card-body">
          <form @submit.prevent="saveLog" class="row g-3">
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
              <td>{{ project.id }} </td>
              <td>{{ project.id }} </td>
              <td>{{ project.id }} </td>
            </tr>
            <tr v-if="projects.length === 0">
              <td colspan="4" class="text-center text-muted p-3">Таблица api_project пуста</td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

</template>

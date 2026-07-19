<script setup>
import AdminLayout from './AdminLayout.vue'
import MrijaControl from './components/MrijaControl.vue'
import ApiLogsManager from '@/components/ApiLogsManager.vue'
import AiAgentConsole from '@/components/AiAgentConsole.vue'
</script>

<template>
  <AdminLayout>
    <main class="col-md-9 ms-sm-auto col-lg-10 px-md-4 pb-5">
      <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
        <h1 class="h2">Dashboard</h1>
      </div>

      <!-- Блок управления железками Mrija (ESP32) -->
      <MrijaControl />

      <!-- Блок логов стандартного API из Postgres -->
      <ApiLogsManager />

      <!-- Блок интеграции с AI-Агентом -->
      <AiAgentConsole />
    </main>
  </AdminLayout>
</template>
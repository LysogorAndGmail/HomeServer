<script setup>
import { onMounted } from 'vue'
import '../../public/dashboard/dashboard.css'
import Header from './Header.vue'
import Sidebar from './Sidebar.vue'

// Принимаем метод логаута как пропсы, чтобы передать его в Header
defineProps({
  onLogout: {
    type: Function,
    required: true
  }
})
</script>

<template>
  <!-- 1. Шапка (Header) -->
  <Header @onLogout="onLogout" />

  <!-- 2. Общая структура разметки -->
  <div class="container-fluid">
    <div class="row">
      
      <!-- Боковое меню (Sidebar) -->
       <Sidebar />

      <!-- ТЕГ <slot />: Сюда Vue автоматически подставит контент из Admin.vue -->
      <slot />

    </div>
  </div>
</template>

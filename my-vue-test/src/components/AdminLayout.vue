<script setup>
import { onMounted } from 'vue'
import '../../public/dashboard/dashboard.css'
import { useRouter } from 'vue-router' // 1. Импортируем роутер
import Header from './Header.vue'
import Sidebar from './Sidebar.vue'

const router = useRouter() // 2. Инициализируем его

// 3. Функция логаута теперь живёт прямо здесь!
const handleHeaderLogout = () => {
  localStorage.removeItem('token') // Удаляем токен
  router.push('/login')            // Редиректим на логин
}
</script>

<template>
  <Header @onLogout="handleHeaderLogout" />

  <div class="container-fluid">
    <div class="row">

      <Sidebar />

      <router-view />

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import routes from '@/routes/route' // Теперь это сработает

const currentPath = ref(window.location.hash)

window.addEventListener('hashchange', () => {
  currentPath.value = window.location.hash
})

const currentView = computed(() => {
  const path = currentPath.value.slice(1) || '/'
  const token = localStorage.getItem('token') // Должно совпадать с Login.vue!
  
  // Если токена нет - показываем логин для ЛЮБОГО пути
  
  if (!token) {
    if (path !== '/login') {
        window.location.hash = '#/login'
    }
    return routes['/login']
    }

  // Если токен есть, но мы на логине - кидаем на главную
  if (token && path === '/login') {
    window.location.hash = '#/'
    return routes['/']
  }
  
  // Если токен есть - показываем то что просят, или главную если путь неизвестен
  return routes[path] || routes['/']
})

// Метод для выхода
const logout = () => {
  localStorage.removeItem('token');
  window.location.hash = '#/login';
};
</script>

<template>
    <!--<span v-if="currentPath !== '#/login'">
      <strong>Панель управления</strong> | 
      <a href="#/">Главная</a> | 
      <button @click="logout" style="cursor:pointer">Выйти</button>
    </span>
    <span v-else>
      <strong>Пожалуйста, авторизуйтесь</strong>
    </span>

  <div style="margin-top: 20px;">
    <p>Статус API: <strong>{{ recordsText }}</strong></p>
  </div>
  
  <hr>-->

  <component :is="currentView" />
</template>

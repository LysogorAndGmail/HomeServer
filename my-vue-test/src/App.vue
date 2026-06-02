<script setup>
import { ref, computed, onMounted } from 'vue'
//import routes from '@/routers/route' // Теперь это сработает

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
  <div>
<!--    <nav>
      <router-link to="/">Admin</router-link> | 
      <router-link to="/login">Login</router-link>
    </nav>

    <hr>-->

    <router-view />
  </div>
</template>

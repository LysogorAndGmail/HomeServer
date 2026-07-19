<script setup>
import { onMounted } from 'vue'
import '../../public/dashboard/dashboard.css'
import { useRouter } from 'vue-router'
import Header from './Header.vue'
import Sidebar from './Sidebar.vue'

const router = useRouter()

const handleHeaderLogout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<template>
  <Header @onLogout="handleHeaderLogout" />

  <div class="container-fluid">
    <div class="row">

      <Sidebar />

      <!-- Оборачиваем роутер-вью в Bootstrap колонку, которая займет 10 из 12 частей экрана -->
      <main class="col-md-9 ms-sm-auto col-lg-10 px-md-4">
        <router-view />
      </main>

    </div>
  </div>
</template>
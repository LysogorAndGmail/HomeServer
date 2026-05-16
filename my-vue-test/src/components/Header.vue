<script setup>
// Определяем событие, которое компонент может отправлять наверх (родителю)
const emit = defineEmits(['onLogout'])

const handleLogout = () => {
  emit('onLogout')
}
</script>

<template>
  <header class="navbar navbar-dark sticky-top bg-dark flex-md-nowrap p-0 shadow">
    <a class="navbar-brand col-md-3 col-lg-2 me-0 px-3" href="#">Admin</a>
    <button class="navbar-toggler position-absolute d-md-none collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#sidebarMenu" aria-controls="sidebarMenu" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <input class="form-control form-control-dark w-100" type="text" placeholder="Search" aria-label="Search">
    <div class="navbar-nav">
      <div class="nav-item text-nowrap">
        <!-- При клике вызываем функцию, которая сообщит родителю, что нужно разлогиниться -->
        <span class="nav-link px-3" @click="handleLogout" style="cursor:pointer">Sign out</span>
      </div>
    </div>
  </header>
</template>

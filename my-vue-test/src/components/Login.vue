<script setup>
import { ref } from 'vue'
import api from '../api' // Убедись, что путь к api.js верный

const password = ref('')
const status = ref('')

const handleLogin = async () => {
  try {
    status.value = 'Enter...'
    
    // Отправляем POST запрос на Java AuthController
    const response = await api.post('/api/login', { 
	 username: 'admin', // for now only admin
	 password: password.value 
    });

    if (response.data.token) {
      // Сохраняем JWT в память браузера
      localStorage.setItem('token', response.data.token);
      status.value = 'Ok! go to home.';
      
      // Можно сделать редирект на главную через секунду
      setTimeout(() => {
        window.location.hash = '#/'
      }, 500);
    }
  } catch (error) {
    console.error('Ошибка входа:', error);
    status.value = 'Error: Invalid login or password';
  }
}
</script>

<template>
  <section id="center">
   <main class="form-signin">
    <div class="text-center">
      <img class="mb-4" src="/public/Signin/bootstrap-logo.svg" alt="" width="72" height="57">
      <p v-if="status">{{ status }}</p>

      <div class="form-floating" bis_skin_checked="1">
        <input type="email" class="form-control" id="floatingInput" placeholder="name@example.com">
        <label for="floatingInput">Email address</label>
      </div>
      <div class="form-floating" bis_skin_checked="1">
        <!--<input type="password" class="form-control" id="floatingPassword" placeholder="Password">-->
        <input 
	  v-model="password" 
          type="password"
          class="form-control"
	  placeholder="Введите пароль" 
          @keyup.enter="handleLogin" />

        <label for="floatingPassword">Password</label>
      </div>

      <div class="checkbox mb-3" bis_skin_checked="1">
        <label>
          <input type="checkbox" value="remember-me"> Remember me
        </label>
      </div>
      <button class="w-100 btn btn-lg btn-primary" @click="handleLogin" type="submit">Sign in</button>
      <p class="mt-5 mb-3 text-muted">© 2026</p>
    </div>
  </main>
  </section>
</template>

<style scoped>
/* Импорт внутри scoped-секции ограничит область видимости */
@import '@/style.css';
@import '@/signin.css';
</style>

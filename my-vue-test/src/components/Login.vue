<script setup>
import { ref, onMounted } from 'vue'
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

// 2. Сделали resIP реактивным объектом, чтобы шаблон видел изменения
const urlIP = 'http://ipinfo.io/json'
const resIP = ref({})
const currentConfigIP = ref('Загрузка...');

const updateIP = async () => {
  try {
    status.value = 'Update IP start ...'

    // Передаем реальный IP, который получили из ipinfo
    const response = await api.post('/api/updateIP', {
        ip: resIP.value.ip || '' 
    });

    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      status.value = 'Ok! IP update success!!!';
    }
  } catch (error) {
    console.error('Error:', error);
    status.value = 'Error: update IP';
  }
}

const fetchCurrentConfig = async () => {
  try {
    const response = await api.get('/api/currentIP');
    currentConfigIP.value = response.data.ip;
  } catch (e) {
    console.error("Ошибка получения IP из конфига", e);
  }
}

// 3. Правильный хук жизненного цикла для Composition API
onMounted(async () => {
  fetchCurrentConfig();
  try {
    // Используем api или стандартный axios. 
    // Если api настроен только на ваш бэкенд (базовый URL), лучше использовать прямой api.get или axios.get
    const response = await api.get(urlIP)
    resIP.value = response.data
  } catch (error) {
    console.error('Не удалось загрузить IP info:', error)
  }
})
</script>

<template>
  <section id="center">
   <main class="form-signin">
    <div class="text-center">
      <p>Текущий IP в конфигурации: <strong>{{ currentConfigIP }}</strong></p> 
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
      <span class="d-block mb-2 pb-2"></span>
      <!-- <button class="btn btn-sm btn-danger" @click="updateIP" type="submit">Update IP</button>-->
      
<div class="bd-example-snippet bd-code-snippet mt-3" bis_skin_checked="1"> <div class="bd-example m-0 border-0" bis_skin_checked="1"> <table class="table table-dark table-striped-columns">
  <thead>
      <tr>
        <th scope="col" colspan="2">Local IP Info</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <th scope="row">IP number:</th>
        <td>{{ resIP.ip }}</td>
      </tr>
	  <tr>
        <th scope="row">Host address:</th>
        <td>{{ resIP.hostname }}</td>
      </tr>
	  <tr>
        <th scope="row">City name:</th>
        <td>{{ resIP.city }}</td>
      </tr>
	  <tr>
        <th scope="row">Region name:</th>
        <td>{{ resIP.region }}</td>
      </tr>
	  <tr>
        <th scope="row">Country code:</th>
        <td>{{ resIP.country }}</td>
      </tr>
	  <tr>
        <th scope="row">GPS:</th>
        <td>{{ resIP.loc }}</td>
      </tr>
	  <tr>
        <th scope="row">Postal code:</th>
        <td>{{ resIP.postal }}</td>
      </tr>
      <tr>
        <th scope="row">Organization:</th>
        <td>{{ resIP.org }}</td>
      </tr>
    </tbody>
  </table>
  </div>
</div>

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

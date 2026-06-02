import { createApp } from 'vue'
//import './style.css'
//import '../public/Signin/signin.css'
import '../public/Signin/bootstrap.min.css'
import App from './App.vue'
import router from './routers' // Автоматически подцепит index.js из папки router

const app = createApp(App)

app.use(router) // Вот тут подключаем

app.mount('#app')

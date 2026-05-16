import axios from 'axios';

const api = axios.create({
  // Обязательно полный путь к бэкенду
  baseURL: 'http://84.141.44.207:8080', 
});

// Этот перехватчик вешает токен на каждый запрос
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token'); // Тот самый ключ из Login.vue
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, error => {
  return Promise.reject(error);
});

export default api;

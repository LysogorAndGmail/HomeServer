import axios from 'axios';

// Берем IP того сервера, с которого загрузился фронтенд
const currentHost = window.location.hostname;

const api = axios.create({
  baseURL: `http://${currentHost}:8080`,
});

// Настраиваем интерцептор (ТОЛЬКО ОДИН РАЗ И ДО ЭКСПОРТА)
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token'); 
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, error => {
  return Promise.reject(error);
});

// Экспортируем ОДИН РАЗ в самом конце
export default api;

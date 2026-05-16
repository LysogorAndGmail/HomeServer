import Admin from '@/components/Admin.vue'
import Login from '@/components/Login.vue'

const routes = {
  '/': Admin,
  '/login': Login
};

// Экспортируем по дефолту, чтобы App.vue мог его импортировать
export default routes;

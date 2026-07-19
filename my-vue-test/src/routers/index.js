// /route/index.js
import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/components/AdminLayout.vue'
import Dashboard from '@/components/Dashboard.vue' // 1. Импортируем новый Dashboard вместо Admin
import Login from '@/components/Login.vue'
import Projects from '@/components/Projects.vue'

const routes = [
  // 1. ОТКРЫТЫЕ МАРШРУТЫ
  {
    path: '/login',
    name: 'Login',
    component: Login
  },

  // 2. ГРУППА ЗАЩИЩЕННЫХ МАРШРУТОВ
  {
    path: '/',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '', // Главная страница админки (URL: /)
        name: 'Dashboard',
        component: Dashboard // 2. Меняем компонент здесь
      },
      {
        path: 'projects', // URL: /projects
        name: 'Projects',
        component: Projects
      }
    ]
  },

  // 3. ЛОВУШКА ДЛЯ ОШИБОК
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(), // Используем чистые URL без решетки (например: /projects)
  routes,
  linkExactActiveClass: 'active'
})

// ГЛОБАЛЬНАЯ ПРОВЕРКА (Остается без изменений, она идеальна)
router.beforeEach((to, from, next) => {
  const isAuthenticated = !!localStorage.getItem('token')

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (to.path === '/login' && isAuthenticated) {
    next('/')
  } else {
    next()
  }
})

export default router
import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/components/AdminLayout.vue' // Ваш шаблон с шапкой и сайдбаром
import Admin from '@/components/Admin.vue'
import Login from '@/components/Login.vue'
import Projects from '@/components/Projects.vue'


const routes = [
  // 1. ОТКРЫТЫЕ МАРШРУТЫ (Доступны всем)
  {
    path: '/login',
    name: 'Login',
    component: Login
  },

  // 2. ГРУППА ЗАЩИЩЕННЫХ МАРШРУТОВ (Нужна авторизация)
  {
    path: '/',
    component: AdminLayout,      // Переносим лейаут на уровень роутера!
    meta: { requiresAuth: true }, // Защита применяется СРАЗУ ко всей группе
    children: [
      {
        path: '',                 // Пустой путь означает главную страницу группы (т.е. просто "/")
        name: 'Dashboard',
        component: Admin
      },
      {
        path: 'projects',
        name: 'Projects',
        component: Projects
      },

     /* Пример, как легко добавлять новые страницы в админку в будущем:
      {
        path: 'users',            // URL будет: /users
        name: 'Users',
        component: Users
      },
      {
        path: 'settings',         // URL будет: /settings
        name: 'Settings',
        component: Settings
      }
     */
    ]
  },

  // 3. ЛОВУШКА ДЛЯ ОШИБОК (Всегда в самом конце)
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  linkExactActiveClass: 'active'
})

// ГЛОБАЛЬНАЯ ПРОВЕРКА (Guard)
router.beforeEach((to, from, next) => {
  // Проверяем, сохранен ли JWT-токен в браузере
  const isAuthenticated = !!localStorage.getItem('token')

  // Если страница требует авторизации, а токена нет — принудительно шлем на /login
  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  }
  // Если пользователь УЖЕ авторизован, но пытается зайти на форму логина — редиректим в админку
  else if (to.path === '/login' && isAuthenticated) {
    next('/')
  }
  // В остальных случаях — всё ок, пускаем дальше
  else {
    next()
  }
})

export default router

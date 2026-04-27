import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import MainLayout from '../components/MainLayout.vue'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: 'document-management',
          name: 'document-management',
          component: () => import('../views/DocumentManagement.vue'),
        },
        {
          path: 'intelligent-qa',
          name: 'intelligent-qa',
          component: () => import('../views/IntelligentQA.vue'),
        },
        {
          path: 'knowledge-graph',
          name: 'knowledge-graph',
          component: () => import('../views/KnowledgeGraph.vue'),
        },
        {
          path: 'user-center',
          name: 'user-center',
          component: () => import('../views/UserCenter.vue'),
        },
        {
          path: 'document-preview/:id',
          name: 'document-preview',
          component: () => import('../views/DocumentPreview.vue'),
        },
      ],
    },
  ],
})

// 导航守卫：检查登录状态
router.beforeEach((to, _from, next) => {
  const publicPages = ['/login']
  const authRequired = !publicPages.includes(to.path)
  const userStore = useUserStore()

  if (authRequired && !userStore.isLogin) {
    // 尝试从后端获取当前用户（恢复 session）
    userStore.fetchCurrentUser().then(() => {
      if (userStore.isLogin) {
        next()
      } else {
        next('/login')
      }
    })
  } else {
    next()
  }
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import MainLayout from '../components/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
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

export default router

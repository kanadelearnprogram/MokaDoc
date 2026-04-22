<template>
  <div id="app" class="page active">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="logo">
        <h1>智能文档系统</h1>
      </div>
      <nav class="nav">
        <router-link 
          to="/document-management" 
          class="nav-item"
          active-class="active"
        >
          📁 文档管理
        </router-link>
        <router-link 
          to="/intelligent-qa" 
          class="nav-item"
          active-class="active"
        >
          💬 智能问答
        </router-link>
        <router-link 
          to="/knowledge-graph" 
          class="nav-item"
          active-class="active"
        >
          🌐 知识图谱
        </router-link>
        <router-link 
          to="/user-center" 
          class="nav-item"
          active-class="active"
        >
          👤 用户中心
        </router-link>
      </nav>
    </aside>

    <!-- 顶部栏 -->
    <header class="topbar">
      <div class="topbar-left">
        <h2>{{ pageTitle }}</h2>
      </div>
      <div class="topbar-right">
        <button class="icon-btn">🔔</button>
        <button class="icon-btn">⚙️</button>
        <div class="user-menu">
          <button class="user-btn">👤 用户</button>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 根据路由名称动态设置页面标题
const pageTitle = computed(() => {
  const titleMap: Record<string, string> = {
    'document-management': '文档管理',
    'intelligent-qa': '智能问答',
    'knowledge-graph': '知识图谱',
    'user-center': '用户中心',
    'document-preview': '文档预览'
  }
  return titleMap[route.name as string] || '智能文档系统'
})
</script>

<style scoped>
/* 样式已从全局 style.css 引入 */
</style>

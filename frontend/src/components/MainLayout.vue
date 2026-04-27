<template>
  <div class="app-layout">
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
.app-layout {
    display: grid;
    grid-template-columns: 240px 1fr;
    grid-template-rows: 64px 1fr;
    grid-template-areas:
        "sidebar header"
        "sidebar main";
    height: 100vh;
}

/* 侧边栏 */
.sidebar {
    grid-area: sidebar;
    background-color: var(--surface-color, #FFFFFF);
    border-right: 1px solid var(--border-color, #E6E3DC);
    padding: 20px;
    overflow-y: auto;
}

.sidebar .logo h1 {
    font-size: 20px;
    color: var(--primary-color);
    margin-bottom: 30px;
    letter-spacing: 1px;
    animation: fadeInDown 0.5s var(--transition-ease, cubic-bezier(0.4, 0, 0.2, 1)) both;
}

@keyframes fadeInDown {
    from { opacity: 0; transform: translateY(-12px); }
    to   { opacity: 1; transform: translateY(0); }
}

/* 顶部栏 */
.topbar {
    grid-area: header;
    background-color: var(--surface-color, #FFFFFF);
    border-bottom: 1px solid var(--border-color, #E6E3DC);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
}

/* 主内容区 */
.main-content {
    grid-area: main;
    padding: 24px;
    overflow-y: auto;
    height: 100%;
    box-sizing: border-box;
    background: var(--bg-color, #F3F0EB);
}

@media (max-width: 768px) {
    .app-layout {
        grid-template-columns: 1fr;
        grid-template-rows: auto auto 1fr;
        grid-template-areas:
            "header"
            "sidebar"
            "main";
    }

    .sidebar {
        width: 100%;
        border-right: none;
        border-bottom: 1px solid var(--border-color, #E6E3DC);
    }
}
</style>

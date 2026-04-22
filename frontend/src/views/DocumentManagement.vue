<template>
  <div id="document-management" class="content-page active">
    <div class="document-header">
      <button class="btn btn-primary">📤 上传文档</button>
      <div class="search-box">
        <input 
          type="text" 
          v-model="searchQuery"
          placeholder="搜索文档..."
        >
        <button class="search-btn">🔍</button>
      </div>
      <div class="filter-dropdown">
        <button class="btn">筛选 ▼</button>
      </div>
    </div>
    
    <div class="view-toggle">
      <button 
        class="btn" 
        :class="{ active: viewMode === 'card' }"
        @click="viewMode = 'card'"
      >
        卡片视图
      </button>
      <button 
        class="btn" 
        :class="{ active: viewMode === 'table' }"
        @click="viewMode = 'table'"
      >
        表格视图
      </button>
    </div>

    <div class="document-list">
      <div 
        v-for="doc in filteredDocuments" 
        :key="doc.id"
        class="document-card"
      >
        <div class="document-icon" :class="getIconClass(doc.type)">
          {{ doc.type.toUpperCase() }}
        </div>
        <h3>{{ doc.name }}</h3>
        <div class="document-meta">
          <span>{{ doc.date }}</span>
          <span>{{ doc.size }}</span>
        </div>
        <div class="document-actions">
          <button class="btn-sm" @click="previewDocument(doc)">👁️ 预览</button>
          <button class="btn-sm">🔗 分享</button>
          <button class="btn-sm danger" @click="deleteDocument(doc)">🗑️ 删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const searchQuery = ref('')
const viewMode = ref('card')

// 模拟文档数据
const documents = ref([
  {
    id: 1,
    name: '机器学习入门.pdf',
    type: 'pdf',
    date: '2024-01-01',
    size: '2.5MB'
  },
  {
    id: 2,
    name: '数据结构笔记.txt',
    type: 'txt',
    date: '2024-01-02',
    size: '1.2MB'
  },
  {
    id: 3,
    name: 'Python教程.md',
    type: 'md',
    date: '2024-01-03',
    size: '3.1MB'
  }
])

// 根据搜索条件过滤文档
const filteredDocuments = computed(() => {
  if (!searchQuery.value) return documents.value
  return documents.value.filter(doc => 
    doc.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

// 获取图标样式类
const getIconClass = (type: string) => {
  const iconMap: Record<string, string> = {
    pdf: 'pdf-icon',
    txt: 'txt-icon',
    md: 'md-icon'
  }
  return iconMap[type] || ''
}

// 预览文档
const previewDocument = (doc: any) => {
  router.push(`/document-preview/${doc.id}`)
}

// 删除文档
const deleteDocument = (doc: any) => {
  if (confirm(`确定要删除 "${doc.name}" 吗？`)) {
    // TODO: 实现删除逻辑
    const index = documents.value.findIndex(d => d.id === doc.id)
    if (index > -1) {
      documents.value.splice(index, 1)
      alert('文档已删除')
    }
  }
}
</script>

<style scoped>
/* 样式已从全局 style.css 引入 */
</style>

<template>
  <div id="knowledge-graph" class="content-page active">
    <div class="graph-container">
      <!-- 左侧：文档选择和搜索 -->
      <div class="graph-left">
        <div class="graph-left-header">
          <h3>文档选择</h3>
          <select 
            v-model="selectedDocument"
            class="form-control"
          >
            <option value="">全部文档</option>
            <option v-for="doc in documents" :key="doc.id" :value="doc.id">
              {{ doc.name }}
            </option>
          </select>
        </div>
        
        <div class="graph-left-search">
          <input 
            type="text" 
            v-model="searchQuery"
            placeholder="搜索知识点..."
          >
          <button class="search-btn">🔍</button>
        </div>
        
        <div class="graph-legend">
          <h3>图例</h3>
          <div class="legend-item">
            <span class="legend-color blue"></span>
            <span>文档</span>
          </div>
          <div class="legend-item">
            <span class="legend-color green"></span>
            <span>章节</span>
          </div>
          <div class="legend-item">
            <span class="legend-color orange"></span>
            <span>知识点</span>
          </div>
        </div>
      </div>

      <!-- 中间：图谱可视化 -->
      <div class="graph-middle">
        <div class="graph-toolbar">
          <button class="btn-sm" @click="zoomIn">🔍 放大</button>
          <button class="btn-sm" @click="zoomOut">🔭 缩小</button>
          <button class="btn-sm" @click="resetZoom">🔄 重置</button>
          <button class="btn-sm" @click="exportGraph">📷 导出</button>
        </div>
        
        <div class="graph-visualization">
          <div 
            class="graph-placeholder"
            @click="handleNodeClick"
          >
            <p>知识图谱可视化区域</p>
            <p style="font-size: 12px; margin-top: 8px;">（点击此处模拟节点交互）</p>
          </div>
        </div>
      </div>

      <!-- 右侧：节点详情 -->
      <div class="graph-right">
        <div class="graph-right-header">
          <h3>节点详情</h3>
        </div>
        
        <div v-if="selectedNode" class="node-details">
          <div class="node-type">
            <span>类型：</span>
            <span>{{ selectedNode.type }}</span>
          </div>
          <div class="node-info">
            <h4>{{ selectedNode.title }}</h4>
            <p>{{ selectedNode.description }}</p>
          </div>
          <button class="btn btn-primary" @click="linkToQA">关联问答</button>
        </div>
        
        <div v-else class="node-details" style="text-align: center; color: #909399;">
          <p>请点击图谱中的节点查看详情</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface Document {
  id: number
  name: string
}

interface Node {
  id: number
  type: string
  title: string
  description: string
}

// 文档列表
const documents = ref<Document[]>([
  { id: 1, name: '机器学习入门.pdf' },
  { id: 2, name: '数据结构笔记.txt' },
  { id: 3, name: 'Python教程.md' }
])

const selectedDocument = ref('')
const searchQuery = ref('')

// 选中的节点
const selectedNode = ref<Node | null>(null)

// 缩放级别
const zoomLevel = ref(100)

// 处理节点点击（模拟）
const handleNodeClick = () => {
  // TODO: 实际项目中这里应该使用 ECharts 或其他图谱库
  selectedNode.value = {
    id: 1,
    type: '知识点',
    title: '机器学习',
    description: '机器学习是人工智能的一个分支，它允许计算机在没有明确编程的情况下学习和改进。'
  }
  alert('节点点击功能：这里将显示节点的详细信息')
}

// 放大
const zoomIn = () => {
  zoomLevel.value = Math.min(zoomLevel.value + 10, 200)
  console.log('放大到:', zoomLevel.value + '%')
}

// 缩小
const zoomOut = () => {
  zoomLevel.value = Math.max(zoomLevel.value - 10, 50)
  console.log('缩小到:', zoomLevel.value + '%')
}

// 重置缩放
const resetZoom = () => {
  zoomLevel.value = 100
  console.log('重置缩放到:', zoomLevel.value + '%')
}

// 导出图谱
const exportGraph = () => {
  alert('导出功能：这里将导出当前图谱为图片')
}

// 关联问答
const linkToQA = () => {
  if (selectedNode.value) {
    router.push({
      path: '/intelligent-qa',
      query: { node: selectedNode.value.title }
    })
  }
}
</script>

<style scoped>
/* 样式已从全局 style.css 引入 */
</style>

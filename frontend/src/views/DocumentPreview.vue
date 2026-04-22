<template>
  <div id="document-preview" class="content-page active">
    <div class="preview-header">
      <div class="preview-info">
        <div class="document-icon pdf-icon">PDF</div>
        <div class="document-info">
          <h3>{{ document.name }}</h3>
          <div class="document-meta">
            <span>{{ document.date }}</span>
            <span>{{ document.size }}</span>
          </div>
        </div>
      </div>
      <div class="preview-actions">
        <button class="btn" @click="goBack">返回列表</button>
        <button class="btn" @click="downloadDocument">下载</button>
        <button class="btn btn-primary" @click="generateQA">生成问答</button>
      </div>
    </div>

    <div class="preview-content">
      <div class="pdf-preview">
        <div class="pdf-controls">
          <button class="btn-sm" @click="prevPage" :disabled="currentPage <= 1">上一页</button>
          <span>第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
          <button class="btn-sm" @click="nextPage" :disabled="currentPage >= totalPages">下一页</button>
          
          <div class="pdf-zoom">
            <span>缩放：</span>
            <button class="btn-sm" @click="zoomOut">-</button>
            <span>{{ zoomLevel }}%</span>
            <button class="btn-sm" @click="zoomIn">+</button>
          </div>
          
          <button class="btn-sm" @click="searchInPdf">搜索</button>
        </div>
        
        <div class="pdf-content">
          <div class="pdf-page">
            <h1>机器学习入门</h1>
            
            <h2>1. 什么是机器学习？</h2>
            <p>机器学习是人工智能的一个分支，它允许计算机在没有明确编程的情况下学习和改进。机器学习算法使用数据来训练模型，然后使用这些模型来预测或做出决策。</p>
            <p>机器学习可以分为监督学习、无监督学习和强化学习等类型。</p>
            
            <h2>2. 监督学习</h2>
            <p>监督学习是一种机器学习方法，其中模型通过标记的训练数据进行学习。标记的训练数据包含输入和相应的输出。</p>
            <p>常见的监督学习算法包括线性回归、逻辑回归、决策树、随机森林和支持向量机等。</p>
            
            <h2>3. 无监督学习</h2>
            <p>无监督学习是一种机器学习方法，其中模型从未标记的数据中学习。模型需要自己发现数据中的模式和结构。</p>
            <p>常见的无监督学习算法包括聚类、降维和异常检测等。</p>
            
            <h2>4. 强化学习</h2>
            <p>强化学习是一种机器学习方法，其中智能体通过与环境交互来学习如何采取行动以最大化累积奖励。</p>
            <p>强化学习在游戏AI、机器人控制和自动驾驶等领域有广泛应用。</p>
          </div>
        </div>
      </div>
    </div>

    <div class="preview-recommendations">
      <h3>相关推荐</h3>
      <div class="recommendation-cards">
        <div class="recommendation-card">
          <h4>相关文档</h4>
          <ul>
            <li><a href="#">Python机器学习实战.pdf</a></li>
            <li><a href="#">深度学习入门.pdf</a></li>
          </ul>
        </div>
        <div class="recommendation-card">
          <h4>相关知识点</h4>
          <ul>
            <li><a href="#">监督学习</a></li>
            <li><a href="#">无监督学习</a></li>
            <li><a href="#">强化学习</a></li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

interface Document {
  id: number
  name: string
  type: string
  date: string
  size: string
}

// 当前文档信息
const document = ref<Document>({
  id: 1,
  name: '机器学习入门.pdf',
  type: 'pdf',
  date: '2024-01-01',
  size: '2.5MB'
})

// PDF 分页控制
const currentPage = ref(1)
const totalPages = ref(10)

// 缩放控制
const zoomLevel = ref(100)

onMounted(() => {
  // 从路由参数获取文档ID
  const docId = route.params.id
  if (docId) {
    // TODO: 根据文档ID加载文档信息
    console.log('加载文档:', docId)
  }
})

// 返回文档列表
const goBack = () => {
  router.push('/document-management')
}

// 下载文档
const downloadDocument = () => {
  alert(`正在下载: ${document.value.name}`)
}

// 生成问答
const generateQA = () => {
  router.push({
    path: '/intelligent-qa',
    query: { docId: document.value.id }
  })
}

// 上一页
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

// 下一页
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

// 放大
const zoomIn = () => {
  zoomLevel.value = Math.min(zoomLevel.value + 10, 200)
}

// 缩小
const zoomOut = () => {
  zoomLevel.value = Math.max(zoomLevel.value - 10, 50)
}

// 在PDF中搜索
const searchInPdf = () => {
  const keyword = prompt('请输入搜索关键词:')
  if (keyword) {
    alert(`搜索: ${keyword}`)
    // TODO: 实现PDF内搜索功能
  }
}
</script>

<style scoped>
/* 样式已从全局 style.css 引入 */
</style>

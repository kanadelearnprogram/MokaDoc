<template>
  <div id="intelligent-qa" class="content-page active">
    <div class="qa-container">
      <!-- 左侧：会话管理和文档选择 -->
      <div class="qa-left">
        <div class="session-management">
          <button class="btn-sm btn-primary" @click="createNewSession">+ 新建会话</button>
          <div class="session-list">
            <div 
              v-for="session in sessions" 
              :key="session.id"
              class="session-item"
              :class="{ active: currentSessionId === session.id }"
              @click="switchSession(session.id)"
            >
              <div class="session-title">{{ session.name }}</div>
              <div class="session-summary">
                {{ session.messages.length > 0 ? session.messages[0].content : '无消息' }}
              </div>
            </div>
          </div>
        </div>
        
        <div class="qa-left-header">
          <button class="btn-sm" @click="selectAllDocuments">全选</button>
          <button class="btn-sm" @click="deselectAllDocuments">取消全选</button>
        </div>
        
        <div class="document-select-list">
          <div 
            v-for="doc in documents" 
            :key="doc.id"
            class="document-select-item"
            :class="{ active: selectedDocs.includes(doc.id) }"
            @click="toggleDocumentSelection(doc.id)"
          >
            <input 
              type="checkbox" 
              :checked="selectedDocs.includes(doc.id)"
              @click.stop
            >
            <span>{{ doc.name }}</span>
          </div>
        </div>
      </div>

      <!-- 中间：聊天区域 -->
      <div class="qa-middle">
        <div class="qa-middle-header">
          <div class="selected-documents">
            <span 
              v-for="docId in selectedDocs" 
              :key="docId"
              class="document-tag"
            >
              {{ getDocumentName(docId) }}
              <button class="tag-remove" @click="removeSelectedDoc(docId)">×</button>
            </span>
          </div>
        </div>
        
        <div class="chat-container" ref="chatContainer">
          <div 
            v-for="(message, index) in currentMessages" 
            :key="index"
            class="chat-message"
            :class="`${message.type}-message`"
          >
            <div class="message-content">
              <p v-html="message.content"></p>
            </div>
          </div>
        </div>
        
        <div class="chat-input">
          <textarea 
            v-model="userInput"
            placeholder="请输入您的问题..."
            @keyup.enter.ctrl="sendMessage"
            :disabled="isStreaming"
          ></textarea>
          <button 
            v-if="!isStreaming" 
            class="btn btn-primary" 
            @click="sendMessage"
          >
            发送
          </button>
          <button 
            v-else 
            class="btn btn-danger" 
            @click="stopStreaming"
          >
            ⏹ 停止
          </button>
        </div>
      </div>

      <!-- 右侧：引用溯源 -->
      <div class="qa-right">
        <div class="qa-right-header">
          <h3>引用溯源</h3>
        </div>
        <div class="reference-content">
          <div 
            v-for="(ref, index) in references" 
            :key="index"
            class="reference-item"
          >
            <h4>[{{ index + 1 }}] {{ ref.source }}</h4>
            <p>{{ ref.content }}</p>
            <button class="btn-sm">查看原文</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onUnmounted } from 'vue'
import { useSSEFetch, createAbortController } from '@/utils/sse'

interface Message {
  type: 'user' | 'ai'
  content: string
}

interface Session {
  id: number
  name: string
  messages: Message[]
}

interface Document {
  id: number
  name: string
}

interface Reference {
  source: string
  content: string
}

// 会话数据
const sessions = ref<Session[]>([
  { id: 1, name: '会话 1', messages: [] },
  { id: 2, name: '会话 2', messages: [] },
  { id: 3, name: '会话 3', messages: [] }
])
const currentSessionId = ref(1)

// 文档数据
const documents = ref<Document[]>([
  { id: 1, name: '机器学习入门.pdf' },
  { id: 2, name: '数据结构笔记.txt' },
  { id: 3, name: 'Python教程.md' }
])
const selectedDocs = ref<number[]>([1])

// 用户输入
const userInput = ref('')

// 引用数据
const references = ref<Reference[]>([
  {
    source: '机器学习入门.pdf',
    content: '机器学习是人工智能的一个分支，它允许计算机在没有明确编程的情况下学习和改进。'
  },
  {
    source: '机器学习入门.pdf',
    content: '机器学习可以分为监督学习、无监督学习和强化学习等类型。'
  }
])

// 聊天容器引用
const chatContainer = ref<HTMLElement | null>(null)

// SSE 中断控制器
let abortController: AbortController | null = null

// 是否正在接收流式响应
const isStreaming = ref(false)

// 获取当前会话的消息
const currentMessages = computed(() => {
  const session = sessions.value.find(s => s.id === currentSessionId.value)
  return session ? session.messages : []
})

// 获取文档名称
const getDocumentName = (docId: number) => {
  const doc = documents.value.find(d => d.id === docId)
  return doc ? doc.name : ''
}

// 创建新会话
const createNewSession = () => {
  const newId = sessions.value.length + 1
  const newSession: Session = {
    id: newId,
    name: `会话 ${newId}`,
    messages: []
  }
  sessions.value.push(newSession)
  currentSessionId.value = newId
}

// 切换会话
const switchSession = (sessionId: number) => {
  currentSessionId.value = sessionId
}

// 切换文档选择
const toggleDocumentSelection = (docId: number) => {
  const index = selectedDocs.value.indexOf(docId)
  if (index > -1) {
    selectedDocs.value.splice(index, 1)
  } else {
    selectedDocs.value.push(docId)
  }
}

// 全选文档
const selectAllDocuments = () => {
  selectedDocs.value = documents.value.map(d => d.id)
}

// 取消全选
const deselectAllDocuments = () => {
  selectedDocs.value = []
}

// 移除选中的文档
const removeSelectedDoc = (docId: number) => {
  const index = selectedDocs.value.indexOf(docId)
  if (index > -1) {
    selectedDocs.value.splice(index, 1)
  }
}

// 发送消息
const sendMessage = async () => {
  const message = userInput.value.trim()
  if (!message) return

  // 如果正在流式传输，先中断
  if (isStreaming.value && abortController) {
    abortController.abort()
    abortController = null
    isStreaming.value = false
  }

  // 添加用户消息
  const session = sessions.value.find(s => s.id === currentSessionId.value)
  if (session) {
    session.messages.push({ type: 'user', content: message })
  }

  // 清空输入框
  userInput.value = ''

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 创建 AI 消息占位符
  let aiMessageContent = ''
  if (session) {
    session.messages.push({ type: 'ai', content: '' })
  }

  // 开始流式请求
  isStreaming.value = true
  abortController = createAbortController()

  // 使用相对路径,Vite 代理会自动转发到后端
  // 实际请求: http://localhost:5174/api/chat/chat-stream → http://localhost:8080/api/chat/chat-stream
  const API_URL = '/api/chat/chat-stream'

  try {
    console.log('发起 SSE 请求:', API_URL, '参数:', { prompt: message })
    
    await useSSEFetch(
      API_URL,
      { prompt: message },
      {
        onMessage: (data) => {
          // 累积接收到的数据
          aiMessageContent += data
          
          // 更新最后一条 AI 消息
          if (session) {
            const lastMessage = session.messages[session.messages.length - 1]
            if (lastMessage && lastMessage.type === 'ai') {
              lastMessage.content = aiMessageContent
            }
          }
          
          // 实时滚动到底部
          scrollToBottom()
        },
        onError: (error) => {
          console.error('SSE error:', error)
          console.error('Error message:', error.message)
          console.error('Error name:', error.name)
          isStreaming.value = false
          abortController = null
          
          // 如果还没有内容，显示错误信息
          if (!aiMessageContent) {
            if (session) {
              const lastMessage = session.messages[session.messages.length - 1]
              if (lastMessage && lastMessage.type === 'ai') {
                // 提供更详细的错误信息
                let errorMsg = '抱歉，AI 服务出现错误'
                
                if (error.message.includes('404')) {
                  errorMsg += '：接口不存在，请检查后端服务是否启动'
                } else if (error.message.includes('401') || error.message.includes('403')) {
                  errorMsg += '：认证失败，请重新登录'
                } else if (error.message.includes('500')) {
                  errorMsg += '：服务器内部错误'
                } else if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
                  errorMsg += '：网络连接失败，请检查后端服务是否运行在 http://localhost:8080'
                } else {
                  errorMsg += '，请稍后重试'
                }
                
                lastMessage.content = errorMsg
              }
            }
          }
        },
        onComplete: () => {
          console.log('SSE completed')
          isStreaming.value = false
          abortController = null
        },
        signal: abortController.signal,
      }
    )
  } catch (error) {
    console.error('Send message error:', error)
    console.error('Error details:', error)
    isStreaming.value = false
    abortController = null
    
    // 在 catch 中也更新错误信息
    if (session) {
      const lastMessage = session.messages[session.messages.length - 1]
      if (lastMessage && lastMessage.type === 'ai' && !lastMessage.content) {
        lastMessage.content = `请求失败：${error instanceof Error ? error.message : '未知错误'}`
      }
    }
  }
}

// 停止流式响应
const stopStreaming = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
    isStreaming.value = false
    console.log('用户手动停止流式响应')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

// 组件卸载时中断 SSE 连接
onUnmounted(() => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
})
</script>

<style scoped>
/* 样式已从全局 style.css 引入 */
</style>

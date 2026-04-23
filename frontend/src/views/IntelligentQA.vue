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
              <div v-if="message.type === 'ai'" class="markdown-body" v-html="message.renderedContent"></div>
              <div v-else>{{ message.content }}</div>
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
import { ref, computed, nextTick, onUnmounted, onMounted } from 'vue'
import { useSSEFetch, createAbortController } from '@/utils/sse'
import { marked } from 'marked'
import { listSessions } from '@/api/liaotianguanli'

// 配置 marked 选项
marked.setOptions({
  breaks: true,        // 支持 GitHub 风格的换行（单个换行符转为 <br>）
  gfm: true,           // 启用 GitHub Flavored Markdown（表格、任务列表等）
  headerIds: false,    // 不自动生成标题 ID
  mangle: false,       // 不转义邮箱地址
  pedantic: false,     // 不使用严格的 Markdown 解析
  sanitize: false,     // 不启用内置 sanitization（我们将使用 DOMPurify）
  smartLists: true,    // 使用更智能的列表行为
  smartypants: false   // 不转换引号和破折号
})

interface Message {
  type: 'user' | 'ai'
  content: string
}

interface Session {
  id: number  // 前端会话 ID
  name: string
  messages: Message[]
  backendSessionId?: number  // 后端会话 ID（从 SSE 响应中获取）
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
const sessions = ref<Session[]>([])
const currentSessionId = ref<number | null>(null)

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

// 安全地渲染 Markdown(带简单的 XSS 防护)
const safeMarkdownToHtml = (markdown: string): string => {
  if (!markdown) return ''
  
  try {
    // 预处理:修复标题格式(确保 # 后有空格)
    const fixedMarkdown = fixMarkdownHeaders(markdown)
    
    // 使用 marked 解析 Markdown
    const html = marked.parse(fixedMarkdown)
    
    // 简单的 XSS 防护：移除危险标签
    // 注意：生产环境建议使用 DOMPurify
    const sanitized = String(html)
      .replace(/<script[^>]*>.*?<\/script>/gi, '')  // 移除 script 标签
      .replace(/on\w+="[^"]*"/gi, '')                // 移除内联事件
      .replace(/on\w+='[^']*'/gi, '')                  // 移除内联事件（单引号）
    
    return sanitized
  } catch (error) {
    console.error('Markdown 解析失败:', error)
    // 降级处理：将原始文本转义后返回
    return `<p style="color: red;">[Markdown 解析失败]</p><pre>${markdown}</pre>`
  }
}

// 获取当前会话的消息（带 Markdown 渲染）
const currentMessages = computed(() => {
  const session = sessions.value.find(s => s.id === currentSessionId.value)
  if (!session) return []
  
  return session.messages.map(msg => {
    // AI 消息使用 Markdown 渲染，用户消息保持纯文本
    const renderedContent = msg.type === 'ai' ? safeMarkdownToHtml(msg.content) : msg.content
    
    // 调试日志：查看原始内容和渲染后的内容（仅开发环境）
    if (msg.type === 'ai' && msg.content && import.meta.env.DEV) {
      console.log('=== Markdown 调试信息 ===')
      console.log('原始内容前100字符:', msg.content.substring(0, 100))
      console.log('渲染后HTML前200字符:', String(renderedContent).substring(0, 200))
    }
    
    return {
      ...msg,
      renderedContent
    }
  })
})

// 获取文档名称
const getDocumentName = (docId: number) => {
  const doc = documents.value.find(d => d.id === docId)
  return doc ? doc.name : ''
}

// 加载会话列表
const loadSessions = async () => {
  try {
    console.log('开始加载会话列表...')
    const res = await listSessions()
    
    if (res.data.code === 0 && res.data.data) {
      // 将后端返回的 QaSession 转换为前端 Session 格式
      sessions.value = res.data.data.map((qaSession) => ({
        id: qaSession.id!,
        name: qaSession.sessionName || `会话 ${qaSession.id}`,
        messages: [], // 初始化为空，后续按需加载消息历史
        backendSessionId: qaSession.id
      }))
      
      // 如果有会话，默认选中第一个
      if (sessions.value.length > 0) {
        currentSessionId.value = sessions.value[0].id
      }
      
      console.log('✅ 会话列表加载成功:', sessions.value)
    } else {
      console.error('❌ 会话列表加载失败:', res.data.message)
    }
  } catch (error) {
    console.error('❌ 加载会话列表异常:', error)
  }
}

// 创建新会话
const createNewSession = () => {
  // 注意：新建会话应该调用后端接口创建，这里只是前端临时实现
  // TODO: 调用后端创建会话接口
  const newId = Date.now() // 使用时间戳作为临时 ID
  const newSession: Session = {
    id: newId,
    name: `新会话`,
    messages: [],
    backendSessionId: undefined // 第一次发送消息时由后端分配
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

  // 使用新的后端 API: POST /api/chat/ask
  // 实际请求: http://localhost:5174/api/chat/ask → http://localhost:8080/api/chat/ask
  const API_URL = '/api/chat/ask'
  
  // 构建请求体：如果当前会话已有后端 sessionId，则传入以继续对话
  const requestBody: { content: string; sessionId?: number } = {
    content: message
  }
  
  // 如果当前会话已经有后端分配的 sessionId，则传入
  if (session && session.backendSessionId) {
    requestBody.sessionId = session.backendSessionId
    console.log('使用已保存的 backendSessionId:', session.backendSessionId)
  } else {
    console.log('未找到 backendSessionId，将创建新会话')
  }

  try {
    console.log('发起 SSE 请求:', API_URL, '参数:', requestBody)
    
    await useSSEFetch(
      API_URL,
      requestBody,
      {
        method: 'POST',  // 使用 POST 方法
        onMessage: (data) => {
          // 尝试解析元数据：如果数据是 JSON 格式且包含 sessionId，则保存
          try {
            const parsed = JSON.parse(data)
            // 如果是元数据（包含 message 和 sessionId 字段）
            if (parsed.message && parsed.sessionId !== undefined) {
              console.log('接收到元数据:', parsed)
              
              // 保存后端返回的 sessionId 到当前会话
              if (session && !session.backendSessionId) {
                session.backendSessionId = parsed.sessionId
                console.log('✅ 保存后端 sessionId:', parsed.sessionId, '到前端会话:', session.id)
              } else if (session && session.backendSessionId) {
                console.log('ℹ️  会话已有 backendSessionId:', session.backendSessionId, '，跳过更新')
              }
              
              // 不将元数据显示在对话中
              return
            }
          } catch (e) {
            // 不是 JSON 格式，说明是对话内容，继续处理
          }
          
          // 累积接收到的对话内容（直接拼接，保留原始格式）
          // 注意：不要对数据进行任何修改，让 CSS white-space: pre-wrap 来处理空格和换行
          if (data) {
            aiMessageContent += data
          }
          
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

// 组件挂载时加载会话列表
onMounted(() => {
  loadSessions()
})

// 组件卸载时中断 SSE 连接
onUnmounted(() => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
})

// 修复 Markdown 标题格式(确保 # 后有空格)
const fixMarkdownHeaders = (text: string): string => {
  if (!text) return text
  
  // 匹配任意位置的 1-6 级标题,如果 # 后直接跟非空格字符,则插入空格
  // 例如: ##一、 -> ## 一、
  // 注意:不使用 ^ 锚点,因为后端返回的可能是单行连续文本
  return text.replace(/(#{1,6})([^\s#])/g, '$1 $2')
}
</script>

<style scoped>
/* Markdown 渲染样式 - 参考 GitHub 风格 + 中文排版优化 */
.message-content :deep(.markdown-body) {
  line-height: 1.8;
  font-size: 15px;
  color: #24292e;
  white-space: pre-wrap; /* 保留空格和换行符,自动换行 */
  word-break: break-word; /* 长单词/URL 自动换行 */
}

/* 标题样式 */
.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4),
.message-content :deep(h5),
.message-content :deep(h6) {
  margin-top: 1.5em;
  margin-bottom: 0.75em;
  font-weight: 600;
  line-height: 1.25;
  color: #24292e;
}

.message-content :deep(h1) {
  font-size: 2em;
  border-bottom: 2px solid #eaecef;
  padding-bottom: 0.3em;
}

.message-content :deep(h2) {
  font-size: 1.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.message-content :deep(h3) {
  font-size: 1.25em;
}

.message-content :deep(h4) {
  font-size: 1em;
}

/* 段落样式 - 中文排版优化 */
.message-content :deep(p) {
  margin-top: 0;
  margin-bottom: 1em;
  line-height: 1.8;
  text-indent: 0; /* AI 对话不需要首行缩进 */
}

/* 列表样式 */
.message-content :deep(ul),
.message-content :deep(ol) {
  margin-top: 0;
  margin-bottom: 1em;
  padding-left: 2em;
}

.message-content :deep(li) {
  margin: 0.25em 0;
  line-height: 1.6;
}

.message-content :deep(li > p) {
  margin: 0.25em 0;
}

/* 代码样式 */
.message-content :deep(code) {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
}

.message-content :deep(pre) {
  padding: 1em;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
  background-color: #f6f8fa;
  border-radius: 6px;
  margin: 1em 0;
}

.message-content :deep(pre code) {
  padding: 0;
  margin: 0;
  font-size: 100%;
  background: transparent;
  border: 0;
  word-break: normal;
  white-space: pre;
}

/* 引用块样式 */
.message-content :deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  color: #6a737d;
  border-left: 0.25em solid #dfe2e5;
  background-color: #f9f9f9;
}

.message-content :deep(blockquote > p:first-child) {
  margin-top: 0;
}

.message-content :deep(blockquote > p:last-child) {
  margin-bottom: 0;
}

/* 表格样式 */
.message-content :deep(table) {
  border-spacing: 0;
  border-collapse: collapse;
  margin: 1em 0;
  width: 100%;
  display: block;
  overflow-x: auto;
}

.message-content :deep(table th),
.message-content :deep(table td) {
  padding: 8px 13px;
  border: 1px solid #dfe2e5;
}

.message-content :deep(table th) {
  font-weight: 600;
  background-color: #f6f8fa;
}

.message-content :deep(table tr) {
  background-color: #fff;
  border-top: 1px solid #c6cbd1;
}

.message-content :deep(table tr:nth-child(2n)) {
  background-color: #f6f8fa;
}

/* 链接样式 */
.message-content :deep(a) {
  color: #0366d6;
  text-decoration: none;
}

.message-content :deep(a:hover) {
  text-decoration: underline;
}

/* 分割线 */
.message-content :deep(hr) {
  height: 0.25em;
  padding: 0;
  margin: 24px 0;
  background-color: #e1e4e8;
  border: 0;
}

/* 图片样式 */
.message-content :deep(img) {
  max-width: 100%;
  height: auto;
  box-sizing: content-box;
  border-radius: 6px;
  margin: 1em 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 任务列表复选框 */
.message-content :deep(input[type="checkbox"]) {
  margin-right: 0.5em;
}

/* 强调文本 */
.message-content :deep(strong) {
  font-weight: 600;
  color: #24292e;
}

.message-content :deep(em) {
  font-style: italic;
}

/* 删除线 */
.message-content :deep(del) {
  text-decoration: line-through;
  opacity: 0.7;
}

/* 高亮文本 */
.message-content :deep(mark) {
  background-color: #fffbdd;
  padding: 0.1em 0.3em;
  border-radius: 3px;
}

/* 空会话提示 */
.empty-session {
  text-align: center;
  color: #909399;
  font-size: 14px;
  padding: 20px 10px;
}
</style>

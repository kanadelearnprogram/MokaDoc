<template>
  <div class="login-container">
    <div class="login-left">
      <div class="logo">
        <h1>智能文档系统</h1>
        <p>让知识触手可及</p>
      </div>
    </div>
    <div class="login-right">
      <div class="form-tabs">
        <button 
          class="tab" 
          :class="{ active: activeTab === 'login' }"
          @click="activeTab = 'login'"
        >
          登录
        </button>
        <button 
          class="tab" 
          :class="{ active: activeTab === 'register' }"
          @click="activeTab = 'register'"
        >
          注册
        </button>
      </div>
      <div class="form-content">
        <!-- 登录表单 -->
        <div v-if="activeTab === 'login'" class="form active">
          <div class="form-group">
            <label>用户名/邮箱</label>
            <input 
              type="text" 
              v-model="loginForm.username"
              placeholder="请输入用户名或邮箱"
            >
          </div>
          <div class="form-group">
            <label>密码</label>
            <input 
              type="password" 
              v-model="loginForm.password"
              placeholder="请输入密码"
            >
          </div>
          <div class="form-options">
            <label>
              <input type="checkbox" v-model="loginForm.rememberMe"> 
              记住我
            </label>
            <a href="#">忘记密码</a>
          </div>
          <button class="btn btn-primary" @click="handleLogin">登录</button>
        </div>

        <!-- 注册表单 -->
        <div v-if="activeTab === 'register'" class="form active">
          <div class="form-group">
            <label>用户名</label>
            <input 
              type="text" 
              v-model="registerForm.username"
              placeholder="请输入用户名"
            >
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input 
              type="email" 
              v-model="registerForm.email"
              placeholder="请输入邮箱"
            >
          </div>
          <div class="form-group">
            <label>密码</label>
            <input 
              type="password" 
              v-model="registerForm.password"
              placeholder="请输入密码"
            >
          </div>
          <div class="form-group">
            <label>确认密码</label>
            <input 
              type="password" 
              v-model="registerForm.confirmPassword"
              placeholder="请确认密码"
            >
          </div>
          <button class="btn btn-primary" @click="handleRegister">注册</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('login')

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

// 注册表单数据
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

// 处理登录
const handleLogin = () => {
  // TODO: 实现登录逻辑
  console.log('登录:', loginForm)
  // 登录成功后跳转到文档管理页面
  router.push('/document-management')
}

// 处理注册
const handleRegister = () => {
  // TODO: 实现注册逻辑
  if (registerForm.password !== registerForm.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }
  console.log('注册:', registerForm)
  alert('注册成功！请登录')
  activeTab.value = 'login'
}
</script>

<style scoped>
/* 样式已从全局 style.css 引入 */
</style>

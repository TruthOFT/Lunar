<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const form = reactive({ account: '', password: '' })

async function handleLogin() {
  loading.value = true
  try {
    await authStore.login(form.account, form.password)
    router.push('/')
  } catch (e: unknown) {
    // a-form does not auto-show errors for async, use message
    const msg = e instanceof Error ? e.message : '登录失败'
    // imported globally from Antd
    ;(window as any).$message?.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-bg" aria-hidden="true">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
    </div>

    <div class="login-card">
      <div class="login-logo">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="currentColor" class="moon-icon" aria-hidden="true">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
        <h1 class="login-title">LunarAdmin</h1>
      </div>
      <p class="login-sub">Licence 管理系统</p>

      <a-form
        :model="form"
        layout="vertical"
        @finish="handleLogin"
        autocomplete="off"
        class="login-form"
      >
        <a-form-item
          name="account"
          :rules="[{ required: true, message: '请输入账号' }]"
        >
          <a-input
            v-model:value="form.account"
            size="large"
            placeholder="账号"
            allow-clear
          >
            <template #prefix>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="opacity:.5" aria-hidden="true">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
            </template>
          </a-input>
        </a-form-item>

        <a-form-item
          name="password"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <a-input-password
            v-model:value="form.password"
            size="large"
            placeholder="密码"
          >
            <template #prefix>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="opacity:.5" aria-hidden="true">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
              </svg>
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item style="margin-bottom: 0">
          <a-button
            type="primary"
            html-type="submit"
            block
            size="large"
            :loading="loading"
          >
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-base);
  position: relative;
  overflow: hidden;
  padding: 24px;
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: rgba(99, 102, 241, 0.08);
  top: -200px;
  left: -100px;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: rgba(212, 168, 67, 0.06);
  bottom: -150px;
  right: -100px;
}

.login-card {
  position: relative;
  z-index: 1;
  background: #111827;
  border: 1px solid rgba(212, 168, 67, 0.18);
  border-radius: 16px;
  padding: 40px 40px 36px;
  width: 100%;
  max-width: 380px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5), 0 0 30px rgba(212, 168, 67, 0.08);
}

.login-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.moon-icon {
  color: #D4A843;
  filter: drop-shadow(0 0 10px rgba(212, 168, 67, 0.5));
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #F0C060 0%, #D4A843 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  letter-spacing: 0.02em;
}

.login-sub {
  font-size: 12px;
  color: #475569;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  margin: 0 0 28px;
}

.login-form {
  margin-top: 4px;
}
</style>

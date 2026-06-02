import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, type UserInfo } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('admin_token') ?? '')
  const user = ref<UserInfo | null>(null)

  async function login(account: string, password: string) {
    const res = await loginApi({ account, password })
    token.value = res.token
    user.value = res.user
    localStorage.setItem('admin_token', res.token)
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('admin_token')
  }

  return { token, user, login, logout }
})

import axios, { type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'

export const BASE_URL = ''

const AUTH_ERROR_CODES = [4007, 4008, 4009]

const myAxios = axios.create({
  baseURL: BASE_URL,
  timeout: 60000,
  withCredentials: false,
})

myAxios.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

myAxios.interceptors.response.use(
  (response) => response.data,
  (error) => {
    message.error(error.message || '网络请求失败')
    return Promise.reject(error)
  },
)

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export function request<T>(url: string, options: AxiosRequestConfig = {}): Promise<T> {
  return myAxios
    .request<never, ApiResponse<T>>({ url, ...options })
    .then((res) => {
      if (AUTH_ERROR_CODES.includes(res.code)) {
        localStorage.removeItem('admin_token')
        if (!window.location.pathname.includes('/login')) {
          message.warning('登录已失效，请重新登录')
          window.location.href = '/login'
        }
        return Promise.reject(new Error(res.message))
      }
      if (res.code !== 0) {
        return Promise.reject(new Error(res.message || '请求失败'))
      }
      return res.data
    })
}

export default myAxios

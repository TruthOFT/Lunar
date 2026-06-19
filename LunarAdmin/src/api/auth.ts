import { request } from '@/request'

export interface UserInfo {
  id: number
  account: string
  nickname: string
  isVip: boolean
  vipExpireTime: string
  licenceType: string
}

export interface AuthResponse {
  token: string
  user: UserInfo
}

export function loginApi(body: { account: string; password: string }) {
  return request<AuthResponse>('/api/auth/login', {
    method: 'POST',
    data: body,
  })
}

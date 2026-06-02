const BASE_URL = 'http://localhost:7000'

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

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export async function loginApi(req: { account: string; password: string }): Promise<AuthResponse> {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  const json: ApiResponse<AuthResponse> = await res.json()
  if (json.code !== 0) throw new Error(json.message || '登录失败')
  return json.data
}

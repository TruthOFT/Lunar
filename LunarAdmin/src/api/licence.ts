const BASE_URL = 'http://localhost:7000'

function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem('admin_token') ?? ''
  return {
    'Content-Type': 'application/json',
    Authorization: token ? `Bearer ${token}` : '',
  }
}

export interface LicenceGenerateRequest {
  count?: number
  licenceType?: string
  durationDays?: number
  expireTime?: string
  remark?: string
}

export interface LicenceItem {
  id: number
  licenceCode: string
  licenceType: string
  durationDays: number
  expireTime: string
  status: number
  remark: string
  createTime: string
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export async function generateLicences(req: LicenceGenerateRequest): Promise<LicenceItem[]> {
  const res = await fetch(`${BASE_URL}/licences/generate`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(req),
  })
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`)
  }
  const json: ApiResponse<LicenceItem[]> = await res.json()
  if (json.code !== 0) {
    throw new Error(json.message || '生成失败')
  }
  return json.data
}

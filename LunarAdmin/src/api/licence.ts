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

export interface LicenceAdminItem {
  id: number
  licenceCode: string
  licenceType: string
  durationDays: number
  expireTime: string
  status: number
  remark: string
  userId: number | null
  usedTime: string
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${url}`, {
    ...options,
    headers: { ...getAuthHeaders(), ...(options.headers ?? {}) },
  })
  const json: ApiResponse<T> = await res.json()
  if (json.code !== 0) throw new Error(json.message || '请求失败')
  return json.data
}

export async function generateLicences(req: LicenceGenerateRequest): Promise<LicenceItem[]> {
  return request<LicenceItem[]>('/licences/generate', {
    method: 'POST',
    body: JSON.stringify(req),
  })
}

export async function listLicences(
  page: number,
  pageSize: number,
  status?: number | null,
): Promise<PageResult<LicenceAdminItem>> {
  const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) })
  if (status !== undefined && status !== null) params.append('status', String(status))
  return request<PageResult<LicenceAdminItem>>(`/licences?${params}`)
}

export async function updateLicence(
  id: number,
  req: { licenceType?: string; durationDays?: number; remark?: string },
): Promise<void> {
  await request<void>(`/licences/${id}`, {
    method: 'PUT',
    body: JSON.stringify(req),
  })
}

export async function deleteLicence(id: number): Promise<void> {
  await request<void>(`/licences/${id}`, { method: 'DELETE' })
}

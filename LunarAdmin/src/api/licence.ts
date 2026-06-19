import { request } from '@/request'

export interface LicenceGenerateRequest {
  count?: number
  licenceType?: string
  remark?: string
}

export interface LicenceItem {
  id: number
  licenceCode: string
  licenceType: string
  expireTime: string
  status: number
  remark: string
  createTime: string
}

export interface LicenceAdminItem {
  id: number
  licenceCode: string
  licenceType: string
  expireTime: string
  status: number
  remark: string
  userId: number | null
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export function generateLicences(data: LicenceGenerateRequest) {
  return request<LicenceItem[]>('/api/licences/generate', {
    method: 'POST',
    data,
  })
}

export function listLicences(page: number, pageSize: number, status?: number | null) {
  return request<PageResult<LicenceAdminItem>>('/api/licences', {
    method: 'GET',
    params: {
      page,
      pageSize,
      ...(status !== undefined && status !== null ? { status } : {}),
    },
  })
}

export function updateLicence(
  id: number,
  data: { licenceType?: string; remark?: string },
) {
  return request<void>(`/api/licences/${id}`, {
    method: 'PUT',
    data,
  })
}

export function deleteLicence(id: number) {
  return request<void>(`/api/licences/${id}`, {
    method: 'DELETE',
  })
}

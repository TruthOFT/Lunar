import { request } from '@/request'

export interface UserAdminItem {
  id: number
  account: string
  nickname: string
  role: number
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export function listUsers(page: number, pageSize: number, keyword?: string) {
  return request<PageResult<UserAdminItem>>('/api/user/list', {
    method: 'GET',
    params: { page, pageSize, ...(keyword ? { keyword } : {}) },
  })
}

export function updateUserRole(id: number, role: number) {
  return request<void>(`/api/user/${id}/role`, {
    method: 'PUT',
    data: { role },
  })
}

export function deleteUser(id: number) {
  return request<void>(`/api/user/${id}`, {
    method: 'DELETE',
  })
}

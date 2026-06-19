import { request } from '@/request'

export interface AppVersionInfo {
  versionCode: number
  versionName: string
  downloadUrl: string
  changelog: string
  forceUpdate: boolean
}

export function getLatestVersion() {
  return request<AppVersionInfo>('/api/app/version/latest', { method: 'GET' })
}

export function saveVersion(data: AppVersionInfo) {
  return request<void>('/api/app/version', { method: 'POST', data })
}

export function uploadApk(formData: FormData) {
  return request<AppVersionInfo>('/api/app/version/upload', {
    method: 'POST',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

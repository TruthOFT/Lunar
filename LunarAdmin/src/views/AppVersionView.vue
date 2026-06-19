<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getLatestVersion, uploadApk, type AppVersionInfo } from '@/api/appVersion'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const uploading = ref(false)
const current = ref<AppVersionInfo | null>(null)

const apkInputRef = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const changelog = ref('')
const forceUpdate = ref(false)

async function loadData() {
  loading.value = true
  try {
    current.value = await getLatestVersion()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
}

async function handleUpload() {
  if (!selectedFile.value) {
    message.warning('请先选择 APK 文件')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('changelog', changelog.value)
    fd.append('forceUpdate', String(forceUpdate.value))
    const res = await uploadApk(fd)
    current.value = res
    selectedFile.value = null
    changelog.value = ''
    forceUpdate.value = false
    // Reset file input
    if (apkInputRef.value) apkInputRef.value.value = ''
    message.success(`上传成功！版本 ${res.versionName}（${res.versionCode}）已生效`)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <a-layout class="layout">
    <!-- ── Sider ── -->
    <a-layout-sider :width="210" class="sider" :collapsed-width="0">
      <div class="sider-logo">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" class="moon-icon" aria-hidden="true">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
        <span class="sider-title">LunarAdmin</span>
      </div>
      <a-menu mode="inline" class="sider-menu" :selected-keys="['appversion']">
        <a-menu-item key="licence" @click="router.push('/')">
          <template #icon>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
            </svg>
          </template>
          Licence 管理
        </a-menu-item>
        <a-menu-item key="users" @click="router.push('/users')">
          <template #icon>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/>
            </svg>
          </template>
          用户管理
        </a-menu-item>
        <a-menu-item key="appversion" @click="router.push('/app-version')">
          <template #icon>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/>
            </svg>
          </template>
          版本管理
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="top-header">
        <span class="page-heading">版本管理</span>
        <a-dropdown placement="bottomRight">
          <button class="user-btn" aria-label="用户菜单">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
            </svg>
            <span>{{ authStore.user?.account ?? '管理员' }}</span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="logout" danger @click="logout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-layout-header>

      <a-layout-content class="content">
        <a-spin :spinning="loading">
          <!-- Current version info -->
          <div class="info-card" v-if="current">
            <div class="card-label">当前线上版本</div>
            <div class="version-badges">
              <a-tag color="blue">versionCode: {{ current.versionCode }}</a-tag>
              <a-tag color="green">{{ current.versionName }}</a-tag>
              <a-tag v-if="current.forceUpdate" color="red">强制更新</a-tag>
            </div>
            <div class="url-text">{{ current.downloadUrl }}</div>
            <div class="changelog-text" v-if="current.changelog">{{ current.changelog }}</div>
          </div>
          <div class="info-card empty-card" v-else-if="!loading">
            <span style="color:#64748b">暂无版本记录</span>
          </div>

          <!-- Upload card -->
          <div class="form-card">
            <div class="form-title">上传新版本 APK</div>

            <div class="upload-area" @click="apkInputRef?.click()">
              <input ref="apkInputRef" type="file" accept=".apk" style="display:none" @change="onFileChange" />
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="1.5" aria-hidden="true">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
              <div class="upload-hint" v-if="!selectedFile">点击选择 .apk 文件，版本号自动读取</div>
              <div class="upload-hint selected" v-else>✓ {{ selectedFile.name }}</div>
            </div>

            <a-form layout="vertical" style="margin-top: 16px">
              <a-form-item label="更新日志">
                <a-textarea v-model:value="changelog" :rows="3" placeholder="本次更新内容（可选）" />
              </a-form-item>
              <a-form-item label="强制更新">
                <a-switch v-model:checked="forceUpdate" />
                <span class="switch-tip">开启后用户无法跳过</span>
              </a-form-item>
              <a-form-item>
                <a-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="handleUpload">
                  上传并发布
                </a-button>
              </a-form-item>
            </a-form>
          </div>
        </a-spin>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.layout { min-height: 100vh; }

.sider { background: #0B0F1A !important; border-right: 1px solid rgba(255,255,255,0.05); }
.sider-logo { display:flex; align-items:center; gap:10px; padding:20px 20px 16px; border-bottom:1px solid rgba(255,255,255,0.05); }
.moon-icon { color:#D4A843; filter:drop-shadow(0 0 6px rgba(212,168,67,0.5)); flex-shrink:0; }
.sider-title { font-size:15px; font-weight:600; background:linear-gradient(135deg,#F0C060,#D4A843); -webkit-background-clip:text; -webkit-text-fill-color:transparent; background-clip:text; letter-spacing:0.02em; }
.sider-menu { background:transparent !important; border-inline-end:none !important; padding:8px; }

.top-header { background:#111827 !important; border-bottom:1px solid rgba(255,255,255,0.05); padding:0 24px; display:flex; align-items:center; justify-content:space-between; height:56px; line-height:56px; }
.page-heading { font-size:15px; font-weight:600; color:#f1f5f9; }
.user-btn { display:flex; align-items:center; gap:7px; background:rgba(255,255,255,0.04); border:1px solid rgba(255,255,255,0.08); border-radius:8px; color:#94a3b8; font-size:13px; padding:6px 12px; cursor:pointer; transition:all 150ms ease; }
.user-btn:hover { background:rgba(212,168,67,0.1); border-color:rgba(212,168,67,0.25); color:#D4A843; }

.content { padding:24px; background:#020617; }

.info-card {
  background:#111827;
  border:1px solid rgba(255,255,255,0.05);
  border-radius:12px;
  padding:20px;
  margin-bottom:16px;
  max-width:680px;
}
.empty-card { color:#64748b; }
.card-label { font-size:11px; font-weight:600; color:#475569; text-transform:uppercase; letter-spacing:0.05em; margin-bottom:10px; }
.version-badges { display:flex; gap:8px; flex-wrap:wrap; margin-bottom:8px; }
.url-text { font-size:12px; color:#64748b; font-family:'Fira Code',monospace; margin-bottom:6px; }
.changelog-text { font-size:13px; color:#94a3b8; white-space:pre-wrap; }

.form-card { background:#111827; border:1px solid rgba(255,255,255,0.05); border-radius:12px; padding:28px; max-width:680px; }
.form-title { font-size:14px; font-weight:600; color:#94a3b8; text-transform:uppercase; letter-spacing:0.05em; margin-bottom:20px; }

.upload-area {
  border:2px dashed rgba(255,255,255,0.1);
  border-radius:10px;
  padding:32px;
  text-align:center;
  cursor:pointer;
  transition:border-color 150ms;
  display:flex;
  flex-direction:column;
  align-items:center;
  gap:10px;
}
.upload-area:hover { border-color:rgba(212,168,67,0.4); }
.upload-hint { font-size:13px; color:#64748b; }
.upload-hint.selected { color:#D4A843; }

.form-card :deep(.ant-form-item-label > label) { color:#cbd5e1; font-size:13px; }
.switch-tip { margin-left:10px; color:#64748b; font-size:12px; }
</style>

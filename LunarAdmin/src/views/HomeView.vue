<script setup lang="ts">
import { ref, computed } from 'vue'
import { generateLicences, type LicenceGenerateRequest, type LicenceItem } from '@/api/licence'

const form = ref({
  count: 1,
  licenceType: 'vip',
  durationDays: 30,
  expireTime: '',
  remark: '',
})

const licences = ref<LicenceItem[]>([])
const loading = ref(false)
const errorMsg = ref('')
const copiedIds = ref<number[]>([])
const toastVisible = ref(false)
const toastText = ref('')
const showTokenModal = ref(false)
const tokenInput = ref(localStorage.getItem('admin_token') ?? '')
const adminToken = ref(localStorage.getItem('admin_token') ?? '')
const batchCopied = ref(false)

const hasToken = computed(() => !!adminToken.value)

function showToast(msg: string) {
  toastText.value = msg
  toastVisible.value = true
  setTimeout(() => (toastVisible.value = false), 2200)
}

function saveToken() {
  const t = tokenInput.value.trim()
  localStorage.setItem('admin_token', t)
  adminToken.value = t
  showTokenModal.value = false
  showToast('Token 已保存')
}

function formatDatetimeForApi(v: string): string {
  return v.replace('T', ' ') + ':00'
}

async function handleGenerate() {
  errorMsg.value = ''
  loading.value = true
  try {
    const req: LicenceGenerateRequest = {
      count: form.value.count,
      licenceType: form.value.licenceType.trim() || 'vip',
      durationDays: form.value.durationDays,
    }
    if (form.value.expireTime) {
      req.expireTime = formatDatetimeForApi(form.value.expireTime)
    }
    if (form.value.remark.trim()) {
      req.remark = form.value.remark.trim()
    }
    licences.value = await generateLicences(req)
    copiedIds.value = []
    batchCopied.value = false
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '生成失败，请检查 Token 是否正确'
  } finally {
    loading.value = false
  }
}

async function copyOne(item: LicenceItem) {
  try {
    await navigator.clipboard.writeText(item.licenceCode)
    copiedIds.value = [...copiedIds.value, item.id]
    setTimeout(() => {
      copiedIds.value = copiedIds.value.filter((id) => id !== item.id)
    }, 2000)
  } catch {
    showToast('复制失败，请手动复制')
  }
}

async function copyAll() {
  try {
    const codes = licences.value.map((l) => l.licenceCode).join('\n')
    await navigator.clipboard.writeText(codes)
    batchCopied.value = true
    showToast(`已复制 ${licences.value.length} 张卡密`)
    setTimeout(() => (batchCopied.value = false), 2000)
  } catch {
    showToast('复制失败')
  }
}

function isCopied(id: number) {
  return copiedIds.value.includes(id)
}
</script>

<template>
  <div class="page">

    <!-- ── Header ── -->
    <header class="header">
      <div class="header-inner">
        <div class="logo-group">
          <span class="logo-icon" aria-hidden="true">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          </span>
          <div>
            <h1 class="logo-title">LunarAdmin</h1>
            <p class="logo-sub">Licence 管理系统</p>
          </div>
        </div>
        <button
          class="btn-icon"
          :class="{ 'btn-icon--warn': !hasToken }"
          @click="showTokenModal = true"
          :title="hasToken ? '已配置 Token · 点击修改' : '未配置 Token · 点击设置'"
          aria-label="Token 设置"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>
          </svg>
          <span v-if="!hasToken" class="warn-dot" aria-hidden="true"></span>
        </button>
      </div>
    </header>

    <!-- ── Main Content ── -->
    <main class="main">

      <!-- Generate Card -->
      <section class="card">
        <div class="card-header">
          <span class="card-accent" aria-hidden="true"></span>
          <h2 class="card-title">生成卡密</h2>
        </div>

        <form class="form" @submit.prevent="handleGenerate" novalidate>
          <div class="form-grid">
            <div class="field">
              <label class="label" for="f-count">生成数量</label>
              <input
                id="f-count"
                v-model.number="form.count"
                type="number"
                class="input"
                min="1"
                max="100"
                required
              />
              <span class="field-hint">1 – 100 张</span>
            </div>

            <div class="field">
              <label class="label" for="f-type">类型</label>
              <input
                id="f-type"
                v-model="form.licenceType"
                type="text"
                class="input"
                placeholder="vip"
              />
            </div>

            <div class="field">
              <label class="label" for="f-days">有效天数</label>
              <input
                id="f-days"
                v-model.number="form.durationDays"
                type="number"
                class="input"
                min="1"
                max="3650"
                required
              />
              <span class="field-hint">激活后生效</span>
            </div>

            <div class="field">
              <label class="label" for="f-expire">
                卡密过期时间
                <span class="label-opt">可选</span>
              </label>
              <input
                id="f-expire"
                v-model="form.expireTime"
                type="datetime-local"
                class="input"
              />
              <span class="field-hint">卡密本身的使用截止</span>
            </div>
          </div>

          <div class="form-row-bottom">
            <div class="field field--grow">
              <label class="label" for="f-remark">
                备注
                <span class="label-opt">可选</span>
              </label>
              <input
                id="f-remark"
                v-model="form.remark"
                type="text"
                class="input"
                placeholder="渠道、活动名称等"
              />
            </div>

            <button type="submit" class="btn-generate" :disabled="loading">
              <span v-if="loading" class="spinner" aria-hidden="true"></span>
              <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17l-6.2 4.3 2.4-7.4L2 9.4h7.6z"/>
              </svg>
              <span>{{ loading ? '生成中…' : '生成卡密' }}</span>
            </button>
          </div>
        </form>
      </section>

      <!-- Error Alert -->
      <div v-if="errorMsg" class="error-box" role="alert">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        {{ errorMsg }}
      </div>

      <!-- Results Card -->
      <section v-if="licences.length > 0" class="card">
        <div class="card-header results-header">
          <div class="card-header-left">
            <span class="card-accent" aria-hidden="true"></span>
            <h2 class="card-title">生成结果</h2>
            <span class="count-badge font-mono">{{ licences.length }}</span>
          </div>
          <button
            class="btn-batch"
            :class="{ 'btn-batch--copied': batchCopied }"
            @click="copyAll"
            :aria-label="`批量复制全部 ${licences.length} 张卡密`"
          >
            <svg v-if="!batchCopied" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <polygon points="12 2 2 7 12 12 22 7 12 2"/>
              <polyline points="2 17 12 22 22 17"/>
              <polyline points="2 12 12 17 22 12"/>
            </svg>
            <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            {{ batchCopied ? '已复制' : '批量复制' }}
          </button>
        </div>

        <div class="table-wrap">
          <table class="table" aria-label="生成的卡密列表">
            <thead>
              <tr>
                <th scope="col">卡密码</th>
                <th scope="col">类型</th>
                <th scope="col">有效天数</th>
                <th scope="col">过期时间</th>
                <th scope="col">状态</th>
                <th scope="col">备注</th>
                <th scope="col">创建时间</th>
                <th scope="col" aria-label="操作"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in licences" :key="item.id">
                <td class="td-code font-mono">{{ item.licenceCode }}</td>
                <td>
                  <span class="type-badge">{{ item.licenceType }}</span>
                </td>
                <td class="td-num">{{ item.durationDays }}天</td>
                <td class="td-time">{{ item.expireTime || '—' }}</td>
                <td>
                  <span
                    class="status-badge"
                    :class="item.status === 1 ? 'badge-unused' : 'badge-used'"
                  >
                    <span class="badge-dot" aria-hidden="true"></span>
                    {{ item.status === 1 ? '未使用' : '已使用' }}
                  </span>
                </td>
                <td class="td-remark">{{ item.remark || '—' }}</td>
                <td class="td-time">{{ item.createTime }}</td>
                <td>
                  <button
                    class="btn-copy"
                    :class="{ 'btn-copy--done': isCopied(item.id) }"
                    @click="copyOne(item)"
                    :aria-label="isCopied(item.id) ? '已复制' : '复制卡密'"
                    :title="isCopied(item.id) ? '已复制' : '复制'"
                  >
                    <svg v-if="!isCopied(item.id)" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                      <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                      <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>
                    </svg>
                    <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

    </main>

    <!-- ── Token Modal ── -->
    <Transition name="fade">
      <div
        v-if="showTokenModal"
        class="modal-overlay"
        @click.self="showTokenModal = false"
        role="dialog"
        aria-modal="true"
        aria-label="Token 设置"
      >
        <div class="modal">
          <div class="modal-header">
            <div class="modal-title-group">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="color: var(--accent-gold)" aria-hidden="true">
                <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
              </svg>
              <h3 class="modal-title">Admin Token</h3>
            </div>
            <button class="btn-icon" @click="showTokenModal = false" aria-label="关闭">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <p class="modal-desc">
            调用 <code>/licences/generate</code> 所需的管理员 JWT Token，存储于浏览器 localStorage。
          </p>

          <div class="modal-body">
            <label class="label" for="token-input">Bearer Token</label>
            <input
              id="token-input"
              v-model="tokenInput"
              type="password"
              class="input"
              placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
              autocomplete="off"
              @keydown.enter="saveToken"
            />
          </div>

          <div class="modal-footer">
            <button class="btn-cancel" @click="showTokenModal = false">取消</button>
            <button class="btn-save" @click="saveToken">保存</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ── Toast ── -->
    <Transition name="toast">
      <div v-if="toastVisible" class="toast" role="status" aria-live="polite">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
        {{ toastText }}
      </div>
    </Transition>

  </div>
</template>

<style scoped>
/* ── Page ─────────────────────────────────────────────────────── */
.page {
  min-height: 100vh;
  background: var(--bg-base);
  background-image:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(99, 102, 241, 0.07) 0%, transparent 60%),
    radial-gradient(ellipse 60% 40% at 85% 85%, rgba(212, 168, 67, 0.04) 0%, transparent 50%);
}

/* ── Header ───────────────────────────────────────────────────── */
.header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(11, 15, 26, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border-subtle);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  color: var(--accent-gold);
  display: flex;
  filter: drop-shadow(0 0 8px rgba(212, 168, 67, 0.45));
}

.logo-title {
  font-size: 17px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--accent-gold-bright) 0%, var(--accent-gold) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.25;
  letter-spacing: 0.02em;
}

.logo-sub {
  font-size: 11px;
  color: var(--text-muted);
  line-height: 1.2;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

/* ── Icon Button ──────────────────────────────────────────────── */
.btn-icon {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-subtle);
  background: var(--bg-card);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 150ms ease, color 150ms ease, background 150ms ease;
}
.btn-icon:hover {
  border-color: var(--border);
  color: var(--accent-gold);
  background: var(--accent-gold-dim);
}
.btn-icon--warn {
  border-color: rgba(248, 113, 113, 0.3);
  color: #F87171;
}
.btn-icon--warn:hover {
  border-color: rgba(248, 113, 113, 0.5);
  background: rgba(248, 113, 113, 0.08);
  color: #F87171;
}

.warn-dot {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 6px;
  height: 6px;
  background: #F87171;
  border-radius: 50%;
  border: 1px solid var(--bg-card);
}

/* ── Main ─────────────────────────────────────────────────────── */
.main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── Card ─────────────────────────────────────────────────────── */
.card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow);
}

.card-header {
  padding: 20px 24px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.results-header {
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-accent {
  flex-shrink: 0;
  width: 3px;
  height: 16px;
  background: linear-gradient(180deg, var(--accent-gold-bright) 0%, var(--accent-gold) 100%);
  border-radius: 2px;
  box-shadow: 0 0 8px rgba(212, 168, 67, 0.4);
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.01em;
}

.count-badge {
  background: var(--accent-gold-dim);
  color: var(--accent-gold);
  border: 1px solid rgba(212, 168, 67, 0.22);
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
}

/* ── Form ─────────────────────────────────────────────────────── */
.form {
  padding: 18px 24px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.form-row-bottom {
  display: flex;
  gap: 14px;
  align-items: flex-end;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field--grow { flex: 1; }

.label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  letter-spacing: 0.02em;
  user-select: none;
}

.label-opt {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 400;
  margin-left: 2px;
}

.input {
  background: var(--bg-input);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: 13px;
  font-family: inherit;
  padding: 8px 12px;
  outline: none;
  width: 100%;
  transition: border-color 150ms ease, box-shadow 150ms ease;
  -webkit-appearance: none;
  appearance: none;
}
.input::placeholder { color: var(--text-muted); }
.input:focus {
  border-color: var(--border-focus);
  box-shadow: 0 0 0 3px rgba(212, 168, 67, 0.1);
}

input[type="datetime-local"]::-webkit-calendar-picker-indicator {
  filter: invert(0.6) sepia(0.5) hue-rotate(10deg);
  cursor: pointer;
  opacity: 0.5;
}
input[type="datetime-local"]::-webkit-calendar-picker-indicator:hover {
  opacity: 0.9;
}
input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  opacity: 0.4;
}

.field-hint {
  font-size: 11px;
  color: var(--text-muted);
  line-height: 1.3;
}

/* ── Generate Button ──────────────────────────────────────────── */
.btn-generate {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background: linear-gradient(135deg, #A8760A 0%, var(--accent-gold) 50%, var(--accent-gold-bright) 100%);
  color: #1a0f00;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  padding: 9px 22px;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: opacity 150ms ease, transform 150ms ease, box-shadow 150ms ease;
  box-shadow: 0 2px 12px rgba(212, 168, 67, 0.28);
  letter-spacing: 0.02em;
}
.btn-generate:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(212, 168, 67, 0.38);
}
.btn-generate:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(212, 168, 67, 0.2);
}
.btn-generate:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(26, 15, 0, 0.3);
  border-top-color: #1a0f00;
  border-radius: 50%;
  animation: spin 0.65s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ── Error ────────────────────────────────────────────────────── */
.error-box {
  background: var(--danger-bg);
  border: 1px solid rgba(248, 113, 113, 0.22);
  border-radius: var(--radius-sm);
  color: var(--danger);
  font-size: 13px;
  padding: 11px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.4;
}

/* ── Batch Copy ───────────────────────────────────────────────── */
.btn-batch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--accent-gold-dim);
  color: var(--accent-gold);
  border: 1px solid rgba(212, 168, 67, 0.2);
  border-radius: var(--radius-xs);
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  padding: 6px 14px;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease;
  letter-spacing: 0.02em;
  white-space: nowrap;
}
.btn-batch:hover {
  background: rgba(212, 168, 67, 0.2);
  border-color: rgba(212, 168, 67, 0.38);
}
.btn-batch--copied {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
  border-color: rgba(16, 185, 129, 0.25);
}

/* ── Table ────────────────────────────────────────────────────── */
.table-wrap {
  overflow-x: auto;
  padding-bottom: 4px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 720px;
}

.table thead tr {
  border-bottom: 1px solid var(--border-subtle);
}

.table th {
  padding: 11px 16px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-align: left;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  white-space: nowrap;
  user-select: none;
}

.table td {
  padding: 12px 16px;
  font-size: 13px;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-subtle);
  vertical-align: middle;
}

.table tbody tr:last-child td {
  border-bottom: none;
}

.table tbody tr {
  transition: background-color 120ms ease;
}
.table tbody tr:hover {
  background: rgba(255, 255, 255, 0.018);
}

.td-code {
  color: var(--text-primary) !important;
  font-size: 12.5px !important;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.td-num { white-space: nowrap; }

.td-time {
  font-size: 12px !important;
  white-space: nowrap;
  color: var(--text-muted) !important;
}

.td-remark {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── Badges ───────────────────────────────────────────────────── */
.type-badge {
  display: inline-block;
  background: rgba(99, 102, 241, 0.12);
  color: #818CF8;
  border: 1px solid rgba(99, 102, 241, 0.2);
  font-size: 10px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 999px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 999px;
  white-space: nowrap;
  border: 1px solid;
}

.badge-unused {
  background: var(--badge-unused-bg);
  color: var(--badge-unused-color);
  border-color: var(--badge-unused-border);
}

.badge-used {
  background: var(--badge-used-bg);
  color: var(--badge-used-color);
  border-color: var(--badge-used-border);
}

.badge-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

/* ── Row Copy Button ──────────────────────────────────────────── */
.btn-copy {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 150ms ease, color 150ms ease, background 150ms ease;
}
.btn-copy:hover {
  border-color: var(--border);
  color: var(--accent-gold);
  background: var(--accent-gold-dim);
}
.btn-copy--done {
  border-color: rgba(16, 185, 129, 0.28) !important;
  color: #10B981 !important;
  background: rgba(16, 185, 129, 0.1) !important;
}

/* ── Token Modal ──────────────────────────────────────────────── */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.78);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 24px;
}

.modal {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  width: 100%;
  max-width: 420px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.55), var(--shadow-gold);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 20px 10px;
}

.modal-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.modal-desc {
  font-size: 12px;
  color: var(--text-muted);
  padding: 0 20px 14px;
  line-height: 1.55;
}

.modal-desc code {
  font-family: 'Fira Code', monospace;
  font-size: 11px;
  background: rgba(255, 255, 255, 0.07);
  padding: 1px 5px;
  border-radius: 3px;
  color: var(--text-secondary);
}

.modal-body {
  padding: 0 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 14px 20px;
  border-top: 1px solid var(--border-subtle);
}

.btn-cancel {
  padding: 8px 16px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 150ms ease, color 150ms ease;
}
.btn-cancel:hover {
  border-color: var(--border);
  color: var(--text-primary);
}

.btn-save {
  padding: 8px 22px;
  border-radius: var(--radius-xs);
  border: none;
  background: linear-gradient(135deg, #A8760A 0%, var(--accent-gold) 100%);
  color: #1a0f00;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: opacity 150ms ease;
}
.btn-save:hover { opacity: 0.88; }

/* ── Toast ────────────────────────────────────────────────────── */
.toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background: var(--bg-card);
  border: 1px solid rgba(16, 185, 129, 0.28);
  color: #10B981;
  font-size: 13px;
  font-weight: 500;
  padding: 10px 16px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.45);
  z-index: 200;
  pointer-events: none;
}

/* ── Transitions ──────────────────────────────────────────────── */
.fade-enter-active,
.fade-leave-active { transition: opacity 180ms ease; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }

.toast-enter-active,
.toast-leave-active { transition: opacity 180ms ease, transform 180ms ease; }
.toast-enter-from,
.toast-leave-to { opacity: 0; transform: translateY(6px); }

/* ── Responsive ───────────────────────────────────────────────── */
@media (max-width: 900px) {
  .form-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .header-inner { padding: 0 16px; }
  .main { padding: 20px 16px 40px; }
  .form-grid { grid-template-columns: 1fr; }
  .form-row-bottom { flex-direction: column; align-items: stretch; }
  .btn-generate { justify-content: center; }
  .card-header, .form { padding-left: 16px; padding-right: 16px; }
}
</style>

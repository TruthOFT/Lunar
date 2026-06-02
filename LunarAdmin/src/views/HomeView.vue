<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import {
  listLicences,
  generateLicences,
  updateLicence,
  deleteLicence,
  type LicenceAdminItem,
} from '@/api/licence'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// ── Table state ────────────────────────────────────────────────
const tableData = ref<LicenceAdminItem[]>([])
const tableLoading = ref(false)
const filterStatus = ref<number | null>(null)
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

async function loadData() {
  tableLoading.value = true
  try {
    const res = await listLicences(pagination.current, pagination.pageSize, filterStatus.value)
    tableData.value = res.records
    pagination.total = res.total
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    tableLoading.value = false
  }
}

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current ?? 1
  pagination.pageSize = pag.pageSize ?? 10
  loadData()
}

function onFilterChange() {
  pagination.current = 1
  loadData()
}

onMounted(loadData)

// ── Generate Modal ─────────────────────────────────────────────
const showGenModal = ref(false)
const genLoading = ref(false)
const genFormRef = ref()
const genForm = reactive({ count: 1, licenceType: 'vip', durationDays: 30, remark: '' })

async function handleGenerate() {
  try {
    await genFormRef.value?.validate()
  } catch {
    return
  }
  genLoading.value = true
  try {
    const res = await generateLicences({
      count: genForm.count,
      licenceType: genForm.licenceType || 'vip',
      durationDays: genForm.durationDays,
      remark: genForm.remark || undefined,
    })
    message.success(`成功生成 ${res.length} 张卡密`)
    showGenModal.value = false
    Object.assign(genForm, { count: 1, licenceType: 'vip', durationDays: 30, remark: '' })
    pagination.current = 1
    loadData()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '生成失败')
  } finally {
    genLoading.value = false
  }
}

// ── Edit Modal ─────────────────────────────────────────────────
const showEditModal = ref(false)
const editLoading = ref(false)
const editFormRef = ref()
const editForm = reactive({ id: 0, licenceType: '', durationDays: 30, remark: '' })

function openEdit(record: LicenceAdminItem) {
  editForm.id = record.id
  editForm.licenceType = record.licenceType
  editForm.durationDays = record.durationDays
  editForm.remark = record.remark
  showEditModal.value = true
}

async function handleEdit() {
  try {
    await editFormRef.value?.validate()
  } catch {
    return
  }
  editLoading.value = true
  try {
    await updateLicence(editForm.id, {
      licenceType: editForm.licenceType,
      durationDays: editForm.durationDays,
      remark: editForm.remark,
    })
    message.success('修改成功')
    showEditModal.value = false
    loadData()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '修改失败')
  } finally {
    editLoading.value = false
  }
}

// ── Delete ─────────────────────────────────────────────────────
async function handleDelete(id: number) {
  try {
    await deleteLicence(id)
    message.success('删除成功')
    if (tableData.value.length === 1 && pagination.current > 1) {
      pagination.current -= 1
    }
    loadData()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

// ── Logout ─────────────────────────────────────────────────────
function logout() {
  authStore.logout()
  router.push('/login')
}

// ── Columns ────────────────────────────────────────────────────
const columns = [
  { title: 'ID', dataIndex: 'id', width: 70, fixed: 'left' as const },
  { title: '卡密码', dataIndex: 'licenceCode', key: 'licenceCode', width: 230 },
  { title: '类型', dataIndex: 'licenceType', key: 'licenceType', width: 80 },
  { title: '天数', dataIndex: 'durationDays', key: 'durationDays', width: 75 },
  { title: '过期时间', dataIndex: 'expireTime', key: 'expireTime', width: 155 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 95 },
  { title: '激活用户', dataIndex: 'userId', key: 'userId', width: 90 },
  { title: '激活时间', dataIndex: 'usedTime', key: 'usedTime', width: 155 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 155 },
  { title: '操作', key: 'actions', width: 100, fixed: 'right' as const },
]
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
      <a-menu mode="inline" class="sider-menu" :selected-keys="['licence']">
        <a-menu-item key="licence">
          <template #icon>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>
            </svg>
          </template>
          Licence 管理
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <!-- ── Content Area ── -->
    <a-layout>
      <!-- Header -->
      <a-layout-header class="top-header">
        <span class="page-heading">Licence 管理</span>
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

      <!-- Content -->
      <a-layout-content class="content">
        <!-- Toolbar -->
        <div class="toolbar">
          <a-space>
            <a-select
              v-model:value="filterStatus"
              :options="[
                { value: null, label: '全部状态' },
                { value: 1, label: '未使用' },
                { value: 2, label: '已使用' },
              ]"
              style="width: 120px"
              @change="onFilterChange"
            />
            <a-button @click="loadData">
              <template #icon>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                  <polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/>
                  <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
                </svg>
              </template>
              刷新
            </a-button>
          </a-space>
          <a-button type="primary" @click="showGenModal = true">
            <template #icon>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M12 5v14M5 12h14"/>
              </svg>
            </template>
            生成卡密
          </a-button>
        </div>

        <!-- Table -->
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="tableLoading"
          :pagination="{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
            showTotal: (total: number) => `共 ${total} 条`,
          }"
          row-key="id"
          :scroll="{ x: 1200 }"
          size="middle"
          @change="handleTableChange"
          class="licence-table"
        >
          <template #bodyCell="{ column, record }">
            <!-- Licence Code -->
            <template v-if="column.key === 'licenceCode'">
              <a-typography-text
                :copyable="{ text: record.licenceCode }"
                style="font-family: 'Fira Code', monospace; font-size: 12px"
              >{{ record.licenceCode }}</a-typography-text>
            </template>

            <!-- Type -->
            <template v-else-if="column.key === 'licenceType'">
              <a-tag color="purple">{{ record.licenceType }}</a-tag>
            </template>

            <!-- Duration -->
            <template v-else-if="column.key === 'durationDays'">
              {{ record.durationDays }}天
            </template>

            <!-- Status -->
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'success' : 'default'">
                {{ record.status === 1 ? '未使用' : '已使用' }}
              </a-tag>
            </template>

            <!-- User ID -->
            <template v-else-if="column.key === 'userId'">
              <span style="color: #64748b">{{ record.userId ?? '—' }}</span>
            </template>

            <!-- Used time / expire time -->
            <template v-else-if="column.key === 'usedTime' || column.key === 'expireTime'">
              <span style="color: #64748b; font-size: 12px">{{ record[column.key as keyof typeof record] || '—' }}</span>
            </template>

            <!-- Actions -->
            <template v-else-if="column.key === 'actions'">
              <a-space size="small">
                <a-button size="small" @click="openEdit(record)">编辑</a-button>
                <a-popconfirm
                  title="确认删除该卡密？"
                  ok-text="删除"
                  cancel-text="取消"
                  ok-type="danger"
                  @confirm="handleDelete(record.id)"
                >
                  <a-button size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-layout-content>
    </a-layout>
  </a-layout>

  <!-- ── Generate Modal ── -->
  <a-modal
    v-model:open="showGenModal"
    title="生成卡密"
    :confirm-loading="genLoading"
    ok-text="生成"
    cancel-text="取消"
    @ok="handleGenerate"
    @cancel="genFormRef?.resetFields()"
  >
    <a-form
      ref="genFormRef"
      :model="genForm"
      layout="vertical"
      style="margin-top: 8px"
    >
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            label="生成数量"
            name="count"
            :rules="[{ required: true, type: 'number', min: 1, max: 100, message: '1-100' }]"
          >
            <a-input-number v-model:value="genForm.count" :min="1" :max="100" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="类型"
            name="licenceType"
            :rules="[{ required: true, message: '请输入类型' }]"
          >
            <a-input v-model:value="genForm.licenceType" placeholder="vip" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            label="有效天数"
            name="durationDays"
            :rules="[{ required: true, type: 'number', min: 1, max: 3650, message: '1-3650' }]"
          >
            <a-input-number v-model:value="genForm.durationDays" :min="1" :max="3650" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="备注" name="remark">
            <a-input v-model:value="genForm.remark" placeholder="可选" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>

  <!-- ── Edit Modal ── -->
  <a-modal
    v-model:open="showEditModal"
    title="编辑卡密"
    :confirm-loading="editLoading"
    ok-text="保存"
    cancel-text="取消"
    @ok="handleEdit"
  >
    <a-form
      ref="editFormRef"
      :model="editForm"
      layout="vertical"
      style="margin-top: 8px"
    >
      <a-form-item
        label="类型"
        name="licenceType"
        :rules="[{ required: true, message: '请输入类型' }]"
      >
        <a-input v-model:value="editForm.licenceType" />
      </a-form-item>
      <a-form-item
        label="有效天数"
        name="durationDays"
        :rules="[{ required: true, type: 'number', min: 1, max: 3650, message: '1-3650' }]"
      >
        <a-input-number v-model:value="editForm.durationDays" :min="1" :max="3650" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-input v-model:value="editForm.remark" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

/* ── Sider ──────────────────────────────────────────────────── */
.sider {
  background: #0B0F1A !important;
  border-right: 1px solid rgba(255, 255, 255, 0.05);
}

.sider-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.moon-icon {
  color: #D4A843;
  filter: drop-shadow(0 0 6px rgba(212, 168, 67, 0.5));
  flex-shrink: 0;
}

.sider-title {
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #F0C060 0%, #D4A843 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.02em;
}

.sider-menu {
  background: transparent !important;
  border-inline-end: none !important;
  padding: 8px;
}

/* ── Header ─────────────────────────────────────────────────── */
.top-header {
  background: #111827 !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  line-height: 56px;
}

.page-heading {
  font-size: 15px;
  font-weight: 600;
  color: #f1f5f9;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  color: #94a3b8;
  font-size: 13px;
  padding: 6px 12px;
  cursor: pointer;
  transition: all 150ms ease;
}
.user-btn:hover {
  background: rgba(212, 168, 67, 0.1);
  border-color: rgba(212, 168, 67, 0.25);
  color: #D4A843;
}

/* ── Content ────────────────────────────────────────────────── */
.content {
  padding: 24px;
  background: #020617;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

/* ── Table ──────────────────────────────────────────────────── */
.licence-table :deep(.ant-table) {
  background: #111827;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.licence-table :deep(.ant-table-thead > tr > th) {
  background: #0B0F1A;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #475569;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.licence-table :deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  font-size: 13px;
}

.licence-table :deep(.ant-table-tbody > tr:last-child > td) {
  border-bottom: none;
}

.licence-table :deep(.ant-table-pagination) {
  padding: 12px 16px;
  margin: 0 !important;
  background: #111827;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 0 0 12px 12px;
}
</style>

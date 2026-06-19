<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import { listUsers, updateUserRole, deleteUser, type UserAdminItem } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// ── Table state ────────────────────────────────────────────────
const tableData = ref<UserAdminItem[]>([])
const tableLoading = ref(false)
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

async function loadData() {
  tableLoading.value = true
  try {
    const res = await listUsers(pagination.current, pagination.pageSize, keyword.value || undefined)
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

function onSearch() {
  pagination.current = 1
  loadData()
}

onMounted(loadData)

// ── Role toggle ────────────────────────────────────────────────
const roleLoading = ref<number | null>(null)

async function handleToggleRole(record: UserAdminItem) {
  const newRole = record.role === 1 ? 0 : 1
  roleLoading.value = record.id
  try {
    await updateUserRole(record.id, newRole)
    message.success(newRole === 1 ? '已设为管理员' : '已取消管理员')
    loadData()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    roleLoading.value = null
  }
}

// ── Delete ─────────────────────────────────────────────────────
async function handleDelete(id: number) {
  try {
    await deleteUser(id)
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
  { title: 'ID', dataIndex: 'id', width: 80, fixed: 'left' as const },
  { title: '账号', dataIndex: 'account', key: 'account', width: 160 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 160 },
  { title: '角色', dataIndex: 'role', key: 'role', width: 100 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'actions', width: 160, fixed: 'right' as const },
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
      <a-menu mode="inline" class="sider-menu" :selected-keys="['users']">
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

    <!-- ── Content Area ── -->
    <a-layout>
      <!-- Header -->
      <a-layout-header class="top-header">
        <span class="page-heading">用户管理</span>
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
            <a-input-search
              v-model:value="keyword"
              placeholder="搜索账号 / 昵称"
              style="width: 220px"
              allow-clear
              @search="onSearch"
              @pressEnter="onSearch"
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
          :scroll="{ x: 900 }"
          size="middle"
          @change="handleTableChange"
          class="user-table"
        >
          <template #bodyCell="{ column, record }">
            <!-- Role -->
            <template v-if="column.key === 'role'">
              <a-tag :color="record.role === 1 ? 'gold' : 'default'">
                {{ record.role === 1 ? '管理员' : '普通用户' }}
              </a-tag>
            </template>

            <!-- Create time -->
            <template v-else-if="column.key === 'createTime'">
              <span style="color: #64748b; font-size: 12px">{{ record.createTime || '—' }}</span>
            </template>

            <!-- Actions -->
            <template v-else-if="column.key === 'actions'">
              <a-space size="small">
                <a-popconfirm
                  :title="record.role === 1 ? '确认取消该用户的管理员权限？' : '确认设置该用户为管理员？'"
                  ok-text="确认"
                  cancel-text="取消"
                  @confirm="handleToggleRole(record)"
                >
                  <a-button
                    size="small"
                    :loading="roleLoading === record.id"
                  >
                    {{ record.role === 1 ? '取消管理员' : '设为管理员' }}
                  </a-button>
                </a-popconfirm>
                <a-popconfirm
                  title="确认删除该用户？此操作不可恢复。"
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
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

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

.user-table :deep(.ant-table) {
  background: #111827;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.user-table :deep(.ant-table-thead > tr > th) {
  background: #0B0F1A;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #475569;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.user-table :deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  font-size: 13px;
}

.user-table :deep(.ant-table-tbody > tr:last-child > td) {
  border-bottom: none;
}

.user-table :deep(.ant-table-pagination) {
  padding: 12px 16px;
  margin: 0 !important;
  background: #111827;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 0 0 12px 12px;
}
</style>

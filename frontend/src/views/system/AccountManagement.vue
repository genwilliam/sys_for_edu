<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiTag from '@/components/ui/UiTag.vue'
import { useAppStore } from '@/stores/app'
import {
  getAuthAccounts,
  getLeaveRequests,
  reviewLeaveRequest,
  setAuthAccountStatus,
} from '@/services/auth'
import type { AuthAccount, LeaveRequest } from '@/types'

const store = useAppStore()
const accounts = ref<AuthAccount[]>([])
const leaveRequests = ref<LeaveRequest[]>([])
const pageError = ref('')
const pendingAccounts = computed(() =>
  accounts.value.filter((account) => account.status === 'PENDING'),
)
const teacherLeaves = computed(() =>
  leaveRequests.value.filter(
    (request) => request.type === 'TEACHER_LEAVE' && request.status === 'PENDING',
  ),
)

async function load() {
  pageError.value = ''
  try {
    const [accountList, leaveList] = await Promise.all([getAuthAccounts(), getLeaveRequests()])
    accounts.value = accountList
    leaveRequests.value = leaveList
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : '账号数据加载失败'
  }
}

async function setStatus(account: AuthAccount, status: 'ACTIVE' | 'DISABLED') {
  try {
    await setAuthAccountStatus(account.username, status)
    await load()
    store.toast(status === 'ACTIVE' ? '账号已审核并启用' : '账号已停用')
  } catch (error) {
    store.toast(error instanceof Error ? error.message : '账号状态更新失败', 'error')
  }
}

async function decideLeave(request: LeaveRequest, status: 'APPROVED' | 'REJECTED') {
  try {
    await reviewLeaveRequest(request.id, status)
    await load()
    store.toast(status === 'APPROVED' ? '已批准教师请假' : '已驳回教师请假')
  } catch (error) {
    store.toast(error instanceof Error ? error.message : '请假审批失败', 'error')
  }
}

function roleName(role: AuthAccount['role']) {
  return role === 'ADMIN' ? '管理员' : role === 'TEACHER' ? '教师' : '学生'
}

onMounted(() => void load())
</script>

<template>
  <div class="account-management">
    <PageHeader
      title="账号审核与请假审批"
      description="审核教师注册、管理账号启用状态并审批教师请假"
    />
    <p v-if="pageError" class="page-error" role="alert">{{ pageError }}</p>

    <UiCard class="management-card">
      <div class="section-heading">
        <div>
          <h2>教师账号审核</h2>
          <p>新注册的教师账号在管理员审核前不能登录。</p>
        </div>
        <UiTag tone="warning">{{ pendingAccounts.length }} 个待审核</UiTag>
      </div>
      <div v-if="accounts.length" class="account-table-wrap">
        <table class="management-table">
          <thead>
            <tr>
              <th>姓名</th>
              <th>账号</th>
              <th>类型</th>
              <th>档案编号</th>
              <th>校区</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="account in accounts" :key="account.username">
              <td>{{ account.name }}</td>
              <td>{{ account.username }}</td>
              <td>{{ roleName(account.role) }}</td>
              <td>{{ account.personId ?? '—' }}</td>
              <td>{{ account.campus }}</td>
              <td>
                <UiTag
                  :tone="
                    account.status === 'ACTIVE'
                      ? 'success'
                      : account.status === 'PENDING'
                        ? 'warning'
                        : 'neutral'
                  "
                >
                  {{
                    account.status === 'ACTIVE'
                      ? '启用'
                      : account.status === 'PENDING'
                        ? '待审核'
                        : '已停用'
                  }}
                </UiTag>
              </td>
              <td v-if="account.role !== 'ADMIN'" class="account-actions">
                <UiButton
                  v-if="account.status !== 'ACTIVE'"
                  size="sm"
                  @click="setStatus(account, 'ACTIVE')"
                  >{{ account.status === 'PENDING' ? '审核通过' : '启用' }}</UiButton
                >
                <UiButton v-else size="sm" variant="outline" @click="setStatus(account, 'DISABLED')"
                  >停用</UiButton
                >
              </td>
              <td v-else>—</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty-state">暂无账号</p>
    </UiCard>

    <UiCard class="management-card">
      <div class="section-heading">
        <div>
          <h2>待审批教师请假</h2>
          <p>教师请假申请由管理员处理。</p>
        </div>
        <UiTag tone="warning">{{ teacherLeaves.length }} 条待审批</UiTag>
      </div>
      <div v-if="teacherLeaves.length" class="leave-list">
        <article v-for="request in teacherLeaves" :key="request.id" class="leave-item">
          <div>
            <strong>{{
              accounts.find((account) => account.personId === request.teacherId)?.name ??
              request.teacherId
            }}</strong>
            <span>{{ request.leaveDate }} · {{ request.reason }}</span>
          </div>
          <div class="account-actions">
            <UiButton size="sm" @click="decideLeave(request, 'APPROVED')">批准</UiButton>
            <UiButton size="sm" variant="outline" @click="decideLeave(request, 'REJECTED')"
              >驳回</UiButton
            >
          </div>
        </article>
      </div>
      <p v-else class="empty-state">暂无待审批的教师请假</p>
    </UiCard>
  </div>
</template>

<style scoped>
.account-management {
  display: grid;
  gap: 18px;
}
.management-card {
  padding: 20px;
}
.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.section-heading h2 {
  font-size: 16px;
  color: var(--gray-800);
}
.section-heading p {
  margin-top: 5px;
  color: var(--gray-500);
  font-size: 13px;
}
.account-table-wrap {
  overflow-x: auto;
}
.management-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 13px;
}
.management-table th,
.management-table td {
  padding: 12px 10px;
  border-bottom: 1px solid var(--gray-100);
  white-space: nowrap;
}
.management-table th {
  color: var(--gray-500);
  font-weight: 500;
}
.management-table td {
  color: var(--gray-700);
}
.account-actions {
  display: flex;
  gap: 8px;
}
.leave-list {
  display: grid;
}
.leave-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border-top: 1px solid var(--gray-100);
}
.leave-item > div:first-child {
  display: grid;
  gap: 6px;
}
.leave-item span,
.empty-state,
.page-error {
  color: var(--gray-500);
  font-size: 13px;
}
.empty-state {
  padding: 14px 0;
}
.page-error {
  color: var(--danger-strong);
}
</style>

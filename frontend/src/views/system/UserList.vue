<script setup lang="ts">
import { computed, ref } from 'vue'
import type { User } from '@/types'
import { users } from '@/data/mock'
import { useAppStore } from '@/stores/app'
import PageHeader from '@/components/PageHeader.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiSelect from '@/components/ui/UiSelect.vue'
import UiTable from '@/components/ui/UiTable.vue'
import type { TableColumn } from '@/types'
import UiModal from '@/components/ui/UiModal.vue'
import UiTag from '@/components/ui/UiTag.vue'
import UiPagination from '@/components/ui/UiPagination.vue'
import UiIcon from '@/components/ui/UiIcon.vue'

const store = useAppStore()

const list = ref<User[]>(JSON.parse(JSON.stringify(users)) as User[])
const keyword = ref('')
const roleFilter = ref('')
const page = ref(1)
const pageSize = 10
const modalOpen = ref(false)
const editing = ref<User | null>(null)

const roleOptions = [
  { label: '超级管理员', value: '超级管理员' },
  { label: '校区校长', value: '校区校长' },
  { label: '教务老师', value: '教务老师' },
  { label: '教师', value: '教师' },
]

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '禁用', value: 'inactive' },
]

const columns: TableColumn[] = [
  { key: 'name', title: '用户', width: '200px' },
  { key: 'role', title: '角色', width: '130px' },
  { key: 'campus', title: '所属校区', width: '130px' },
  { key: 'lastLogin', title: '最近登录', width: '170px' },
  { key: 'status', title: '状态', width: '100px' },
  { key: 'actions', title: '操作', width: '100px', align: 'center' },
]

const filtered = computed(() =>
  list.value.filter((u) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || u.name.includes(kw) || u.username.includes(kw)
    const matchRole = !roleFilter.value || u.role === roleFilter.value
    return matchKw && matchRole
  }),
)

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const form = ref<User>({
  id: '',
  username: '',
  name: '',
  role: '教务老师',
  campus: '朝阳中心',
  status: 'active',
  lastLogin: '—',
})

function openCreate() {
  editing.value = null
  form.value = {
    id: `U${String(Date.now()).slice(-6)}`,
    username: '',
    name: '',
    role: '教务老师',
    campus: '朝阳中心',
    status: 'active',
    lastLogin: '—',
  }
  modalOpen.value = true
}

function openEdit(row: User) {
  editing.value = row
  form.value = { ...row }
  modalOpen.value = true
}

function save() {
  if (!form.value.name.trim() || !form.value.username.trim()) {
    store.toast('请填写姓名与账号', 'warning')
    return
  }
  if (editing.value) {
    Object.assign(editing.value, form.value)
    store.toast('用户信息已更新')
  } else {
    list.value.unshift({ ...form.value })
    store.toast('用户创建成功')
  }
  modalOpen.value = false
}

function remove(row: User) {
  list.value = list.value.filter((u) => u.id !== row.id)
  store.toast('已删除该用户', 'info')
}
</script>

<template>
  <div>
    <PageHeader title="用户管理" description="管理系统账号、角色分配与启用状态">
      <UiButton icon="plus" @click="openCreate">新增用户</UiButton>
    </PageHeader>

    <UiCard>
      <div class="page-toolbar">
        <div class="page-toolbar__search">
          <UiInput v-model="keyword" icon="search" placeholder="搜索姓名 / 账号" />
        </div>
        <div class="page-toolbar__select">
          <UiSelect v-model="roleFilter" :options="roleOptions" placeholder="全部角色" />
        </div>
        <div class="page-toolbar__spacer" />
        <span class="page-toolbar__count">共 {{ filtered.length }} 个用户</span>
      </div>

      <UiTable :columns="columns" :data="paged">
        <template #cell-name="{ row }">
          <div class="avatar-cell">
            <span class="avatar">{{ row.name.charAt(0) }}</span>
            <div>
              <div class="cell-primary">{{ row.name }}</div>
              <div class="cell-sub">@{{ row.username }}</div>
            </div>
          </div>
        </template>
        <template #cell-role="{ row }">
          <UiTag tone="brand">{{ row.role }}</UiTag>
        </template>
        <template #cell-status="{ row }">
          <UiTag :tone="row.status === 'active' ? 'success' : 'neutral'" dot>
            {{ row.status === 'active' ? '启用' : '禁用' }}
          </UiTag>
        </template>
        <template #cell-actions="{ row }">
          <div class="row-actions">
            <button class="icon-btn" title="编辑" @click="openEdit(row)">
              <UiIcon name="edit" :size="16" />
            </button>
            <button class="icon-btn icon-btn--danger" title="删除" @click="remove(row)">
              <UiIcon name="trash" :size="16" />
            </button>
          </div>
        </template>
      </UiTable>

      <UiPagination v-model="page" :total="filtered.length" :page-size="pageSize" />
    </UiCard>

    <UiModal v-model="modalOpen" :title="editing ? '编辑用户' : '新增用户'" width="560px">
      <div class="form-grid">
        <UiInput v-model="form.name" label="姓名" placeholder="真实姓名" required />
        <UiInput v-model="form.username" label="登录账号" placeholder="登录用户名" required />
        <UiSelect v-model="form.role" label="角色" :options="roleOptions" />
        <UiSelect
          v-model="form.campus"
          label="所属校区"
          :options="[
            { label: '全部校区', value: '全部校区' },
            { label: '朝阳中心', value: '朝阳中心' },
            { label: '中关村', value: '中关村' },
            { label: '总部基地', value: '总部基地' },
            { label: '万达', value: '万达' },
          ]"
        />
        <UiSelect v-model="form.status" label="状态" :options="statusOptions" />
      </div>
      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton @click="save">保存</UiButton>
      </template>
    </UiModal>
  </div>
</template>

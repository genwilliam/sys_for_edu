<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Role } from '@/types'
import { useAppStore } from '@/stores/app'
import PageHeader from '@/components/PageHeader.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiModal from '@/components/ui/UiModal.vue'
import UiTag from '@/components/ui/UiTag.vue'
import UiIcon from '@/components/ui/UiIcon.vue'

const store = useAppStore()

const list = computed(() => store.state.roles)
const modalOpen = ref(false)
const editing = ref<Role | null>(null)

const allPermissions = [
  '校区管理',
  '教师管理',
  '学生管理',
  '班级管理',
  '课程管理',
  '教室管理',
  '排课管理',
  '考勤管理',
  '订单管理',
  '回访管理',
  '数据报表',
  '用户管理',
  '角色权限',
]

const form = ref<Role>({
  id: '',
  name: '',
  code: '',
  description: '',
  memberCount: 0,
  permissions: [],
})

const selected = ref<string[]>([])

function openCreate() {
  editing.value = null
  form.value = { id: '', name: '', code: '', description: '', memberCount: 0, permissions: [] }
  selected.value = []
  modalOpen.value = true
}

function openEdit(role: Role) {
  editing.value = role
  form.value = { ...role }
  selected.value = role.permissions.includes('全部权限')
    ? [...allPermissions]
    : [...role.permissions]
  modalOpen.value = true
}

function togglePermission(p: string) {
  const i = selected.value.indexOf(p)
  if (i === -1) selected.value.push(p)
  else selected.value.splice(i, 1)
}

async function save() {
  if (!form.value.name.trim()) {
    store.toast('请填写角色名称', 'warning')
    return
  }
  const permissions = selected.value.includes('全部权限') ? ['全部权限'] : [...selected.value]
  if (editing.value) {
    if (
      !(await store.updateBusinessRecord('roles', editing.value.id, {
        ...form.value,
        permissions,
      }))
    )
      return
    store.toast('角色权限已更新')
  } else {
    if (
      !(await store.createBusinessRecord('roles', {
        ...form.value,
        permissions,
        memberCount: 0,
      }))
    )
      return
    store.toast('角色创建成功')
  }
  modalOpen.value = false
}

async function remove(role: Role) {
  if (!(await store.deleteBusinessRecord('roles', role.id))) return
  store.toast('已删除该角色', 'info')
}
</script>

<template>
  <div>
    <PageHeader title="角色权限" description="配置系统角色及其可访问的功能模块权限">
      <UiButton icon="plus" @click="openCreate">新增角色</UiButton>
    </PageHeader>

    <div class="role-grid">
      <UiCard v-for="role in list" :key="role.id" class="role-card">
        <div class="role-card__head">
          <span class="role-card__icon">
            <UiIcon name="shield" :size="20" />
          </span>
          <div class="role-card__title">
            <div class="role-card__name">{{ role.name }}</div>
            <div class="role-card__code">{{ role.code }}</div>
          </div>
          <div class="role-card__actions">
            <button class="icon-btn" title="编辑" @click="openEdit(role)">
              <UiIcon name="edit" :size="16" />
            </button>
            <button class="icon-btn icon-btn--danger" title="删除" @click="remove(role)">
              <UiIcon name="trash" :size="16" />
            </button>
          </div>
        </div>

        <p class="role-card__desc">{{ role.description }}</p>

        <div class="role-card__meta">
          <UiTag tone="brand">{{ role.memberCount }} 名成员</UiTag>
        </div>

        <div class="role-card__perms">
          <UiTag v-for="p in role.permissions" :key="p" tone="neutral">{{ p }}</UiTag>
        </div>
      </UiCard>
    </div>

    <UiModal
      v-model="modalOpen"
      :title="editing ? `编辑角色 · ${editing.name}` : '新增角色'"
      width="620px"
    >
      <div class="form-grid">
        <UiInput v-model="form.name" label="角色名称" placeholder="如：校区校长" required />
        <UiInput v-model="form.code" label="角色编码" placeholder="如：campus_principal" />
        <div class="form-grid__full">
          <UiInput v-model="form.description" label="角色描述" placeholder="简要描述该角色的职责" />
        </div>
      </div>

      <div class="perm-section">
        <p class="perm-section__label">功能权限</p>
        <div class="perm-grid">
          <label
            v-for="p in allPermissions"
            :key="p"
            class="perm-item"
            :class="{ 'perm-item--checked': selected.includes(p) }"
          >
            <input type="checkbox" :checked="selected.includes(p)" @change="togglePermission(p)" />
            <span>{{ p }}</span>
          </label>
        </div>
      </div>

      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton @click="save">保存</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<style scoped>
.role-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.role-card__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--brand-50);
  color: var(--brand-600);
  flex-shrink: 0;
}

.role-card__title {
  flex: 1;
}

.role-card__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--gray-800);
}

.role-card__code {
  font-size: 12px;
  color: var(--gray-400);
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.role-card__desc {
  margin-top: 12px;
  font-size: 13px;
  color: var(--gray-500);
  line-height: 1.6;
}

.role-card__meta {
  margin-top: 12px;
}

.role-card__perms {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.perm-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--gray-100);
}

.perm-section__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--gray-700);
  margin-bottom: 12px;
}

.perm-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.perm-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--gray-700);
  cursor: pointer;
  transition: all var(--transition);
}

.perm-item:hover {
  border-color: var(--brand-300);
}

.perm-item--checked {
  background: var(--brand-50);
  border-color: var(--brand-400);
  color: var(--brand-700);
}

.perm-item input {
  accent-color: var(--brand-600);
  width: 15px;
  height: 15px;
}

@media (max-width: 900px) {
  .role-grid {
    grid-template-columns: 1fr;
  }

  .perm-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

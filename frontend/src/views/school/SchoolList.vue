<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Campus } from '@/types'
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

const list = computed(() => store.state.campuses)
const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = 10

const modalOpen = ref(false)
const editing = ref<Campus | null>(null)

const statusOptions = [
  { label: '营业中', value: 'active' },
  { label: '已停业', value: 'inactive' },
]

const columns: TableColumn[] = [
  { key: 'name', title: '校区名称' },
  { key: 'principal', title: '负责人', width: '110px' },
  { key: 'phone', title: '联系电话', width: '140px' },
  { key: 'classroomCount', title: '教室数', width: '90px', align: 'center' },
  { key: 'studentCount', title: '学员数', width: '90px', align: 'center' },
  { key: 'status', title: '状态', width: '100px' },
  { key: 'actions', title: '操作', width: '100px', align: 'center' },
]

const filtered = computed(() =>
  list.value.filter((c) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || c.name.includes(kw) || c.principal.includes(kw)
    const matchStatus = !statusFilter.value || c.status === statusFilter.value
    return matchKw && matchStatus
  }),
)

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const form = ref<Campus>({
  id: '',
  name: '',
  address: '',
  principal: '',
  phone: '',
  status: 'active',
  classroomCount: 0,
  studentCount: 0,
  openedDate: '',
})

function openCreate() {
  editing.value = null
  form.value = {
    id: '',
    name: '',
    address: '',
    principal: '',
    phone: '',
    status: 'active',
    classroomCount: 0,
    studentCount: 0,
    openedDate: new Date().toISOString().slice(0, 10),
  }
  modalOpen.value = true
}

function openEdit(row: Campus) {
  editing.value = row
  form.value = { ...row }
  modalOpen.value = true
}

async function save() {
  if (!form.value.name.trim()) {
    store.toast('请填写校区名称', 'warning')
    return
  }
  if (editing.value) {
    if (!(await store.updateBusinessRecord('campuses', editing.value.id, form.value))) return
    store.toast('校区信息已更新')
  } else {
    const created = await store.createBusinessRecord('campuses', {
      ...form.value,
      classroomCount: Number(form.value.classroomCount) || 0,
    })
    if (!created) return
    store.toast('校区创建成功')
  }
  modalOpen.value = false
}

async function remove(row: Campus) {
  if (!(await store.deleteBusinessRecord('campuses', row.id))) return
  store.toast('已删除该校区', 'info')
}
</script>

<template>
  <div>
    <PageHeader title="校区管理" description="管理学校下属各校区的档案、负责人与运营状态">
      <UiButton variant="outline" icon="download" @click="store.toast('已导出校区数据')"
        >导出</UiButton
      >
      <UiButton icon="plus" @click="openCreate">新增校区</UiButton>
    </PageHeader>

    <UiCard>
      <div class="page-toolbar">
        <div class="page-toolbar__search">
          <UiInput v-model="keyword" icon="search" placeholder="搜索校区名称 / 负责人" />
        </div>
        <div class="page-toolbar__select">
          <UiSelect v-model="statusFilter" :options="statusOptions" placeholder="全部状态" />
        </div>
        <div class="page-toolbar__spacer" />
        <span class="page-toolbar__count">共 {{ filtered.length }} 个校区</span>
      </div>

      <UiTable :columns="columns" :data="paged">
        <template #cell-name="{ row }">
          <div class="cell-primary">{{ row.name }}</div>
          <div class="cell-sub">{{ row.address }}</div>
        </template>
        <template #cell-status="{ row }">
          <UiTag :tone="row.status === 'active' ? 'success' : 'neutral'" dot>
            {{ row.status === 'active' ? '营业中' : '已停业' }}
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

    <UiModal v-model="modalOpen" :title="editing ? '编辑校区' : '新增校区'" width="640px">
      <div class="form-grid">
        <UiInput v-model="form.name" label="校区名称" placeholder="如：朝阳中心" required />
        <UiInput v-model="form.principal" label="负责人" placeholder="负责人姓名" />
        <UiInput v-model="form.phone" label="联系电话" placeholder="座机 / 手机号" />
        <UiSelect v-model="form.status" label="运营状态" :options="statusOptions" />
        <UiInput v-model="form.openedDate" label="开业日期" type="date" class="form-grid__full" />
        <div class="form-grid__full">
          <UiInput v-model="form.address" label="详细地址" placeholder="省市区 + 街道门牌号" />
        </div>
      </div>
      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton @click="save">保存</UiButton>
      </template>
    </UiModal>
  </div>
</template>

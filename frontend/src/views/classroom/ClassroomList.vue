<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Classroom } from '@/types'
import { classrooms } from '@/data/mock'
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

const list = ref<Classroom[]>(JSON.parse(JSON.stringify(classrooms)) as Classroom[])
const keyword = ref('')
const campusFilter = ref('')
const page = ref(1)
const pageSize = 10

const modalOpen = ref(false)
const editing = ref<Classroom | null>(null)

const statusOptions = [
  { label: '空闲', value: '空闲' },
  { label: '使用中', value: '使用中' },
  { label: '维护中', value: '维护中' },
]

const columns: TableColumn[] = [
  { key: 'name', title: '教室', width: '180px' },
  { key: 'campus', title: '所属校区', width: '140px' },
  { key: 'capacity', title: '容量', width: '80px', align: 'center' },
  { key: 'equipment', title: '设备配置', width: '280px' },
  { key: 'status', title: '状态', width: '100px' },
  { key: 'actions', title: '操作', width: '100px', align: 'center' },
]

const statusTone: Record<string, 'success' | 'brand' | 'warning'> = {
  空闲: 'success',
  使用中: 'brand',
  维护中: 'warning',
}

const filtered = computed(() =>
  list.value.filter((c) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || c.name.includes(kw) || c.type.includes(kw)
    const matchCampus = !campusFilter.value || c.campus === campusFilter.value
    return matchKw && matchCampus
  }),
)

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const form = ref<Classroom>({
  id: '',
  name: '',
  campus: '朝阳中心',
  capacity: 20,
  type: '标准教室',
  equipment: [],
  status: '空闲',
})

const equipmentText = ref('')

function openCreate() {
  editing.value = null
  form.value = {
    id: `R${String(Date.now()).slice(-6)}`,
    name: '',
    campus: '朝阳中心',
    capacity: 20,
    type: '标准教室',
    equipment: [],
    status: '空闲',
  }
  equipmentText.value = ''
  modalOpen.value = true
}

function openEdit(row: Classroom) {
  editing.value = row
  form.value = { ...row }
  equipmentText.value = row.equipment.join(', ')
  modalOpen.value = true
}

function save() {
  if (!form.value.name.trim()) {
    store.toast('请填写教室名称', 'warning')
    return
  }
  const equipment = equipmentText.value
    .split(/[,，、]/)
    .map((s) => s.trim())
    .filter(Boolean)
  if (editing.value) {
    Object.assign(editing.value, form.value, {
      capacity: Number(form.value.capacity) || 0,
      equipment,
    })
    store.toast('教室信息已更新')
  } else {
    list.value.unshift({ ...form.value, capacity: Number(form.value.capacity) || 0, equipment })
    store.toast('教室创建成功')
  }
  modalOpen.value = false
}

function remove(row: Classroom) {
  list.value = list.value.filter((c) => c.id !== row.id)
  store.toast('已删除该教室', 'info')
}
</script>

<template>
  <div>
    <PageHeader title="教室管理" description="维护教室资源、容量、设备配置与使用状态">
      <UiButton variant="outline" icon="download" @click="store.toast('已导出教室数据')"
        >导出</UiButton
      >
      <UiButton icon="plus" @click="openCreate">新增教室</UiButton>
    </PageHeader>

    <UiCard>
      <div class="page-toolbar">
        <div class="page-toolbar__search">
          <UiInput v-model="keyword" icon="search" placeholder="搜索教室名称 / 类型" />
        </div>
        <div class="page-toolbar__select">
          <UiSelect
            v-model="campusFilter"
            :options="[
              { label: '朝阳中心', value: '朝阳中心' },
              { label: '中关村', value: '中关村' },
              { label: '总部基地', value: '总部基地' },
            ]"
            placeholder="全部校区"
          />
        </div>
        <div class="page-toolbar__spacer" />
        <span class="page-toolbar__count">共 {{ filtered.length }} 间教室</span>
      </div>

      <UiTable :columns="columns" :data="paged">
        <template #cell-name="{ row }">
          <div class="cell-primary">{{ row.name }}</div>
          <div class="cell-sub">{{ row.type }}</div>
        </template>
        <template #cell-equipment="{ row }">
          <div class="equipment">
            <UiTag v-for="e in row.equipment" :key="e" tone="neutral">{{ e }}</UiTag>
          </div>
        </template>
        <template #cell-status="{ row }">
          <UiTag :tone="statusTone[row.status] ?? 'neutral'" dot>{{ row.status }}</UiTag>
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

    <UiModal v-model="modalOpen" :title="editing ? '编辑教室' : '新增教室'" width="560px">
      <div class="form-grid">
        <UiInput v-model="form.name" label="教室名称" placeholder="如：教室 301" required />
        <UiSelect
          v-model="form.campus"
          label="所属校区"
          :options="[
            { label: '朝阳中心', value: '朝阳中心' },
            { label: '中关村', value: '中关村' },
            { label: '总部基地', value: '总部基地' },
            { label: '万达', value: '万达' },
          ]"
        />
        <UiInput v-model="form.capacity" label="容量（人）" type="number" />
        <UiSelect
          v-model="form.type"
          label="教室类型"
          :options="[
            { label: '标准教室', value: '标准教室' },
            { label: '多媒体教室', value: '多媒体教室' },
            { label: '实验室', value: '实验室' },
          ]"
        />
        <UiSelect v-model="form.status" label="状态" :options="statusOptions" />
        <div class="form-grid__full">
          <UiInput
            v-model="equipmentText"
            label="设备配置"
            placeholder="多个设备用逗号分隔，如：投影仪, 白板, 空调"
          />
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
.equipment {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>

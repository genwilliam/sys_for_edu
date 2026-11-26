<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Course } from '@/types'
import { courses } from '@/data/mock'
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

const list = ref<Course[]>(JSON.parse(JSON.stringify(courses)) as Course[])
const keyword = ref('')
const categoryFilter = ref('')
const page = ref(1)
const pageSize = 10

const modalOpen = ref(false)
const editing = ref<Course | null>(null)

const categoryOptions = [
  { label: '英语', value: '英语' },
  { label: '数学', value: '数学' },
  { label: '语文', value: '语文' },
  { label: '理科', value: '理科' },
]

const statusOptions = [
  { label: '上架', value: 'active' },
  { label: '下架', value: 'inactive' },
]

const columns: TableColumn[] = [
  { key: 'name', title: '课程名称', width: '240px' },
  { key: 'category', title: '类别', width: '90px' },
  { key: 'grade', title: '年级段', width: '140px' },
  { key: 'teacher', title: '主讲教师', width: '110px' },
  { key: 'hours', title: '课时', width: '80px', align: 'center' },
  { key: 'price', title: '价格', width: '110px', align: 'right' },
  { key: 'status', title: '状态', width: '90px' },
  { key: 'actions', title: '操作', width: '100px', align: 'center' },
]

const filtered = computed(() =>
  list.value.filter((c) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || c.name.includes(kw) || c.teacher.includes(kw)
    const matchCategory = !categoryFilter.value || c.category === categoryFilter.value
    return matchKw && matchCategory
  }),
)

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const form = ref<Course>({
  id: '',
  name: '',
  category: '英语',
  grade: '小学 1-3 年级',
  teacher: '',
  price: 0,
  hours: 32,
  status: 'active',
  description: '',
})

function openCreate() {
  editing.value = null
  form.value = {
    id: `CO${String(Date.now()).slice(-6)}`,
    name: '',
    category: '英语',
    grade: '小学 1-3 年级',
    teacher: '',
    price: 0,
    hours: 32,
    status: 'active',
    description: '',
  }
  modalOpen.value = true
}

function openEdit(row: Course) {
  editing.value = row
  form.value = { ...row }
  modalOpen.value = true
}

function save() {
  if (!form.value.name.trim()) {
    store.toast('请填写课程名称', 'warning')
    return
  }
  if (editing.value) {
    Object.assign(editing.value, form.value, {
      price: Number(form.value.price) || 0,
      hours: Number(form.value.hours) || 0,
    })
    store.toast('课程信息已更新')
  } else {
    list.value.unshift({
      ...form.value,
      price: Number(form.value.price) || 0,
      hours: Number(form.value.hours) || 0,
    })
    store.toast('课程创建成功')
  }
  modalOpen.value = false
}

function remove(row: Course) {
  list.value = list.value.filter((c) => c.id !== row.id)
  store.toast('已删除该课程', 'info')
}
</script>

<template>
  <div>
    <PageHeader title="课程管理" description="维护课程体系、定价、课时与上架状态">
      <UiButton variant="outline" icon="download" @click="store.toast('已导出课程数据')"
        >导出</UiButton
      >
      <UiButton icon="plus" @click="openCreate">新增课程</UiButton>
    </PageHeader>

    <UiCard>
      <div class="page-toolbar">
        <div class="page-toolbar__search">
          <UiInput v-model="keyword" icon="search" placeholder="搜索课程 / 教师" />
        </div>
        <div class="page-toolbar__select">
          <UiSelect v-model="categoryFilter" :options="categoryOptions" placeholder="全部类别" />
        </div>
        <div class="page-toolbar__spacer" />
        <span class="page-toolbar__count">共 {{ filtered.length }} 门课程</span>
      </div>

      <UiTable :columns="columns" :data="paged">
        <template #cell-name="{ row }">
          <div class="cell-primary">{{ row.name }}</div>
          <div class="cell-sub">{{ row.description }}</div>
        </template>
        <template #cell-category="{ row }">
          <UiTag tone="brand">{{ row.category }}</UiTag>
        </template>
        <template #cell-price="{ row }">
          <span class="price">¥ {{ row.price.toLocaleString() }}</span>
        </template>
        <template #cell-status="{ row }">
          <UiTag :tone="row.status === 'active' ? 'success' : 'neutral'" dot>
            {{ row.status === 'active' ? '上架' : '下架' }}
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

    <UiModal v-model="modalOpen" :title="editing ? '编辑课程' : '新增课程'" width="640px">
      <div class="form-grid">
        <UiInput v-model="form.name" label="课程名称" placeholder="如：英语启蒙" required />
        <UiSelect v-model="form.category" label="课程类别" :options="categoryOptions" />
        <UiSelect
          v-model="form.grade"
          label="年级段"
          :options="[
            { label: '小学 1-3 年级', value: '小学 1-3 年级' },
            { label: '小学 4-6 年级', value: '小学 4-6 年级' },
            { label: '初中 7-9 年级', value: '初中 7-9 年级' },
          ]"
        />
        <UiSelect
          v-model="form.teacher"
          label="主讲教师"
          :options="[
            { label: '陈雅婷', value: '陈雅婷' },
            { label: '张伟', value: '张伟' },
            { label: '李娜', value: '李娜' },
            { label: '王强', value: '王强' },
            { label: '孙浩', value: '孙浩' },
          ]"
        />
        <UiInput v-model="form.hours" label="课时" type="number" />
        <UiInput v-model="form.price" label="价格（元）" type="number" />
        <UiSelect v-model="form.status" label="状态" :options="statusOptions" />
        <div class="form-grid__full">
          <UiInput v-model="form.description" label="课程简介" placeholder="一句话描述课程特色" />
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
.price {
  font-weight: 600;
  color: var(--brand-600);
}
</style>

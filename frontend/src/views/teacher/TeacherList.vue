<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Teacher } from '@/types'
import { teachers } from '@/data/mock'
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

const list = ref<Teacher[]>(JSON.parse(JSON.stringify(teachers)) as Teacher[])
const keyword = ref('')
const subjectFilter = ref('')
const page = ref(1)
const pageSize = 10

const modalOpen = ref(false)
const editing = ref<Teacher | null>(null)

const subjectOptions = [
  { label: '英语', value: '英语' },
  { label: '数学', value: '数学' },
  { label: '语文', value: '语文' },
  { label: '物理', value: '物理' },
  { label: '化学', value: '化学' },
]

const genderOptions = [
  { label: '男', value: 'male' },
  { label: '女', value: 'female' },
]

const statusOptions = [
  { label: '在职', value: 'active' },
  { label: '离职', value: 'inactive' },
]

const columns: TableColumn[] = [
  { key: 'name', title: '教师', width: '200px' },
  { key: 'subject', title: '科目', width: '100px' },
  { key: 'title', title: '职称', width: '110px' },
  { key: 'campus', title: '所属校区', width: '120px' },
  { key: 'rating', title: '评分', width: '90px' },
  { key: 'studentCount', title: '学员数', width: '90px', align: 'center' },
  { key: 'status', title: '状态', width: '90px' },
  { key: 'actions', title: '操作', width: '100px', align: 'center' },
]

const filtered = computed(() =>
  list.value.filter((t) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || t.name.includes(kw) || t.phone.includes(kw)
    const matchSubject = !subjectFilter.value || t.subject === subjectFilter.value
    return matchKw && matchSubject
  }),
)

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const form = ref<Teacher>({
  id: '',
  name: '',
  gender: 'female',
  phone: '',
  subject: '英语',
  title: '中级讲师',
  status: 'active',
  joinDate: '',
  rating: 4.5,
  campus: '',
  studentCount: 0,
})

function openCreate() {
  editing.value = null
  form.value = {
    id: `T${String(Date.now()).slice(-6)}`,
    name: '',
    gender: 'female',
    phone: '',
    subject: '英语',
    title: '中级讲师',
    status: 'active',
    joinDate: new Date().toISOString().slice(0, 10),
    rating: 4.5,
    campus: '朝阳中心',
    studentCount: 0,
  }
  modalOpen.value = true
}

function openEdit(row: Teacher) {
  editing.value = row
  form.value = { ...row }
  modalOpen.value = true
}

function save() {
  if (!form.value.name.trim()) {
    store.toast('请填写教师姓名', 'warning')
    return
  }
  if (editing.value) {
    Object.assign(editing.value, form.value)
    store.toast('教师信息已更新')
  } else {
    list.value.unshift({ ...form.value })
    store.toast('教师添加成功')
  }
  modalOpen.value = false
}

function remove(row: Teacher) {
  list.value = list.value.filter((t) => t.id !== row.id)
  store.toast('已移除该教师', 'info')
}
</script>

<template>
  <div>
    <PageHeader title="教师管理" description="维护教师档案、任教科目、职称与在职状态">
      <UiButton variant="outline" icon="download" @click="store.toast('已导出教师数据')"
        >导出</UiButton
      >
      <UiButton icon="plus" @click="openCreate">新增教师</UiButton>
    </PageHeader>

    <UiCard>
      <div class="page-toolbar">
        <div class="page-toolbar__search">
          <UiInput v-model="keyword" icon="search" placeholder="搜索教师姓名 / 手机号" />
        </div>
        <div class="page-toolbar__select">
          <UiSelect v-model="subjectFilter" :options="subjectOptions" placeholder="全部科目" />
        </div>
        <div class="page-toolbar__spacer" />
        <span class="page-toolbar__count">共 {{ filtered.length }} 名教师</span>
      </div>

      <UiTable :columns="columns" :data="paged">
        <template #cell-name="{ row }">
          <div class="avatar-cell">
            <span class="avatar" :class="row.gender === 'female' ? 'avatar--green' : ''">
              {{ row.name.charAt(0) }}
            </span>
            <div>
              <div class="cell-primary">{{ row.name }}</div>
              <div class="cell-sub">{{ row.phone }}</div>
            </div>
          </div>
        </template>
        <template #cell-subject="{ row }">
          <UiTag tone="brand">{{ row.subject }}</UiTag>
        </template>
        <template #cell-rating="{ row }">
          <span class="rating"
            ><UiIcon name="star" :size="14" class="rating__star" />{{ row.rating }}</span
          >
        </template>
        <template #cell-status="{ row }">
          <UiTag :tone="row.status === 'active' ? 'success' : 'neutral'" dot>
            {{ row.status === 'active' ? '在职' : '离职' }}
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

    <UiModal v-model="modalOpen" :title="editing ? '编辑教师' : '新增教师'" width="640px">
      <div class="form-grid">
        <UiInput v-model="form.name" label="姓名" placeholder="教师姓名" required />
        <UiSelect v-model="form.gender" label="性别" :options="genderOptions" />
        <UiInput v-model="form.phone" label="手机号" placeholder="11 位手机号" />
        <UiSelect v-model="form.subject" label="任教科目" :options="subjectOptions" />
        <UiSelect
          v-model="form.title"
          label="职称"
          :options="[
            { label: '高级讲师', value: '高级讲师' },
            { label: '教学主管', value: '教学主管' },
            { label: '中级讲师', value: '中级讲师' },
          ]"
        />
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
        <UiSelect v-model="form.status" label="状态" :options="statusOptions" />
        <UiInput v-model="form.joinDate" label="入职日期" type="date" />
      </div>
      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton @click="save">保存</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<style scoped>
.rating {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  color: var(--gray-700);
}

.rating__star {
  color: var(--warning);
}
</style>

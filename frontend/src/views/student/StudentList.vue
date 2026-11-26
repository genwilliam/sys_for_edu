<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Student } from '@/types'
import { students } from '@/data/mock'
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

const list = ref<Student[]>(JSON.parse(JSON.stringify(students)) as Student[])
const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = 10

const modalOpen = ref(false)
const editing = ref<Student | null>(null)

const statusOptions = [
  { label: '在读', value: '在读' },
  { label: '停课', value: '停课' },
  { label: '结业', value: '结业' },
]

const genderOptions = [
  { label: '男', value: 'male' },
  { label: '女', value: 'female' },
]

const columns: TableColumn[] = [
  { key: 'name', title: '学生', width: '190px' },
  { key: 'age', title: '年龄', width: '70px', align: 'center' },
  { key: 'campus', title: '校区', width: '110px' },
  { key: 'className', title: '班级', width: '170px' },
  { key: 'courseCount', title: '在读课程', width: '90px', align: 'center' },
  { key: 'enrollDate', title: '报名日期', width: '120px' },
  { key: 'status', title: '状态', width: '90px' },
  { key: 'actions', title: '操作', width: '100px', align: 'center' },
]

const statusTone: Record<string, 'success' | 'warning' | 'neutral'> = {
  在读: 'success',
  停课: 'warning',
  结业: 'neutral',
}

const filtered = computed(() =>
  list.value.filter((s) => {
    const kw = keyword.value.trim()
    const matchKw =
      !kw || s.name.includes(kw) || s.guardian.includes(kw) || s.className.includes(kw)
    const matchStatus = !statusFilter.value || s.status === statusFilter.value
    return matchKw && matchStatus
  }),
)

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const form = ref<Student>({
  id: '',
  name: '',
  gender: 'male',
  age: 8,
  phone: '',
  guardian: '',
  campus: '朝阳中心',
  className: '',
  status: '在读',
  enrollDate: '',
  courseCount: 1,
})

function openCreate() {
  editing.value = null
  form.value = {
    id: `S${String(Date.now()).slice(-6)}`,
    name: '',
    gender: 'male',
    age: 8,
    phone: '',
    guardian: '',
    campus: '朝阳中心',
    className: '',
    status: '在读',
    enrollDate: new Date().toISOString().slice(0, 10),
    courseCount: 1,
  }
  modalOpen.value = true
}

function openEdit(row: Student) {
  editing.value = row
  form.value = { ...row }
  modalOpen.value = true
}

function save() {
  if (!form.value.name.trim()) {
    store.toast('请填写学生姓名', 'warning')
    return
  }
  if (editing.value) {
    Object.assign(editing.value, form.value, { age: Number(form.value.age) || 8 })
    store.toast('学生信息已更新')
  } else {
    list.value.unshift({ ...form.value, age: Number(form.value.age) || 8 })
    store.toast('学生添加成功')
  }
  modalOpen.value = false
}

function remove(row: Student) {
  list.value = list.value.filter((s) => s.id !== row.id)
  store.toast('已移除该学生', 'info')
}
</script>

<template>
  <div>
    <PageHeader title="学生管理" description="管理学员档案、所在班级、报名情况与在读状态">
      <UiButton variant="outline" icon="download" @click="store.toast('已导出学生数据')"
        >导出</UiButton
      >
      <UiButton icon="plus" @click="openCreate">新增学生</UiButton>
    </PageHeader>

    <UiCard>
      <div class="page-toolbar">
        <div class="page-toolbar__search">
          <UiInput v-model="keyword" icon="search" placeholder="搜索姓名 / 家长 / 班级" />
        </div>
        <div class="page-toolbar__select">
          <UiSelect v-model="statusFilter" :options="statusOptions" placeholder="全部状态" />
        </div>
        <div class="page-toolbar__spacer" />
        <span class="page-toolbar__count">共 {{ filtered.length }} 名学生</span>
      </div>

      <UiTable :columns="columns" :data="paged">
        <template #cell-name="{ row }">
          <div class="avatar-cell">
            <span
              class="avatar"
              :class="row.gender === 'female' ? 'avatar--green' : 'avatar--amber'"
            >
              {{ row.name.charAt(0) }}
            </span>
            <div>
              <div class="cell-primary">{{ row.name }}</div>
              <div class="cell-sub">{{ row.guardian }}</div>
            </div>
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

    <UiModal v-model="modalOpen" :title="editing ? '编辑学生' : '新增学生'" width="640px">
      <div class="form-grid">
        <UiInput v-model="form.name" label="姓名" placeholder="学生姓名" required />
        <UiSelect v-model="form.gender" label="性别" :options="genderOptions" />
        <UiInput v-model="form.age" label="年龄" type="number" />
        <UiInput v-model="form.phone" label="联系电话" placeholder="家长手机号" />
        <UiInput v-model="form.guardian" label="监护人" placeholder="如：林国栋（父）" />
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
        <UiInput v-model="form.className" label="所在班级" placeholder="班级名称" />
        <UiSelect v-model="form.status" label="状态" :options="statusOptions" />
        <UiInput v-model="form.enrollDate" label="报名日期" type="date" class="form-grid__full" />
      </div>
      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton @click="save">保存</UiButton>
      </template>
    </UiModal>
  </div>
</template>

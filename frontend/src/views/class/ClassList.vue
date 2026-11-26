<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ClassGroup } from '@/types'
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

const list = computed(() => store.state.classGroups)
const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = 10

const modalOpen = ref(false)
const editing = ref<ClassGroup | null>(null)

const statusOptions = [
  { label: '开课中', value: '开课中' },
  { label: '未开班', value: '未开班' },
  { label: '已结课', value: '已结课' },
]

const columns: TableColumn[] = [
  { key: 'name', title: '班级', width: '190px' },
  { key: 'course', title: '课程', width: '120px' },
  { key: 'teacher', title: '授课教师', width: '110px' },
  { key: 'studentCount', title: '学员', width: '110px' },
  { key: 'room', title: '教室', width: '100px' },
  { key: 'schedule', title: '上课时间', width: '170px' },
  { key: 'status', title: '状态', width: '100px' },
  { key: 'actions', title: '操作', width: '100px', align: 'center' },
]

const statusTone: Record<string, 'success' | 'warning' | 'neutral'> = {
  开课中: 'success',
  未开班: 'warning',
  已结课: 'neutral',
}

const filtered = computed(() =>
  list.value.filter((c) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || c.name.includes(kw) || c.teacher.includes(kw) || c.course.includes(kw)
    const matchStatus = !statusFilter.value || c.status === statusFilter.value
    return matchKw && matchStatus
  }),
)

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const form = ref<ClassGroup>({
  id: '',
  name: '',
  grade: '小学 1-3 年级',
  campus: '朝阳中心',
  course: '',
  teacher: '',
  studentCount: 0,
  capacity: 32,
  room: '',
  status: '未开班',
  schedule: '',
})

function openCreate() {
  editing.value = null
  form.value = {
    id: '',
    name: '',
    grade: '小学 1-3 年级',
    campus: '朝阳中心',
    course: '',
    teacher: '',
    studentCount: 0,
    capacity: 32,
    room: '',
    status: '未开班',
    schedule: '',
  }
  modalOpen.value = true
}

function openEdit(row: ClassGroup) {
  editing.value = row
  form.value = { ...row }
  modalOpen.value = true
}

function openDetail(row: ClassGroup) {
  store.navigate(`/class/${encodeURIComponent(row.id)}`)
}

async function save() {
  if (!form.value.name.trim()) {
    store.toast('请填写班级名称', 'warning')
    return
  }
  if (editing.value) {
    const updated = await store.updateBusinessRecord('classes', editing.value.id, {
      ...form.value,
      capacity: Number(form.value.capacity) || 32,
      studentCount: Number(form.value.studentCount) || 0,
    })
    if (!updated) return
    store.toast('班级信息已更新')
  } else {
    const created = await store.createBusinessRecord('classes', {
      ...form.value,
      capacity: Number(form.value.capacity) || 32,
      studentCount: Number(form.value.studentCount) || 0,
    })
    if (!created) return
    store.state.classStudentAssignments[created.id] = []
    store.toast('班级创建成功')
  }
  modalOpen.value = false
}

async function remove(row: ClassGroup) {
  if (!(await store.deleteBusinessRecord('classes', row.id))) return
  delete store.state.classStudentAssignments[row.id]
  store.toast('已删除该班级', 'info')
}
</script>

<template>
  <div>
    <PageHeader title="班级管理" description="管理班级开班信息、任课教师、学员容量与上课安排">
      <UiButton variant="outline" icon="download" @click="store.toast('已导出班级数据')"
        >导出</UiButton
      >
      <UiButton icon="plus" @click="openCreate">新增班级</UiButton>
    </PageHeader>

    <UiCard>
      <div class="page-toolbar">
        <div class="page-toolbar__search">
          <UiInput v-model="keyword" icon="search" placeholder="搜索班级 / 教师 / 课程" />
        </div>
        <div class="page-toolbar__select">
          <UiSelect v-model="statusFilter" :options="statusOptions" placeholder="全部状态" />
        </div>
        <div class="page-toolbar__spacer" />
        <span class="page-toolbar__count">共 {{ filtered.length }} 个班级</span>
      </div>

      <UiTable :columns="columns" :data="paged">
        <template #cell-name="{ row }">
          <button type="button" class="class-name-link" @click="openDetail(row)">
            {{ row.name }}
          </button>
          <div class="cell-sub">{{ row.grade }} · {{ row.campus }}</div>
        </template>
        <template #cell-studentCount="{ row }">
          <span :class="{ 'is-full': store.studentsInClass(row.id).length >= row.capacity }">
            {{ store.studentsInClass(row.id).length }} / {{ row.capacity }} 人
          </span>
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

    <UiModal v-model="modalOpen" :title="editing ? '编辑班级' : '新增班级'" width="640px">
      <div class="form-grid">
        <UiInput v-model="form.name" label="班级名称" placeholder="如：英语启蒙 A 班" required />
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
          v-model="form.course"
          label="课程"
          :options="[
            { label: '英语启蒙', value: '英语启蒙' },
            { label: '数学思维', value: '数学思维' },
            { label: '语文素养', value: '语文素养' },
            { label: '物理竞赛', value: '物理竞赛' },
            { label: '化学实验', value: '化学实验' },
          ]"
        />
        <UiSelect
          v-model="form.teacher"
          label="授课教师"
          :options="[
            { label: '陈雅婷', value: '陈雅婷' },
            { label: '张伟', value: '张伟' },
            { label: '李娜', value: '李娜' },
            { label: '王强', value: '王强' },
            { label: '孙浩', value: '孙浩' },
          ]"
        />
        <UiInput v-model="form.room" label="教室" placeholder="如：教室 301" />
        <UiInput v-model="form.capacity" label="容量（人）" type="number" />
        <UiSelect v-model="form.status" label="状态" :options="statusOptions" />
        <UiInput v-model="form.schedule" label="上课时间" placeholder="如：周六 09:00-10:30" />
      </div>
      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton @click="save">保存</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<style scoped>
.is-full {
  color: var(--danger-strong);
  font-weight: 600;
}

.class-name-link {
  padding: 0;
  color: var(--brand-700);
  font-weight: 600;
  text-align: left;
}

.class-name-link:hover {
  text-decoration: underline;
}
</style>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Attendance, AttendanceStatus } from '@/types'
import { attendanceRecords } from '@/data/mock'
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

const store = useAppStore()

const list = ref<Attendance[]>(JSON.parse(JSON.stringify(attendanceRecords)) as Attendance[])
const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = 10
const modalOpen = ref(false)

const statusOptions = ['出勤', '缺勤', '迟到', '请假'].map((s) => ({ label: s, value: s }))

const statusTone: Record<AttendanceStatus, 'success' | 'danger' | 'warning' | 'info'> = {
  出勤: 'success',
  缺勤: 'danger',
  迟到: 'warning',
  请假: 'info',
}

const columns: TableColumn[] = [
  { key: 'student', title: '学生', width: '110px' },
  { key: 'className', title: '班级 / 课程', width: '210px' },
  { key: 'date', title: '日期', width: '120px' },
  { key: 'time', title: '时间', width: '120px' },
  { key: 'teacher', title: '授课教师', width: '110px' },
  { key: 'status', title: '考勤状态', width: '110px' },
  { key: 'note', title: '备注', width: '180px' },
]

const filtered = computed(() =>
  list.value.filter((a) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || a.student.includes(kw) || a.className.includes(kw)
    const matchStatus = !statusFilter.value || a.status === statusFilter.value
    return matchKw && matchStatus
  }),
)

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const form = ref<Attendance>({
  id: '',
  student: '',
  className: '',
  course: '',
  date: new Date().toISOString().slice(0, 10),
  time: '09:00-10:30',
  status: '出勤',
  teacher: '',
  note: '',
})

function openCreate() {
  form.value = {
    id: `A${String(Date.now()).slice(-6)}`,
    student: '',
    className: '',
    course: '',
    date: new Date().toISOString().slice(0, 10),
    time: '09:00-10:30',
    status: '出勤',
    teacher: '',
    note: '',
  }
  modalOpen.value = true
}

function save() {
  if (!form.value.student.trim()) {
    store.toast('请填写学生姓名', 'warning')
    return
  }
  list.value.unshift({ ...form.value })
  modalOpen.value = false
  store.toast('考勤记录已提交')
}
</script>

<template>
  <div>
    <PageHeader title="考勤管理" description="录入与查看课堂考勤，实时统计缺勤、迟到情况">
      <UiButton variant="outline" icon="download" @click="store.toast('已导出考勤报表')"
        >导出报表</UiButton
      >
      <UiButton icon="plus" @click="openCreate">考勤录入</UiButton>
    </PageHeader>

    <UiCard>
      <div class="page-toolbar">
        <div class="page-toolbar__search">
          <UiInput v-model="keyword" icon="search" placeholder="搜索学生 / 班级" />
        </div>
        <div class="page-toolbar__select">
          <UiSelect v-model="statusFilter" :options="statusOptions" placeholder="全部状态" />
        </div>
        <div class="page-toolbar__spacer" />
        <span class="page-toolbar__count">共 {{ filtered.length }} 条记录</span>
      </div>

      <UiTable :columns="columns" :data="paged">
        <template #cell-student="{ row }">
          <span class="cell-primary">{{ row.student }}</span>
        </template>
        <template #cell-className="{ row }">
          <div class="cell-primary">{{ row.className }}</div>
          <div class="cell-sub">{{ row.course }}</div>
        </template>
        <template #cell-status="{ row }">
          <UiTag :tone="statusTone[row.status as AttendanceStatus] ?? 'neutral'" dot>
            {{ row.status }}
          </UiTag>
        </template>
        <template #cell-note="{ row }">
          <span :class="row.note ? '' : 'text-muted'">{{ row.note || '—' }}</span>
        </template>
      </UiTable>

      <UiPagination v-model="page" :total="filtered.length" :page-size="pageSize" />
    </UiCard>

    <UiModal v-model="modalOpen" title="考勤录入" width="560px">
      <div class="form-grid">
        <UiInput v-model="form.student" label="学生姓名" placeholder="学生姓名" required />
        <UiSelect v-model="form.status" label="考勤状态" :options="statusOptions" />
        <UiInput v-model="form.className" label="班级" placeholder="如：英语启蒙 A 班" />
        <UiInput v-model="form.course" label="课程" placeholder="课程名称" />
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
        <UiInput v-model="form.date" label="日期" type="date" />
        <UiInput v-model="form.time" label="上课时间" placeholder="09:00-10:30" />
        <div class="form-grid__full">
          <UiInput v-model="form.note" label="备注" placeholder="如：迟到 15 分钟" />
        </div>
      </div>
      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton @click="save">提交</UiButton>
      </template>
    </UiModal>
  </div>
</template>

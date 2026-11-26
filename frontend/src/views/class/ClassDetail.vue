<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getScheduleItems } from '@/services/schedule'
import { useAppStore } from '@/stores/app'
import type { ClassGroup, ScheduleItem, Student, TableColumn } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiSelect from '@/components/ui/UiSelect.vue'
import UiTable from '@/components/ui/UiTable.vue'
import UiTabs from '@/components/ui/UiTabs.vue'
import UiTag from '@/components/ui/UiTag.vue'

const store = useAppStore()
const classId = computed(() => {
  const encodedId = store.state.currentPath.split('/')[2] ?? ''
  try {
    return decodeURIComponent(encodedId)
  } catch {
    return encodedId
  }
})
const classGroup = computed<ClassGroup | undefined>(() =>
  store.state.classGroups.find(({ id }) => id === classId.value),
)
const classTeacher = computed(() =>
  store.state.teachers.find(({ name }) => name === classGroup.value?.teacher),
)
const activeTab = ref('students')
const studentKeyword = ref('')
const studentStatus = ref('')
const studentCampus = ref('')
const addKeyword = ref('')
const selectedStudentIds = ref<string[]>([])
const classSchedule = ref<ScheduleItem[]>([])
const scheduleLoading = ref(false)
const scheduleError = ref('')

const tabs = [
  { label: '学生信息', value: 'students' },
  { label: '添加学生', value: 'add' },
  { label: '班级课表', value: 'schedule' },
]
const studentColumns: TableColumn[] = [
  { key: 'name', title: '姓名', width: '110px' },
  { key: 'id', title: '学号', width: '100px' },
  { key: 'gender', title: '性别', width: '70px' },
  { key: 'age', title: '年龄', width: '70px' },
  { key: 'phone', title: '手机号', width: '120px' },
  { key: 'campus', title: '所属校区', width: '110px' },
  { key: 'source', title: '来源', width: '100px' },
  { key: 'birthDate', title: '出生日期', width: '110px' },
  { key: 'enrollDate', title: '注册日期', width: '110px' },
  { key: 'classNames', title: '班级', width: '180px' },
  { key: 'actions', title: '操作', width: '80px', align: 'center' },
]
const scheduleColumns: TableColumn[] = [
  { key: 'weekday', title: '星期', width: '90px' },
  { key: 'time', title: '上课时间', width: '150px' },
  { key: 'course', title: '课程', width: '140px' },
  { key: 'teacher', title: '授课教师', width: '120px' },
  { key: 'room', title: '教室', width: '120px' },
  { key: 'type', title: '类型', width: '100px' },
  { key: 'status', title: '状态', width: '100px' },
]

const classStudentIds = computed(() => store.studentsInClass(classId.value))
const classStudents = computed(() =>
  store.state.students.filter((student) => classStudentIds.value.includes(student.id)),
)
const campusOptions = computed(() =>
  [
    { label: '全部校区', value: '' },
    ...new Set(store.state.students.map(({ campus }) => campus)),
  ].map((option) => (typeof option === 'string' ? { label: option, value: option } : option)),
)
const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '在读', value: '在读' },
  { label: '停课', value: '停课' },
  { label: '结业', value: '结业' },
]
const filteredStudents = computed(() => {
  const keyword = studentKeyword.value.trim().toLowerCase()
  return classStudents.value.filter((student) => {
    const matchesKeyword =
      !keyword ||
      student.name.toLowerCase().includes(keyword) ||
      student.id.toLowerCase().includes(keyword) ||
      student.phone.toLowerCase().includes(keyword)
    return (
      matchesKeyword &&
      (!studentStatus.value || student.status === studentStatus.value) &&
      (!studentCampus.value || student.campus === studentCampus.value)
    )
  })
})
const availableStudents = computed(() => {
  const keyword = addKeyword.value.trim().toLowerCase()
  return store.state.students.filter((student) => {
    const matchesKeyword =
      !keyword ||
      student.name.toLowerCase().includes(keyword) ||
      student.id.toLowerCase().includes(keyword) ||
      student.phone.toLowerCase().includes(keyword)
    return !classStudentIds.value.includes(student.id) && matchesKeyword
  })
})
const classNamesForStudent = (studentId: string) =>
  store.state.classGroups
    .filter((group) => store.studentsInClass(group.id).includes(studentId))
    .map(({ name }) => name)

function studentRow(student: Student) {
  return {
    ...student,
    gender: student.gender === 'female' ? '女' : '男',
    source: '未填写',
    birthDate: '未填写',
    classNames: classNamesForStudent(student.id).join('、') || '未填写',
  }
}

const studentRows = computed(() => filteredStudents.value.map(studentRow))
const scheduleRows = computed(() =>
  classSchedule.value.map((item) => ({
    ...item,
    time: `${item.startTime}–${item.endTime}`,
  })),
)

async function loadClassSchedule() {
  if (!classGroup.value) return
  scheduleLoading.value = true
  scheduleError.value = ''
  try {
    const items = await getScheduleItems({ course: classGroup.value.course })
    classSchedule.value = items.filter(({ className }) => className === classGroup.value?.name)
  } catch (error) {
    scheduleError.value = error instanceof Error ? error.message : '班级课表加载失败'
  } finally {
    scheduleLoading.value = false
  }
}

onMounted(() => void loadClassSchedule())

async function addSelectedStudents() {
  if (!classGroup.value || selectedStudentIds.value.length === 0) return
  const updated = await store.addStudentsToClass(classGroup.value.id, selectedStudentIds.value)
  if (!updated) return
  store.toast(`已添加 ${selectedStudentIds.value.length} 名学生`)
  selectedStudentIds.value = []
  activeTab.value = 'students'
}

function openAddTab() {
  activeTab.value = 'add'
  selectedStudentIds.value = []
}

async function removeStudent(student: Student) {
  if (!classGroup.value) return
  const confirmed = window.confirm(
    `确定将学生「${student.name}」移出班级「${classGroup.value.name}」吗？`,
  )
  if (!confirmed) return
  if (!(await store.removeStudentFromClass(classGroup.value.id, student.id))) return
  store.toast(`已将${student.name}移出班级`, 'info')
}
</script>

<template>
  <div v-if="classGroup">
    <PageHeader :title="classGroup.name" description="班级详情">
      <UiButton variant="outline" icon="chevron-left" @click="store.navigate('/class')">
        返回班级列表
      </UiButton>
    </PageHeader>

    <UiCard class="class-info-card">
      <div class="class-info-heading">
        <div>
          <div class="class-title">{{ classGroup.name }}</div>
          <div class="class-subtitle">{{ classGroup.grade }} · {{ classGroup.course }}</div>
        </div>
        <UiTag :tone="classGroup.status === '开课中' ? 'success' : 'warning'" dot>
          {{ classGroup.status }}
        </UiTag>
      </div>
      <div class="class-info-grid">
        <div>
          <span>所属校区</span><strong>{{ classGroup.campus }}</strong>
        </div>
        <div>
          <span>授课教师</span>
          <strong>{{ classGroup.teacher }} · {{ classTeacher?.phone ?? '未登记联系方式' }}</strong>
        </div>
        <div>
          <span>班级人数</span
          ><strong>{{ classStudents.length }} / {{ classGroup.capacity }} 人</strong>
        </div>
        <div>
          <span>教室</span><strong>{{ classGroup.room || '未填写' }}</strong>
        </div>
        <div>
          <span>上课时间</span><strong>{{ classGroup.schedule || '未填写' }}</strong>
        </div>
      </div>
    </UiCard>

    <UiCard class="detail-card">
      <div class="detail-toolbar">
        <UiTabs v-model="activeTab" :tabs="tabs" />
        <span class="page-toolbar__spacer" />
        <span v-if="activeTab === 'students'" class="student-total">
          共 {{ classStudents.length }} 名学生
        </span>
        <UiButton v-if="activeTab === 'students'" icon="plus" @click="openAddTab"
          >添加学生</UiButton
        >
        <UiButton
          v-if="activeTab === 'add'"
          icon="check"
          :disabled="selectedStudentIds.length === 0"
          @click="addSelectedStudents"
          >加入班级（{{ selectedStudentIds.length }}）</UiButton
        >
      </div>

      <template v-if="activeTab === 'students'">
        <div class="filter-toolbar">
          <UiInput v-model="studentKeyword" icon="search" placeholder="搜索姓名 / 学号 / 手机号" />
          <UiSelect v-model="studentStatus" :options="statusOptions" placeholder="" />
          <UiSelect v-model="studentCampus" :options="campusOptions" placeholder="" />
        </div>
        <UiTable :columns="studentColumns" :data="studentRows" row-key="id">
          <template #cell-gender="{ value }">{{ value }}</template>
          <template #cell-actions="{ row }">
            <button type="button" class="text-action" @click="removeStudent(row)">移出班级</button>
          </template>
          <template #empty>暂无班级学生，可在“添加学生”中从现有学生清单选择。</template>
        </UiTable>
      </template>

      <template v-else-if="activeTab === 'add'">
        <div class="add-toolbar">
          <p>从现有学生清单中选择，可同时加入多个班级。</p>
          <UiInput v-model="addKeyword" icon="search" placeholder="搜索姓名 / 学号 / 手机号" />
        </div>
        <div class="select-list">
          <label v-for="student in availableStudents" :key="student.id" class="student-option">
            <input v-model="selectedStudentIds" type="checkbox" :value="student.id" />
            <span class="student-option__name">{{ student.name }}</span>
            <span>{{ student.id }}</span>
            <span>{{ student.campus }}</span>
            <span>{{ student.phone }}</span>
            <span>{{ student.status }}</span>
          </label>
          <p v-if="availableStudents.length === 0" class="empty-state">没有符合条件的可添加学生</p>
        </div>
      </template>

      <template v-else>
        <p v-if="scheduleError" class="schedule-error" role="alert">{{ scheduleError }}</p>
        <p v-else-if="scheduleLoading" class="empty-state">正在加载班级课表…</p>
        <UiTable :columns="scheduleColumns" :data="scheduleRows" row-key="id">
          <template #cell-status="{ row }">
            <UiTag :tone="row.status === '待上课' ? 'brand' : 'neutral'" dot>{{
              row.status
            }}</UiTag>
          </template>
          <template #empty>暂无该班级的课表安排。</template>
        </UiTable>
      </template>
    </UiCard>
  </div>
  <UiCard v-else class="not-found">
    <p>未找到该班级。</p>
    <UiButton variant="outline" @click="store.navigate('/class')">返回班级列表</UiButton>
  </UiCard>
</template>

<style scoped>
.class-info-card {
  padding: 22px 24px;
  margin-bottom: 20px;
}

.class-info-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--gray-100);
}

.class-title {
  color: var(--gray-900);
  font-size: 19px;
  font-weight: 700;
}

.class-subtitle {
  margin-top: 5px;
  color: var(--gray-500);
  font-size: 13px;
}

.class-info-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px;
  padding-top: 18px;
}

.class-info-grid div {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.class-info-grid span,
.student-total {
  color: var(--gray-500);
  font-size: 12px;
}

.class-info-grid strong {
  color: var(--gray-800);
  font-size: 13px;
  font-weight: 600;
}

.detail-card {
  overflow: hidden;
}

.detail-toolbar,
.filter-toolbar,
.add-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
}

.detail-toolbar {
  border-bottom: 1px solid var(--gray-100);
}

.page-toolbar__spacer {
  flex: 1;
}

.filter-toolbar {
  max-width: 760px;
}

.filter-toolbar > :first-child,
.add-toolbar > :last-child {
  flex: 1;
}

.add-toolbar {
  justify-content: space-between;
}

.add-toolbar p {
  color: var(--gray-500);
  font-size: 13px;
}

.select-list {
  max-height: 480px;
  overflow: auto;
  padding: 0 20px 16px;
}

.student-option {
  display: grid;
  grid-template-columns: 24px 1.2fr 0.8fr 1fr 1fr 0.7fr;
  align-items: center;
  gap: 12px;
  min-height: 48px;
  border-bottom: 1px solid var(--gray-100);
  color: var(--gray-600);
  font-size: 13px;
  cursor: pointer;
}

.student-option__name {
  color: var(--gray-800);
  font-weight: 600;
}

.student-option input {
  accent-color: var(--brand-600);
}

.text-action {
  color: var(--danger);
  font-size: 12px;
  white-space: nowrap;
}

.empty-state {
  padding: 36px 20px;
  color: var(--gray-500);
  text-align: center;
}

.schedule-error {
  padding: 18px 20px;
  color: var(--danger-strong);
  font-size: 13px;
}

.not-found {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
  color: var(--gray-700);
}

@media (max-width: 900px) {
  .class-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-toolbar {
    flex-wrap: wrap;
  }

  .filter-toolbar > * {
    min-width: 160px;
  }

  .student-option {
    grid-template-columns: 24px 1fr 1fr;
  }
}
</style>

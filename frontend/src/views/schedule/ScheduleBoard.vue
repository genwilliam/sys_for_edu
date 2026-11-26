<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ScheduleItem, TableColumn } from '@/types'
import { campuses, classGroups, classrooms, courses, scheduleItems, teachers } from '@/data/mock'
import {
  autoPlanSchedule,
  cancelScheduleItem,
  createScheduleBatch,
  createScheduleItem,
  getScheduleItems,
  updateScheduleItem,
  type AutoPlanRequest,
  type ScheduleDraft,
  type ScheduleFilters,
} from '@/services/schedule'
import { useAppStore } from '@/stores/app'
import PageHeader from '@/components/PageHeader.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiSelect from '@/components/ui/UiSelect.vue'
import UiTable from '@/components/ui/UiTable.vue'
import UiModal from '@/components/ui/UiModal.vue'
import UiTag from '@/components/ui/UiTag.vue'
import UiTabs from '@/components/ui/UiTabs.vue'
import StatCard from '@/components/StatCard.vue'

const store = useAppStore()
const list = ref<ScheduleItem[]>([])
const view = ref('week')
const modalOpen = ref(false)
const autoModalOpen = ref(false)
const loading = ref(false)
const saving = ref(false)
const planning = ref(false)
const pageError = ref('')
const formError = ref('')
const autoError = ref('')
const editingId = ref('')
const filters = ref<Required<ScheduleFilters>>({
  campus: '',
  course: '',
  teacher: '',
  room: '',
  weekday: '',
  status: '',
})
const autoResult = ref<{
  items: ScheduleDraft[]
  complete: boolean
  message: string
  explanation: string
} | null>(null)

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const timeSlots = ['09:00', '10:30', '14:00', '16:00', '19:00']
const typeOptions = [
  { label: '常规课', value: '常规课' },
  { label: '试听课', value: '试听课' },
  { label: '补课', value: '补课' },
]
const statusOptions = [
  { label: '待上课', value: '待上课' },
  { label: '已上课', value: '已上课' },
  { label: '已取消', value: '已取消' },
]
const courseOptions = [
  ...new Set([...courses.map(({ name }) => name), ...scheduleItems.map(({ course }) => course)]),
].map((name) => ({ label: name, value: name }))
const teacherOptions = teachers.map(({ name }) => ({ label: name, value: name }))
const roomOptions = classrooms.map(({ name }) => ({ label: name, value: name }))
const campusOptions = campuses.map(({ name }) => {
  const campusName = name.replace(/^(总校区|海淀校区|丰台校区|通州校区) · /, '')
  return { label: campusName, value: campusName }
})
const classOptions = [
  ...new Set([
    ...classGroups.map(({ name }) => name),
    ...scheduleItems.map(({ className }) => className),
  ]),
].map((name) => ({ label: name, value: name }))
const weekdayOptions = weekDays.map((day) => ({ label: day, value: day }))
const visibleTimeSlots = computed(() =>
  [...new Set([...timeSlots, ...list.value.map(({ startTime }) => startTime)])].sort(),
)
const blankDraft = (): ScheduleItem => ({
  id: '',
  course: courseOptions[0]?.value ?? '',
  className: '',
  teacher: teacherOptions[0]?.value ?? '',
  room: roomOptions[0]?.value ?? '',
  campus: campusOptions[0]?.value ?? '',
  weekday: weekDays[0] ?? '周一',
  startTime: '09:00',
  endTime: '10:30',
  type: '常规课',
  status: '待上课',
})
const form = ref<ScheduleItem>(blankDraft())
const autoForm = ref<AutoPlanRequest>({
  course: courseOptions[0]?.value ?? '',
  className: classOptions[0]?.value ?? '',
  teacher: teacherOptions[0]?.value ?? '',
  room: roomOptions[0]?.value ?? '',
  campus: campusOptions[0]?.value ?? '',
  weeklySessions: 2,
  durationMinutes: 90,
  weekdays: [...weekDays],
  startTime: '09:00',
  endTime: '19:00',
  type: '常规课',
})
const weeklySessionsText = computed({
  get: () => String(autoForm.value.weeklySessions),
  set: (value: string) => {
    autoForm.value.weeklySessions = Number(value)
  },
})
const durationMinutesText = computed({
  get: () => String(autoForm.value.durationMinutes),
  set: (value: string) => {
    autoForm.value.durationMinutes = Number(value)
  },
})

const typeTone: Record<string, 'brand' | 'warning' | 'info'> = {
  常规课: 'brand',
  试听课: 'warning',
  补课: 'info',
}
const statusTone: Record<string, 'brand' | 'success' | 'danger'> = {
  待上课: 'brand',
  已上课: 'success',
  已取消: 'danger',
}
const conflictedIds = computed(() => {
  const byTeacher = new Map<string, string[]>()
  const byRoom = new Map<string, string[]>()
  for (const item of list.value.filter(({ status }) => status !== '已取消')) {
    const tKey = `${item.teacher}|${item.weekday}|${item.startTime}`
    const rKey = `${item.campus}|${item.room}|${item.weekday}|${item.startTime}`
    byTeacher.set(tKey, [...(byTeacher.get(tKey) ?? []), item.id])
    byRoom.set(rKey, [...(byRoom.get(rKey) ?? []), item.id])
  }
  const ids = new Set<string>()
  for (const arr of [...byTeacher.values(), ...byRoom.values()]) {
    if (arr.length > 1) arr.forEach((id) => ids.add(id))
  }
  return ids
})
const conflictCount = computed(() => conflictedIds.value.size)
const activeCount = computed(() => list.value.filter((s) => s.status === '待上课').length)
const columns: TableColumn[] = [
  { key: 'course', title: '课程 / 班级', width: '210px' },
  { key: 'teacher', title: '教师', width: '100px' },
  { key: 'room', title: '教室', width: '110px' },
  { key: 'campus', title: '校区', width: '110px' },
  { key: 'time', title: '时间', width: '160px' },
  { key: 'type', title: '类型', width: '90px' },
  { key: 'status', title: '状态', width: '90px' },
  { key: 'conflict', title: '冲突', width: '90px', align: 'center' },
  { key: 'actions', title: '操作', width: '140px', align: 'center' },
]

async function loadItems() {
  loading.value = true
  pageError.value = ''
  try {
    list.value = await getScheduleItems(filters.value)
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : '排课数据加载失败'
  } finally {
    loading.value = false
  }
}

watch(filters, () => void loadItems(), { deep: true, immediate: true })

function isConflicted(id: string) {
  return conflictedIds.value.has(id)
}

const palette: Record<string, string> = {
  英语: '#6366f1',
  数学: '#10b981',
  语文: '#f59e0b',
  物理: '#0ea5e9',
  化学: '#f43f5e',
}

function courseColor(course: string) {
  for (const [key, color] of Object.entries(palette)) {
    if (course.includes(key)) return color
  }
  return '#6366f1'
}

function itemsAt(day: string, slot: string) {
  return list.value.filter((s) => s.weekday === day && s.startTime === slot)
}

function openCreate() {
  editingId.value = ''
  formError.value = ''
  form.value = blankDraft()
  modalOpen.value = true
}

function openEdit(item: ScheduleItem) {
  editingId.value = item.id
  formError.value = ''
  form.value = { ...item }
  modalOpen.value = true
}

function toDraft(item: ScheduleItem): ScheduleDraft {
  const { id: _id, ...draft } = item
  return draft
}

async function save() {
  if (!form.value.className.trim()) {
    formError.value = '请填写班级名称'
    return
  }
  if (form.value.startTime >= form.value.endTime) {
    formError.value = '结束时间必须晚于开始时间'
    return
  }
  saving.value = true
  formError.value = ''
  pageError.value = ''
  try {
    if (editingId.value) await updateScheduleItem(editingId.value, toDraft(form.value))
    else await createScheduleItem(toDraft(form.value))
    modalOpen.value = false
    store.toast(editingId.value ? '排课已更新' : '排课已创建')
    await loadItems()
  } catch (error) {
    formError.value = error instanceof Error ? error.message : '保存排课失败'
  } finally {
    saving.value = false
  }
}

async function cancelItem(item: ScheduleItem) {
  if (
    item.status === '已取消' ||
    !window.confirm(`确定取消「${item.course} · ${item.className}」吗？`)
  )
    return
  pageError.value = ''
  try {
    await cancelScheduleItem(item.id)
    store.toast('排课已取消')
    await loadItems()
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : '取消排课失败'
  }
}

function openAutoPlan() {
  autoError.value = ''
  autoResult.value = null
  autoModalOpen.value = true
}

async function generatePlan() {
  autoError.value = ''
  autoResult.value = null
  if (!autoForm.value.weekdays.length) {
    autoError.value = '请至少选择一个可排课星期'
    return
  }
  if (autoForm.value.startTime >= autoForm.value.endTime) {
    autoError.value = '结束时间必须晚于开始时间'
    return
  }
  planning.value = true
  try {
    autoResult.value = await autoPlanSchedule(autoForm.value)
  } catch (error) {
    autoError.value = error instanceof Error ? error.message : '自动排课失败'
  } finally {
    planning.value = false
  }
}

async function savePlan() {
  if (!autoResult.value?.complete) return
  planning.value = true
  autoError.value = ''
  try {
    await createScheduleBatch(autoResult.value.items)
    autoModalOpen.value = false
    store.toast('排课方案已保存')
    await loadItems()
  } catch (error) {
    autoError.value = error instanceof Error ? error.message : '保存排课方案失败'
  } finally {
    planning.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader title="排课管理" description="按教师、教室、课程多维度排课，自动检测资源冲突">
      <UiButton variant="outline" icon="calendar" @click="openAutoPlan">自动排课</UiButton>
      <UiButton icon="plus" @click="openCreate">新建排课</UiButton>
    </PageHeader>

    <section class="schedule-stats">
      <StatCard label="本周课次" :value="list.length" unit="节" icon="calendar" tone="brand" />
      <StatCard label="待上课" :value="activeCount" unit="节" icon="clock" tone="info" />
      <StatCard
        label="资源冲突"
        :value="conflictCount"
        unit="处"
        icon="alert-triangle"
        tone="danger"
      />
    </section>

    <p v-if="pageError" class="notice notice--error" role="alert">{{ pageError }}</p>
    <UiCard>
      <div class="schedule-toolbar">
        <UiTabs
          v-model="view"
          :tabs="[
            { label: '周视图', value: 'week' },
            { label: '列表视图', value: 'list' },
          ]"
        />
        <div class="page-toolbar__spacer" />
        <UiButton variant="outline" size="sm" icon="refresh" :disabled="loading" @click="loadItems">
          {{ loading ? '加载中…' : '刷新' }}
        </UiButton>
      </div>
      <div class="filter-grid">
        <UiSelect
          v-model="filters.campus"
          label="校区"
          :options="[{ label: '全部校区', value: '' }, ...campusOptions]"
          placeholder=""
        />
        <UiSelect
          v-model="filters.course"
          label="课程"
          :options="[{ label: '全部课程', value: '' }, ...courseOptions]"
          placeholder=""
        />
        <UiSelect
          v-model="filters.teacher"
          label="教师"
          :options="[{ label: '全部教师', value: '' }, ...teacherOptions]"
          placeholder=""
        />
        <UiSelect
          v-model="filters.room"
          label="教室"
          :options="[{ label: '全部教室', value: '' }, ...roomOptions]"
          placeholder=""
        />
        <UiSelect
          v-model="filters.weekday"
          label="星期"
          :options="[{ label: '全部星期', value: '' }, ...weekdayOptions]"
          placeholder=""
        />
        <UiSelect
          v-model="filters.status"
          label="状态"
          :options="[{ label: '全部状态', value: '' }, ...statusOptions]"
          placeholder=""
        />
      </div>
      <div v-if="loading && list.length === 0" class="empty-state">正在加载排课数据…</div>
      <div v-else-if="!loading && list.length === 0" class="empty-state">暂无排课数据</div>

      <div v-else-if="view === 'week'" class="timetable-wrap">
        <table class="timetable">
          <thead>
            <tr>
              <th class="timetable__time-head" />
              <th v-for="d in weekDays" :key="d">{{ d }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="slot in visibleTimeSlots" :key="slot">
              <td class="timetable__time">{{ slot }}</td>
              <td v-for="d in weekDays" :key="d" class="timetable__cell">
                <button
                  v-for="item in itemsAt(d, slot)"
                  :key="item.id"
                  type="button"
                  class="course-block"
                  :class="{ 'course-block--conflict': isConflicted(item.id) }"
                  :style="{ '--course-color': courseColor(item.course) }"
                  @click="openEdit(item)"
                >
                  <span class="course-block__name">{{ item.course }}</span>
                  <span class="course-block__meta">{{ item.teacher }} · {{ item.room }}</span>
                  <span v-if="isConflicted(item.id)" class="course-block__badge">冲突</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="schedule-list">
        <UiTable :columns="columns" :data="list">
          <template #cell-course="{ row }">
            <div class="cell-primary">{{ row.course }}</div>
            <div class="cell-sub">{{ row.className }}</div>
          </template>
          <template #cell-time="{ row }">
            <span>{{ row.weekday }} {{ row.startTime }}-{{ row.endTime }}</span>
          </template>
          <template #cell-type="{ row }">
            <UiTag :tone="typeTone[row.type] ?? 'neutral'">{{ row.type }}</UiTag>
          </template>
          <template #cell-status="{ row }">
            <UiTag :tone="statusTone[row.status] ?? 'neutral'" dot>{{ row.status }}</UiTag>
          </template>
          <template #cell-conflict="{ row }">
            <UiTag v-if="isConflicted(row.id)" tone="danger">有冲突</UiTag>
            <span v-else class="text-muted">—</span>
          </template>
          <template #cell-actions="{ row }">
            <div class="row-actions">
              <button type="button" class="text-action" @click="openEdit(row)">编辑</button>
              <button
                type="button"
                class="text-action text-action--danger"
                :disabled="row.status === '已取消'"
                @click="cancelItem(row)"
              >
                取消
              </button>
            </div>
          </template>
        </UiTable>
      </div>
    </UiCard>

    <UiModal v-model="modalOpen" :title="editingId ? '编辑排课' : '新建排课'" width="720px">
      <p v-if="formError" class="notice notice--error" role="alert">{{ formError }}</p>
      <div class="form-grid">
        <UiSelect v-model="form.course" label="课程" :options="courseOptions" required />
        <UiSelect v-model="form.className" label="班级" :options="classOptions" required />
        <UiSelect v-model="form.teacher" label="教师" :options="teacherOptions" />
        <UiSelect v-model="form.room" label="教室" :options="roomOptions" />
        <UiSelect v-model="form.campus" label="校区" :options="campusOptions" />
        <UiSelect v-model="form.weekday" label="星期" :options="weekdayOptions" />
        <UiInput v-model="form.startTime" label="开始时间" type="time" />
        <UiInput v-model="form.endTime" label="结束时间" type="time" />
        <UiSelect v-model="form.type" label="课程类型" :options="typeOptions" />
        <UiSelect v-model="form.status" label="状态" :options="statusOptions" />
      </div>
      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存排课' }}</UiButton>
      </template>
    </UiModal>

    <UiModal v-model="autoModalOpen" title="自动排课" width="760px">
      <p v-if="autoError" class="notice notice--error" role="alert">{{ autoError }}</p>
      <div class="form-grid">
        <UiSelect v-model="autoForm.course" label="课程" :options="courseOptions" />
        <UiSelect v-model="autoForm.className" label="班级" :options="classOptions" />
        <UiSelect v-model="autoForm.teacher" label="教师" :options="teacherOptions" />
        <UiSelect v-model="autoForm.room" label="教室" :options="roomOptions" />
        <UiSelect v-model="autoForm.campus" label="校区" :options="campusOptions" />
        <UiInput v-model="weeklySessionsText" label="每周课次" type="number" />
        <UiInput v-model="durationMinutesText" label="课时分钟" type="number" />
        <UiInput v-model="autoForm.startTime" label="可排开始时间" type="time" />
        <UiInput v-model="autoForm.endTime" label="可排结束时间" type="time" />
        <UiSelect v-model="autoForm.type" label="课程类型" :options="typeOptions" />
      </div>
      <fieldset class="weekday-picker">
        <legend>可排星期（可多选）</legend>
        <label v-for="day in weekDays" :key="day">
          <input v-model="autoForm.weekdays" type="checkbox" :value="day" />
          {{ day }}
        </label>
      </fieldset>
      <div v-if="autoResult" class="plan-result">
        <UiTag :tone="autoResult.complete ? 'success' : 'warning'">
          {{ autoResult.complete ? '完整方案' : '未完成' }}
        </UiTag>
        <p>{{ autoResult.message }}</p>
        <p class="text-muted">{{ autoResult.explanation }}</p>
        <div v-if="autoResult.items.length" class="candidate-list">
          <div
            v-for="(item, index) in autoResult.items"
            :key="`${item.weekday}-${item.startTime}-${index}`"
          >
            {{ item.weekday }} {{ item.startTime }}–{{ item.endTime }} · {{ item.course }} ·
            {{ item.teacher }} · {{ item.room }}
          </div>
        </div>
        <p v-else class="text-muted">没有候选时段</p>
      </div>
      <template #footer>
        <UiButton variant="outline" :disabled="planning" @click="generatePlan">
          {{ planning ? '正在计算…' : '生成方案' }}
        </UiButton>
        <UiButton :disabled="planning || !autoResult?.complete" @click="savePlan"
          >保存方案</UiButton
        >
      </template>
    </UiModal>
  </div>
</template>

<style scoped>
.schedule-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.schedule-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--gray-100);
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--gray-100);
}

.timetable-wrap {
  overflow-x: auto;
  padding: 16px 20px 20px;
}

.timetable {
  width: 100%;
  min-width: 840px;
  border-collapse: collapse;
  table-layout: fixed;
}

.timetable th {
  padding: 10px 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--gray-600);
  background: var(--gray-50);
  border: 1px solid var(--gray-100);
}

.timetable__time-head {
  width: 64px;
}

.timetable__time {
  padding: 10px 8px;
  font-size: 12px;
  color: var(--gray-500);
  text-align: center;
  background: var(--gray-50);
  border: 1px solid var(--gray-100);
  font-weight: 500;
}

.timetable__cell {
  height: 84px;
  padding: 6px;
  border: 1px solid var(--gray-100);
  vertical-align: top;
}

.course-block {
  position: relative;
  display: block;
  width: 100%;
  padding: 6px 8px;
  margin-bottom: 6px;
  border: 0;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--course-color);
  background: color-mix(in srgb, var(--course-color) 10%, #fff);
  text-align: left;
  cursor: pointer;
  transition:
    transform var(--transition),
    box-shadow var(--transition);
}

.course-block:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.course-block--conflict {
  border-left-color: var(--danger);
  background: var(--danger-soft);
}

.course-block__name {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--gray-800);
  line-height: 1.3;
}

.course-block__meta {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: var(--gray-500);
  line-height: 1.3;
}

.course-block__badge {
  position: absolute;
  top: 4px;
  right: 4px;
  padding: 1px 5px;
  border-radius: var(--radius-full);
  background: var(--danger);
  color: #fff;
  font-size: 10px;
  line-height: 1.5;
}

.schedule-list {
  padding: 16px 20px 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.notice {
  padding: 10px 12px;
  margin: 12px 20px;
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.modal-body > .notice {
  margin: 0 0 16px;
}

.notice--error {
  color: var(--danger);
  background: var(--danger-soft);
}

.empty-state {
  padding: 36px 20px;
  color: var(--gray-500);
  text-align: center;
}

.row-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.text-action {
  color: var(--brand-600);
  font-size: 12px;
}

.text-action--danger {
  color: var(--danger);
}

.text-action:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.weekday-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  padding: 12px;
  margin: 18px 0;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-sm);
}

.weekday-picker legend {
  padding: 0 6px;
  color: var(--gray-700);
  font-size: 13px;
}

.weekday-picker label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--gray-700);
}

.plan-result {
  padding: 14px;
  border-radius: var(--radius-sm);
  background: var(--gray-50);
}

.plan-result p {
  margin: 10px 0 0;
  font-size: 13px;
}

.candidate-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .filter-grid {
    grid-template-columns: repeat(3, minmax(120px, 1fr));
  }
}

@media (max-width: 900px) {
  .schedule-stats {
    grid-template-columns: 1fr;
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ScheduleItem } from '@/types'
import { scheduleItems } from '@/data/mock'
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
import UiTabs from '@/components/ui/UiTabs.vue'
import UiIcon from '@/components/ui/UiIcon.vue'
import StatCard from '@/components/StatCard.vue'

const store = useAppStore()

const list = ref<ScheduleItem[]>(JSON.parse(JSON.stringify(scheduleItems)) as ScheduleItem[])
const view = ref('week')
const modalOpen = ref(false)

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const timeSlots = ['09:00', '10:30', '14:00', '16:00', '19:00']

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

// 冲突检测：同一教师或同一教室在同一时段被重复占用
const conflictedIds = computed(() => {
  const byTeacher = new Map<string, string[]>()
  const byRoom = new Map<string, string[]>()
  for (const item of list.value) {
    const tKey = `${item.teacher}|${item.weekday}|${item.startTime}`
    const rKey = `${item.room}|${item.weekday}|${item.startTime}`
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

const columns: TableColumn[] = [
  { key: 'course', title: '课程 / 班级', width: '210px' },
  { key: 'teacher', title: '教师', width: '100px' },
  { key: 'room', title: '教室', width: '110px' },
  { key: 'campus', title: '校区', width: '110px' },
  { key: 'time', title: '时间', width: '160px' },
  { key: 'type', title: '类型', width: '90px' },
  { key: 'status', title: '状态', width: '90px' },
  { key: 'conflict', title: '冲突', width: '90px', align: 'center' },
]

const form = ref<ScheduleItem>({
  id: '',
  course: '英语启蒙',
  className: '',
  teacher: '陈雅婷',
  room: '教室 301',
  campus: '朝阳中心',
  weekday: '周一',
  startTime: '09:00',
  endTime: '10:30',
  type: '常规课',
  status: '待上课',
})

function openCreate() {
  form.value = {
    id: `SC${String(Date.now()).slice(-6)}`,
    course: '英语启蒙',
    className: '',
    teacher: '陈雅婷',
    room: '教室 301',
    campus: '朝阳中心',
    weekday: '周一',
    startTime: '09:00',
    endTime: '10:30',
    type: '常规课',
    status: '待上课',
  }
  modalOpen.value = true
}

function save() {
  if (!form.value.className.trim()) {
    store.toast('请填写班级名称', 'warning')
    return
  }
  const teacherBusy = list.value.some(
    (s) =>
      s.teacher === form.value.teacher &&
      s.weekday === form.value.weekday &&
      s.startTime === form.value.startTime,
  )
  const roomBusy = list.value.some(
    (s) =>
      s.room === form.value.room &&
      s.weekday === form.value.weekday &&
      s.startTime === form.value.startTime,
  )
  list.value.unshift({ ...form.value })
  modalOpen.value = false
  if (teacherBusy || roomBusy) {
    store.toast(`排课冲突：${teacherBusy ? '该教师' : '该教室'}在此时段已被占用，请调整`, 'warning')
  } else {
    store.toast('排课成功')
  }
}

const courseOptions = [
  { label: '英语启蒙', value: '英语启蒙' },
  { label: '英语听说', value: '英语听说' },
  { label: '数学思维', value: '数学思维' },
  { label: '语文素养', value: '语文素养' },
  { label: '物理竞赛', value: '物理竞赛' },
  { label: '化学实验', value: '化学实验' },
]

const timeOptions = timeSlots.map((t) => ({ label: t, value: t }))
</script>

<template>
  <div>
    <PageHeader title="排课管理" description="按教师、教室、课程多维度排课，自动检测资源冲突">
      <UiButton variant="outline" icon="download" @click="store.toast('已导出排课表')"
        >导出课表</UiButton
      >
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
        <UiButton variant="outline" size="sm" icon="refresh" @click="store.toast('已刷新排课数据')">
          刷新
        </UiButton>
      </div>

      <!-- 周视图 -->
      <div v-if="view === 'week'" class="timetable-wrap">
        <table class="timetable">
          <thead>
            <tr>
              <th class="timetable__time-head" />
              <th v-for="d in weekDays" :key="d">{{ d }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="slot in timeSlots" :key="slot">
              <td class="timetable__time">{{ slot }}</td>
              <td v-for="d in weekDays" :key="d" class="timetable__cell">
                <div
                  v-for="item in itemsAt(d, slot)"
                  :key="item.id"
                  class="course-block"
                  :class="{ 'course-block--conflict': isConflicted(item.id) }"
                  :style="{ '--course-color': courseColor(item.course) }"
                >
                  <div class="course-block__name">{{ item.course }}</div>
                  <div class="course-block__meta">{{ item.teacher }} · {{ item.room }}</div>
                  <span v-if="isConflicted(item.id)" class="course-block__badge">冲突</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 列表视图 -->
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
        </UiTable>
      </div>
    </UiCard>

    <UiModal v-model="modalOpen" title="新建排课" width="640px">
      <div class="form-grid">
        <UiSelect v-model="form.course" label="课程" :options="courseOptions" required />
        <UiInput v-model="form.className" label="班级" placeholder="如：英语启蒙 A 班" required />
        <UiSelect
          v-model="form.teacher"
          label="教师"
          :options="[
            { label: '陈雅婷', value: '陈雅婷' },
            { label: '张伟', value: '张伟' },
            { label: '李娜', value: '李娜' },
            { label: '王强', value: '王强' },
            { label: '孙浩', value: '孙浩' },
            { label: '吴涛', value: '吴涛' },
            { label: '周雨桐', value: '周雨桐' },
          ]"
        />
        <UiSelect
          v-model="form.room"
          label="教室"
          :options="[
            { label: '教室 301', value: '教室 301' },
            { label: '教室 302', value: '教室 302' },
            { label: '教室 401', value: '教室 401' },
            { label: '教室 205', value: '教室 205' },
            { label: '教室 201', value: '教室 201' },
            { label: '教室 101', value: '教室 101' },
          ]"
        />
        <UiSelect
          v-model="form.weekday"
          label="星期"
          :options="weekDays.map((d) => ({ label: d, value: d }))"
        />
        <UiSelect
          v-model="form.type"
          label="课程类型"
          :options="[
            { label: '常规课', value: '常规课' },
            { label: '试听课', value: '试听课' },
            { label: '补课', value: '补课' },
          ]"
        />
        <UiSelect v-model="form.startTime" label="开始时间" :options="timeOptions" />
        <UiSelect v-model="form.endTime" label="结束时间" :options="timeOptions" />
      </div>
      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton @click="save">确认排课</UiButton>
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
  font-size: 12.5px;
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
  padding: 6px 8px;
  margin-bottom: 6px;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--course-color);
  background: color-mix(in srgb, var(--course-color) 10%, #fff);
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
  font-size: 12.5px;
  font-weight: 600;
  color: var(--gray-800);
  line-height: 1.3;
}

.course-block__meta {
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

@media (max-width: 900px) {
  .schedule-stats {
    grid-template-columns: 1fr;
  }
}
</style>

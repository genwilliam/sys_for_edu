<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiTag from '@/components/ui/UiTag.vue'
import { useAppStore } from '@/stores/app'
import { createLeaveRequest, getLeaveRequests, reviewLeaveRequest } from '@/services/auth'
import { getScheduleItems } from '@/services/schedule'
import type { LeaveRequest, ScheduleItem } from '@/types'

const store = useAppStore()
const schedule = ref<ScheduleItem[]>([])
const leaveRequests = ref<LeaveRequest[]>([])
const loadingError = ref('')
const leaveDate = ref('')
const reason = ref('')
const selectedClassId = ref('')
const submitting = ref(false)

const user = computed(() => store.state.user)
const isTeacher = computed(() => user.value?.role === 'TEACHER')
const ownClasses = computed(() =>
  store.state.classGroups.filter((group) =>
    isTeacher.value
      ? group.teacher === user.value?.name
      : store.studentsInClass(group.id).includes(user.value?.personId ?? ''),
  ),
)
const pendingApprovals = computed(() =>
  leaveRequests.value.filter(
    (request) => request.type === 'STUDENT_LEAVE' && request.status === 'PENDING',
  ),
)

async function loadWorkData() {
  loadingError.value = ''
  try {
    const [items, requests] = await Promise.all([getScheduleItems(), getLeaveRequests()])
    schedule.value = items
    leaveRequests.value = requests
  } catch (error) {
    loadingError.value = error instanceof Error ? error.message : '工作台数据加载失败'
  }
}

async function submitLeave() {
  if (!leaveDate.value || !reason.value.trim()) {
    store.toast('请填写请假日期和原因', 'warning')
    return
  }
  if (!isTeacher.value && !selectedClassId.value) {
    store.toast('请选择要请假的班级', 'warning')
    return
  }
  submitting.value = true
  try {
    await createLeaveRequest({
      type: isTeacher.value ? 'TEACHER_LEAVE' : 'STUDENT_LEAVE',
      ...(isTeacher.value ? {} : { classId: selectedClassId.value }),
      leaveDate: leaveDate.value,
      reason: reason.value.trim(),
    })
    leaveDate.value = ''
    reason.value = ''
    await loadWorkData()
    store.toast(isTeacher.value ? '教师请假已提交，等待管理员处理' : '请假申请已提交')
  } catch (error) {
    store.toast(error instanceof Error ? error.message : '请假申请提交失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function decideLeave(request: LeaveRequest, status: 'APPROVED' | 'REJECTED') {
  try {
    await reviewLeaveRequest(request.id, status)
    await loadWorkData()
    store.toast(status === 'APPROVED' ? '已批准请假' : '已驳回请假')
  } catch (error) {
    store.toast(error instanceof Error ? error.message : '审批失败', 'error')
  }
}

function studentNames(classId: string) {
  const memberIds = store.studentsInClass(classId)
  return store.state.students.filter((student) => memberIds.includes(student.id))
}

function statusLabel(status: LeaveRequest['status']) {
  return status === 'PENDING' ? '待审批' : status === 'APPROVED' ? '已批准' : '已驳回'
}

onMounted(() => void loadWorkData())
</script>

<template>
  <div class="my-work">
    <PageHeader
      :title="isTeacher ? '教师工作台' : '学生工作台'"
      description="查看个人课表、关联班级及请假申请"
    />

    <p v-if="loadingError" class="work-error" role="alert">{{ loadingError }}</p>

    <UiCard class="work-card">
      <div class="work-heading">
        <h2>我的课表</h2>
        <span>{{ schedule.length }} 条课程安排</span>
      </div>
      <div v-if="schedule.length" class="work-list">
        <article v-for="item in schedule" :key="item.id" class="work-row">
          <strong>{{ item.weekday }} {{ item.startTime }}–{{ item.endTime }}</strong>
          <div>
            <b>{{ item.course }}</b>
            <span>{{ item.className }} · {{ item.room }} · {{ item.campus }}</span>
          </div>
          <span v-if="isTeacher">学生 {{ item.studentIds?.length ?? 0 }} 人</span>
          <span v-else>授课教师：{{ item.teacher }}</span>
        </article>
      </div>
      <p v-else class="work-empty">暂无课程安排</p>
    </UiCard>

    <UiCard class="work-card">
      <div class="work-heading">
        <h2>{{ isTeacher ? '我负责的班级' : '我的班级' }}</h2>
        <span>{{ ownClasses.length }} 个班级</span>
      </div>
      <div v-if="ownClasses.length" class="class-grid">
        <article v-for="group in ownClasses" :key="group.id" class="class-card">
          <div class="class-card__top">
            <strong>{{ group.name }}</strong>
            <UiTag tone="brand">{{ group.course }}</UiTag>
          </div>
          <p>{{ group.campus }} · {{ group.schedule }}</p>
          <p v-if="isTeacher">
            联系方式：{{
              store.state.teachers.find((teacher) => teacher.id === user?.personId)?.phone
            }}
          </p>
          <p v-else>任课教师：{{ group.teacher }}</p>
          <div v-if="isTeacher" class="class-card__students">
            <b>班级学生（{{ studentNames(group.id).length }}）</b>
            <span v-for="student in studentNames(group.id)" :key="student.id">
              {{ student.name }} · {{ student.phone }}
            </span>
          </div>
        </article>
      </div>
      <p v-else class="work-empty">暂无关联班级</p>
    </UiCard>

    <UiCard class="work-card">
      <div class="work-heading">
        <h2>提交请假</h2>
        <span>{{ isTeacher ? '请假申请将由管理员处理' : '请假申请将由任课教师审批' }}</span>
      </div>
      <form class="leave-form" @submit.prevent="submitLeave">
        <label v-if="!isTeacher">
          请假班级
          <select v-model="selectedClassId" required>
            <option value="" disabled>请选择班级</option>
            <option v-for="group in ownClasses" :key="group.id" :value="group.id">
              {{ group.name }}
            </option>
          </select>
        </label>
        <label>
          请假日期
          <input v-model="leaveDate" type="date" required />
        </label>
        <label class="leave-form__reason">
          请假原因
          <input
            v-model="reason"
            type="text"
            maxlength="1000"
            required
            placeholder="请简要说明原因"
          />
        </label>
        <UiButton type="submit" :disabled="submitting">
          {{ submitting ? '提交中…' : '提交申请' }}
        </UiButton>
      </form>
    </UiCard>

    <UiCard class="work-card">
      <div class="work-heading">
        <h2>{{ isTeacher ? `待审批学生请假（${pendingApprovals.length}）` : '我的请假记录' }}</h2>
      </div>
      <div v-if="leaveRequests.length" class="work-list">
        <article v-for="request in leaveRequests" :key="request.id" class="leave-row">
          <div>
            <b
              >{{ request.leaveDate }} ·
              {{ request.type === 'STUDENT_LEAVE' ? '学生请假' : '教师请假' }}</b
            >
            <span>{{ request.reason }}</span>
            <small v-if="request.reviewNote">{{ request.reviewNote }}</small>
          </div>
          <UiTag
            :tone="
              request.status === 'APPROVED'
                ? 'success'
                : request.status === 'PENDING'
                  ? 'warning'
                  : 'neutral'
            "
          >
            {{ statusLabel(request.status) }}
          </UiTag>
          <div
            v-if="isTeacher && request.type === 'STUDENT_LEAVE' && request.status === 'PENDING'"
            class="leave-actions"
          >
            <UiButton size="sm" @click="decideLeave(request, 'APPROVED')">批准</UiButton>
            <UiButton size="sm" variant="outline" @click="decideLeave(request, 'REJECTED')"
              >驳回</UiButton
            >
          </div>
        </article>
      </div>
      <p v-else class="work-empty">暂无请假申请</p>
    </UiCard>
  </div>
</template>

<style scoped>
.my-work {
  display: grid;
  gap: 18px;
}
.work-card {
  padding: 20px;
}
.work-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.work-heading h2 {
  font-size: 16px;
  color: var(--gray-800);
}
.work-heading > span {
  color: var(--gray-500);
  font-size: 13px;
}
.work-list {
  display: grid;
}
.work-row,
.leave-row {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px 0;
  border-top: 1px solid var(--gray-100);
}
.work-row > strong {
  min-width: 150px;
  color: var(--brand-700);
}
.work-row > div,
.leave-row > div:first-child {
  display: grid;
  gap: 4px;
  flex: 1;
}
.work-row span,
.leave-row span,
.leave-row small {
  color: var(--gray-500);
  font-size: 13px;
}
.work-empty {
  color: var(--gray-500);
  font-size: 13px;
  padding: 10px 0;
}
.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}
.class-card {
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  padding: 14px;
}
.class-card__top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}
.class-card p {
  color: var(--gray-600);
  font-size: 13px;
  margin-top: 8px;
}
.class-card__students {
  display: grid;
  gap: 5px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--gray-100);
}
.class-card__students b {
  font-size: 13px;
}
.class-card__students span {
  font-size: 12px;
  color: var(--gray-600);
}
.leave-form {
  display: flex;
  align-items: end;
  gap: 12px;
  flex-wrap: wrap;
}
.leave-form label {
  display: grid;
  gap: 6px;
  min-width: 180px;
  color: var(--gray-700);
  font-size: 13px;
}
.leave-form input,
.leave-form select {
  height: 40px;
  padding: 0 10px;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-sm);
}
.leave-form__reason {
  flex: 1;
}
.leave-row {
  flex-wrap: wrap;
}
.leave-actions {
  display: flex;
  gap: 8px;
}
.work-error {
  color: var(--danger-strong);
}
@media (max-width: 640px) {
  .work-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
  .leave-form label {
    width: 100%;
  }
}
</style>

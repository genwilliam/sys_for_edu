<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiEmpty from '@/components/ui/UiEmpty.vue'
import UiTag from '@/components/ui/UiTag.vue'
import { getScheduleItems } from '@/services/schedule'
import { useAppStore } from '@/stores/app'
import type { ScheduleItem, TabItem, Teacher } from '@/types'

const store = useAppStore()
const scheduleItems = ref<ScheduleItem[]>([])
const scheduleError = ref('')
const route = computed(() => {
  const [kind, id = ''] = store.state.currentPath.slice(1).split('/')
  return {
    kind: kind === 'teacher' ? 'teacher' : 'student',
    id: decodeURIComponent(id),
  } as const
})

const teacher = computed<Teacher | null>(() => {
  if (route.value.kind !== 'teacher') return null
  const selected = store.state.profile
  if (selected?.kind === 'teacher' && selected.person.id === route.value.id) {
    return selected.person
  }
  return store.state.teachers.find((item) => item.id === route.value.id) ?? null
})

const student = computed(() => {
  if (route.value.kind !== 'student') return null
  const selected = store.state.profile
  if (selected?.kind === 'student' && selected.person.id === route.value.id) {
    return selected.person
  }
  return store.state.students.find((item) => item.id === route.value.id) ?? null
})

const personName = computed(() => teacher.value?.name ?? student.value?.name ?? '')
const person = computed(() => teacher.value ?? student.value)
const isTeacher = computed(() => route.value.kind === 'teacher')
const activeTab = ref('basic')

const teacherTabs: TabItem[] = [
  { label: '我的课程', value: 'courses' },
  { label: '班级信息', value: 'classes' },
  { label: '教师课表', value: 'schedule' },
  { label: '上课记录', value: 'lessons' },
  { label: '基本信息', value: 'basic' },
  { label: '回访记录', value: 'visits' },
]

const studentTabs: TabItem[] = [
  { label: '课程信息', value: 'courses' },
  { label: '班级信息', value: 'classes' },
  { label: '学生课表', value: 'schedule' },
  { label: '签到记录', value: 'lessons' },
  { label: '基本信息', value: 'basic' },
  { label: '订单记录', value: 'orders' },
  { label: '积分记录', value: 'points' },
  { label: '调班记录', value: 'transfers' },
  { label: '回访记录', value: 'visits' },
]

const tabs = computed(() => (isTeacher.value ? teacherTabs : studentTabs))
const relatedSchedule = computed(() =>
  scheduleItems.value.filter((item) =>
    isTeacher.value
      ? item.teacher === personName.value
      : item.studentIds?.includes(student.value?.id ?? '') ||
        item.className === student.value?.className,
  ),
)
const relatedCourses = computed(() => {
  if (teacher.value)
    return store.state.courses.filter((item) => item.teacher === teacher.value?.name)
  const courseNames = new Set(relatedSchedule.value.map((item) => item.course))
  return store.state.courses.filter((item) => courseNames.has(item.name))
})
const relatedClasses = computed(() =>
  isTeacher.value
    ? store.state.classGroups.filter((item) => item.teacher === personName.value)
    : store.state.classGroups.filter(
        (item) =>
          student.value !== null && store.studentsInClass(item.id).includes(student.value.id),
      ),
)
const studentsByClass = (classId: string) => {
  const memberIds = store.studentsInClass(classId)
  return store.state.students.filter((item) => memberIds.includes(item.id))
}
const relatedLessons = computed(() =>
  store.state.attendance.filter((item) =>
    isTeacher.value ? item.teacher === personName.value : item.student === personName.value,
  ),
)
const relatedOrders = computed(() =>
  store.state.orders.filter((item) => item.student === personName.value),
)
const relatedVisits = computed(() =>
  store.state.visits.filter((item) => item.student === personName.value),
)

onMounted(async () => {
  try {
    scheduleItems.value = await getScheduleItems()
  } catch (error) {
    scheduleError.value = error instanceof Error ? error.message : '档案课表加载失败'
    store.toast(scheduleError.value, 'error')
  }
})

const fields = computed(() => {
  if (teacher.value) {
    return [
      { label: '教师姓名', value: teacher.value.name },
      { label: '教师编号', value: teacher.value.id },
      { label: '教师性别', value: genderLabel(teacher.value.gender) },
      { label: '任教学科', value: teacher.value.subject },
      { label: '联系电话', value: teacher.value.phone },
      { label: '所属校区', value: teacher.value.campus },
      { label: '在职状态', value: teacher.value.status === 'active' ? '在职' : '离职' },
      { label: '职称', value: teacher.value.title },
      { label: '入职日期', value: teacher.value.joinDate },
      { label: '学员人数', value: `${teacher.value.studentCount} 人` },
      { label: '教学评分', value: `${teacher.value.rating} 分` },
    ]
  }
  if (student.value) {
    return [
      { label: '学生姓名', value: student.value.name },
      { label: '学生编号', value: student.value.id },
      { label: '学生性别', value: genderLabel(student.value.gender) },
      { label: '年龄', value: `${student.value.age} 岁` },
      { label: '联系电话', value: student.value.phone },
      { label: '监护人', value: student.value.guardian },
      { label: '所属校区', value: student.value.campus },
      { label: '所在班级', value: student.value.className },
      { label: '在读状态', value: student.value.status },
      { label: '报名日期', value: student.value.enrollDate },
      { label: '在读课程', value: `${student.value.courseCount} 门` },
    ]
  }
  return []
})

function genderLabel(gender: string) {
  return gender === 'female' ? '女' : '男'
}

function returnToList() {
  store.navigate(isTeacher.value ? '/teacher' : '/student')
}
</script>

<template>
  <div v-if="person" class="person-detail">
    <PageHeader
      :title="isTeacher ? '教师档案' : '学生档案'"
      :description="`查看${isTeacher ? '教师' : '学生'}的基本资料及相关教务记录`"
    >
      <UiButton variant="outline" icon="chevron-left" @click="returnToList">
        返回{{ isTeacher ? '教师' : '学生' }}列表
      </UiButton>
    </PageHeader>

    <section class="profile-hero">
      <div
        class="profile-avatar"
        :class="[
          person.gender === 'female' ? 'profile-avatar--female' : 'profile-avatar--male',
          !isTeacher && 'profile-avatar--student',
        ]"
        aria-hidden="true"
      >
        {{ person.name.charAt(0) }}
      </div>
      <div class="profile-hero__identity">
        <div class="profile-hero__name-row">
          <h2>{{ person.name }}</h2>
          <UiTag v-if="teacher" :tone="teacher.status === 'active' ? 'success' : 'neutral'" dot>
            {{ teacher.status === 'active' ? '在职' : '离职' }}
          </UiTag>
          <UiTag v-else-if="student" :tone="student.status === '在读' ? 'success' : 'neutral'" dot>
            {{ student.status }}
          </UiTag>
        </div>
        <p>{{ isTeacher ? teacher?.title : student?.className }}</p>
        <p>{{ person.id }} · {{ person.campus }}</p>
      </div>
      <div class="profile-hero__facts">
        <div>
          <span>{{ isTeacher ? '任教学科' : '监护人' }}</span>
          <strong>{{ teacher?.subject ?? student?.guardian }}</strong>
        </div>
        <div>
          <span>{{ isTeacher ? '入职日期' : '报名日期' }}</span>
          <strong>{{ teacher?.joinDate ?? student?.enrollDate }}</strong>
        </div>
        <div>
          <span>联系电话</span>
          <strong>{{ person.phone }}</strong>
        </div>
      </div>
    </section>

    <section class="profile-panel">
      <nav class="profile-tabs" aria-label="档案详情分类">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          class="profile-tabs__item"
          :class="{ 'profile-tabs__item--active': activeTab === tab.value }"
          type="button"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
        </button>
      </nav>

      <div class="profile-content">
        <template v-if="activeTab === 'basic'">
          <div class="profile-content__heading">
            <h3>{{ isTeacher ? '教师信息' : '学生信息' }}</h3>
            <span class="profile-content__hint">档案编号：{{ person.id }}</span>
          </div>
          <dl class="profile-fields">
            <div v-for="field in fields" :key="field.label" class="profile-fields__item">
              <dt>{{ field.label }}</dt>
              <dd>{{ field.value || '暂无' }}</dd>
            </div>
          </dl>
        </template>

        <template v-else-if="activeTab === 'courses'">
          <div class="profile-content__heading">
            <h3>{{ isTeacher ? '任教课程' : '课程信息' }}</h3>
            <span class="profile-content__hint">共 {{ relatedCourses.length }} 门</span>
          </div>
          <div v-if="relatedCourses.length" class="record-grid">
            <article v-for="course in relatedCourses" :key="course.id" class="record-card">
              <div class="record-card__top">
                <strong>{{ course.name }}</strong>
                <UiTag :tone="course.status === 'active' ? 'success' : 'neutral'">
                  {{ course.status === 'active' ? '开课中' : '已停课' }}
                </UiTag>
              </div>
              <p>{{ course.category }} · {{ course.grade }}</p>
              <p>{{ course.hours }} 课时 · ¥{{ course.price }}/期</p>
              <p v-if="isTeacher">任教教师：{{ course.teacher }}</p>
            </article>
          </div>
          <UiEmpty v-else text="暂无课程记录" description="当前演示数据中没有关联课程" />
        </template>

        <template v-else-if="activeTab === 'classes'">
          <div class="profile-content__heading">
            <h3>班级信息</h3>
            <span class="profile-content__hint">共 {{ relatedClasses.length }} 个班级</span>
          </div>
          <div v-if="relatedClasses.length" class="record-grid">
            <article v-for="group in relatedClasses" :key="group.id" class="record-card">
              <div class="record-card__top">
                <strong>{{ group.name }}</strong>
                <UiTag tone="brand">{{ group.status }}</UiTag>
              </div>
              <p>{{ group.course }} · {{ group.grade }}</p>
              <p>{{ group.campus }} · {{ group.room }}</p>
              <p>{{ group.studentCount }}/{{ group.capacity }} 人 · {{ group.schedule }}</p>
              <p v-if="isTeacher && teacher">
                任课教师：{{ teacher.name }} · 联系方式：{{ teacher.phone }}
              </p>
              <p v-else-if="!isTeacher">
                任课教师：{{ group.teacher }} · 联系方式：
                {{
                  store.state.teachers.find((item) => item.name === group.teacher)?.phone ??
                  '未登记'
                }}
              </p>
              <p v-if="isTeacher && studentsByClass(group.id).length">
                班级学员：{{
                  studentsByClass(group.id)
                    .map((item) => item.name)
                    .join('、')
                }}
              </p>
            </article>
          </div>
          <UiEmpty v-else text="暂无班级记录" description="当前演示数据中没有关联班级" />
        </template>

        <template v-else-if="activeTab === 'schedule'">
          <div class="profile-content__heading">
            <h3>{{ isTeacher ? '教师课表' : '学生课表' }}</h3>
            <span class="profile-content__hint">共 {{ relatedSchedule.length }} 条</span>
          </div>
          <div v-if="relatedSchedule.length" class="record-list">
            <article
              v-for="item in relatedSchedule"
              :key="item.id"
              class="schedule-record"
              :class="{ 'schedule-record--cancelled': item.status === '已取消' }"
            >
              <div class="schedule-record__time">
                <strong>{{ item.weekday }}</strong>
                <span>{{ item.startTime }}–{{ item.endTime }}</span>
              </div>
              <div class="schedule-record__main">
                <strong>{{ item.course }}</strong>
                <span>{{ item.className }} · {{ item.room }} · {{ item.campus }}</span>
              </div>
              <UiTag :tone="item.status === '已取消' ? 'neutral' : 'brand'">
                {{ item.status }}
              </UiTag>
            </article>
          </div>
          <UiEmpty v-else text="暂无课表安排" description="关联的排课记录会显示在这里" />
        </template>

        <template v-else-if="activeTab === 'lessons'">
          <div class="profile-content__heading">
            <h3>{{ isTeacher ? '上课记录' : '签到记录' }}</h3>
            <span class="profile-content__hint">共 {{ relatedLessons.length }} 条</span>
          </div>
          <div v-if="relatedLessons.length" class="record-list">
            <article v-for="record in relatedLessons" :key="record.id" class="simple-record">
              <div>
                <strong>{{ record.course }} · {{ record.className }}</strong>
                <span
                  >{{ record.date }} {{ record.time }} ·
                  {{ isTeacher ? record.student : record.teacher }}</span
                >
                <span v-if="record.note">{{ record.note }}</span>
              </div>
              <UiTag
                :tone="
                  record.status === '出勤'
                    ? 'success'
                    : record.status === '请假'
                      ? 'warning'
                      : 'danger'
                "
              >
                {{ record.status }}
              </UiTag>
            </article>
          </div>
          <UiEmpty v-else text="暂无上课记录" description="当前演示数据中没有相关签到记录" />
        </template>

        <template v-else-if="activeTab === 'orders'">
          <div class="profile-content__heading">
            <h3>订单记录</h3>
            <span class="profile-content__hint">共 {{ relatedOrders.length }} 笔</span>
          </div>
          <div v-if="relatedOrders.length" class="record-list">
            <article v-for="order in relatedOrders" :key="order.id" class="simple-record">
              <div>
                <strong>{{ order.course }} · {{ order.orderNo }}</strong>
                <span>{{ order.date }} · {{ order.method }}</span>
              </div>
              <div class="simple-record__right">
                <strong>¥{{ order.amount }}</strong>
                <UiTag :tone="order.payStatus === '已支付' ? 'success' : 'warning'">
                  {{ order.payStatus }}
                </UiTag>
              </div>
            </article>
          </div>
          <UiEmpty v-else text="暂无订单记录" description="该学生的订单信息会显示在这里" />
        </template>

        <template v-else-if="activeTab === 'visits'">
          <div class="profile-content__heading">
            <h3>回访记录</h3>
            <span class="profile-content__hint">共 {{ relatedVisits.length }} 条</span>
          </div>
          <div v-if="relatedVisits.length" class="record-list">
            <article v-for="visit in relatedVisits" :key="visit.id" class="simple-record">
              <div>
                <strong>{{ visit.type }} · {{ visit.visitor }}</strong>
                <span>{{ visit.date }} · {{ visit.content }}</span>
                <span v-if="visit.nextDate">下次回访：{{ visit.nextDate }}</span>
              </div>
              <UiTag :tone="visit.result === '满意' ? 'success' : 'warning'">
                {{ visit.result }}
              </UiTag>
            </article>
          </div>
          <UiEmpty v-else text="暂无回访记录" description="与此档案关联的回访记录会显示在这里" />
        </template>

        <UiEmpty
          v-else-if="activeTab === 'points'"
          text="暂无积分记录"
          description="当前系统尚未接入积分业务"
        />
        <UiEmpty
          v-else-if="activeTab === 'transfers'"
          text="暂无调班记录"
          description="调班记录将在相关业务上线后显示"
        />
      </div>
    </section>
  </div>

  <UiEmpty v-else text="未找到该档案" description="档案可能已被移除，请返回列表重新选择">
    <UiButton variant="outline" @click="returnToList">
      返回{{ isTeacher ? '教师' : '学生' }}列表
    </UiButton>
  </UiEmpty>
</template>

<style scoped>
.profile-hero,
.profile-panel {
  background: #fff;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-lg);
}

.profile-hero {
  display: grid;
  grid-template-columns: auto minmax(180px, 1fr) minmax(360px, 1.5fr);
  align-items: center;
  gap: 24px;
  padding: 28px 32px;
  margin-bottom: 20px;
}

.profile-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 104px;
  height: 104px;
  border: 5px solid #fff;
  border-radius: 18px;
  box-shadow: 0 0 0 1px var(--gray-200);
  font-size: 40px;
  font-weight: 700;
}

.profile-avatar--female {
  background: linear-gradient(145deg, #fbcfe8, #a5b4fc);
  color: #7c3aed;
}

.profile-avatar--male {
  background: linear-gradient(145deg, #bfdbfe, #93c5fd);
  color: #1d4ed8;
}

.profile-avatar--student {
  border-radius: 50%;
}

.profile-hero__identity {
  min-width: 0;
}

.profile-hero__name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.profile-hero__name-row h2 {
  font-size: 21px;
  font-weight: 700;
  color: var(--gray-900);
}

.profile-hero__identity p {
  margin-top: 8px;
  color: var(--gray-500);
  font-size: 13px;
}

.profile-hero__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 28px;
  padding-left: 28px;
  border-left: 1px solid var(--gray-100);
}

.profile-hero__facts div {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.profile-hero__facts span,
.profile-content__hint {
  color: var(--gray-400);
  font-size: 12px;
}

.profile-hero__facts strong {
  overflow: hidden;
  color: var(--gray-700);
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-panel {
  overflow: hidden;
}

.profile-tabs {
  display: flex;
  overflow-x: auto;
  padding: 0 24px;
  border-bottom: 1px solid var(--gray-200);
}

.profile-tabs__item {
  position: relative;
  flex: 0 0 auto;
  min-width: 120px;
  height: 58px;
  padding: 0 18px;
  color: var(--gray-500);
  font-size: 14px;
  transition: color var(--transition);
}

.profile-tabs__item:hover,
.profile-tabs__item--active {
  color: var(--brand-600);
}

.profile-tabs__item--active {
  font-weight: 600;
}

.profile-tabs__item--active::after {
  position: absolute;
  right: 18px;
  bottom: -1px;
  left: 18px;
  height: 2px;
  background: var(--brand-500);
  content: '';
}

.profile-content {
  min-height: 360px;
  padding: 30px clamp(20px, 6vw, 72px) 40px;
}

.profile-content__heading {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
}

.profile-content__heading h3 {
  color: var(--gray-800);
  font-size: 15px;
  font-weight: 700;
}

.profile-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 48px;
}

.profile-fields__item {
  display: grid;
  grid-template-columns: minmax(90px, 130px) 1fr;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--gray-100);
}

.profile-fields__item dt {
  color: var(--gray-500);
}

.profile-fields__item dd {
  color: var(--gray-800);
  font-weight: 500;
}

.record-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.record-card,
.schedule-record,
.simple-record {
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  background: #fff;
}

.record-card {
  padding: 16px;
}

.record-card__top,
.schedule-record,
.simple-record {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.record-card__top strong,
.schedule-record__main strong,
.simple-record strong {
  color: var(--gray-800);
  font-size: 14px;
}

.record-card p {
  margin-top: 10px;
  color: var(--gray-500);
  font-size: 13px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schedule-record,
.simple-record {
  padding: 14px 16px;
}

.schedule-record__time {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 100px;
  color: var(--brand-700);
}

.schedule-record__time span,
.schedule-record__main span,
.simple-record span {
  color: var(--gray-500);
  font-size: 12.5px;
}

.schedule-record__main,
.simple-record > div:first-child {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.schedule-record--cancelled {
  opacity: 0.6;
}

.simple-record__right {
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 6px;
}

@media (max-width: 900px) {
  .profile-hero {
    grid-template-columns: auto 1fr;
  }

  .profile-hero__facts {
    grid-column: 1 / -1;
    padding: 18px 0 0;
    border-top: 1px solid var(--gray-100);
    border-left: 0;
  }

  .record-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .profile-hero {
    gap: 16px;
    padding: 20px;
  }

  .profile-avatar {
    width: 76px;
    height: 76px;
    font-size: 30px;
  }

  .profile-hero__facts,
  .profile-fields {
    grid-template-columns: 1fr;
  }

  .profile-tabs {
    padding: 0 8px;
  }

  .profile-content {
    padding: 24px 16px;
  }

  .schedule-record,
  .simple-record {
    align-items: flex-start;
  }

  .schedule-record__time {
    min-width: 64px;
  }
}
</style>

<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import UiCard from '@/components/ui/UiCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiIcon from '@/components/ui/UiIcon.vue'
import UiTag from '@/components/ui/UiTag.vue'
import StatCard from '@/components/StatCard.vue'
import LineChart from '@/components/charts/LineChart.vue'
import DonutChart from '@/components/charts/DonutChart.vue'
import BarChart from '@/components/charts/BarChart.vue'
import {
  dashboardStats,
  revenueTrend,
  courseDistribution,
  campusDistribution,
  scheduleItems,
} from '@/data/mock'

defineOptions({ name: 'DashboardView' })

const store = useAppStore()

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
})

const todayClasses = scheduleItems.filter((s) => s.status === '待上课').slice(0, 5)

const statusTone: Record<string, 'success' | 'warning' | 'info' | 'danger' | 'brand'> = {
  待上课: 'brand',
  已上课: 'success',
  已取消: 'danger',
}
</script>

<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <section class="banner">
      <div class="banner__content">
        <p class="banner__date">{{ today }}</p>
        <h2 class="banner__title">{{ greeting }}，{{ store.state.user?.name }} 👋</h2>
        <p class="banner__desc">今日共有 8 节课程待上课，6 名新学员待跟进，祝您工作顺利。</p>
        <div class="banner__actions">
          <UiButton icon="plus" @click="store.toast('新建学生表单已准备')">新建学生</UiButton>
          <UiButton variant="outline" icon="calendar" @click="store.toast('进入排课管理')">
            新建排课
          </UiButton>
        </div>
      </div>
      <div class="banner__deco">
        <UiIcon name="graduation-cap" :size="120" />
      </div>
    </section>

    <!-- 统计卡片 -->
    <section class="stats-grid">
      <StatCard
        label="在校学生"
        :value="dashboardStats.studentTotal"
        unit="人"
        icon="users"
        tone="brand"
        :trend="dashboardStats.studentGrowth"
      />
      <StatCard
        label="在职教师"
        :value="dashboardStats.teacherTotal"
        unit="人"
        icon="graduation-cap"
        tone="success"
        :trend="dashboardStats.teacherGrowth"
      />
      <StatCard
        label="开班班级"
        :value="dashboardStats.classTotal"
        unit="个"
        icon="layers"
        tone="info"
        :trend="dashboardStats.classGrowth"
      />
      <StatCard
        label="本月营收"
        :value="dashboardStats.monthRevenue"
        unit="元"
        icon="wallet"
        tone="warning"
        :trend="dashboardStats.revenueGrowth"
      />
      <StatCard
        label="本月出勤率"
        :value="dashboardStats.attendanceRate"
        unit="%"
        icon="clipboard-check"
        tone="success"
      />
      <StatCard
        label="待处理订单"
        :value="dashboardStats.pendingOrders"
        unit="笔"
        icon="receipt"
        tone="danger"
      />
    </section>

    <!-- 图表行 -->
    <section class="chart-row">
      <UiCard title="年度营收趋势" class="chart-row__main">
        <template #extra>
          <UiTag tone="success" dot>同比 +15.8%</UiTag>
        </template>
        <LineChart
          :labels="revenueTrend.map((i) => i.label)"
          :values="revenueTrend.map((i) => i.value)"
        />
      </UiCard>

      <UiCard title="课程类型分布" class="chart-row__side">
        <DonutChart :segments="courseDistribution" />
      </UiCard>
    </section>

    <!-- 底部行 -->
    <section class="chart-row">
      <UiCard title="校区学员分布" class="chart-row__side">
        <BarChart
          :labels="campusDistribution.map((i) => i.label)"
          :values="campusDistribution.map((i) => i.value)"
          unit="人"
          color="#10b981"
        />
      </UiCard>

      <UiCard title="今日课程" class="chart-row__main">
        <template #extra>
          <button class="card-link" type="button" @click="store.navigate('/schedule')">
            查看全部
          </button>
        </template>
        <ul class="class-list">
          <li v-for="c in todayClasses" :key="c.id" class="class-item">
            <div class="class-item__time">
              <span class="class-item__time-start">{{ c.startTime }}</span>
              <span class="class-item__time-end">{{ c.endTime }}</span>
            </div>
            <div class="class-item__main">
              <p class="class-item__name">{{ c.course }} · {{ c.className }}</p>
              <p class="class-item__meta">{{ c.teacher }} · {{ c.room }} · {{ c.campus }}</p>
            </div>
            <UiTag :tone="statusTone[c.status] ?? 'neutral'" dot>{{ c.status }}</UiTag>
          </li>
        </ul>
      </UiCard>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ---- 横幅 ---- */
.banner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 32px;
  border-radius: var(--radius-lg);
  background: linear-gradient(120deg, #4f46e5 0%, #6366f1 45%, #818cf8 100%);
  color: #fff;
  overflow: hidden;
}

.banner__content {
  position: relative;
  z-index: 2;
}

.banner__date {
  font-size: 13px;
  color: #c7d2fe;
}

.banner__title {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
}

.banner__desc {
  margin-top: 8px;
  font-size: 14px;
  color: #e0e7ff;
}

.banner__actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.banner__actions :deep(.ui-btn--outline) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.35);
  color: #fff;
}

.banner__deco {
  position: absolute;
  right: 40px;
  bottom: -20px;
  color: rgba(255, 255, 255, 0.12);
  transform: rotate(-8deg);
}

/* ---- 统计 ---- */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

/* ---- 图表行 ---- */
.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.chart-row__main {
  min-width: 0;
}

.chart-row__side {
  min-width: 0;
}

.card-link {
  font-size: 13px;
  color: var(--brand-600);
  font-weight: 500;
}

.card-link:hover {
  text-decoration: underline;
}

/* ---- 今日课程 ---- */
.class-list {
  display: flex;
  flex-direction: column;
}

.class-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--gray-100);
}

.class-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.class-item__time {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 52px;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  background: var(--brand-50);
  color: var(--brand-700);
  line-height: 1.2;
}

.class-item__time-start {
  font-weight: 700;
  font-size: 13px;
}

.class-item__time-end {
  font-size: 11px;
  opacity: 0.8;
}

.class-item__main {
  flex: 1;
  min-width: 0;
}

.class-item__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-800);
}

.class-item__meta {
  margin-top: 2px;
  font-size: 12.5px;
  color: var(--gray-500);
}

@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1100px) {
  .chart-row {
    grid-template-columns: 1fr;
  }
}
</style>

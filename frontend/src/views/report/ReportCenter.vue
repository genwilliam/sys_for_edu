<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import PageHeader from '@/components/PageHeader.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiIcon from '@/components/ui/UiIcon.vue'
import UiTag from '@/components/ui/UiTag.vue'
import StatCard from '@/components/StatCard.vue'
import LineChart from '@/components/charts/LineChart.vue'
import DonutChart from '@/components/charts/DonutChart.vue'
import BarChart from '@/components/charts/BarChart.vue'

const store = useAppStore()

const currentMonth = new Date().toISOString().slice(0, 7)
const monthlyRevenue = computed(() =>
  store.state.orders
    .filter(({ date, payStatus }) => date.startsWith(currentMonth) && payStatus === '已支付')
    .reduce((total, { amount }) => total + amount, 0),
)
const monthlyClassCount = computed(
  () => store.state.attendance.filter(({ date }) => date.startsWith(currentMonth)).length,
)
const monthlyNewStudents = computed(
  () => store.state.students.filter(({ enrollDate }) => enrollDate.startsWith(currentMonth)).length,
)
const paymentRate = computed(() => {
  if (!store.state.orders.length) return 0
  const paidOrders = store.state.orders.filter(({ payStatus }) => payStatus === '已支付').length
  return Number(((paidOrders / store.state.orders.length) * 100).toFixed(1))
})
const revenueTrend = computed(() => {
  const totals = new Map<number, number>()
  for (const order of store.state.orders) {
    if (order.payStatus !== '已支付') continue
    const month = Number(order.date.slice(5, 7))
    totals.set(month, (totals.get(month) ?? 0) + order.amount)
  }
  return Array.from({ length: 12 }, (_, index) => {
    const month = index + 1
    return { label: `${month}月`, value: Math.round((totals.get(month) ?? 0) / 1000) }
  })
})
const courseDistribution = computed(() => {
  const totals = new Map<string, number>()
  for (const course of store.state.courses) {
    totals.set(course.category, (totals.get(course.category) ?? 0) + 1)
  }
  return [...totals].map(([label, value], index) => ({
    label,
    value,
    color: ['#6366f1', '#10b981', '#f59e0b', '#0ea5e9', '#f43f5e'][index % 5]!,
  }))
})
const campusDistribution = computed(() =>
  store.state.campuses.map(({ name }) => {
    const campusName = name.replace(/^(总校区|海淀校区|丰台校区|通州校区) · /, '')
    return {
      label: campusName,
      value: store.state.students.filter((student) => student.campus === campusName).length,
    }
  }),
)
const attendanceTrend = computed(() => {
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return weekdays
    .slice(1)
    .concat(weekdays[0]!)
    .map((label, index) => {
      const records = store.state.attendance.filter((record) => {
        const date = new Date(`${record.date}T00:00:00`)
        return date.getDay() === (index + 1) % 7 && record.status !== '请假'
      })
      const presentCount = records.filter(
        ({ status }) => status === '出勤' || status === '迟到',
      ).length
      return {
        label,
        value: records.length ? Number(((presentCount / records.length) * 100).toFixed(1)) : 0,
      }
    })
})

const reports = [
  { name: '月度营收报表', dimension: '按校区 / 按月', updated: '2025-06-15 10:30', type: '财务' },
  { name: '学员出勤率统计', dimension: '按班级 / 按周', updated: '2025-06-15 09:00', type: '教务' },
  { name: '课消明细表', dimension: '按课程 / 按教师', updated: '2025-06-14 18:20', type: '教务' },
  { name: '新签与续费分析', dimension: '按销售 / 按月', updated: '2025-06-14 17:00', type: '运营' },
  { name: '教师带班汇总', dimension: '按教师 / 按学期', updated: '2025-06-13 16:40', type: '人事' },
]

const typeTone: Record<string, 'brand' | 'info' | 'success' | 'warning'> = {
  财务: 'success',
  教务: 'brand',
  运营: 'warning',
  人事: 'info',
}
</script>

<template>
  <div>
    <PageHeader title="报表中心" description="多维度经营分析，支持报表查看与一键导出">
      <UiButton variant="outline" icon="download" @click="store.toast('已生成合并报表')"
        >导出全部</UiButton
      >
    </PageHeader>

    <section class="report-stats">
      <StatCard
        label="本月营收"
        :value="monthlyRevenue"
        unit="元"
        icon="wallet"
        tone="brand"
        :trend="0"
      />
      <StatCard
        label="本月考勤记录"
        :value="monthlyClassCount"
        unit="条"
        icon="layers"
        tone="info"
      />
      <StatCard
        label="新增学员"
        :value="monthlyNewStudents"
        unit="人"
        icon="users"
        tone="success"
      />
      <StatCard
        label="订单支付率"
        :value="paymentRate"
        unit="%"
        icon="trending-up"
        tone="warning"
      />
    </section>

    <section class="chart-row">
      <UiCard title="营收趋势（万元）" class="chart-row__main">
        <template #extra>
          <UiTag tone="success" dot>环比增长 15.8%</UiTag>
        </template>
        <LineChart
          :labels="revenueTrend.map((i) => i.label)"
          :values="revenueTrend.map((i) => i.value)"
        />
      </UiCard>
      <UiCard title="课程类型占比" class="chart-row__side">
        <DonutChart :segments="courseDistribution" />
      </UiCard>
    </section>

    <section class="chart-row">
      <UiCard title="校区学员分布" class="chart-row__side">
        <BarChart
          :labels="campusDistribution.map((i) => i.label)"
          :values="campusDistribution.map((i) => i.value)"
          unit="人"
          color="#6366f1"
        />
      </UiCard>
      <UiCard title="本周出勤率（%）" class="chart-row__main">
        <LineChart
          :labels="attendanceTrend.map((i) => i.label)"
          :values="attendanceTrend.map((i) => i.value)"
          color="#10b981"
        />
      </UiCard>
    </section>

    <UiCard title="报表下载">
      <table class="report-table">
        <thead>
          <tr>
            <th>报表名称</th>
            <th>统计维度</th>
            <th>类型</th>
            <th>更新时间</th>
            <th class="report-table__actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in reports" :key="r.name">
            <td class="report-table__name">
              <UiIcon name="file-text" :size="16" class="report-table__icon" />
              {{ r.name }}
            </td>
            <td class="text-secondary">{{ r.dimension }}</td>
            <td>
              <UiTag :tone="typeTone[r.type] ?? 'neutral'">{{ r.type }}</UiTag>
            </td>
            <td class="text-secondary">{{ r.updated }}</td>
            <td class="report-table__actions">
              <UiButton
                variant="outline"
                size="sm"
                icon="download"
                @click="store.toast(`正在下载：${r.name}`)"
              >
                下载
              </UiButton>
            </td>
          </tr>
        </tbody>
      </table>
    </UiCard>
  </div>
</template>

<style scoped>
.report-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-row__main {
  min-width: 0;
}

.chart-row__side {
  min-width: 0;
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13.5px;
}

.report-table th {
  text-align: left;
  padding: 12px 0;
  color: var(--gray-500);
  font-weight: 600;
  border-bottom: 1px solid var(--gray-200);
}

.report-table td {
  padding: 14px 0;
  border-bottom: 1px solid var(--gray-100);
  color: var(--gray-700);
}

.report-table__name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--gray-800);
}

.report-table__icon {
  color: var(--brand-500);
}

.report-table__actions {
  text-align: right;
}

@media (max-width: 1100px) {
  .report-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-row {
    grid-template-columns: 1fr;
  }
}
</style>

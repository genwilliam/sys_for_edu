<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Order, PayStatus } from '@/types'
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
import StatCard from '@/components/StatCard.vue'

const store = useAppStore()

const list = computed(() => store.state.orders)
const keyword = ref('')
const payFilter = ref('')
const page = ref(1)
const pageSize = 10
const modalOpen = ref(false)

const payOptions = [
  { label: '已支付', value: '已支付' },
  { label: '待支付', value: '待支付' },
  { label: '已退款', value: '已退款' },
]

const payTone: Record<PayStatus, 'success' | 'warning' | 'danger'> = {
  已支付: 'success',
  待支付: 'warning',
  已退款: 'danger',
}

const totalAmount = computed(() =>
  list.value.filter((o) => o.payStatus === '已支付').reduce((s, o) => s + o.amount, 0),
)

const pendingAmount = computed(() =>
  list.value.filter((o) => o.payStatus === '待支付').reduce((s, o) => s + o.amount, 0),
)

const columns: TableColumn[] = [
  { key: 'orderNo', title: '订单号', width: '180px' },
  { key: 'student', title: '学员', width: '110px' },
  { key: 'course', title: '课程', width: '140px' },
  { key: 'amount', title: '金额', width: '110px', align: 'right' },
  { key: 'method', title: '支付方式', width: '110px' },
  { key: 'date', title: '下单日期', width: '120px' },
  { key: 'payStatus', title: '支付状态', width: '110px' },
  { key: 'salesman', title: '销售', width: '100px' },
]

const filtered = computed(() =>
  list.value.filter((o) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || o.orderNo.includes(kw) || o.student.includes(kw) || o.course.includes(kw)
    const matchPay = !payFilter.value || o.payStatus === payFilter.value
    return matchKw && matchPay
  }),
)

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const form = ref<Order>({
  id: '',
  orderNo: '',
  student: '',
  course: '',
  amount: 0,
  payStatus: '待支付',
  method: '微信支付',
  date: new Date().toISOString().slice(0, 10),
  salesman: '王海燕',
})

function openCreate() {
  form.value = {
    id: '',
    orderNo: `SF${Date.now()}`,
    student: '',
    course: '',
    amount: 0,
    payStatus: '待支付',
    method: '微信支付',
    date: new Date().toISOString().slice(0, 10),
    salesman: '王海燕',
  }
  modalOpen.value = true
}

async function save() {
  if (!form.value.student.trim() || !form.value.course.trim()) {
    store.toast('请填写学员与课程', 'warning')
    return
  }
  if (
    !(await store.createBusinessRecord('orders', {
      ...form.value,
      amount: Number(form.value.amount) || 0,
    }))
  )
    return
  modalOpen.value = false
  store.toast('订单创建成功')
}
</script>

<template>
  <div>
    <PageHeader title="订单管理" description="管理学员报名订单、支付状态与收款情况">
      <UiButton variant="outline" icon="download" @click="store.toast('已导出订单数据')"
        >导出</UiButton
      >
      <UiButton icon="plus" @click="openCreate">新建订单</UiButton>
    </PageHeader>

    <section class="order-stats">
      <StatCard label="累计订单" :value="list.length" unit="笔" icon="receipt" tone="brand" />
      <StatCard label="已收金额" :value="totalAmount" unit="元" icon="wallet" tone="success" />
      <StatCard
        label="待收金额"
        :value="pendingAmount"
        unit="元"
        icon="alert-triangle"
        tone="warning"
      />
    </section>

    <UiCard>
      <div class="page-toolbar">
        <div class="page-toolbar__search">
          <UiInput v-model="keyword" icon="search" placeholder="搜索订单号 / 学员 / 课程" />
        </div>
        <div class="page-toolbar__select">
          <UiSelect v-model="payFilter" :options="payOptions" placeholder="全部支付状态" />
        </div>
        <div class="page-toolbar__spacer" />
        <span class="page-toolbar__count">共 {{ filtered.length }} 笔订单</span>
      </div>

      <UiTable :columns="columns" :data="paged">
        <template #cell-orderNo="{ row }">
          <span class="order-no">{{ row.orderNo }}</span>
        </template>
        <template #cell-amount="{ row }">
          <span class="price">¥ {{ row.amount.toLocaleString() }}</span>
        </template>
        <template #cell-payStatus="{ row }">
          <UiTag :tone="payTone[row.payStatus as PayStatus] ?? 'neutral'" dot>
            {{ row.payStatus }}
          </UiTag>
        </template>
      </UiTable>

      <UiPagination v-model="page" :total="filtered.length" :page-size="pageSize" />
    </UiCard>

    <UiModal v-model="modalOpen" title="新建订单" width="560px">
      <div class="form-grid">
        <UiInput v-model="form.student" label="学员姓名" placeholder="学员姓名" required />
        <UiSelect
          v-model="form.course"
          label="课程"
          :options="[
            { label: '英语启蒙', value: '英语启蒙' },
            { label: '数学思维', value: '数学思维' },
            { label: '语文素养', value: '语文素养' },
            { label: '物理竞赛', value: '物理竞赛' },
            { label: '化学实验', value: '化学实验' },
            { label: '英语听说', value: '英语听说' },
          ]"
        />
        <UiInput v-model="form.amount" label="金额（元）" type="number" />
        <UiSelect
          v-model="form.method"
          label="支付方式"
          :options="[
            { label: '微信支付', value: '微信支付' },
            { label: '支付宝', value: '支付宝' },
            { label: '银行卡', value: '银行卡' },
          ]"
        />
        <UiSelect v-model="form.payStatus" label="支付状态" :options="payOptions" />
        <UiInput v-model="form.date" label="下单日期" type="date" />
        <UiSelect
          v-model="form.salesman"
          label="销售"
          :options="[
            { label: '王海燕', value: '王海燕' },
            { label: '李建国', value: '李建国' },
            { label: '张敏', value: '张敏' },
          ]"
        />
        <div class="form-grid__full">
          <UiInput v-model="form.orderNo" label="订单号" placeholder="自动生成，可修改" />
        </div>
      </div>
      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton @click="save">创建订单</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<style scoped>
.order-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.order-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 13px;
  color: var(--gray-700);
}

.price {
  font-weight: 600;
  color: var(--brand-600);
}

@media (max-width: 900px) {
  .order-stats {
    grid-template-columns: 1fr;
  }
}
</style>

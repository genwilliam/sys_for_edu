<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Visit } from '@/types'
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

const list = computed(() => store.state.visits)
const keyword = ref('')
const typeFilter = ref('')
const page = ref(1)
const pageSize = 10
const modalOpen = ref(false)

const typeOptions = ['电话回访', '面谈', '微信', '家访'].map((t) => ({ label: t, value: t }))
const resultOptions = ['满意', '一般', '待跟进'].map((r) => ({ label: r, value: r }))

const typeTone: Record<string, 'brand' | 'info' | 'success' | 'warning'> = {
  电话回访: 'brand',
  面谈: 'info',
  微信: 'success',
  家访: 'warning',
}

const resultTone: Record<string, 'success' | 'warning' | 'neutral'> = {
  满意: 'success',
  一般: 'warning',
  待跟进: 'neutral',
}

const columns: TableColumn[] = [
  { key: 'student', title: '学员', width: '110px' },
  { key: 'type', title: '回访方式', width: '110px' },
  { key: 'content', title: '回访内容', width: '340px' },
  { key: 'visitor', title: '回访人', width: '100px' },
  { key: 'date', title: '回访日期', width: '120px' },
  { key: 'nextDate', title: '下次跟进', width: '120px' },
  { key: 'result', title: '结果', width: '100px' },
]

const filtered = computed(() =>
  list.value.filter((v) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || v.student.includes(kw) || v.content.includes(kw)
    const matchType = !typeFilter.value || v.type === typeFilter.value
    return matchKw && matchType
  }),
)

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const form = ref<Visit>({
  id: '',
  student: '',
  type: '电话回访',
  content: '',
  visitor: '王海燕',
  date: new Date().toISOString().slice(0, 10),
  nextDate: '',
  result: '待跟进',
})

function openCreate() {
  form.value = {
    id: '',
    student: '',
    type: '电话回访',
    content: '',
    visitor: '王海燕',
    date: new Date().toISOString().slice(0, 10),
    nextDate: '',
    result: '待跟进',
  }
  modalOpen.value = true
}

async function save() {
  if (!form.value.student.trim() || !form.value.content.trim()) {
    store.toast('请填写学员与回访内容', 'warning')
    return
  }
  if (!(await store.createBusinessRecord('visits', form.value))) return
  modalOpen.value = false
  store.toast('回访记录已保存')
}
</script>

<template>
  <div>
    <PageHeader title="回访管理" description="记录学员回访情况，跟踪满意度与续费意向">
      <UiButton variant="outline" icon="download" @click="store.toast('已导出回访记录')"
        >导出</UiButton
      >
      <UiButton icon="plus" @click="openCreate">新增回访</UiButton>
    </PageHeader>

    <UiCard>
      <div class="page-toolbar">
        <div class="page-toolbar__search">
          <UiInput v-model="keyword" icon="search" placeholder="搜索学员 / 回访内容" />
        </div>
        <div class="page-toolbar__select">
          <UiSelect v-model="typeFilter" :options="typeOptions" placeholder="全部方式" />
        </div>
        <div class="page-toolbar__spacer" />
        <span class="page-toolbar__count">共 {{ filtered.length }} 条记录</span>
      </div>

      <UiTable :columns="columns" :data="paged">
        <template #cell-student="{ row }">
          <span class="cell-primary">{{ row.student }}</span>
        </template>
        <template #cell-type="{ row }">
          <UiTag :tone="typeTone[row.type] ?? 'neutral'">{{ row.type }}</UiTag>
        </template>
        <template #cell-nextDate="{ row }">
          <span :class="row.nextDate ? '' : 'text-muted'">{{ row.nextDate || '—' }}</span>
        </template>
        <template #cell-result="{ row }">
          <UiTag :tone="resultTone[row.result] ?? 'neutral'" dot>{{ row.result }}</UiTag>
        </template>
      </UiTable>

      <UiPagination v-model="page" :total="filtered.length" :page-size="pageSize" />
    </UiCard>

    <UiModal v-model="modalOpen" title="新增回访" width="560px">
      <div class="form-grid">
        <UiInput v-model="form.student" label="学员姓名" placeholder="学员姓名" required />
        <UiSelect v-model="form.type" label="回访方式" :options="typeOptions" />
        <UiSelect
          v-model="form.visitor"
          label="回访人"
          :options="[
            { label: '王海燕', value: '王海燕' },
            { label: '李建国', value: '李建国' },
            { label: '张敏', value: '张敏' },
          ]"
        />
        <UiSelect v-model="form.result" label="结果" :options="resultOptions" />
        <UiInput v-model="form.date" label="回访日期" type="date" />
        <UiInput v-model="form.nextDate" label="下次跟进日期" type="date" />
        <div class="form-grid__full">
          <UiInput v-model="form.content" label="回访内容" placeholder="记录回访要点与家长反馈" />
        </div>
      </div>
      <template #footer>
        <UiButton variant="outline" @click="modalOpen = false">取消</UiButton>
        <UiButton @click="save">保存</UiButton>
      </template>
    </UiModal>
  </div>
</template>

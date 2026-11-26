<script setup lang="ts">
import { computed } from 'vue'
import UiIcon from './UiIcon.vue'

const props = withDefaults(
  defineProps<{
    total: number
    pageSize?: number
  }>(),
  {
    pageSize: 10,
  },
)

const page = defineModel<number>({ default: 1 })

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const pages = computed(() => {
  const count = totalPages.value
  const current = page.value
  const set = new Set<number>([1, count, current - 1, current, current + 1])
  const list = [...set].filter((p) => p >= 1 && p <= count).sort((a, b) => a - b)

  const result: (number | '...')[] = []
  for (let i = 0; i < list.length; i++) {
    const p = list[i]
    if (p === undefined) continue
    if (i > 0) {
      const prev = result[result.length - 1]
      if (typeof prev === 'number' && p - prev > 1) result.push('...')
    }
    result.push(p)
  }
  return result
})

function go(p: number) {
  if (p >= 1 && p <= totalPages.value) page.value = p
}
</script>

<template>
  <div class="ui-pagination">
    <span class="ui-pagination__total">共 {{ total }} 条</span>
    <button class="ui-pagination__btn" :disabled="page <= 1" @click="go(page - 1)">
      <UiIcon name="chevron-left" :size="16" />
    </button>
    <template v-for="(p, i) in pages" :key="`${p}-${i}`">
      <span v-if="p === '...'" class="ui-pagination__ellipsis">…</span>
      <button
        v-else
        class="ui-pagination__btn"
        :class="{ 'ui-pagination__btn--active': p === page }"
        @click="go(p)"
      >
        {{ p }}
      </button>
    </template>
    <button class="ui-pagination__btn" :disabled="page >= totalPages" @click="go(page + 1)">
      <UiIcon name="chevron-right" :size="16" />
    </button>
  </div>
</template>

<style scoped>
.ui-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  padding: 14px 20px;
}

.ui-pagination__total {
  font-size: 13px;
  color: var(--gray-500);
  margin-right: 8px;
}

.ui-pagination__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 6px;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--gray-600);
  font-size: 13px;
  transition:
    border-color var(--transition),
    background-color var(--transition),
    color var(--transition);
}

.ui-pagination__btn:hover:not(:disabled) {
  border-color: var(--brand-400);
  color: var(--brand-600);
}

.ui-pagination__btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.ui-pagination__btn--active {
  background: var(--brand-600);
  border-color: var(--brand-600);
  color: #fff;
}

.ui-pagination__btn--active:hover {
  color: #fff;
}

.ui-pagination__ellipsis {
  color: var(--gray-400);
  padding: 0 2px;
}
</style>

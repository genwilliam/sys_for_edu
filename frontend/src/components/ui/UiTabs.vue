<script setup lang="ts">
import type { TabItem } from '@/types'

defineProps<{
  tabs: TabItem[]
}>()

const model = defineModel<string>({ default: '' })
</script>

<template>
  <div class="ui-tabs">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      class="ui-tabs__item"
      :class="{ 'ui-tabs__item--active': model === tab.value }"
      type="button"
      @click="model = tab.value"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<style scoped>
.ui-tabs {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  background: var(--gray-100);
  border-radius: var(--radius-md);
}

.ui-tabs__item {
  height: 32px;
  padding: 0 16px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--gray-600);
  transition:
    background-color var(--transition),
    color var(--transition);
}

.ui-tabs__item:hover {
  color: var(--gray-800);
}

.ui-tabs__item--active {
  background: #fff;
  color: var(--brand-700);
  box-shadow: var(--shadow-sm);
}
</style>

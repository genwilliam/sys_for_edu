<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    labels: string[]
    values: number[]
    color?: string
    unit?: string
    height?: number
  }>(),
  {
    color: '#6366f1',
    unit: '',
    height: 220,
  },
)

const max = computed(() => Math.max(...props.values, 1))

function barHeight(v: number) {
  return `${Math.round((v / max.value) * 100)}%`
}
</script>

<template>
  <div class="bar-chart">
    <div class="bar-chart__bars" :style="{ height: `${height}px` }">
      <div v-for="(v, i) in values" :key="i" class="bar-chart__col">
        <div class="bar-chart__value">{{ v }}{{ unit }}</div>
        <div class="bar-chart__track">
          <div class="bar-chart__bar" :style="{ height: barHeight(v), background: color }" />
        </div>
        <div class="bar-chart__label">{{ labels[i] }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bar-chart__bars {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.bar-chart__col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  min-width: 0;
}

.bar-chart__value {
  font-size: 12px;
  font-weight: 600;
  color: var(--gray-600);
  margin-bottom: 6px;
  white-space: nowrap;
}

.bar-chart__track {
  flex: 1;
  width: 100%;
  max-width: 48px;
  display: flex;
  align-items: flex-end;
  background: var(--gray-50);
  border-radius: 6px 6px 0 0;
}

.bar-chart__bar {
  width: 100%;
  border-radius: 6px 6px 0 0;
  transition: height 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  min-height: 4px;
}

.bar-chart__label {
  margin-top: 8px;
  font-size: 12px;
  color: var(--gray-500);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}
</style>

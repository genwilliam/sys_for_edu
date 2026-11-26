<script setup lang="ts">
import { computed } from 'vue'
import UiIcon from './ui/UiIcon.vue'

const props = withDefaults(
  defineProps<{
    label: string
    value: number | string
    unit?: string
    icon: string
    tone?: 'brand' | 'success' | 'warning' | 'info' | 'danger'
    trend?: number
    trendLabel?: string
  }>(),
  {
    unit: '',
    tone: 'brand',
    trend: 0,
    trendLabel: '较上月',
  },
)

const trendUp = computed(() => (props.trend ?? 0) >= 0)
</script>

<template>
  <div class="stat-card">
    <div class="stat-card__icon" :class="`stat-card__icon--${tone}`">
      <UiIcon :name="icon" :size="22" />
    </div>
    <div class="stat-card__content">
      <p class="stat-card__label">{{ label }}</p>
      <p class="stat-card__value">
        {{ value }}
        <span v-if="unit" class="stat-card__unit">{{ unit }}</span>
      </p>
      <p v-if="trend !== 0" class="stat-card__trend">
        <span class="stat-card__trend-value" :class="trendUp ? 'is-up' : 'is-down'">
          <UiIcon :name="trendUp ? 'trending-up' : 'trending-down'" :size="14" />
          {{ Math.abs(trend) }}%
        </span>
        <span class="stat-card__trend-label">{{ trendLabel }}</span>
      </p>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.stat-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.stat-card__icon--brand {
  background: var(--brand-50);
  color: var(--brand-600);
}

.stat-card__icon--success {
  background: var(--success-soft);
  color: var(--success-strong);
}

.stat-card__icon--warning {
  background: var(--warning-soft);
  color: var(--warning-strong);
}

.stat-card__icon--info {
  background: var(--info-soft);
  color: var(--info-strong);
}

.stat-card__icon--danger {
  background: var(--danger-soft);
  color: var(--danger-strong);
}

.stat-card__label {
  font-size: 13px;
  color: var(--gray-500);
}

.stat-card__value {
  font-size: 26px;
  font-weight: 700;
  color: var(--gray-900);
  line-height: 1.2;
  margin-top: 2px;
}

.stat-card__unit {
  font-size: 13px;
  font-weight: 500;
  color: var(--gray-500);
  margin-left: 2px;
}

.stat-card__trend {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.stat-card__trend-value {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12.5px;
  font-weight: 600;
}

.stat-card__trend-value.is-up {
  color: var(--success-strong);
}

.stat-card__trend-value.is-down {
  color: var(--danger-strong);
}

.stat-card__trend-label {
  font-size: 12px;
  color: var(--gray-400);
}
</style>

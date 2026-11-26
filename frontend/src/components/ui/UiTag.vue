<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    tone?: 'brand' | 'success' | 'warning' | 'danger' | 'info' | 'neutral'
    dot?: boolean
  }>(),
  {
    tone: 'neutral',
    dot: false,
  },
)

const classes = computed(() => ['ui-tag', `ui-tag--${props.tone}`])
</script>

<template>
  <span :class="classes">
    <span v-if="dot" class="ui-tag__dot" />
    <slot />
  </span>
</template>

<style scoped>
.ui-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.7;
  white-space: nowrap;
}

.ui-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.ui-tag--brand {
  background: var(--brand-50);
  color: var(--brand-700);
}

.ui-tag--success {
  background: var(--success-soft);
  color: var(--success-strong);
}

.ui-tag--warning {
  background: var(--warning-soft);
  color: var(--warning-strong);
}

.ui-tag--danger {
  background: var(--danger-soft);
  color: var(--danger-strong);
}

.ui-tag--info {
  background: var(--info-soft);
  color: var(--info-strong);
}

.ui-tag--neutral {
  background: var(--gray-100);
  color: var(--gray-600);
}
</style>

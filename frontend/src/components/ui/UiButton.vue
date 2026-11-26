<script setup lang="ts">
import { computed } from 'vue'
import UiIcon from './UiIcon.vue'

const props = withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'success'
    size?: 'sm' | 'md' | 'lg'
    icon?: string
    block?: boolean
    disabled?: boolean
    type?: 'button' | 'submit'
  }>(),
  {
    variant: 'primary',
    size: 'md',
    icon: '',
    block: false,
    disabled: false,
    type: 'button',
  },
)

const classes = computed(() => [
  'ui-btn',
  `ui-btn--${props.variant}`,
  `ui-btn--${props.size}`,
  { 'ui-btn--block': props.block },
])
</script>

<template>
  <button :type="type" :class="classes" :disabled="disabled">
    <UiIcon v-if="icon" :name="icon" :size="size === 'sm' ? 14 : 16" />
    <span v-if="$slots.default" class="ui-btn__label"><slot /></span>
  </button>
</template>

<style scoped>
.ui-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: var(--radius-sm);
  font-weight: 500;
  white-space: nowrap;
  transition:
    background-color var(--transition),
    border-color var(--transition),
    color var(--transition),
    box-shadow var(--transition);
  border: 1px solid transparent;
}

.ui-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.ui-btn--sm {
  height: 30px;
  padding: 0 10px;
  font-size: 12.5px;
}

.ui-btn--md {
  height: 36px;
  padding: 0 14px;
  font-size: 13.5px;
}

.ui-btn--lg {
  height: 44px;
  padding: 0 20px;
  font-size: 15px;
}

.ui-btn--block {
  width: 100%;
}

.ui-btn--primary {
  background: var(--brand-600);
  color: #fff;
}

.ui-btn--primary:hover:not(:disabled) {
  background: var(--brand-700);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.28);
}

.ui-btn--secondary {
  background: var(--brand-50);
  color: var(--brand-700);
}

.ui-btn--secondary:hover:not(:disabled) {
  background: var(--brand-100);
}

.ui-btn--outline {
  background: #fff;
  color: var(--gray-700);
  border-color: var(--gray-200);
}

.ui-btn--outline:hover:not(:disabled) {
  border-color: var(--brand-400);
  color: var(--brand-600);
}

.ui-btn--ghost {
  background: transparent;
  color: var(--gray-600);
}

.ui-btn--ghost:hover:not(:disabled) {
  background: var(--gray-100);
  color: var(--gray-800);
}

.ui-btn--danger {
  background: var(--danger);
  color: #fff;
}

.ui-btn--danger:hover:not(:disabled) {
  background: var(--danger-strong);
}

.ui-btn--success {
  background: var(--success);
  color: #fff;
}

.ui-btn--success:hover:not(:disabled) {
  background: var(--success-strong);
}
</style>

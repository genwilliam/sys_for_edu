<script setup lang="ts" generic="T extends string = string">
import type { SelectOption } from '@/types'
import UiIcon from './UiIcon.vue'

withDefaults(
  defineProps<{
    label?: string
    placeholder?: string
    options?: SelectOption[]
    required?: boolean
  }>(),
  {
    label: '',
    placeholder: '请选择',
    options: () => [],
    required: false,
  },
)

const model = defineModel<T>({ required: true })

function onChange(e: Event) {
  model.value = (e.target as HTMLSelectElement).value as T
}
</script>

<template>
  <div class="ui-field">
    <label v-if="label" class="ui-field__label">
      {{ label }}
      <span v-if="required" class="ui-field__required">*</span>
    </label>
    <div class="ui-select-wrap">
      <select :value="model" class="ui-select" @change="onChange">
        <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
        <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
      <UiIcon name="chevron-down" :size="16" class="ui-select-wrap__icon" />
    </div>
  </div>
</template>

<style scoped>
.ui-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ui-field__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--gray-700);
}

.ui-field__required {
  color: var(--danger);
  margin-left: 2px;
}

.ui-select-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.ui-select-wrap__icon {
  position: absolute;
  right: 10px;
  color: var(--gray-400);
  pointer-events: none;
}

.ui-select {
  width: 100%;
  height: 36px;
  padding: 0 32px 0 12px;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-sm);
  background: #fff;
  font-size: 13.5px;
  color: var(--gray-800);
  appearance: none;
  cursor: pointer;
  transition:
    border-color var(--transition),
    box-shadow var(--transition);
}

.ui-select:focus {
  outline: none;
  border-color: var(--brand-500);
  box-shadow: 0 0 0 3px var(--brand-100);
}
</style>

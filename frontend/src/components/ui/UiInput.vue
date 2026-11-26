<script setup lang="ts" generic="T extends string | number = string">
import UiIcon from './UiIcon.vue'

withDefaults(
  defineProps<{
    label?: string
    placeholder?: string
    type?: string
    icon?: string
    hint?: string
    required?: boolean
  }>(),
  {
    label: '',
    placeholder: '',
    type: 'text',
    icon: '',
    hint: '',
    required: false,
  },
)

const model = defineModel<T>({ required: true })

function onInput(e: Event) {
  model.value = (e.target as HTMLInputElement).value as T
}
</script>

<template>
  <div class="ui-field">
    <label v-if="label" class="ui-field__label">
      {{ label }}
      <span v-if="required" class="ui-field__required">*</span>
    </label>
    <div class="ui-input-wrap">
      <UiIcon v-if="icon" :name="icon" :size="16" class="ui-input-wrap__icon" />
      <input
        :value="model"
        :type="type"
        :placeholder="placeholder"
        class="ui-input"
        :class="{ 'ui-input--with-icon': icon }"
        @input="onInput"
      />
    </div>
    <p v-if="hint" class="ui-field__hint">{{ hint }}</p>
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

.ui-field__hint {
  font-size: 12px;
  color: var(--gray-400);
}

.ui-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.ui-input-wrap__icon {
  position: absolute;
  left: 12px;
  color: var(--gray-400);
  pointer-events: none;
}

.ui-input {
  width: 100%;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-sm);
  background: #fff;
  font-size: 13.5px;
  color: var(--gray-800);
  transition:
    border-color var(--transition),
    box-shadow var(--transition);
}

.ui-input--with-icon {
  padding-left: 34px;
}

.ui-input::placeholder {
  color: var(--gray-400);
}

.ui-input:focus {
  outline: none;
  border-color: var(--brand-500);
  box-shadow: 0 0 0 3px var(--brand-100);
}
</style>

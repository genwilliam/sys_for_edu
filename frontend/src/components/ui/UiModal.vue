<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import UiIcon from './UiIcon.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title?: string
    width?: string
    footer?: boolean
  }>(),
  {
    title: '',
    width: '520px',
    footer: true,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

function close() {
  emit('update:modelValue', false)
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.modelValue) close()
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click.self="close">
        <div class="modal-panel" :style="{ width }">
          <div class="modal-header">
            <h3 class="modal-title">{{ title }}</h3>
            <button class="modal-close" type="button" @click="close">
              <UiIcon name="x" :size="18" />
            </button>
          </div>
          <div class="modal-body">
            <slot />
          </div>
          <div v-if="footer" class="modal-footer">
            <slot name="footer">
              <button type="button" class="modal-btn modal-btn--cancel" @click="close">取消</button>
              <button type="button" class="modal-btn modal-btn--ok" @click="close">确定</button>
            </slot>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 10vh 20px 20px;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
}

.modal-panel {
  display: flex;
  flex-direction: column;
  max-height: 80vh;
  max-width: 100%;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--gray-100);
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--gray-800);
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  color: var(--gray-500);
  transition: background-color var(--transition);
}

.modal-close:hover {
  background: var(--gray-100);
  color: var(--gray-700);
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--gray-100);
  background: var(--gray-50);
}

.modal-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: var(--radius-sm);
  font-size: 13.5px;
  font-weight: 500;
}

.modal-btn--cancel {
  background: #fff;
  border: 1px solid var(--gray-200);
  color: var(--gray-600);
}

.modal-btn--cancel:hover {
  border-color: var(--gray-300);
}

.modal-btn--ok {
  background: var(--brand-600);
  color: #fff;
}

.modal-btn--ok:hover {
  background: var(--brand-700);
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal-panel,
.modal-leave-active .modal-panel {
  transition: transform 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-panel,
.modal-leave-to .modal-panel {
  transform: translateY(-12px) scale(0.98);
}
</style>

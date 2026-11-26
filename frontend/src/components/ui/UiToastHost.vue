<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import UiIcon from './UiIcon.vue'

const store = useAppStore()

const iconMap: Record<string, string> = {
  success: 'check-circle',
  error: 'alert-circle',
  warning: 'alert-triangle',
  info: 'info',
}
</script>

<template>
  <Teleport to="body">
    <TransitionGroup name="toast" tag="div" class="ui-toast-host">
      <div
        v-for="t in store.state.toasts"
        :key="t.id"
        class="ui-toast"
        :class="`ui-toast--${t.type}`"
      >
        <UiIcon :name="iconMap[t.type] ?? 'info'" :size="18" />
        <span class="ui-toast__msg">{{ t.message }}</span>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<style scoped>
.ui-toast-host {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.ui-toast {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 240px;
  max-width: 360px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: var(--shadow-lg);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--gray-800);
}

.ui-toast--success {
  color: var(--success-strong);
}

.ui-toast--error {
  color: var(--danger-strong);
}

.ui-toast--warning {
  color: var(--warning-strong);
}

.ui-toast--info {
  color: var(--info-strong);
}

.ui-toast__msg {
  color: var(--gray-700);
  font-weight: 400;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>

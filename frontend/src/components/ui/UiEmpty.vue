<script setup lang="ts">
import UiIcon from './UiIcon.vue'

withDefaults(
  defineProps<{
    icon?: string
    text?: string
    description?: string
  }>(),
  {
    icon: 'file-text',
    text: '暂无数据',
    description: '',
  },
)
</script>

<template>
  <div class="ui-empty">
    <div class="ui-empty__icon">
      <UiIcon :name="icon" :size="40" />
    </div>
    <p class="ui-empty__text">{{ text }}</p>
    <p v-if="description" class="ui-empty__desc">{{ description }}</p>
    <div class="ui-empty__action">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.ui-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  text-align: center;
}

.ui-empty__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--gray-100);
  color: var(--gray-400);
  margin-bottom: 16px;
}

.ui-empty__text {
  font-size: 15px;
  font-weight: 500;
  color: var(--gray-600);
}

.ui-empty__desc {
  font-size: 13px;
  color: var(--gray-400);
  margin-top: 6px;
}

.ui-empty__action {
  margin-top: 16px;
}
</style>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    extra?: string
    padding?: boolean
  }>(),
  {
    title: '',
    extra: '',
    padding: true,
  },
)
</script>

<template>
  <div class="ui-card">
    <div v-if="title || $slots.extra" class="ui-card__header">
      <div class="ui-card__title">
        <slot name="title">{{ title }}</slot>
      </div>
      <div class="ui-card__extra">
        <slot name="extra">{{ extra }}</slot>
      </div>
    </div>
    <div class="ui-card__body" :class="{ 'ui-card__body--no-padding': !padding }">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.ui-card {
  background: #fff;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.ui-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--gray-100);
}

.ui-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--gray-800);
}

.ui-card__extra {
  font-size: 13px;
  color: var(--gray-500);
}

.ui-card__body {
  padding: 20px;
}

.ui-card__body--no-padding {
  padding: 0;
}
</style>

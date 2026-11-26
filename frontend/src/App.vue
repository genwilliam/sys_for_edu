<script setup lang="ts">
import MainLayout from '@/layouts/MainLayout.vue'
import LoginView from '@/views/Login.vue'
import UiToastHost from '@/components/ui/UiToastHost.vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
</script>

<template>
  <LoginView v-if="!store.state.isLoggedIn" />
  <MainLayout v-else />
  <UiToastHost />
</template>

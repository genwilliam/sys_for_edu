<script setup lang="ts">
import { onMounted } from 'vue'
import MainLayout from '@/layouts/MainLayout.vue'
import LoginView from '@/views/Login.vue'
import UiToastHost from '@/components/ui/UiToastHost.vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()

onMounted(() => void store.restoreSession())
</script>

<template>
  <div v-if="store.state.authLoading || store.state.dataLoading" class="backend-loading">
    正在连接教务数据服务…
  </div>
  <template v-else>
    <div v-if="store.state.dataError" class="backend-error" role="alert">
      业务数据服务不可用，页面无法读取或保存数据：{{ store.state.dataError }}
    </div>
    <LoginView v-if="!store.state.isLoggedIn" />
    <MainLayout v-else />
  </template>
  <UiToastHost />
</template>

<style scoped>
.backend-loading,
.backend-error {
  padding: 14px 20px;
  color: #92400e;
  background: #fffbeb;
  font-size: 13px;
  text-align: center;
}

.backend-error {
  color: #991b1b;
  background: #fef2f2;
}
</style>

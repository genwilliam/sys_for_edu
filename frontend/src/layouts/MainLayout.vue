<script setup lang="ts">
import { computed } from 'vue'
import { getView } from '@/router'
import { useAppStore } from '@/stores/app'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const store = useAppStore()

const mainMargin = computed(() =>
  store.state.sidebarCollapsed ? 'var(--sidebar-collapsed-width)' : 'var(--sidebar-width)',
)

const currentView = computed(() => getView(store.state.currentPath))
</script>

<template>
  <div class="main-layout">
    <Sidebar />

    <div class="main-area" :style="{ marginLeft: mainMargin }">
      <Header />

      <main class="main-content">
        <Transition name="fade" mode="out-in">
          <component :is="currentView" :key="store.state.currentPath" />
        </Transition>
      </main>
    </div>
  </div>
</template>

<style scoped>
.main-area {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left var(--transition);
}

.main-content {
  flex: 1;
  padding: 24px;
}

@media (max-width: 900px) {
  .main-content {
    padding: 16px;
  }
}
</style>

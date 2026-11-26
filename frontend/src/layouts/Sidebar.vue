<script setup lang="ts">
import { computed } from 'vue'
import { menuGroups } from '@/router'
import { useAppStore } from '@/stores/app'
import UiIcon from '@/components/ui/UiIcon.vue'

defineOptions({ name: 'AppSidebar' })

const store = useAppStore()

const collapsed = computed(() => store.state.sidebarCollapsed)

function isActive(path: string) {
  return store.state.currentPath === path || store.state.currentPath.startsWith(`${path}/`)
}
</script>

<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': collapsed }">
    <div class="sidebar__brand">
      <div class="sidebar__logo">
        <UiIcon name="graduation-cap" :size="22" />
      </div>
      <div v-show="!collapsed" class="sidebar__brand-text">
        <span class="sidebar__brand-name">智慧教培</span>
        <span class="sidebar__brand-sub">教务管理系统</span>
      </div>
    </div>

    <nav class="sidebar__nav">
      <div v-for="group in menuGroups" :key="group.label" class="nav-group">
        <p v-show="!collapsed" class="nav-group__label">{{ group.label }}</p>
        <div v-if="collapsed" class="nav-group__divider" />
        <a
          v-for="item in group.items"
          :key="item.path"
          class="nav-item"
          :class="{ 'nav-item--active': isActive(item.path) }"
          :title="collapsed ? item.title : ''"
          @click="store.navigate(item.path)"
        >
          <span class="nav-item__icon">
            <UiIcon :name="item.icon" :size="18" />
          </span>
          <span v-show="!collapsed" class="nav-item__label">{{ item.title }}</span>
          <span v-if="item.badge && !collapsed" class="nav-item__badge">{{ item.badge }}</span>
        </a>
      </div>
    </nav>

    <div class="sidebar__footer">
      <button class="nav-item nav-item--collapse" type="button" @click="store.toggleSidebar()">
        <span class="nav-item__icon">
          <UiIcon :name="collapsed ? 'chevron-right' : 'chevron-left'" :size="18" />
        </span>
        <span v-show="!collapsed" class="nav-item__label">收起菜单</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 50;
  width: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #1e1b4b 0%, #17162e 100%);
  color: #cbd5e1;
  transition: width var(--transition);
  overflow: hidden;
}

.sidebar--collapsed {
  width: var(--sidebar-collapsed-width);
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: var(--header-height);
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.sidebar--collapsed .sidebar__brand {
  padding: 0;
  justify-content: center;
}

.sidebar__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #818cf8, #6366f1);
  color: #fff;
  flex-shrink: 0;
}

.sidebar__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.sidebar__brand-name {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}

.sidebar__brand-sub {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.sidebar__nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 12px;
}

.sidebar__nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar__nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
}

.nav-group {
  margin-bottom: 14px;
}

.nav-group__label {
  padding: 0 12px;
  margin-bottom: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: #64748b;
  text-transform: uppercase;
}

.nav-group__divider {
  height: 1px;
  margin: 6px 12px;
  background: rgba(255, 255, 255, 0.08);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  margin-bottom: 2px;
  border-radius: var(--radius-sm);
  color: #cbd5e1;
  cursor: pointer;
  white-space: nowrap;
  transition:
    background-color var(--transition),
    color var(--transition);
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.nav-item--active {
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.32), rgba(99, 102, 241, 0.12));
  color: #fff;
  position: relative;
}

.nav-item--active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 2px;
  background: #818cf8;
}

.nav-item--active .nav-item__icon {
  color: #a5b4fc;
}

.sidebar--collapsed .nav-item {
  justify-content: center;
  padding: 0;
}

.nav-item__icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  color: #94a3b8;
}

.nav-item__label {
  flex: 1;
  font-size: 13.5px;
}

.nav-item__badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--radius-full);
  background: var(--danger);
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.sidebar__footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.nav-item--collapse {
  margin-bottom: 0;
  color: #94a3b8;
}
</style>

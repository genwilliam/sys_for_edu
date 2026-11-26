<script setup lang="ts">
import { computed, ref } from 'vue'
import { getMeta } from '@/router'
import { useAppStore } from '@/stores/app'
import UiIcon from '@/components/ui/UiIcon.vue'

defineOptions({ name: 'AppHeader' })

const store = useAppStore()
const menuOpen = ref(false)

const meta = computed(() => getMeta(store.state.currentPath))
const isDashboard = computed(() => store.state.currentPath === '/dashboard')
const initial = computed(() => (store.state.user?.name ?? '管').charAt(0))

function logout() {
  menuOpen.value = false
  store.logout()
}
</script>

<template>
  <header class="header">
    <div class="header__left">
      <button
        class="header__icon-btn header__collapse"
        type="button"
        @click="store.toggleSidebar()"
      >
        <UiIcon name="menu" :size="20" />
      </button>
      <nav class="breadcrumb">
        <template v-if="isDashboard">
          <span class="breadcrumb__current">工作台</span>
        </template>
        <template v-else>
          <span class="breadcrumb__muted">{{ meta.group }}</span>
          <UiIcon name="chevron-right" :size="14" class="breadcrumb__sep" />
          <span class="breadcrumb__current">{{ meta.title }}</span>
        </template>
      </nav>
    </div>

    <div class="header__right">
      <div class="header__search">
        <UiIcon name="search" :size="16" />
        <input type="text" placeholder="搜索学生、班级、课程…" />
      </div>

      <button class="header__icon-btn" type="button" title="通知">
        <UiIcon name="bell" :size="20" />
        <span class="header__dot" />
      </button>

      <div class="header__user">
        <button class="header__user-btn" type="button" @click="menuOpen = !menuOpen">
          <span class="header__avatar">{{ initial }}</span>
          <div class="header__user-info">
            <span class="header__user-name">{{ store.state.user?.name }}</span>
            <span class="header__user-role">{{ store.state.user?.role }}</span>
          </div>
          <UiIcon name="chevron-down" :size="16" class="header__chevron" />
        </button>

        <Transition name="pop">
          <div v-if="menuOpen" class="header__menu">
            <div class="header__menu-head">
              <span class="header__avatar header__avatar--lg">{{ initial }}</span>
              <div>
                <p class="header__menu-name">{{ store.state.user?.name }}</p>
                <p class="header__menu-role">
                  {{ store.state.user?.role }} · {{ store.state.user?.campus }}
                </p>
              </div>
            </div>
            <div class="header__menu-list">
              <button class="header__menu-item" type="button">
                <UiIcon name="user" :size="16" /> 个人设置
              </button>
              <button
                class="header__menu-item header__menu-item--danger"
                type="button"
                @click="logout"
              >
                <UiIcon name="logout" :size="16" /> 退出登录
              </button>
            </div>
          </div>
        </Transition>
        <div v-if="menuOpen" class="header__menu-backdrop" @click="menuOpen = false" />
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--gray-200);
}

.header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header__icon-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
  color: var(--gray-600);
  transition: background-color var(--transition);
}

.header__icon-btn:hover {
  background: var(--gray-100);
}

.header__dot {
  position: absolute;
  top: 9px;
  right: 9px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--danger);
  border: 2px solid #fff;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.breadcrumb__muted {
  color: var(--gray-400);
}

.breadcrumb__sep {
  color: var(--gray-300);
}

.breadcrumb__current {
  color: var(--gray-800);
  font-weight: 500;
}

.header__search {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 240px;
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-full);
  background: var(--gray-50);
  color: var(--gray-400);
}

.header__search input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--gray-800);
}

.header__search input:focus {
  outline: none;
}

.header__user {
  position: relative;
}

.header__user-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px;
  border-radius: var(--radius-md);
}

.header__user-btn:hover {
  background: var(--gray-100);
}

.header__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #818cf8, #6366f1);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.header__avatar--lg {
  width: 40px;
  height: 40px;
  font-size: 16px;
}

.header__user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.header__user-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--gray-800);
}

.header__user-role {
  font-size: 11.5px;
  color: var(--gray-400);
}

.header__chevron {
  color: var(--gray-400);
}

.header__menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 60;
  width: 240px;
  background: #fff;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.header__menu-backdrop {
  position: fixed;
  inset: 0;
  z-index: 59;
}

.header__menu-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--gray-100);
}

.header__menu-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-800);
}

.header__menu-role {
  font-size: 12px;
  color: var(--gray-500);
  margin-top: 2px;
}

.header__menu-list {
  padding: 6px;
}

.header__menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 38px;
  padding: 0 10px;
  border-radius: var(--radius-sm);
  font-size: 13.5px;
  color: var(--gray-700);
  text-align: left;
}

.header__menu-item:hover {
  background: var(--gray-100);
}

.header__menu-item--danger {
  color: var(--danger-strong);
}

.header__menu-item--danger:hover {
  background: var(--danger-soft);
}

.pop-enter-active,
.pop-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>

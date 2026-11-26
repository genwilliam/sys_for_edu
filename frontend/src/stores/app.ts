import { reactive } from 'vue'
import type { Toast, User } from '@/types'

interface AppState {
  currentPath: string
  isLoggedIn: boolean
  user: User | null
  sidebarCollapsed: boolean
  mobileSidebarOpen: boolean
  toasts: Toast[]
}

const state = reactive<AppState>({
  currentPath: '/dashboard',
  isLoggedIn: false,
  user: null,
  sidebarCollapsed: false,
  mobileSidebarOpen: false,
  toasts: [],
})

let toastId = 0

export function useAppStore() {
  function navigate(path: string) {
    state.currentPath = path
    state.mobileSidebarOpen = false
  }

  function login(username: string) {
    state.user = {
      id: 'U001',
      username,
      name: username === 'admin' ? '系统管理员' : '王海燕',
      role: username === 'admin' ? '超级管理员' : '校区校长',
      campus: username === 'admin' ? '全部校区' : '朝阳中心',
      status: 'active',
      lastLogin: '刚刚',
    }
    state.isLoggedIn = true
    state.currentPath = '/dashboard'
  }

  function logout() {
    state.isLoggedIn = false
    state.user = null
    state.currentPath = '/dashboard'
  }

  function toggleSidebar() {
    state.sidebarCollapsed = !state.sidebarCollapsed
  }

  function toast(message: string, type: Toast['type'] = 'success') {
    const id = ++toastId
    state.toasts.push({ id, type, message })
    setTimeout(() => dismissToast(id), 2600)
  }

  function dismissToast(id: number) {
    const index = state.toasts.findIndex((t) => t.id === id)
    if (index !== -1) state.toasts.splice(index, 1)
  }

  return {
    state,
    navigate,
    login,
    logout,
    toggleSidebar,
    toast,
    dismissToast,
  }
}

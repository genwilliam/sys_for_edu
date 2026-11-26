import { reactive } from 'vue'
import type { Student, Teacher, Toast, User } from '@/types'

type SelectedProfile = { kind: 'teacher'; person: Teacher } | { kind: 'student'; person: Student }

interface AppState {
  currentPath: string
  isLoggedIn: boolean
  user: User | null
  profile: SelectedProfile | null
  sidebarCollapsed: boolean
  mobileSidebarOpen: boolean
  toasts: Toast[]
}

const state = reactive<AppState>({
  currentPath: '/dashboard',
  isLoggedIn: false,
  user: null,
  profile: null,
  sidebarCollapsed: false,
  mobileSidebarOpen: false,
  toasts: [],
})

let toastId = 0

export function useAppStore() {
  function navigate(path: string) {
    state.currentPath = path
    if (!path.startsWith('/teacher/') && !path.startsWith('/student/')) {
      state.profile = null
    }
    state.mobileSidebarOpen = false
  }

  function openProfile(kind: SelectedProfile['kind'], person: Teacher | Student) {
    if (kind === 'teacher' && 'subject' in person) {
      state.profile = { kind, person }
    } else if (kind === 'student' && 'guardian' in person) {
      state.profile = { kind, person }
    } else {
      return
    }
    navigate(`/${kind}/${encodeURIComponent(person.id)}`)
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
    state.profile = null
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
    openProfile,
    login,
    logout,
    toggleSidebar,
    toast,
    dismissToast,
  }
}

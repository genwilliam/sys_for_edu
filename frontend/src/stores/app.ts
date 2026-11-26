import { reactive } from 'vue'
import type {
  Attendance,
  Campus,
  ClassGroup,
  Classroom,
  Course,
  Order,
  Role,
  SessionUser,
  Student,
  Teacher,
  Toast,
  User,
  Visit,
} from '@/types'
import {
  createBusinessRecord as createRecordRequest,
  deleteBusinessRecord as deleteRecordRequest,
  getBusinessRecords,
  updateBusinessRecord as updateRecordRequest,
  type BusinessDomain,
} from '@/services/businessData'
import {
  clearAuthToken,
  changePassword as changePasswordRequest,
  getAuthToken,
  getSession,
  login as loginRequest,
  logoutRequest,
  setAuthToken,
} from '@/services/auth'

type SelectedProfile = { kind: 'teacher'; person: Teacher } | { kind: 'student'; person: Student }
type ClassMembership = { id: string; studentIds: string[] }
type IdentifiedRecord = { id: string }

interface AppState {
  currentPath: string
  isLoggedIn: boolean
  user: SessionUser | null
  profile: SelectedProfile | null
  sidebarCollapsed: boolean
  mobileSidebarOpen: boolean
  toasts: Toast[]
  dataLoading: boolean
  authLoading: boolean
  dataError: string
  campuses: Campus[]
  teachers: Teacher[]
  students: Student[]
  classGroups: ClassGroup[]
  courses: Course[]
  classrooms: Classroom[]
  attendance: Attendance[]
  orders: Order[]
  visits: Visit[]
  users: User[]
  roles: Role[]
  classMemberships: ClassMembership[]
  classStudentAssignments: Record<string, string[]>
}

const state = reactive<AppState>({
  currentPath: '/dashboard',
  isLoggedIn: false,
  user: null,
  profile: null,
  sidebarCollapsed: false,
  mobileSidebarOpen: false,
  toasts: [],
  dataLoading: false,
  authLoading: true,
  dataError: '',
  campuses: [],
  teachers: [],
  students: [],
  classGroups: [],
  courses: [],
  classrooms: [],
  attendance: [],
  orders: [],
  visits: [],
  users: [],
  roles: [],
  classMemberships: [],
  classStudentAssignments: {},
})

let toastId = 0

export function useAppStore() {
  function setSessionUser(user: SessionUser) {
    state.user = user
    state.isLoggedIn = true
    state.currentPath = user.mustChangePassword || user.role === 'ADMIN' ? '/dashboard' : '/my-work'
  }

  async function restoreSession() {
    const token = getAuthToken()
    if (!token) {
      state.authLoading = false
      return
    }
    try {
      const user = await getSession()
      setSessionUser(user)
      if (!user.mustChangePassword) await loadBusinessData()
    } catch {
      clearAuthToken()
      state.user = null
      state.isLoggedIn = false
    } finally {
      state.authLoading = false
    }
  }

  async function login(username: string, password: string, remember: boolean) {
    try {
      const session = await loginRequest(username, password)
      setAuthToken(session.token, remember)
      setSessionUser(session.user)
      if (!session.user.mustChangePassword) await loadBusinessData()
      return session.user
    } catch (error) {
      toast(error instanceof Error ? error.message : '登录失败', 'error')
      return null
    }
  }

  async function changeAccountPassword(newPassword: string) {
    try {
      await changePasswordRequest(newPassword)
      clearAuthToken()
      state.user = null
      state.isLoggedIn = false
      state.currentPath = '/dashboard'
      toast('密码已修改，请使用新密码重新登录')
      return true
    } catch (error) {
      toast(error instanceof Error ? error.message : '密码修改失败', 'error')
      return false
    }
  }

  async function loadBusinessData() {
    state.dataLoading = true
    state.dataError = ''
    try {
      const [
        campuses,
        teachers,
        students,
        classGroups,
        courses,
        classrooms,
        attendance,
        orders,
        visits,
        users,
        roles,
        classMemberships,
      ] = await Promise.all([
        getBusinessRecords<Campus>('campuses'),
        getBusinessRecords<Teacher>('teachers'),
        getBusinessRecords<Student>('students'),
        getBusinessRecords<ClassGroup>('classes'),
        getBusinessRecords<Course>('courses'),
        getBusinessRecords<Classroom>('classrooms'),
        getBusinessRecords<Attendance>('attendance'),
        getBusinessRecords<Order>('orders'),
        getBusinessRecords<Visit>('visits'),
        getBusinessRecords<User>('users'),
        getBusinessRecords<Role>('roles'),
        getBusinessRecords<ClassMembership>('class-memberships'),
      ])
      state.campuses.splice(0, state.campuses.length, ...campuses)
      state.teachers.splice(0, state.teachers.length, ...teachers)
      state.students.splice(0, state.students.length, ...students)
      state.classGroups.splice(0, state.classGroups.length, ...classGroups)
      state.courses.splice(0, state.courses.length, ...courses)
      state.classrooms.splice(0, state.classrooms.length, ...classrooms)
      state.attendance.splice(0, state.attendance.length, ...attendance)
      state.orders.splice(0, state.orders.length, ...orders)
      state.visits.splice(0, state.visits.length, ...visits)
      state.users.splice(0, state.users.length, ...users)
      state.roles.splice(0, state.roles.length, ...roles)
      state.classMemberships.splice(0, state.classMemberships.length, ...classMemberships)
      state.classStudentAssignments = Object.fromEntries(
        classMemberships.map(({ id, studentIds }) => [id, studentIds]),
      )
    } catch (error) {
      state.dataError = error instanceof Error ? error.message : '业务数据加载失败'
      toast(`无法从后端加载业务数据：${state.dataError}`, 'error')
    } finally {
      state.dataLoading = false
    }
  }

  function recordsForDomain(domain: BusinessDomain): IdentifiedRecord[] {
    switch (domain) {
      case 'campuses':
        return state.campuses
      case 'teachers':
        return state.teachers
      case 'students':
        return state.students
      case 'classes':
        return state.classGroups
      case 'courses':
        return state.courses
      case 'classrooms':
        return state.classrooms
      case 'attendance':
        return state.attendance
      case 'orders':
        return state.orders
      case 'visits':
        return state.visits
      case 'users':
        return state.users
      case 'roles':
        return state.roles
      case 'class-memberships':
        return state.classMemberships
    }
  }

  function syncMembershipAssignments() {
    state.classStudentAssignments = Object.fromEntries(
      state.classMemberships.map(({ id, studentIds }) => [id, studentIds]),
    )
  }

  async function createBusinessRecord<T extends IdentifiedRecord>(
    domain: BusinessDomain,
    record: T,
  ): Promise<T | null> {
    try {
      const created = await createRecordRequest(domain, record)
      recordsForDomain(domain).unshift(created)
      if (domain === 'class-memberships') syncMembershipAssignments()
      if (domain === 'classes') {
        state.classMemberships.unshift({ id: created.id, studentIds: [] })
        syncMembershipAssignments()
      }
      return created
    } catch (error) {
      toast(error instanceof Error ? error.message : '创建记录失败', 'error')
      return null
    }
  }

  async function updateBusinessRecord<T extends IdentifiedRecord>(
    domain: BusinessDomain,
    id: string,
    record: T,
  ): Promise<T | null> {
    try {
      const updated = await updateRecordRequest(domain, id, record)
      const records = recordsForDomain(domain)
      const index = records.findIndex((item) => item.id === id)
      if (index >= 0) records.splice(index, 1, updated)
      if (domain === 'class-memberships') syncMembershipAssignments()
      return updated
    } catch (error) {
      toast(error instanceof Error ? error.message : '更新记录失败', 'error')
      return null
    }
  }

  async function deleteBusinessRecord(domain: BusinessDomain, id: string): Promise<boolean> {
    try {
      await deleteRecordRequest(domain, id)
      const records = recordsForDomain(domain)
      const index = records.findIndex((item) => item.id === id)
      if (index >= 0) records.splice(index, 1)
      if (domain === 'classes') {
        const membershipIndex = state.classMemberships.findIndex((item) => item.id === id)
        if (membershipIndex >= 0) state.classMemberships.splice(membershipIndex, 1)
        syncMembershipAssignments()
      }
      if (domain === 'students') {
        state.classMemberships = state.classMemberships.map((membership) => ({
          ...membership,
          studentIds: membership.studentIds.filter((studentId) => studentId !== id),
        }))
        syncMembershipAssignments()
      }
      if (domain === 'class-memberships') syncMembershipAssignments()
      return true
    } catch (error) {
      toast(error instanceof Error ? error.message : '删除记录失败', 'error')
      return false
    }
  }

  function studentsInClass(classId: string): string[] {
    return state.classStudentAssignments[classId] ?? []
  }

  async function addStudentsToClass(classId: string, studentIds: string[]) {
    const assigned = new Set(studentsInClass(classId))
    const updated = [...assigned, ...studentIds.filter((studentId) => !assigned.has(studentId))]
    const membership = state.classMemberships.find(({ id }) => id === classId)
    if (membership) {
      return updateBusinessRecord('class-memberships', classId, {
        id: classId,
        studentIds: updated,
      })
    }
    return createBusinessRecord('class-memberships', { id: classId, studentIds: updated })
  }

  async function removeStudentFromClass(classId: string, studentId: string) {
    const membership = state.classMemberships.find(({ id }) => id === classId)
    if (!membership) return null
    const studentIds = studentsInClass(classId).filter((id) => id !== studentId)
    return updateBusinessRecord('class-memberships', classId, { id: classId, studentIds })
  }

  function navigate(path: string) {
    if (state.user?.role === 'TEACHER' || state.user?.role === 'STUDENT') {
      if (path !== '/my-work' && !path.startsWith('/teacher/') && !path.startsWith('/student/')) {
        state.currentPath = '/my-work'
        return
      }
      if (state.user.personId && !path.endsWith(encodeURIComponent(state.user.personId))) {
        state.currentPath = '/my-work'
        return
      }
    } else if (state.user?.role !== 'ADMIN') {
      return
    }
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

  async function logout() {
    try {
      if (getAuthToken()) await logoutRequest()
    } catch (error) {
      toast(error instanceof Error ? error.message : '服务器退出登录失败', 'error')
    } finally {
      clearAuthToken()
      state.isLoggedIn = false
      state.user = null
      state.profile = null
      state.currentPath = '/dashboard'
    }
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
    loadBusinessData,
    restoreSession,
    login,
    changeAccountPassword,
    createBusinessRecord,
    updateBusinessRecord,
    deleteBusinessRecord,
    navigate,
    openProfile,
    logout,
    toggleSidebar,
    toast,
    dismissToast,
    studentsInClass,
    addStudentsToClass,
    removeStudentFromClass,
  }
}

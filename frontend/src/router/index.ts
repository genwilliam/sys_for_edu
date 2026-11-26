import type { Component } from 'vue'

import Dashboard from '@/views/Dashboard.vue'
import LoginView from '@/views/Login.vue'
import SchoolList from '@/views/school/SchoolList.vue'
import TeacherList from '@/views/teacher/TeacherList.vue'
import StudentList from '@/views/student/StudentList.vue'
import ClassList from '@/views/class/ClassList.vue'
import CourseList from '@/views/course/CourseList.vue'
import ClassroomList from '@/views/classroom/ClassroomList.vue'
import ScheduleBoard from '@/views/schedule/ScheduleBoard.vue'
import AttendanceList from '@/views/attendance/AttendanceList.vue'
import OrderList from '@/views/order/OrderList.vue'
import VisitList from '@/views/visit/VisitList.vue'
import ReportCenter from '@/views/report/ReportCenter.vue'
import UserList from '@/views/system/UserList.vue'
import RoleList from '@/views/system/RoleList.vue'

export interface MenuItem {
  path: string
  title: string
  icon: string
  component?: Component
  badge?: string
}

export interface MenuGroup {
  label: string
  items: MenuItem[]
}

export interface RouteMeta {
  title: string
  group: string
  icon: string
}

export const menuGroups: MenuGroup[] = [
  {
    label: '概览',
    items: [{ path: '/dashboard', title: '工作台', icon: 'dashboard', component: Dashboard }],
  },
  {
    label: '基础数据',
    items: [
      { path: '/school', title: '校区管理', icon: 'building', component: SchoolList },
      { path: '/teacher', title: '教师管理', icon: 'graduation-cap', component: TeacherList },
      { path: '/student', title: '学生管理', icon: 'users', component: StudentList },
      { path: '/class', title: '班级管理', icon: 'layers', component: ClassList },
      { path: '/course', title: '课程管理', icon: 'book-open', component: CourseList },
      { path: '/classroom', title: '教室管理', icon: 'door-open', component: ClassroomList },
    ],
  },
  {
    label: '教务管理',
    items: [
      { path: '/schedule', title: '排课管理', icon: 'calendar', component: ScheduleBoard },
      {
        path: '/attendance',
        title: '考勤管理',
        icon: 'clipboard-check',
        component: AttendanceList,
      },
    ],
  },
  {
    label: '运营管理',
    items: [
      { path: '/order', title: '订单管理', icon: 'receipt', component: OrderList },
      { path: '/visit', title: '回访管理', icon: 'phone', component: VisitList },
    ],
  },
  {
    label: '数据报表',
    items: [{ path: '/report', title: '报表中心', icon: 'bar-chart', component: ReportCenter }],
  },
  {
    label: '系统管理',
    items: [
      { path: '/system/users', title: '用户管理', icon: 'user-cog', component: UserList },
      { path: '/system/roles', title: '角色权限', icon: 'shield', component: RoleList },
    ],
  },
]

const routeMap: Record<string, RouteMeta> = {}
const viewMap: Record<string, Component> = {}
const pathToTitle: Record<string, string> = {}

for (const group of menuGroups) {
  for (const item of group.items) {
    routeMap[item.path] = { title: item.title, group: group.label, icon: item.icon }
    pathToTitle[item.path] = item.title
    if (item.component) viewMap[item.path] = item.component
  }
}

export function getView(path: string): Component {
  return viewMap[path] ?? LoginView
}

export function getMeta(path: string): RouteMeta {
  return routeMap[path] ?? { title: '未知页面', group: '', icon: 'info' }
}

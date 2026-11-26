export type Gender = 'male' | 'female'

export type CommonStatus = 'active' | 'inactive'

export interface Campus {
  id: string
  name: string
  address: string
  principal: string
  phone: string
  status: CommonStatus
  classroomCount: number
  studentCount: number
  openedDate: string
}

export interface Teacher {
  id: string
  name: string
  gender: Gender
  phone: string
  subject: string
  title: string
  status: CommonStatus
  joinDate: string
  rating: number
  campus: string
  studentCount: number
}

export interface Student {
  id: string
  name: string
  gender: Gender
  age: number
  phone: string
  guardian: string
  campus: string
  className: string
  status: '在读' | '停课' | '结业'
  enrollDate: string
  courseCount: number
}

export interface ClassGroup {
  id: string
  name: string
  grade: string
  campus: string
  course: string
  teacher: string
  studentCount: number
  capacity: number
  room: string
  status: '开课中' | '未开班' | '已结课'
  schedule: string
}

export interface Course {
  id: string
  name: string
  category: string
  grade: string
  teacher: string
  price: number
  hours: number
  status: CommonStatus
  description: string
}

export interface Classroom {
  id: string
  name: string
  campus: string
  capacity: number
  type: string
  equipment: string[]
  status: '空闲' | '使用中' | '维护中'
}

export interface ScheduleItem {
  id: string
  course: string
  className: string
  teacher: string
  room: string
  campus: string
  weekday: string
  startTime: string
  endTime: string
  type: '常规课' | '试听课' | '补课'
  status: '待上课' | '已上课' | '已取消'
  studentIds?: string[]
}

export type AttendanceStatus = '出勤' | '缺勤' | '迟到' | '请假'

export interface Attendance {
  id: string
  student: string
  className: string
  course: string
  date: string
  time: string
  status: AttendanceStatus
  teacher: string
  note: string
}

export type PayStatus = '已支付' | '待支付' | '已退款'

export interface Order {
  id: string
  orderNo: string
  student: string
  course: string
  amount: number
  payStatus: PayStatus
  method: string
  date: string
  salesman: string
}

export interface Visit {
  id: string
  student: string
  type: '电话回访' | '面谈' | '微信' | '家访'
  content: string
  visitor: string
  date: string
  nextDate: string
  result: '满意' | '一般' | '待跟进'
}

export interface User {
  id: string
  username: string
  name: string
  role: string
  campus: string
  status: CommonStatus
  lastLogin: string
}

export type AccountRole = 'ADMIN' | 'TEACHER' | 'STUDENT'

export interface SessionUser {
  username: string
  role: AccountRole
  personId: string | null
  name: string
  campus: string
  mustChangePassword: boolean
}

export interface AuthSession {
  token: string
  user: SessionUser
}

export interface AuthAccount {
  username: string
  role: AccountRole
  personId: string | null
  name: string
  campus: string
  status: 'ACTIVE' | 'PENDING' | 'DISABLED'
  createdAt: string
}

export interface LeaveRequest {
  id: string
  type: 'STUDENT_LEAVE' | 'TEACHER_LEAVE'
  requesterRole: AccountRole
  requesterPersonId: string
  studentId: string | null
  teacherId: string | null
  classId: string | null
  leaveDate: string
  reason: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  reviewedBy: string | null
  reviewNote: string | null
  createdAt: string
}

export interface Role {
  id: string
  name: string
  code: string
  description: string
  memberCount: number
  permissions: string[]
}

export interface Toast {
  id: number
  type: 'success' | 'error' | 'warning' | 'info'
  message: string
}

/* ---- UI 组件共享类型 ---- */
export interface TableColumn {
  key: string
  title: string
  width?: string
  align?: 'left' | 'center' | 'right'
}

export interface SelectOption {
  label: string
  value: string
}

export interface TabItem {
  label: string
  value: string
}

export interface DonutSegment {
  label: string
  value: number
  color: string
}

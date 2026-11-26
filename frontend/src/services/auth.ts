import type { AuthAccount, AuthSession, LeaveRequest, SessionUser } from '@/types'

const TOKEN_KEY = 'sys-for-edu-auth-token'

export function getAuthToken(): string | null {
  return localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY)
}

export function setAuthToken(token: string, remember: boolean): void {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(TOKEN_KEY, token)
}

export function clearAuthToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}

export async function authRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getAuthToken()
  const response = await fetch(`/api/auth${path}`, {
    ...init,
    headers: {
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  })
  const text = await response.text()
  let data: unknown
  if (text) {
    try {
      data = JSON.parse(text) as unknown
    } catch {
      data = text
    }
  }
  if (!response.ok) {
    const body = data as Record<string, unknown> | string | null
    const message =
      typeof body === 'string'
        ? body
        : body && typeof body === 'object' && typeof body.message === 'string'
          ? body.message
          : `请求失败（HTTP ${response.status}）`
    throw new Error(message)
  }
  if (response.status === 204 || !text) return undefined as T
  return data as T
}

export function login(username: string, password: string): Promise<AuthSession> {
  return authRequest<AuthSession>('/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function getSession(): Promise<SessionUser> {
  return authRequest<SessionUser>('/session')
}

export function registerAccount(input: {
  role: 'TEACHER' | 'STUDENT'
  personId: string
  phone: string
  username: string
  password: string
}): Promise<{ role: string; name: string; pendingApproval: boolean }> {
  return authRequest('/register', { method: 'POST', body: JSON.stringify(input) })
}

export function changePassword(newPassword: string): Promise<void> {
  return authRequest('/change-password', {
    method: 'POST',
    body: JSON.stringify({ newPassword }),
  })
}

export function logoutRequest(): Promise<void> {
  return authRequest('/logout', { method: 'POST' })
}

export function getAuthAccounts(): Promise<AuthAccount[]> {
  return authRequest<AuthAccount[]>('/accounts')
}

export function setAuthAccountStatus(username: string, status: 'ACTIVE' | 'DISABLED') {
  return authRequest<void>(`/accounts/${encodeURIComponent(username)}`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}

export function getLeaveRequests(): Promise<LeaveRequest[]> {
  return authRequest<LeaveRequest[]>('/leave-requests')
}

export function createLeaveRequest(input: {
  type: LeaveRequest['type']
  classId?: string
  leaveDate: string
  reason: string
}): Promise<LeaveRequest> {
  return authRequest<LeaveRequest>('/leave-requests', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function reviewLeaveRequest(
  id: string,
  status: 'APPROVED' | 'REJECTED',
  note = '',
): Promise<LeaveRequest> {
  return authRequest<LeaveRequest>(`/leave-requests/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: JSON.stringify({ status, note }),
  })
}

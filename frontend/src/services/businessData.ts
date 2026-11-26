export type BusinessDomain =
  | 'campuses'
  | 'teachers'
  | 'students'
  | 'classes'
  | 'courses'
  | 'classrooms'
  | 'attendance'
  | 'orders'
  | 'visits'
  | 'users'
  | 'roles'
  | 'class-memberships'

export class BusinessDataApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message)
    this.name = 'BusinessDataApiError'
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api/data${path}`, {
    ...init,
    headers: {
      ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
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
        : body && typeof body === 'object'
          ? ([
              body.message,
              body.error,
              body.detail,
              body.title,
              typeof body.errors === 'string' ? body.errors : undefined,
              Array.isArray(body.errors) ? body.errors.join('；') : undefined,
            ].find((value): value is string => typeof value === 'string' && value.length > 0) ??
            `请求失败（HTTP ${response.status}）`)
          : `请求失败（HTTP ${response.status}）`
    throw new BusinessDataApiError(message, response.status)
  }

  if (response.status === 204 || !text) return undefined as T
  return data as T
}

export function getBusinessRecords<T>(domain: BusinessDomain): Promise<T[]> {
  return request<T[]>(`/${domain}`)
}

export function createBusinessRecord<T extends { id: string }>(
  domain: BusinessDomain,
  record: T,
): Promise<T> {
  return request<T>(`/${domain}`, { method: 'POST', body: JSON.stringify(record) })
}

export function updateBusinessRecord<T extends { id: string }>(
  domain: BusinessDomain,
  id: string,
  record: T,
): Promise<T> {
  return request<T>(`/${domain}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(record),
  })
}

export function deleteBusinessRecord(domain: BusinessDomain, id: string): Promise<void> {
  return request<void>(`/${domain}/${encodeURIComponent(id)}`, { method: 'DELETE' })
}
import { getAuthToken } from '@/services/auth'

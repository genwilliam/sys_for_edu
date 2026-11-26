import type { ScheduleItem } from '@/types'

export type ScheduleDraft = Omit<ScheduleItem, 'id'>

export interface ScheduleFilters {
  campus?: string
  course?: string
  teacher?: string
  room?: string
  weekday?: string
  status?: string
}

export interface AutoPlanRequest {
  course: string
  className: string
  teacher: string
  room: string
  campus: string
  weeklySessions: number
  durationMinutes: number
  weekdays: string[]
  startTime: string
  endTime: string
  type: ScheduleItem['type']
}

export interface AutoPlanResponse {
  items: ScheduleDraft[]
  complete: boolean
  message: string
  explanation: string
}

export class ScheduleApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message)
    this.name = 'ScheduleApiError'
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api/schedule-items${path}`, {
    ...init,
    headers: {
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
    throw new ScheduleApiError(message, response.status)
  }

  if (response.status === 204 || !text) return undefined as T
  return data as T
}

export function getScheduleItems(filters: ScheduleFilters = {}): Promise<ScheduleItem[]> {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value) params.set(key, value)
  }
  const query = params.toString()
  return request<ScheduleItem[]>(query ? `?${query}` : '')
}

export function createScheduleItem(item: ScheduleDraft): Promise<ScheduleItem> {
  return request<ScheduleItem>('', { method: 'POST', body: JSON.stringify(item) })
}

export function updateScheduleItem(id: string, item: ScheduleDraft): Promise<ScheduleItem> {
  return request<ScheduleItem>(`/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(item),
  })
}

export function cancelScheduleItem(id: string): Promise<ScheduleItem> {
  return request<ScheduleItem>(`/${encodeURIComponent(id)}/cancel`, { method: 'PATCH' })
}

export function createScheduleBatch(items: ScheduleDraft[]): Promise<ScheduleItem[]> {
  return request<ScheduleItem[]>('/batch', { method: 'POST', body: JSON.stringify({ items }) })
}

export function autoPlanSchedule(requestBody: AutoPlanRequest): Promise<AutoPlanResponse> {
  return request<AutoPlanResponse>('/auto-plan', {
    method: 'POST',
    body: JSON.stringify(requestBody),
  })
}

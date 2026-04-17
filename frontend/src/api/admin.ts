import client from './client'

export interface AdminUser {
  id: string
  email: string
  displayName: string
  role: string
  status: string
  loginCount: number
  totalInterviews: number
  avgScore: number
  totalTokensUsed: number
  createdAt: string
  lastLogin: string | null
}

export interface DayCount {
  date: string
  count: number
}

export interface PlatformMetrics {
  totalUsers: number
  pendingUsers: number
  activeUsers: number
  suspendedUsers: number
  newUsersLast7Days: number
  newUsersLast30Days: number
  activeUsersLast7Days: number
  activeUsersLast30Days: number
  dormantUsers: number
  powerUsers: number
  totalInterviews: number
  interviewsLast7Days: number
  totalTokensUsed: number
  newUsersByDay: DayCount[]
  interviewsByDay: DayCount[]
}

export const getUsers = () => client.get<AdminUser[]>('/api/v1/admin/users')
export const approveUser = (id: string) => client.post<AdminUser>(`/api/v1/admin/users/${id}/approve`)
export const rejectUser = (id: string) => client.post<AdminUser>(`/api/v1/admin/users/${id}/reject`)
export const suspendUser = (id: string) => client.post<AdminUser>(`/api/v1/admin/users/${id}/suspend`)
export const reactivateUser = (id: string) => client.post<AdminUser>(`/api/v1/admin/users/${id}/reactivate`)
export const getMetrics = () => client.get<PlatformMetrics>('/api/v1/admin/metrics')

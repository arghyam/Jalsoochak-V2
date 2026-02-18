// API service for dashboard data

import { apiClient } from '@/shared/lib/axios'
import type { DashboardData, DashboardLevel } from '../../types'

export const dashboardApi = {
  /**
   * Get dashboard data for a specific level and entity
   * @param level - Dashboard level (central, state, district, etc.)
   * @param entityId - Optional entity ID (state code, district ID, etc.)
   * @returns Dashboard data
   */
  getDashboardData: async (level: DashboardLevel, entityId?: string): Promise<DashboardData> => {
    if (level !== 'central' && !entityId) {
      throw new Error(`entityId is required for dashboard level: ${level}`)
    }
    const endpoint =
      level === 'central' ? '/api/dashboard/central' : `/api/dashboard/${level}/${entityId}`
    const response = await apiClient.get<DashboardData>(endpoint)
    return response.data
  },
}

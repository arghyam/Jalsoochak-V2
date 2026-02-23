import { useQuery } from '@tanstack/react-query'
import { dashboardMockService } from '../services/mock/dashboard-mock'
import type { DashboardData, DashboardLevel } from '../types'

/**
 * Hook to fetch dashboard data
 * Currently uses mock data
 */
export function useDashboardData(level: DashboardLevel, entityId?: string) {
  return useQuery<DashboardData>({
    queryKey: ['dashboard', level, entityId],
    queryFn: () => dashboardMockService.getDashboardData(level, entityId),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

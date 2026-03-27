export interface SuperAdminStats {
  totalStatesManaged: number
  activeStates: number
  inactiveStates: number
}

export interface IngestionDataPoint {
  month: string
  successfulIngestions: number
  failedIngestions: number
}

export interface Notification {
  id: string
  message: string
  timestamp: Date
}

export interface SuperAdminOverviewData {
  stats: SuperAdminStats
  ingestionData: IngestionDataPoint[]
  notifications: Notification[]
}

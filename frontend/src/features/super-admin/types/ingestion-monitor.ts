export interface IngestionStats {
  totalIngestions: number
  successfulIngestions: number
  failedIngestions: number
  currentWarnings: number
  successRate: number
  failureRate: number
}

export interface IngestionChartDataPoint {
  month: string
  successfulIngestions: number
  failedIngestions: number
}

export type IngestionLogStatus = 'successful' | 'warning' | 'failed'

export interface IngestionLogEntry {
  id: string
  title: string
  description: string
  batchId: string
  sourceSystem: string
  processingTime: string
  recordsProcessed?: number
  status: IngestionLogStatus
  timestamp: Date
  issueDetails?: string
  errorDetails?: string
}

export interface IngestionMonitorData {
  stats: IngestionStats
  chartData: IngestionChartDataPoint[]
  logs: IngestionLogEntry[]
}

export interface IngestionFilterOption {
  value: string
  label: string
}

export const STATE_FILTER_OPTIONS: IngestionFilterOption[] = [
  { value: 'all', label: 'All States/UTs' },
  { value: 'karnataka', label: 'Karnataka' },
  { value: 'maharashtra', label: 'Maharashtra' },
  { value: 'tamil-nadu', label: 'Tamil Nadu' },
  { value: 'andhra-pradesh', label: 'Andhra Pradesh' },
  { value: 'telangana', label: 'Telangana' },
  { value: 'kerala', label: 'Kerala' },
  { value: 'gujarat', label: 'Gujarat' },
  { value: 'rajasthan', label: 'Rajasthan' },
]

export const TIME_FILTER_OPTIONS: IngestionFilterOption[] = [
  { value: '7', label: 'Last 7 Days' },
  { value: '14', label: 'Last 14 Days' },
  { value: '30', label: 'Last 30 Days' },
  { value: '90', label: 'Last 90 Days' },
]

export const STATUS_FILTER_OPTIONS: IngestionFilterOption[] = [
  { value: 'all', label: 'Status' },
  { value: 'successful', label: 'Successful' },
  { value: 'warning', label: 'Warning' },
  { value: 'failed', label: 'Failed' },
]

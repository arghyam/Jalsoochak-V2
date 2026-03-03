export interface ActivityLog extends Record<string, unknown> {
  id: string
  timestamp: Date
  action: string
  status: 'Success' | 'Failed'
}

export type ActivityStatus = 'Success' | 'Failed'

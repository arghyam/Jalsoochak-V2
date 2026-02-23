export interface StatsCard {
  id: string
  title: string
  value: string | number
  subtitle?: string
}

export interface DemandSupplyDataPoint {
  period: string
  Demand: number
  Supply: number
}

export interface DailyIngestionDataPoint {
  day: string
  count: number
}

export interface OverviewData {
  stats: {
    pumpOperatorsSynced: number
    configurationStatus: string
    todayApiIngestion: string
    pendingDataSync: number
    activeIntegrations: number
  }
  demandSupplyData: DemandSupplyDataPoint[]
  dailyIngestionData: DailyIngestionDataPoint[]
}

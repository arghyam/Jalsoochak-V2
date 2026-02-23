export interface SystemRulesConfiguration {
  id: string
  coverage: string
  continuity: string
  quantity: string
  regularity: string
  isConfigured: boolean
}

export interface SystemRulesOption {
  value: string
  label: string
}

// Coverage options - percentage values
export const COVERAGE_OPTIONS: SystemRulesOption[] = [
  { value: '30', label: '30%' },
  { value: '40', label: '40%' },
  { value: '50', label: '50%' },
  { value: '60', label: '60%' },
  { value: '70', label: '70%' },
  { value: '80', label: '80%' },
  { value: '90', label: '90%' },
]

// Continuity options - number of days
export const CONTINUITY_OPTIONS: SystemRulesOption[] = [
  { value: '1', label: '1 Day' },
  { value: '2', label: '2 Days' },
  { value: '3', label: '3 Days' },
  { value: '4', label: '4 Days' },
  { value: '5', label: '5 Days' },
  { value: '7', label: '7 Days' },
  { value: '10', label: '10 Days' },
]

// Quantity options - LPCD values
export const QUANTITY_OPTIONS: SystemRulesOption[] = [
  { value: '40', label: '40 LPCD' },
  { value: '50', label: '50 LPCD' },
  { value: '55', label: '55 LPCD' },
  { value: '60', label: '60 LPCD' },
  { value: '70', label: '70 LPCD' },
  { value: '80', label: '80 LPCD' },
  { value: '90', label: '90 LPCD' },
  { value: '100', label: '100 LPCD' },
]

// Regularity options - percentage values
export const REGULARITY_OPTIONS: SystemRulesOption[] = [
  { value: '30', label: '30%' },
  { value: '40', label: '40%' },
  { value: '50', label: '50%' },
  { value: '60', label: '60%' },
  { value: '70', label: '70%' },
  { value: '80', label: '80%' },
  { value: '90', label: '90%' },
]

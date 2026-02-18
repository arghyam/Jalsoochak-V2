export interface ThresholdConfiguration {
  id: string
  coverage: string
  continuity: string
  quantity: string
  regularity: string
  isConfigured: boolean
}

export interface ThresholdOption {
  value: string
  label: string
}

// Coverage options - percentage values
export const COVERAGE_OPTIONS: ThresholdOption[] = [
  { value: '50', label: '50%' },
  { value: '60', label: '60%' },
  { value: '70', label: '70%' },
  { value: '75', label: '75%' },
  { value: '80', label: '80%' },
  { value: '85', label: '85%' },
  { value: '90', label: '90%' },
  { value: '95', label: '95%' },
]

// Continuity options - number of days
export const CONTINUITY_OPTIONS: ThresholdOption[] = [
  { value: '1', label: '1' },
  { value: '2', label: '2' },
  { value: '3', label: '3' },
  { value: '4', label: '4' },
  { value: '5', label: '5' },
  { value: '7', label: '7' },
  { value: '10', label: '10' },
]

// Quantity options - LPCD values
export const QUANTITY_OPTIONS: ThresholdOption[] = [
  { value: '40', label: '40' },
  { value: '50', label: '50' },
  { value: '55', label: '55' },
  { value: '60', label: '60' },
  { value: '70', label: '70' },
  { value: '80', label: '80' },
  { value: '90', label: '90' },
  { value: '100', label: '100' },
]

// Regularity options - percentage values
export const REGULARITY_OPTIONS: ThresholdOption[] = [
  { value: '50', label: '50%' },
  { value: '60', label: '60%' },
  { value: '70', label: '70%' },
  { value: '75', label: '75%' },
  { value: '80', label: '80%' },
  { value: '85', label: '85%' },
  { value: '90', label: '90%' },
  { value: '95', label: '95%' },
]

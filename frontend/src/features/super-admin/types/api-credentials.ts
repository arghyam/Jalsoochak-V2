export type ApiCredentialStatus = 'active' | 'inactive'

export interface ApiCredential {
  id: string
  stateUtName: string
  apiKey: string
  lastUsed: Date
  createdOn: Date
  nextRotation: Date
  status: ApiCredentialStatus
}

export interface ApiCredentialsData {
  credentials: ApiCredential[]
}

export interface ApiCredentialsFilterOption {
  value: string
  label: string
}

export const STATUS_FILTER_OPTIONS: ApiCredentialsFilterOption[] = [
  { value: 'all', label: 'Status' },
  { value: 'active', label: 'Active' },
  { value: 'inactive', label: 'Inactive' },
]

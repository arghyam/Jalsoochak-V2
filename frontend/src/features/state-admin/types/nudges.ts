export interface NudgeMessage {
  language: string
  message: string
}

export interface NudgeTemplate {
  id: string
  name: string
  type: string
  availableVariables: string[]
  messages: NudgeMessage[]
}

export interface NudgeTemplateFormData {
  language: string
  message: string
}

export interface AlertTypeOption {
  value: string
  label: string
}

export const AVAILABLE_ALERT_TYPES: AlertTypeOption[] = [
  { value: 'no-water-alert', label: 'No-Water Alert' },
  { value: 'low-quantity-alert', label: 'Low Quantity Alert' },
  { value: 'operator-inactivity', label: 'Operator Inactivity' },
]

export interface LanguageOption {
  value: string
  label: string
}

export const AVAILABLE_LANGUAGES: LanguageOption[] = [
  { value: 'english', label: 'English' },
  { value: 'hindi', label: 'Hindi' },
  { value: 'telugu', label: 'Telugu' },
  { value: 'kannada', label: 'Kannada' },
  { value: 'tamil', label: 'Tamil' },
]

// Available variables by alert type
export const ALERT_VARIABLES: Record<string, string[]> = {
  'no-water-alert': ['{operator_name}', '{village_name}', '{days}'],
  'low-quantity-alert': ['{operator_name}', '{village_name}', '{LPCD}'],
  'operator-inactivity': ['{operator_name}', '{village_name}', '{days}', '{last_report_day}'],
}

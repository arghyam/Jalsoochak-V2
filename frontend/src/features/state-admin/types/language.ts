export interface LanguageOption {
  value: string
  label: string
}

export interface LanguageConfiguration extends Record<string, unknown> {
  id: string
  primaryLanguage: string
  secondaryLanguage?: string
  isConfigured: boolean
}

export const AVAILABLE_LANGUAGES: LanguageOption[] = [
  { value: 'telugu', label: 'Telugu' },
  { value: 'hindi', label: 'Hindi' },
  { value: 'english', label: 'English' },
  { value: 'tamil', label: 'Tamil' },
  { value: 'kannada', label: 'Kannada' },
  { value: 'malayalam', label: 'Malayalam' },
  { value: 'bengali', label: 'Bengali' },
  { value: 'marathi', label: 'Marathi' },
  { value: 'gujarati', label: 'Gujarati' },
  { value: 'punjabi', label: 'Punjabi' },
]

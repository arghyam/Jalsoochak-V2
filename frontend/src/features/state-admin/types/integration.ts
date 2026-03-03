export interface IntegrationConfiguration extends Record<string, unknown> {
  id: string
  whatsappBusinessAccountName: string
  senderPhoneNumber: string
  whatsappBusinessAccountId: string
  apiAccessToken: string
  isConfigured: boolean
}

export interface IntegrationFormData {
  whatsappBusinessAccountName: string
  senderPhoneNumber: string
  whatsappBusinessAccountId: string
  apiAccessToken: string
}

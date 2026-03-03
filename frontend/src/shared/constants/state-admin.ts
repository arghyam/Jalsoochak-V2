export const NOTIFY_ROLES = {
  SECTION_OFFICER: 'SectionOfficer',
  ASSISTANT_EXECUTIVE_ENGINEER: 'AssistantExecutiveEngineer',
  EXECUTIVE_ENGINEER: 'ExecutiveEngineer',
} as const

export type NotifyRole = (typeof NOTIFY_ROLES)[keyof typeof NOTIFY_ROLES]

export const NOTIFY_ROLE_LABELS: Record<NotifyRole, string> = {
  [NOTIFY_ROLES.SECTION_OFFICER]: 'Section Officer',
  [NOTIFY_ROLES.ASSISTANT_EXECUTIVE_ENGINEER]: 'Assistant Executive Engineer',
  [NOTIFY_ROLES.EXECUTIVE_ENGINEER]: 'Executive Engineer',
}

export const CONDITION_TYPES = {
  NO_SUBMISSION: 'no_submission',
  THRESHOLD_BREACH: 'threshold_breach',
  ANOMALY_DETECTED: 'anomaly_detected',
} as const

export type ConditionType = (typeof CONDITION_TYPES)[keyof typeof CONDITION_TYPES]

export const CONDITION_TYPE_LABELS: Record<ConditionType, string> = {
  [CONDITION_TYPES.NO_SUBMISSION]: 'No Submission',
  [CONDITION_TYPES.THRESHOLD_BREACH]: 'Threshold Breach',
  [CONDITION_TYPES.ANOMALY_DETECTED]: 'Anomaly Detected',
}

export const MESSAGE_FREQUENCIES = {
  DAILY: 'daily',
  WEEKLY: 'weekly',
  MONTHLY: 'monthly',
} as const

export type MessageFrequency = (typeof MESSAGE_FREQUENCIES)[keyof typeof MESSAGE_FREQUENCIES]

export const MESSAGE_FREQUENCY_LABELS: Record<MessageFrequency, string> = {
  [MESSAGE_FREQUENCIES.DAILY]: 'Daily',
  [MESSAGE_FREQUENCIES.WEEKLY]: 'Weekly',
  [MESSAGE_FREQUENCIES.MONTHLY]: 'Monthly',
}

export const WATER_NORM_CATEGORIES = {
  RURAL: 'rural',
  URBAN: 'urban',
} as const

export type WaterNormCategory = (typeof WATER_NORM_CATEGORIES)[keyof typeof WATER_NORM_CATEGORIES]

export const WATER_NORM_CATEGORY_LABELS: Record<WaterNormCategory, string> = {
  [WATER_NORM_CATEGORIES.RURAL]: 'Rural',
  [WATER_NORM_CATEGORIES.URBAN]: 'Urban',
}

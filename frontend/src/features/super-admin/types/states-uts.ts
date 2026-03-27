export type StateUTStatus = 'active' | 'inactive'

export interface StateAdminDetails {
  firstName: string
  lastName: string
  email: string
  phone: string
  secondaryEmail?: string
  contactNumber?: string
}

export interface StateUT {
  id: string
  name: string
  code: string
  status: StateUTStatus
  lastSyncDate: Date
  totalDistricts: number
  admin: StateAdminDetails
}

export interface CreateStateUTInput {
  name: string
  code: string
  admin: StateAdminDetails
}

export interface UpdateStateUTInput {
  admin: Omit<StateAdminDetails, 'email'> & { email?: string }
}

// All 36 Indian States and Union Territories with their codes
export const INDIAN_STATES_UTS: { name: string; code: string }[] = [
  { name: 'Andhra Pradesh', code: '28' },
  { name: 'Arunachal Pradesh', code: '12' },
  { name: 'Assam', code: '18' },
  { name: 'Bihar', code: '10' },
  { name: 'Chhattisgarh', code: '22' },
  { name: 'Goa', code: '30' },
  { name: 'Gujarat', code: '24' },
  { name: 'Haryana', code: '06' },
  { name: 'Himachal Pradesh', code: '02' },
  { name: 'Jharkhand', code: '20' },
  { name: 'Karnataka', code: '29' },
  { name: 'Kerala', code: '32' },
  { name: 'Madhya Pradesh', code: '23' },
  { name: 'Maharashtra', code: '27' },
  { name: 'Manipur', code: '14' },
  { name: 'Meghalaya', code: '17' },
  { name: 'Mizoram', code: '15' },
  { name: 'Nagaland', code: '13' },
  { name: 'Odisha', code: '21' },
  { name: 'Punjab', code: '03' },
  { name: 'Rajasthan', code: '08' },
  { name: 'Sikkim', code: '11' },
  { name: 'Tamil Nadu', code: '33' },
  { name: 'Telangana', code: '36' },
  { name: 'Tripura', code: '16' },
  { name: 'Uttar Pradesh', code: '09' },
  { name: 'Uttarakhand', code: '05' },
  { name: 'West Bengal', code: '19' },
  // Union Territories
  { name: 'Andaman and Nicobar Islands', code: '35' },
  { name: 'Chandigarh', code: '04' },
  { name: 'Dadra and Nagar Haveli and Daman and Diu', code: '26' },
  { name: 'Delhi', code: '07' },
  { name: 'Jammu and Kashmir', code: '01' },
  { name: 'Ladakh', code: '37' },
  { name: 'Lakshadweep', code: '31' },
  { name: 'Puducherry', code: '34' },
]

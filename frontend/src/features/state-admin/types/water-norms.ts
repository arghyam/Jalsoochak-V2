export interface DistrictOverride {
  id: string
  districtName: string
  quantity: number
}

export interface WaterNormsConfiguration extends Record<string, unknown> {
  id: string
  stateQuantity: number
  districtOverrides: DistrictOverride[]
  isConfigured: boolean
}

export interface DistrictOption {
  value: string
  label: string
}

export const AVAILABLE_DISTRICTS: DistrictOption[] = [
  { value: 'hyderabad', label: 'Hyderabad' },
  { value: 'rangareddy', label: 'Rangareddy' },
  { value: 'medchal', label: 'Medchal-Malkajgiri' },
  { value: 'warangal-urban', label: 'Warangal Urban' },
  { value: 'warangal-rural', label: 'Warangal Rural' },
  { value: 'karimnagar', label: 'Karimnagar' },
  { value: 'khammam', label: 'Khammam' },
  { value: 'nalgonda', label: 'Nalgonda' },
  { value: 'mahbubnagar', label: 'Mahbubnagar' },
  { value: 'nizamabad', label: 'Nizamabad' },
  { value: 'adilabad', label: 'Adilabad' },
  { value: 'sangareddy', label: 'Sangareddy' },
  { value: 'vikarabad', label: 'Vikarabad' },
  { value: 'medak', label: 'Medak' },
  { value: 'siddipet', label: 'Siddipet' },
  { value: 'nirmal', label: 'Nirmal' },
  { value: 'mancherial', label: 'Mancherial' },
  { value: 'peddapalli', label: 'Peddapalli' },
  { value: 'jayashankar', label: 'Jayashankar Bhupalpally' },
  { value: 'jangaon', label: 'Jangaon' },
  { value: 'yadadri', label: 'Yadadri Bhuvanagiri' },
  { value: 'suryapet', label: 'Suryapet' },
  { value: 'nagarkurnool', label: 'Nagarkurnool' },
  { value: 'wanaparthy', label: 'Wanaparthy' },
  { value: 'jogulamba', label: 'Jogulamba Gadwal' },
  { value: 'rajanna', label: 'Rajanna Sircilla' },
  { value: 'kamareddy', label: 'Kamareddy' },
  { value: 'mahabubabad', label: 'Mahabubabad' },
  { value: 'bhadradri', label: 'Bhadradri Kothagudem' },
  { value: 'mulugu', label: 'Mulugu' },
  { value: 'narayanpet', label: 'Narayanpet' },
  { value: 'komaram-bheem', label: 'Komaram Bheem Asifabad' },
  { value: 'jagtial', label: 'Jagtial' },
]

import type { SuperAdminOverviewData } from '../types/overview'
import type { SystemRulesConfiguration } from '../types/system-rules'
import type { IngestionMonitorData } from '../types/ingestion-monitor'
import type { ApiCredentialsData } from '../types/api-credentials'
import type { StateUT, CreateStateUTInput, UpdateStateUTInput } from '../types/states-uts'

export const mockSuperAdminOverviewData: SuperAdminOverviewData = {
  stats: {
    totalStatesManaged: 28,
    activeStates: 26,
    inactiveStates: 2,
  },
  ingestionData: [
    { month: 'Jan', successfulIngestions: 520, failedIngestions: 16 },
    { month: 'Feb', successfulIngestions: 680, failedIngestions: 20 },
    { month: 'Mar', successfulIngestions: 850, failedIngestions: 28 },
    { month: 'Apr', successfulIngestions: 320, failedIngestions: 24 },
    { month: 'May', successfulIngestions: 580, failedIngestions: 18 },
    { month: 'Jun', successfulIngestions: 720, failedIngestions: 22 },
    { month: 'Jul', successfulIngestions: 420, failedIngestions: 8 },
    { month: 'Aug', successfulIngestions: 850, failedIngestions: 26 },
    { month: 'Sep', successfulIngestions: 680, failedIngestions: 20 },
    { month: 'Oct', successfulIngestions: 620, failedIngestions: 18 },
    { month: 'Nov', successfulIngestions: 650, failedIngestions: 22 },
    { month: 'Dec', successfulIngestions: 820, failedIngestions: 24 },
  ],
  notifications: [
    {
      id: '1',
      message: 'Data ingestion delay detected for state Telangana (3 hours).',
      timestamp: new Date('2025-11-20T10:30:00'),
    },
    {
      id: '2',
      message: 'API authentication failure for state Haryana.',
      timestamp: new Date('2025-11-20T09:32:00'),
    },
    {
      id: '3',
      message: 'API authentication failure for state Uttar Pradesh.',
      timestamp: new Date('2025-11-20T09:24:00'),
    },
  ],
}

export const getMockSuperAdminOverviewData = (): Promise<SuperAdminOverviewData> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockSuperAdminOverviewData)
    }, 300)
  })
}

// System Rules Configuration Mock Data
let mockSystemRulesConfiguration: SystemRulesConfiguration = {
  id: '',
  coverage: '',
  continuity: '',
  quantity: '',
  regularity: '',
  isConfigured: false,
}

export const getMockSystemRulesConfiguration = (): Promise<SystemRulesConfiguration> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ ...mockSystemRulesConfiguration })
    }, 300)
  })
}

export const saveMockSystemRulesConfiguration = (
  config: Omit<SystemRulesConfiguration, 'id'>
): Promise<SystemRulesConfiguration> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const savedConfig: SystemRulesConfiguration = {
        id: '1',
        coverage: config.coverage,
        continuity: config.continuity,
        quantity: config.quantity,
        regularity: config.regularity,
        isConfigured: true,
      }
      mockSystemRulesConfiguration = savedConfig
      resolve(savedConfig)
    }, 500)
  })
}

// Ingestion Monitor Mock Data
export const mockIngestionMonitorData: IngestionMonitorData = {
  stats: {
    totalIngestions: 5234,
    successfulIngestions: 4890,
    failedIngestions: 344,
    currentWarnings: 12,
    successRate: 93.4,
    failureRate: 6.6,
  },
  chartData: [
    { month: 'Jan', successfulIngestions: 520, failedIngestions: 16 },
    { month: 'Feb', successfulIngestions: 680, failedIngestions: 20 },
    { month: 'Mar', successfulIngestions: 850, failedIngestions: 28 },
    { month: 'Apr', successfulIngestions: 320, failedIngestions: 24 },
    { month: 'May', successfulIngestions: 580, failedIngestions: 18 },
    { month: 'Jun', successfulIngestions: 720, failedIngestions: 22 },
    { month: 'Jul', successfulIngestions: 420, failedIngestions: 8 },
    { month: 'Aug', successfulIngestions: 850, failedIngestions: 26 },
    { month: 'Sep', successfulIngestions: 680, failedIngestions: 20 },
    { month: 'Oct', successfulIngestions: 620, failedIngestions: 18 },
    { month: 'Nov', successfulIngestions: 650, failedIngestions: 22 },
    { month: 'Dec', successfulIngestions: 820, failedIngestions: 24 },
  ],
  logs: [
    {
      id: '1',
      title: "District data package 'CAL-D001' ingested successfully.",
      description: 'Records processed: 12,345',
      batchId: '5f7c3d1e-8a9b-4c0d-9e2f',
      sourceSystem: 'State Data Hub',
      processingTime: '150ms',
      recordsProcessed: 12345,
      status: 'successful',
      timestamp: new Date('2025-11-20T10:30:00'),
    },
    {
      id: '2',
      title: "Partial ingestion for 'FL-D010'. 5% of records malformed.",
      description:
        "Issue: Data validation for 50 out of 1000 records due to missing 'district_id'. Review schema compliance for future submissions.",
      batchId: '5f7c3d1e-8a9b-4c0d-9e2f',
      sourceSystem: 'State Data Hub',
      processingTime: '150ms',
      status: 'warning',
      timestamp: new Date('2025-11-20T10:30:00'),
      issueDetails:
        "Data validation for 50 out of 1000 records due to missing 'district_id'. Review schema compliance for future submissions.",
    },
    {
      id: '3',
      title: "Database connection lost during 'CAL-D002' ingestion. Reconnection attempt failed.",
      description:
        'Error: DB_CONNECT_TIMEOUT Service Status: Database service appears offline. Data ingestion from this district is paused.',
      batchId: '5f7c3d1e-8a9b-4c0d-9e2f',
      sourceSystem: 'State Data Hub',
      processingTime: '150ms',
      status: 'failed',
      timestamp: new Date('2025-11-20T10:30:00'),
      errorDetails:
        'DB_CONNECT_TIMEOUT Service Status: Database service appears offline. Data ingestion from this district is paused.',
    },
  ],
}

export const getMockIngestionMonitorData = (): Promise<IngestionMonitorData> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockIngestionMonitorData)
    }, 300)
  })
}

// API Credentials Mock Data
export const mockApiCredentialsData: ApiCredentialsData = {
  credentials: [
    {
      id: '1',
      stateUtName: 'Maharashtra',
      apiKey: 'ak-r88880z1a',
      lastUsed: new Date('2025-09-08T15:00:00'),
      createdOn: new Date('2025-02-05'),
      nextRotation: new Date('2025-09-10'),
      status: 'active',
    },
    {
      id: '2',
      stateUtName: 'Himachal Pradesh',
      apiKey: 'ak-r88880z1a',
      lastUsed: new Date('2025-09-08T15:00:00'),
      createdOn: new Date('2025-02-05'),
      nextRotation: new Date('2025-09-10'),
      status: 'inactive',
    },
    {
      id: '3',
      stateUtName: 'Gujarat',
      apiKey: 'ak-r88880z1a',
      lastUsed: new Date('2025-09-08T15:00:00'),
      createdOn: new Date('2025-02-05'),
      nextRotation: new Date('2025-09-10'),
      status: 'inactive',
    },
    {
      id: '4',
      stateUtName: 'Haryana',
      apiKey: 'ak-r88880z1a',
      lastUsed: new Date('2025-09-08T15:00:00'),
      createdOn: new Date('2025-02-05'),
      nextRotation: new Date('2025-09-10'),
      status: 'active',
    },
    {
      id: '5',
      stateUtName: 'Punjab',
      apiKey: 'ak-r88880z1a',
      lastUsed: new Date('2025-09-08T15:00:00'),
      createdOn: new Date('2025-02-05'),
      nextRotation: new Date('2025-09-10'),
      status: 'active',
    },
    {
      id: '6',
      stateUtName: 'Uttar Pradesh',
      apiKey: 'ak-r88880z1a',
      lastUsed: new Date('2025-09-08T15:00:00'),
      createdOn: new Date('2025-02-05'),
      nextRotation: new Date('2025-09-10'),
      status: 'active',
    },
  ],
}

export const getMockApiCredentialsData = (): Promise<ApiCredentialsData> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockApiCredentialsData)
    }, 300)
  })
}

export const generateApiKey = (_stateId: string): Promise<string> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const randomPart = Math.random().toString(36).substring(2, 10)
      resolve(`ak-${randomPart}`)
    }, 300)
  })
}

export const sendApiKey = (_stateId: string): Promise<{ success: boolean }> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ success: true })
    }, 300)
  })
}

// States/UTs Mock Data
const mockStatesUTsData: StateUT[] = [
  {
    id: '1',
    name: 'Andhra Pradesh',
    code: '28',
    status: 'active',
    lastSyncDate: new Date('2025-09-08T15:00:00'),
    totalDistricts: 29,
    admin: {
      firstName: 'Rajesh',
      lastName: 'Kumar',
      email: 'rajesh.kumar@jalsoochak.com',
      phone: '9876543210',
      secondaryEmail: '',
      contactNumber: '',
    },
  },
  {
    id: '2',
    name: 'Arunachal Pradesh',
    code: '12',
    status: 'active',
    lastSyncDate: new Date('2025-11-02T09:30:00'),
    totalDistricts: 28,
    admin: {
      firstName: 'Tenzin',
      lastName: 'Dorje',
      email: 'tenzin.dorje@jalsoochak.com',
      phone: '9876543211',
    },
  },
  {
    id: '3',
    name: 'Assam',
    code: '18',
    status: 'active',
    lastSyncDate: new Date('2025-08-22T13:30:00'),
    totalDistricts: 35,
    admin: {
      firstName: 'Priya',
      lastName: 'Sharma',
      email: 'priya.sharma@jalsoochak.com',
      phone: '9876543212',
    },
  },
  {
    id: '4',
    name: 'Bihar',
    code: '10',
    status: 'active',
    lastSyncDate: new Date('2025-02-16T18:00:00'),
    totalDistricts: 38,
    admin: {
      firstName: 'Amit',
      lastName: 'Singh',
      email: 'amit.singh@jalsoochak.com',
      phone: '9876543213',
    },
  },
  {
    id: '5',
    name: 'Chhattisgarh',
    code: '22',
    status: 'active',
    lastSyncDate: new Date('2025-04-29T11:00:00'),
    totalDistricts: 33,
    admin: {
      firstName: 'Sunita',
      lastName: 'Verma',
      email: 'sunita.verma@jalsoochak.com',
      phone: '9876543214',
    },
  },
  {
    id: '6',
    name: 'Goa',
    code: '30',
    status: 'active',
    lastSyncDate: new Date('2025-12-04T16:30:00'),
    totalDistricts: 2,
    admin: {
      firstName: 'Miguel',
      lastName: 'Fernandes',
      email: 'miguel.fernandes@jalsoochak.com',
      phone: '9876543215',
    },
  },
  {
    id: '7',
    name: 'Gujarat',
    code: '24',
    status: 'inactive',
    lastSyncDate: new Date('2025-07-19T07:00:00'),
    totalDistricts: 33,
    admin: {
      firstName: 'Kiran',
      lastName: 'Patel',
      email: 'kiran.patel@jalsoochak.com',
      phone: '9876543216',
    },
  },
  {
    id: '8',
    name: 'Haryana',
    code: '06',
    status: 'active',
    lastSyncDate: new Date('2025-03-06T14:00:00'),
    totalDistricts: 23,
    admin: {
      firstName: 'Deepak',
      lastName: 'Yadav',
      email: 'deepak.yadav@jalsoochak.com',
      phone: '9876543217',
    },
  },
  {
    id: '9',
    name: 'Himachal Pradesh',
    code: '02',
    status: 'inactive',
    lastSyncDate: new Date('2025-05-14T12:30:00'),
    totalDistricts: 12,
    admin: {
      firstName: 'Anita',
      lastName: 'Thakur',
      email: 'anita.thakur@jalsoochak.com',
      phone: '9876543218',
    },
  },
  {
    id: '10',
    name: 'Jharkhand',
    code: '20',
    status: 'active',
    lastSyncDate: new Date('2025-09-11T20:30:00'),
    totalDistricts: 24,
    admin: {
      firstName: 'Ravi',
      lastName: 'Oraon',
      email: 'ravi.oraon@jalsoochak.com',
      phone: '9876543219',
    },
  },
]

export const getMockStatesUTsData = (): Promise<StateUT[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve([...mockStatesUTsData])
    }, 300)
  })
}

export const getStateUTById = (id: string): Promise<StateUT | null> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const state = mockStatesUTsData.find((s) => s.id === id)
      resolve(state ? { ...state } : null)
    }, 300)
  })
}

export const createStateUT = (data: CreateStateUTInput): Promise<StateUT> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const newState: StateUT = {
        id: String(mockStatesUTsData.length + 1),
        name: data.name,
        code: data.code,
        status: 'active',
        lastSyncDate: new Date(),
        totalDistricts: 0,
        admin: {
          firstName: data.admin.firstName,
          lastName: data.admin.lastName,
          email: data.admin.email,
          phone: data.admin.phone,
          secondaryEmail: data.admin.secondaryEmail,
          contactNumber: data.admin.contactNumber,
        },
      }
      mockStatesUTsData.push(newState)
      resolve({ ...newState })
    }, 500)
  })
}

export const updateStateUT = (id: string, data: UpdateStateUTInput): Promise<StateUT | null> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const index = mockStatesUTsData.findIndex((s) => s.id === id)
      if (index === -1) {
        resolve(null)
        return
      }
      mockStatesUTsData[index] = {
        ...mockStatesUTsData[index],
        admin: {
          ...mockStatesUTsData[index].admin,
          firstName: data.admin.firstName,
          lastName: data.admin.lastName,
          phone: data.admin.phone,
          secondaryEmail: data.admin.secondaryEmail,
          contactNumber: data.admin.contactNumber,
        },
      }
      resolve({ ...mockStatesUTsData[index] })
    }, 500)
  })
}

export const updateStateUTStatus = (
  id: string,
  status: 'active' | 'inactive'
): Promise<StateUT | null> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const index = mockStatesUTsData.findIndex((s) => s.id === id)
      if (index === -1) {
        resolve(null)
        return
      }
      mockStatesUTsData[index] = {
        ...mockStatesUTsData[index],
        status,
      }
      resolve({ ...mockStatesUTsData[index] })
    }, 300)
  })
}

export const getAssignedStateNames = (): string[] => {
  return mockStatesUTsData.map((s) => s.name)
}

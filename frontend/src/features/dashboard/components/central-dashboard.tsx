import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, Flex, Text, Heading, Grid, Icon, Image, Select, Avatar } from '@chakra-ui/react'
import { useDashboardData } from '../hooks/use-dashboard-data'
import { KPICard } from './kpi-card'
import {
  IndiaMapChart,
  DemandSupplyChart,
  AllStatesPerformanceChart,
  SupplySubmissionRateChart,
  PumpOperatorsChart,
  ImageSubmissionStatusChart,
  IssueTypeBreakdownChart,
  WaterSupplyOutagesChart,
} from './charts'
import {
  AllBlocksTable,
  AllDistrictsTable,
  AllStatesTable,
  PumpOperatorsPerformanceTable,
  PhotoEvidenceComplianceTable,
  AllGramPanchayatsTable,
} from './tables'
import { DateRangePicker, LoadingSpinner, SearchableSelect } from '@/shared/components/common'
import { MdOutlineWaterDrop, MdArrowUpward, MdArrowDownward } from 'react-icons/md'
import { AiOutlineHome, AiOutlineInfoCircle } from 'react-icons/ai'
import waterTapIcon from '@/assets/media/water-tap_1822589 1.svg'
import type { DateRange, SearchableSelectOption } from '@/shared/components/common'
import type { EntityPerformance } from '../types'
import { SearchLayout, FilterLayout } from '@/shared/components/layout'
import {
  mockFilterStates,
  mockFilterDistricts,
  mockFilterBlocks,
  mockFilterGramPanchayats,
  mockFilterVillages,
  mockFilterSchemes,
  mockDistrictPerformanceByState,
  mockBlockPerformanceByDistrict,
  mockGramPanchayatPerformanceByBlock,
  mockVillagePerformanceByGramPanchayat,
} from '../services/mock/dashboard-mock'

const storageKey = 'central-dashboard-filters'

type StoredFilters = {
  selectedState?: string
  selectedDistrict?: string
  selectedBlock?: string
  selectedGramPanchayat?: string
  selectedVillage?: string
  selectedDuration?: DateRange
  selectedScheme?: string
  selectedDepartmentState?: string
  selectedDepartmentZone?: string
  selectedDepartmentCircle?: string
  selectedDepartmentDivision?: string
  selectedDepartmentSubdivision?: string
  selectedDepartmentVillage?: string
  filterTabIndex?: number
}

const getStoredFilters = (): StoredFilters => {
  if (typeof window === 'undefined') return {}
  try {
    const saved = window.localStorage.getItem(storageKey)
    if (!saved) return {}
    const parsed = JSON.parse(saved) as StoredFilters
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    try {
      window.localStorage.removeItem(storageKey)
    } catch {
      // Ignore storage errors (quota/private mode)
    }
    return {}
  }
}

export function CentralDashboard() {
  const navigate = useNavigate()
  const { data, isLoading, error } = useDashboardData('central')
  const [storedFilters] = useState(() => getStoredFilters())
  const initialDuration =
    storedFilters.selectedDuration &&
    typeof storedFilters.selectedDuration === 'object' &&
    'startDate' in storedFilters.selectedDuration &&
    'endDate' in storedFilters.selectedDuration
      ? storedFilters.selectedDuration
      : null
  const [selectedState, setSelectedState] = useState(storedFilters.selectedState ?? '')
  const [selectedDistrict, setSelectedDistrict] = useState(storedFilters.selectedDistrict ?? '')
  const [selectedBlock, setSelectedBlock] = useState(storedFilters.selectedBlock ?? '')
  const [selectedGramPanchayat, setSelectedGramPanchayat] = useState(
    storedFilters.selectedGramPanchayat ?? ''
  )
  const [selectedVillage, setSelectedVillage] = useState(storedFilters.selectedVillage ?? '')
  const [selectedDuration, setSelectedDuration] = useState<DateRange | null>(initialDuration)
  const [selectedScheme, setSelectedScheme] = useState(storedFilters.selectedScheme ?? '')
  const [selectedDepartmentState, setSelectedDepartmentState] = useState(
    storedFilters.selectedDepartmentState ?? ''
  )
  const [selectedDepartmentZone, setSelectedDepartmentZone] = useState(
    storedFilters.selectedDepartmentZone ?? ''
  )
  const [selectedDepartmentCircle, setSelectedDepartmentCircle] = useState(
    storedFilters.selectedDepartmentCircle ?? ''
  )
  const [selectedDepartmentDivision, setSelectedDepartmentDivision] = useState(
    storedFilters.selectedDepartmentDivision ?? ''
  )
  const [selectedDepartmentSubdivision, setSelectedDepartmentSubdivision] = useState(
    storedFilters.selectedDepartmentSubdivision ?? ''
  )
  const [selectedDepartmentVillage, setSelectedDepartmentVillage] = useState(
    storedFilters.selectedDepartmentVillage ?? ''
  )
  const [performanceState, setPerformanceState] = useState('')
  const [filterTabIndex, setFilterTabIndex] = useState(
    typeof storedFilters.filterTabIndex === 'number' ? storedFilters.filterTabIndex : 0
  )
  const isStateSelected = Boolean(selectedState)
  const isDistrictSelected = Boolean(selectedDistrict)
  const isBlockSelected = Boolean(selectedBlock)
  const isGramPanchayatSelected = Boolean(selectedGramPanchayat)
  const isDepartmentStateSelected = Boolean(selectedDepartmentState)
  const emptyOptions: SearchableSelectOption[] = []
  const isAdvancedEnabled = Boolean(selectedState && selectedDistrict)
  const districtTableData =
    mockDistrictPerformanceByState[selectedState] ?? ([] as EntityPerformance[])
  const blockTableData =
    mockBlockPerformanceByDistrict[selectedDistrict] ?? ([] as EntityPerformance[])
  const gramPanchayatTableData =
    mockGramPanchayatPerformanceByBlock[selectedBlock] ?? ([] as EntityPerformance[])
  const villageTableData =
    mockVillagePerformanceByGramPanchayat[selectedGramPanchayat] ?? ([] as EntityPerformance[])
  const supplySubmissionRateData = isGramPanchayatSelected
    ? villageTableData
    : isBlockSelected
      ? gramPanchayatTableData
      : isDistrictSelected
        ? blockTableData
        : isStateSelected
          ? districtTableData
          : (data?.mapData ?? ([] as EntityPerformance[]))
  const supplySubmissionRateLabel = isGramPanchayatSelected
    ? 'Villages'
    : isBlockSelected
      ? 'Gram Panchayats'
      : isDistrictSelected
        ? 'Blocks'
        : isStateSelected
          ? 'Districts'
          : 'States/UTs'
  const districtOptions = selectedState ? (mockFilterDistricts[selectedState] ?? []) : emptyOptions
  const blockOptions = selectedDistrict ? (mockFilterBlocks[selectedDistrict] ?? []) : emptyOptions
  const gramPanchayatOptions = selectedBlock
    ? (mockFilterGramPanchayats[selectedBlock] ?? [])
    : emptyOptions
  const villageOptions = selectedGramPanchayat
    ? (mockFilterVillages[selectedGramPanchayat] ?? [])
    : emptyOptions
  const handleStateChange = (value: string) => {
    setSelectedState(value)
    setSelectedDistrict('')
    setSelectedBlock('')
    setSelectedGramPanchayat('')
    setSelectedVillage('')
  }
  const handleDistrictChange = (value: string) => {
    setSelectedDistrict(value)
    setSelectedBlock('')
    setSelectedGramPanchayat('')
    setSelectedVillage('')
  }
  const handleBlockChange = (value: string) => {
    setSelectedBlock(value)
    setSelectedGramPanchayat('')
    setSelectedVillage('')
  }
  const handleGramPanchayatChange = (value: string) => {
    setSelectedGramPanchayat(value)
    setSelectedVillage('')
  }
  const handleDepartmentStateChange = (value: string) => {
    setSelectedDepartmentState(value)
    setSelectedDepartmentZone('')
    setSelectedDepartmentCircle('')
    setSelectedDepartmentDivision('')
    setSelectedDepartmentSubdivision('')
    setSelectedDepartmentVillage('')
  }
  const handleClearFilters = () => {
    setSelectedState('')
    setSelectedDistrict('')
    setSelectedBlock('')
    setSelectedGramPanchayat('')
    setSelectedVillage('')
    setSelectedDuration(null)
    setSelectedScheme('')
    setSelectedDepartmentState('')
    setSelectedDepartmentZone('')
    setSelectedDepartmentCircle('')
    setSelectedDepartmentDivision('')
    setSelectedDepartmentSubdivision('')
    setSelectedDepartmentVillage('')
  }

  useEffect(() => {
    const payload = {
      selectedState,
      selectedDistrict,
      selectedBlock,
      selectedGramPanchayat,
      selectedVillage,
      selectedDuration,
      selectedScheme,
      selectedDepartmentState,
      selectedDepartmentZone,
      selectedDepartmentCircle,
      selectedDepartmentDivision,
      selectedDepartmentSubdivision,
      selectedDepartmentVillage,
      filterTabIndex,
    }
    try {
      localStorage.setItem(storageKey, JSON.stringify(payload))
    } catch {
      // Ignore storage errors (quota/private mode)
    }
  }, [
    filterTabIndex,
    selectedBlock,
    selectedDistrict,
    selectedDuration,
    selectedDepartmentCircle,
    selectedDepartmentDivision,
    selectedDepartmentState,
    selectedDepartmentSubdivision,
    selectedDepartmentVillage,
    selectedDepartmentZone,
    selectedGramPanchayat,
    selectedScheme,
    selectedState,
    selectedVillage,
  ])

  const handleStateClick = (stateId: string, _stateName: string) => {
    navigate(`/states/${stateId}`)
  }

  const handleStateHover = (_stateId: string, _stateName: string, _metrics: unknown) => {
    // Hover tooltip is handled by ECharts
  }

  if (isLoading) {
    return (
      <Flex h="100vh" align="center" justify="center">
        <LoadingSpinner />
      </Flex>
    )
  }

  if (error) {
    return (
      <Flex h="100vh" align="center" justify="center">
        <Box textAlign="center">
          <Heading fontSize="2xl" fontWeight="bold" color="red.600">
            Error loading dashboard
          </Heading>
          <Text mt={2} color="gray.600">
            {error instanceof Error ? error.message : 'Unknown error'}
          </Text>
        </Box>
      </Flex>
    )
  }

  if (!data) return null

  if (
    !data.kpis ||
    !data.mapData ||
    !data.demandSupply ||
    !data.imageSubmissionStatus ||
    !data.pumpOperators ||
    !data.photoEvidenceCompliance ||
    !data.waterSupplyOutages ||
    !data.topPerformers ||
    !data.worstPerformers ||
    !data.regularityData ||
    !data.continuityData
  ) {
    return (
      <Flex h="100vh" align="center" justify="center">
        <Box textAlign="center">
          <Heading fontSize="2xl" fontWeight="bold" color="red.600">
            Invalid data structure
          </Heading>
          <Text mt={2} color="gray.600">
            Dashboard data is incomplete
          </Text>
        </Box>
      </Flex>
    )
  }

  const waterSupplyOutagesData = isGramPanchayatSelected
    ? (mockVillagePerformanceByGramPanchayat[selectedGramPanchayat] ?? []).map((village, index) => {
        if (data.waterSupplyOutages.length === 0) {
          return {
            district: village.name,
            electricityFailure: 0,
            pipelineLeak: 0,
            pumpFailure: 0,
            valveIssue: 0,
            sourceDrying: 0,
          }
        }
        const source = data.waterSupplyOutages[index % data.waterSupplyOutages.length]
        return { ...source, district: village.name }
      })
    : isBlockSelected
      ? (mockGramPanchayatPerformanceByBlock[selectedBlock] ?? []).map((gramPanchayat, index) => {
          if (data.waterSupplyOutages.length === 0) {
            return {
              district: gramPanchayat.name,
              electricityFailure: 0,
              pipelineLeak: 0,
              pumpFailure: 0,
              valveIssue: 0,
              sourceDrying: 0,
            }
          }
          const source = data.waterSupplyOutages[index % data.waterSupplyOutages.length]
          return { ...source, district: gramPanchayat.name }
        })
      : isDistrictSelected
        ? (mockBlockPerformanceByDistrict[selectedDistrict] ?? []).map((block, index) => {
            if (data.waterSupplyOutages.length === 0) {
              return {
                district: block.name,
                electricityFailure: 0,
                pipelineLeak: 0,
                pumpFailure: 0,
                valveIssue: 0,
                sourceDrying: 0,
              }
            }
            const source = data.waterSupplyOutages[index % data.waterSupplyOutages.length]
            return { ...source, district: block.name }
          })
        : data.waterSupplyOutages

  const coreMetrics = [
    {
      label: 'Coverage',
      value: '78.4%',
      trend: { direction: 'up', text: '+0.5% vs last month' },
    },
    {
      label: 'Continuity',
      value: '94',
      trend: { direction: 'down', text: '-1 vs last month' },
    },
    {
      label: 'Quantity',
      value: '78.4%',
      trend: { direction: 'up', text: '+2 LPCD vs last month' },
    },
    {
      label: 'Regularity',
      value: '78.4%',
      trend: { direction: 'down', text: '-3% vs last month' },
    },
  ] as const
  const villagePumpOperatorDetails = {
    name: 'Ajay Yadav',
    scheme: 'Rural Water Supply 001',
    stationLocation: 'Central Pumping Station',
    lastSubmission: '11-02-24, 1:00pm',
    reportingRate: '85%',
    missingSubmissionCount: '3',
    inactiveDays: '2',
  }

  const pumpOperatorsTotal = data.pumpOperators.reduce((total, item) => total + item.value, 0)
  const leadingPumpOperators = data.leadingPumpOperators ?? []
  const bottomPumpOperators = data.bottomPumpOperators ?? []
  const operatorsPerformanceTable = [...leadingPumpOperators, ...bottomPumpOperators]
  const villagePhotoEvidenceRows = data.photoEvidenceCompliance.map((row) => ({
    ...row,
    name: villagePumpOperatorDetails.name,
  }))

  return (
    <Box>
      <SearchLayout />
      <FilterLayout
        onClear={handleClearFilters}
        activeTab={filterTabIndex}
        onTabChange={setFilterTabIndex}
      >
        {filterTabIndex === 0 ? (
          <>
            <SearchableSelect
              options={mockFilterStates}
              value={selectedState}
              onChange={handleStateChange}
              placeholder="States/UTs"
              required
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor="neutral.400"
              borderColor="neutral.400"
              isFilter={true}
            />
            <SearchableSelect
              options={districtOptions}
              value={selectedDistrict}
              onChange={handleDistrictChange}
              placeholder="District"
              required
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor="neutral.400"
              borderColor="neutral.400"
              isFilter={true}
            />
            <SearchableSelect
              options={blockOptions}
              value={selectedBlock}
              onChange={handleBlockChange}
              placeholder="Block"
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor={isAdvancedEnabled ? 'neutral.400' : 'neutral.300'}
              borderColor={isAdvancedEnabled ? 'neutral.400' : 'neutral.300'}
              disabled={!isAdvancedEnabled}
              isFilter={true}
            />
            <SearchableSelect
              options={gramPanchayatOptions}
              value={selectedGramPanchayat}
              onChange={handleGramPanchayatChange}
              placeholder="Gram Panchayat"
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor={isAdvancedEnabled ? 'neutral.400' : 'neutral.300'}
              borderColor={isAdvancedEnabled ? 'neutral.400' : 'neutral.300'}
              disabled={!isAdvancedEnabled}
              isFilter={true}
            />
            <SearchableSelect
              options={villageOptions}
              value={selectedVillage}
              onChange={setSelectedVillage}
              placeholder="Village"
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor={isAdvancedEnabled ? 'neutral.400' : 'neutral.300'}
              borderColor={isAdvancedEnabled ? 'neutral.400' : 'neutral.300'}
              disabled={!isAdvancedEnabled}
              isFilter={true}
            />

            <SearchableSelect
              options={mockFilterSchemes}
              value={selectedScheme}
              onChange={setSelectedScheme}
              placeholder="Scheme"
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor={isAdvancedEnabled ? 'neutral.400' : 'neutral.300'}
              borderColor={isAdvancedEnabled ? 'neutral.400' : 'neutral.300'}
              disabled={!isAdvancedEnabled}
              isFilter={true}
            />

            <DateRangePicker
              value={selectedDuration}
              onChange={setSelectedDuration}
              placeholder="Duration"
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor={isAdvancedEnabled ? 'neutral.400' : 'neutral.300'}
              borderColor={isAdvancedEnabled ? 'neutral.400' : 'neutral.300'}
              disabled={!isAdvancedEnabled}
              isFilter={true}
            />
          </>
        ) : (
          <>
            <SearchableSelect
              options={mockFilterStates}
              value={selectedDepartmentState}
              onChange={handleDepartmentStateChange}
              placeholder="States/UTs"
              required
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor="neutral.400"
              borderColor="neutral.400"
              isFilter={true}
            />
            <SearchableSelect
              options={emptyOptions}
              value={selectedDepartmentZone}
              onChange={setSelectedDepartmentZone}
              placeholder="Zone"
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor={isDepartmentStateSelected ? 'neutral.400' : 'neutral.300'}
              borderColor={isDepartmentStateSelected ? 'neutral.400' : 'neutral.300'}
              disabled={!isDepartmentStateSelected}
              isFilter={true}
            />
            <SearchableSelect
              options={emptyOptions}
              value={selectedDepartmentCircle}
              onChange={setSelectedDepartmentCircle}
              placeholder="Circle"
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor={isDepartmentStateSelected ? 'neutral.400' : 'neutral.300'}
              borderColor={isDepartmentStateSelected ? 'neutral.400' : 'neutral.300'}
              disabled={!isDepartmentStateSelected}
              isFilter={true}
            />
            <SearchableSelect
              options={emptyOptions}
              value={selectedDepartmentDivision}
              onChange={setSelectedDepartmentDivision}
              placeholder="Division"
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor={isDepartmentStateSelected ? 'neutral.400' : 'neutral.300'}
              borderColor={isDepartmentStateSelected ? 'neutral.400' : 'neutral.300'}
              disabled={!isDepartmentStateSelected}
              isFilter={true}
            />
            <SearchableSelect
              options={emptyOptions}
              value={selectedDepartmentSubdivision}
              onChange={setSelectedDepartmentSubdivision}
              placeholder="Subdivision"
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor={isDepartmentStateSelected ? 'neutral.400' : 'neutral.300'}
              borderColor={isDepartmentStateSelected ? 'neutral.400' : 'neutral.300'}
              disabled={!isDepartmentStateSelected}
              isFilter={true}
            />
            <SearchableSelect
              options={emptyOptions}
              value={selectedDepartmentVillage}
              onChange={setSelectedDepartmentVillage}
              placeholder="Village"
              width={{
                base: '100%',
                sm: 'calc(50% - 12px)',
                md: 'calc(33.333% - 12px)',
                lg: 'calc(25% - 12px)',
                xl: '162px',
              }}
              height="32px"
              borderRadius="4px"
              fontSize="sm"
              textColor={isDepartmentStateSelected ? 'neutral.400' : 'neutral.300'}
              borderColor={isDepartmentStateSelected ? 'neutral.400' : 'neutral.300'}
              disabled={!isDepartmentStateSelected}
              isFilter={true}
            />
          </>
        )}
      </FilterLayout>

      {/* KPI Cards */}
      <Grid templateColumns={{ base: '1fr', lg: 'repeat(3, 1fr)' }} gap={4} mb={6}>
        <KPICard
          title="Number of schemes"
          value={data.kpis.totalSchemes}
          icon={
            <Flex
              w="48px"
              h="48px"
              borderRadius="100px"
              bg="primary.25"
              align="center"
              justify="center"
            >
              <Icon as={MdOutlineWaterDrop} boxSize="28px" color="primary.500" />
            </Flex>
          }
        />
        <KPICard
          title="Total Number of Rural Households"
          value={data.kpis.totalRuralHouseholds}
          icon={
            <Flex
              w="48px"
              h="48px"
              borderRadius="100px"
              bg="#FFFBD7"
              align="center"
              justify="center"
            >
              <Icon as={AiOutlineHome} boxSize="28px" color="#CA8A04" />
            </Flex>
          }
        />
        <KPICard
          title="Functional Household Tap Connection"
          value={data.kpis.functionalTapConnections}
          icon={
            <Flex
              w="48px"
              h="48px"
              borderRadius="100px"
              bg="#E1FFEA"
              align="center"
              justify="center"
            >
              <Image src={waterTapIcon} alt="" boxSize="24px" />
            </Flex>
          }
        />
      </Grid>

      {/* Map and Core Metrics */}
      <Grid templateColumns={{ base: '1fr', lg: 'repeat(2, minmax(0, 1fr))' }} gap={6} mb={6}>
        <Box
          bg="white"
          borderWidth="0.5px"
          borderRadius="12px"
          borderColor="#E4E4E7"
          pt="24px"
          pb="10px"
          pl="16px"
          pr="16px"
          w="full"
          h="731px"
        >
          <IndiaMapChart
            data={data.mapData}
            onStateClick={handleStateClick}
            onStateHover={handleStateHover}
            height="100%"
          />
        </Box>
        {selectedVillage ? (
          <Flex direction="column" gap="28px" w="full">
            <Box
              bg="white"
              borderWidth="0.5px"
              borderRadius="12px"
              borderColor="#E4E4E7"
              pt="24px"
              pb="24px"
              pl="16px"
              pr="16px"
              w="full"
              h="330px"
            >
              <Text textStyle="bodyText3" fontWeight="400" mb={4}>
                Core Metrics
              </Text>
              <Box>
                <Grid templateColumns="repeat(2, 1fr)" gap="12px">
                  {coreMetrics.map((metric) => {
                    const isPositive = metric.trend.direction === 'up'
                    const TrendIcon = isPositive ? MdArrowUpward : MdArrowDownward
                    const trendColor = isPositive ? '#079455' : '#D92D20'

                    return (
                      <Box
                        key={metric.label}
                        px="16px"
                        py="12px"
                        h="112px"
                        bg="#FAFAFA"
                        borderRadius="8px"
                      >
                        <Flex direction="column" align="center" gap="4px" h="100%" w="full">
                          <Flex align="center" justify="center" w="full" position="relative">
                            <Text textStyle="bodyText4" fontWeight="400" color="neutral.600">
                              {metric.label}
                            </Text>
                            <Icon
                              as={AiOutlineInfoCircle}
                              boxSize="16px"
                              color="neutral.400"
                              position="absolute"
                              right="0"
                            />
                          </Flex>
                          <Text textStyle="bodyText2" fontWeight="400" color="neutral.950">
                            {metric.value}
                          </Text>
                          <Flex align="center" gap={1}>
                            <Icon as={TrendIcon} boxSize="16px" color={trendColor} />
                            <Text textStyle="bodyText4" fontWeight="400" color={trendColor}>
                              {metric.trend.text}
                            </Text>
                          </Flex>
                        </Flex>
                      </Box>
                    )
                  })}
                </Grid>
              </Box>
            </Box>
            <Box
              bg="white"
              borderWidth="0.5px"
              borderRadius="12px"
              borderColor="#E4E4E7"
              pt="24px"
              pb="24px"
              pl="16px"
              pr="16px"
              w="full"
              h="373px"
            >
              <Text textStyle="bodyText3" fontWeight="400" mb={4}>
                Pump Operator Details
              </Text>
              <Flex align="center" gap={3} mb={6}>
                <Avatar name={villagePumpOperatorDetails.name} boxSize="44px" />
                <Text textStyle="bodyText4" fontSize="14px" fontWeight="500" color="neutral.950">
                  {villagePumpOperatorDetails.name}
                </Text>
              </Flex>
              <Grid templateColumns="1fr auto" columnGap="24px" rowGap="12px" alignItems="center">
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.600">
                  Scheme name/ Scheme ID
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.950" textAlign="right">
                  {villagePumpOperatorDetails.scheme}
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.600">
                  Station location
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.950" textAlign="right">
                  {villagePumpOperatorDetails.stationLocation}
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.600">
                  Last submission
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.950" textAlign="right">
                  {villagePumpOperatorDetails.lastSubmission}
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.600">
                  Reporting rate
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.950" textAlign="right">
                  {villagePumpOperatorDetails.reportingRate}
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.600">
                  Missing submission count
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.950" textAlign="right">
                  {villagePumpOperatorDetails.missingSubmissionCount}
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.600">
                  Inactive days
                </Text>
                <Text textStyle="bodyText4" fontWeight="400" color="neutral.950" textAlign="right">
                  {villagePumpOperatorDetails.inactiveDays}
                </Text>
              </Grid>
            </Box>
          </Flex>
        ) : (
          <Box
            bg="white"
            borderWidth="0.5px"
            borderRadius="12px"
            borderColor="#E4E4E7"
            pt="24px"
            pb="24px"
            pl="16px"
            pr="16px"
            w="full"
            h="731px"
          >
            <Text textStyle="bodyText3" fontWeight="400" mb={4}>
              Core Metrics
            </Text>
            <Flex direction="column" gap="16px">
              {coreMetrics.map((metric) => {
                const isPositive = metric.trend.direction === 'up'
                const TrendIcon = isPositive ? MdArrowUpward : MdArrowDownward
                const trendColor = isPositive ? '#079455' : '#D92D20'

                return (
                  <Box key={metric.label} bg="#FAFAFA" borderRadius="8px" px="16px" py="24px">
                    <Flex direction="column" align="center" gap="4px" h="92px" w="full">
                      <Flex align="center" justify="center" w="full" position="relative">
                        <Text textStyle="bodyText4" fontWeight="400" color="neutral.600">
                          {metric.label}
                        </Text>
                        <Icon
                          as={AiOutlineInfoCircle}
                          boxSize="16px"
                          color="neutral.400"
                          position="absolute"
                          right="0"
                        />
                      </Flex>
                      <Text textStyle="bodyText2" fontWeight="400" color="neutral.900">
                        {metric.value}
                      </Text>
                      <Flex align="center" gap={1}>
                        <Icon as={TrendIcon} boxSize="16px" color={trendColor} />
                        <Text textStyle="bodyText4" fontWeight="400" color={trendColor}>
                          {metric.trend.text}
                        </Text>
                      </Flex>
                    </Flex>
                  </Box>
                )
              })}
            </Flex>
          </Box>
        )}
      </Grid>

      {selectedVillage ? (
        <>
          <Grid templateColumns={{ base: '1fr', lg: 'repeat(2, 1fr)' }} gap={6} mb={6}>
            <Box bg="white" borderWidth="1px" borderRadius="lg" px={4} py={6} h="536px">
              <PhotoEvidenceComplianceTable
                data={villagePhotoEvidenceRows}
                showVillageColumn={false}
              />
            </Box>
            <Box bg="white" borderWidth="1px" borderRadius="lg" p={4} h="536px">
              <Text textStyle="bodyText3" fontWeight="400" mb={2}>
                Demand vs Supply
              </Text>
              <DemandSupplyChart data={data.demandSupply} height="418px" />
            </Box>
          </Grid>
          <Grid templateColumns={{ base: '1fr', lg: 'repeat(2, 1fr)' }} gap={6} mb={6}>
            <Box
              bg="white"
              borderWidth="0.5px"
              borderRadius="12px"
              borderColor="#E4E4E7"
              pt="24px"
              pb="24px"
              pl="16px"
              pr="16px"
              h="523px"
              w="full"
              minW={0}
            >
              <Text textStyle="bodyText3" fontWeight="400" mb="0px">
                Image Submission Status
              </Text>
              <ImageSubmissionStatusChart data={data.imageSubmissionStatus} height="406px" />
            </Box>
            <Box
              bg="white"
              borderWidth="0.5px"
              borderRadius="12px"
              borderColor="#E4E4E7"
              pt="24px"
              pb="24px"
              pl="16px"
              pr="16px"
              h="523px"
              w="full"
              minW={0}
            >
              <Text textStyle="bodyText3" fontWeight="400" mb={2}>
                Issue Type Breakdown
              </Text>
              <IssueTypeBreakdownChart data={waterSupplyOutagesData} height="400px" />
            </Box>
          </Grid>
        </>
      ) : /* Submission + Outages Charts */
      isStateSelected ? (
        <Grid templateColumns={{ base: '1fr', lg: 'repeat(2, minmax(0, 1fr))' }} gap={6} mb={6}>
          <Box
            bg="white"
            borderWidth="0.5px"
            borderRadius="12px"
            borderColor="#E4E4E7"
            pt="24px"
            pb="24px"
            pl="16px"
            pr="16px"
            h="523px"
            w="full"
            minW={0}
          >
            <Text textStyle="bodyText3" fontWeight="400" mb="0px">
              Image Submission Status
            </Text>
            <ImageSubmissionStatusChart data={data.imageSubmissionStatus} height="406px" />
          </Box>
          <Box
            bg="white"
            borderWidth="0.5px"
            borderRadius="12px"
            borderColor="#E4E4E7"
            pt="24px"
            pb="24px"
            pl="16px"
            pr="16px"
            h="523px"
            w="full"
            minW={0}
          >
            <Text textStyle="bodyText3" fontWeight="400" mb={2}>
              Water Supply Outages
            </Text>
            <WaterSupplyOutagesChart
              data={waterSupplyOutagesData}
              height="400px"
              xAxisLabel={
                isGramPanchayatSelected
                  ? 'Villages'
                  : isBlockSelected
                    ? 'Gram Panchayats'
                    : isDistrictSelected
                      ? 'Blocks'
                      : 'Districts'
              }
            />
          </Box>
        </Grid>
      ) : null}

      {/* Performance + Demand vs Supply Charts */}
      {!selectedVillage ? (
        <Grid templateColumns={{ base: '1fr', lg: 'repeat(2, minmax(0, 1fr))' }} gap={6} mb={6}>
          <Box
            bg="white"
            borderWidth="1px"
            borderRadius="lg"
            p={4}
            h="536px"
            w="full"
            minW="250px"
            justifySelf={{ base: 'center', md: 'stretch' }}
          >
            <Flex align="center" justify="space-between">
              <Text textStyle="bodyText3" fontWeight="400">
                {selectedGramPanchayat
                  ? 'All Villages Performance'
                  : isBlockSelected
                    ? 'All Gram Panchayats Performance'
                    : isDistrictSelected
                      ? 'All Blocks Performance'
                      : isStateSelected
                        ? 'All Districts Performance'
                        : 'All States/UTs Performance'}
              </Text>
              {!isStateSelected &&
              !isDistrictSelected &&
              !isBlockSelected &&
              !selectedGramPanchayat ? (
                <Select
                  h="32px"
                  maxW="120px"
                  fontSize="14px"
                  fontWeight="600"
                  borderRadius="4px"
                  borderColor="neutral.400"
                  borderWidth="1px"
                  bg="white"
                  color="neutral.400"
                  placeholder="Select"
                  appearance="none"
                  value={performanceState}
                  onChange={(event) => setPerformanceState(event.target.value)}
                  _focus={{
                    borderColor: 'primary.500',
                    boxShadow: 'none',
                  }}
                >
                  <option value="Punjab">Punjab</option>
                </Select>
              ) : null}
            </Flex>
            <AllStatesPerformanceChart
              data={
                selectedGramPanchayat
                  ? villageTableData
                  : isBlockSelected
                    ? gramPanchayatTableData
                    : isDistrictSelected
                      ? blockTableData
                      : isStateSelected
                        ? districtTableData
                        : performanceState
                          ? data.mapData
                              .filter((state) => state.name === performanceState)
                              .slice(0, 1)
                          : data.mapData
              }
              height="440px"
              entityLabel={
                selectedGramPanchayat
                  ? 'Villages'
                  : isBlockSelected
                    ? 'Gram Panchayats'
                    : isDistrictSelected
                      ? 'Blocks'
                      : isStateSelected
                        ? 'Districts'
                        : 'States/UTs'
              }
            />
          </Box>
          <Box bg="white" borderWidth="1px" borderRadius="lg" p={4} h="536px" minW={0}>
            <Text textStyle="bodyText3" fontWeight="400" mb={2}>
              Demand vs Supply
            </Text>
            <DemandSupplyChart data={data.demandSupply} height="418px" />
          </Box>
        </Grid>
      ) : null}

      {/* All States/Districts/Pump Operators + Submission Rate */}
      {!selectedVillage && isBlockSelected ? (
        <>
          <Grid templateColumns={{ base: '1fr', lg: 'repeat(2, 1fr)' }} gap={6} mb={6}>
            <Box bg="white" borderWidth="1px" borderRadius="lg" px={4} py={6} h="526px">
              <PhotoEvidenceComplianceTable data={data.photoEvidenceCompliance} />
            </Box>
            <Box bg="white" borderWidth="1px" borderRadius="lg" px={4} py={6} h="526px">
              <Text textStyle="bodyText3" fontWeight="400" mb={2}>
                Reading Submission Rate
              </Text>
              <SupplySubmissionRateChart
                data={supplySubmissionRateData}
                height="383px"
                entityLabel={supplySubmissionRateLabel}
              />
            </Box>
          </Grid>
          <Grid templateColumns={{ base: '1fr', lg: 'repeat(2, 1fr)' }} gap={6} mb={6}>
            <Box bg="white" borderWidth="1px" borderRadius="lg" px={4} py={6} h="510px">
              <Text textStyle="bodyText3" fontWeight="400" mb="16px">
                {selectedGramPanchayat ? 'All Villages' : 'All Gram Panchayats'}
              </Text>
              <AllGramPanchayatsTable
                data={selectedGramPanchayat ? villageTableData : gramPanchayatTableData}
              />
            </Box>
            <Box bg="white" borderWidth="1px" borderRadius="lg" px={4} py={6} h="510px">
              <Flex align="center" justify="space-between" mb="40px">
                <Text textStyle="bodyText3" fontWeight="400">
                  Pump Operators
                </Text>
                <Text textStyle="bodyText3" fontWeight="400">
                  Total: {pumpOperatorsTotal}
                </Text>
              </Flex>
              <PumpOperatorsChart
                data={data.pumpOperators}
                height="360px"
                note="Note: Active pump operators submit readings at least 30 days in a month."
              />
            </Box>
          </Grid>
        </>
      ) : !selectedVillage ? (
        <Grid templateColumns={{ base: '1fr', lg: 'repeat(2, minmax(0, 1fr))' }} gap={6} mb={6}>
          <Box bg="white" borderWidth="1px" borderRadius="lg" px={4} py={6} h="510px" minW={0}>
            {isDistrictSelected ? (
              <>
                <Flex align="center" justify="space-between" mb="40px">
                  <Text textStyle="bodyText3" fontWeight="400">
                    Pump Operators
                  </Text>
                  <Text textStyle="bodyText3" fontWeight="400">
                    Total: {pumpOperatorsTotal}
                  </Text>
                </Flex>
                <PumpOperatorsChart
                  data={data.pumpOperators}
                  height="360px"
                  note="Note: Active pump operators submit readings at least 30 days in a month."
                />
              </>
            ) : (
              <>
                <Text textStyle="bodyText3" fontWeight="400" mb="16px">
                  {isStateSelected ? 'All Districts' : 'All States/UTs'}
                </Text>
                {isStateSelected ? (
                  <AllDistrictsTable data={districtTableData} />
                ) : (
                  <AllStatesTable data={data.mapData} />
                )}
              </>
            )}
          </Box>
          <Box bg="white" borderWidth="1px" borderRadius="lg" px={4} py={6} h="510px" minW={0}>
            <Text textStyle="bodyText3" fontWeight="400" mb={2}>
              Reading Submission Rate
            </Text>
            <SupplySubmissionRateChart
              data={supplySubmissionRateData}
              height="383px"
              entityLabel={supplySubmissionRateLabel}
            />
          </Box>
        </Grid>
      ) : null}

      {/* Operators Performance + All Blocks */}
      {!selectedVillage && isDistrictSelected && !isBlockSelected ? (
        <Grid templateColumns={{ base: '1fr', lg: 'repeat(2, 1fr)' }} gap={6} mb={6}>
          <Box
            bg="white"
            borderWidth="1px"
            borderRadius="lg"
            px={4}
            py={6}
            h="430px"
            w="full"
            minW={0}
          >
            <PumpOperatorsPerformanceTable
              title="Operators Performance Table"
              data={operatorsPerformanceTable}
            />
          </Box>
          <Box bg="white" borderWidth="1px" borderRadius="lg" px={4} py={6} h="430px">
            <Text textStyle="bodyText3" fontWeight="400" mb="16px">
              All Blocks
            </Text>
            <AllBlocksTable data={blockTableData} />
          </Box>
        </Grid>
      ) : null}

      {/* Pump Operator Performance Table */}
      {!selectedVillage && isDistrictSelected && isBlockSelected ? (
        <Grid templateColumns={{ base: '1fr', lg: 'repeat(2, 1fr)' }} gap={6} mb={6}>
          <Box bg="white" borderWidth="1px" borderRadius="lg" px={4} py={6} w="full" minW={0}>
            <PumpOperatorsPerformanceTable
              title="Operators Performance Table"
              data={operatorsPerformanceTable}
            />
          </Box>
          <Box
            display={{ base: 'none', lg: 'block' }}
            borderRadius="12px"
            borderWidth="0.5px"
            borderColor="transparent"
            bg="transparent"
          />
        </Grid>
      ) : null}

      {/* Pump Operators now lives beside Submission Rate when district is selected */}
    </Box>
  )
}

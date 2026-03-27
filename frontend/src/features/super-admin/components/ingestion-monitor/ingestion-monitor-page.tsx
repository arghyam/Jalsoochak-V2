import { useEffect, useState } from 'react'
import {
  Box,
  Flex,
  SimpleGrid,
  Heading,
  Text,
  Icon,
  Stack,
  Button,
  HStack,
  Badge,
  useBreakpointValue,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { getMockIngestionMonitorData } from '../../services/mock-data'
import { BarLineChart } from '@/shared/components/charts/bar-line-chart'
import { SearchableSelect } from '@/shared/components/common'
import type { IngestionMonitorData, IngestionLogEntry } from '../../types/ingestion-monitor'
import {
  STATE_FILTER_OPTIONS,
  TIME_FILTER_OPTIONS,
  STATUS_FILTER_OPTIONS,
} from '../../types/ingestion-monitor'
import { BiFilterAlt } from 'react-icons/bi'
import { FiDownload } from 'react-icons/fi'
import { IoCloudOutline, IoCloseCircleOutline, IoWarningOutline } from 'react-icons/io5'
import { BsCheck2Circle } from 'react-icons/bs'

export function IngestionMonitorPage() {
  const { t, i18n } = useTranslation(['super-admin', 'common'])
  const [data, setData] = useState<IngestionMonitorData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [stateFilter, setStateFilter] = useState('all')
  const [timeFilter, setTimeFilter] = useState('7')
  const [statusFilter, setStatusFilter] = useState('all')
  const [retryKey, setRetryKey] = useState(0)

  // Responsive values
  const statsColumns = useBreakpointValue({ base: 1, sm: 2, lg: 4 }) ?? 4
  const showExportText = useBreakpointValue({ base: false, md: true }) ?? true

  useEffect(() => {
    document.title = `${t('ingestionMonitor.title')} | JalSoochak`
  }, [t])

  useEffect(() => {
    let isMounted = true

    const fetchData = async () => {
      setIsLoading(true)
      setError(null)
      try {
        // When switching to real API, filters will be passed here:
        const result = await getMockIngestionMonitorData()
        if (isMounted) {
          setData(result)
        }
      } catch (err) {
        if (isMounted) {
          console.error('Failed to fetch ingestion monitor data:', err)
          setError(t('common:toast.failedToLoad'))
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    fetchData()

    return () => {
      isMounted = false
    }
    // Re-fetch when filters change
  }, [stateFilter, timeFilter, retryKey])

  const formatTimestamp = (date: Date): string => {
    let hours = date.getHours()
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const ampm = hours >= 12 ? 'pm' : 'am'

    hours = hours % 12
    hours = hours ? hours : 12
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const year = date.getFullYear()

    return `${hours}:${minutes}${ampm} | ${day}-${month}-${year}`
  }

  const getStatusBadge = (status: IngestionLogEntry['status']) => {
    const statusConfig = {
      successful: {
        bg: '#E1FFEA',
        color: '#079455',
        label: t('common:status.successful'),
      },
      warning: {
        bg: '#FFF0DB',
        color: '#F79009',
        label: t('common:status.warning'),
      },
      failed: {
        bg: '#FEE4E2',
        color: '#D92D20',
        label: t('common:status.failed'),
      },
    }

    const config = statusConfig[status]
    return (
      <Badge
        bg={config.bg}
        color={config.color}
        px={2}
        py={0.5}
        borderRadius="16px"
        fontSize="12px"
        lineHeight="16px"
        fontWeight="500"
        textTransform="capitalize"
        height="24px"
        width="71px"
        alignItems="center"
        display="flex"
        justifyContent="center"
      >
        {config.label}
      </Badge>
    )
  }

  const filteredLogs =
    data?.logs.filter((log) => {
      if (statusFilter !== 'all' && log.status !== statusFilter) {
        return false
      }
      return true
    }) || []

  if (isLoading) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={5}>
          {t('ingestionMonitor.title')}
        </Heading>
        <Flex role="status" aria-live="polite" h="64" align="center" justify="center">
          <Text color="neutral.600">{t('common:loading')}</Text>
        </Flex>
      </Box>
    )
  }

  if (error) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={5}>
          {t('ingestionMonitor.title')}
        </Heading>
        <Flex h="64" align="center" justify="center" direction="column" gap={4} role="alert">
          <Text color="error.500">{error}</Text>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => {
              setStateFilter('all')
              setTimeFilter('7')
              setRetryKey((prev) => prev + 1)
            }}
          >
            {t('common:retry')}
          </Button>
        </Flex>
      </Box>
    )
  }

  if (!data) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={5}>
          {t('ingestionMonitor.title')}
        </Heading>
        <Flex h="64" align="center" justify="center">
          <Text color="neutral.600">{t('common:noDataAvailable')}</Text>
        </Flex>
      </Box>
    )
  }

  const statsCards = [
    {
      title: t('ingestionMonitor.stats.totalIngestions'),
      value: data.stats.totalIngestions.toLocaleString(i18n.language),
      subtitle: t('ingestionMonitor.stats.acrossAllStatesUts'),
      icon: IoCloudOutline,
      iconBg: '#EBF4FA',
      iconColor: '#3291D1',
    },
    {
      title: t('ingestionMonitor.stats.successfulIngestions'),
      value: data.stats.successfulIngestions.toLocaleString(i18n.language),
      subtitle: t('ingestionMonitor.stats.successRate', { rate: data.stats.successRate }),
      subtitleColor: '#079455',
      icon: BsCheck2Circle,
      iconBg: '#E1FFEA',
      iconColor: '#079455',
    },
    {
      title: t('ingestionMonitor.stats.failedIngestions'),
      value: data.stats.failedIngestions.toLocaleString(i18n.language),
      subtitle: t('ingestionMonitor.stats.failureRate', { rate: data.stats.failureRate }),
      subtitleColor: '#D92D20',
      icon: IoCloseCircleOutline,
      iconBg: '#FEE4E2',
      iconColor: '#D92D20',
    },
    {
      title: t('ingestionMonitor.stats.currentWarnings'),
      value: data.stats.currentWarnings.toLocaleString(i18n.language),
      subtitle: t('ingestionMonitor.stats.issuesRequiringAttention'),
      icon: IoWarningOutline,
      iconBg: '#FFF3CD',
      iconColor: '#CC8800',
    },
  ]

  return (
    <Box w="full">
      {/* Page Header */}
      <Box mb={5} h={12}>
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }}>
          {t('ingestionMonitor.title')}
        </Heading>
      </Box>

      <Stack gap={6}>
        {/* Filters Row */}
        <Flex
          justify="space-between"
          align={{ base: 'stretch', md: 'center' }}
          bg="white"
          py={4}
          px={{ base: 3, md: 6 }}
          border="0.5px"
          borderColor="neutral.200"
          borderRadius="12px"
          h={{ base: 'auto', md: 16 }}
          flexDirection={{ base: 'column', md: 'row' }}
          gap={{ base: 3, md: 0 }}
        >
          <HStack spacing={2} h={{ base: 'auto', md: 8 }} flexWrap="wrap" gap={{ base: 2, md: 2 }}>
            <HStack spacing={2}>
              <Icon as={BiFilterAlt} boxSize={5} aria-hidden="true" />
              <Text fontSize="14px" fontWeight="500">
                {t('common:filters')}
              </Text>
            </HStack>
            <SearchableSelect
              options={STATE_FILTER_OPTIONS}
              value={stateFilter}
              onChange={setStateFilter}
              placeholder={t('ingestionMonitor.filters.allStatesUts')}
              width={{ base: '100%', sm: '160px' }}
              fontSize="14px"
              textColor="neutral.400"
              borderRadius="4px"
              borderColor="neutral.400"
              height="32px"
              aria-label={t('ingestionMonitor.filters.allStatesUts')}
            />
            <SearchableSelect
              options={TIME_FILTER_OPTIONS}
              value={timeFilter}
              onChange={setTimeFilter}
              placeholder={t('ingestionMonitor.filters.lastDays', { days: 7 })}
              width={{ base: '100%', sm: '140px' }}
              fontSize="14px"
              textColor="neutral.400"
              borderColor="neutral.400"
              borderRadius="4px"
              height="32px"
              aria-label={t('ingestionMonitor.filters.lastDays', { days: 7 })}
            />
          </HStack>
          <Button
            variant="primary"
            size="sm"
            leftIcon={<Icon as={FiDownload} boxSize={4} aria-hidden="true" />}
            w={{ base: 'full', md: 'auto' }}
            aria-label={t('common:button.exportData')}
          >
            {showExportText ? t('common:button.exportData') : ''}
          </Button>
        </Flex>

        {/* Stats Cards */}
        <SimpleGrid
          columns={statsColumns}
          spacing={7}
          mb={1}
          height={{ base: 'auto', md: '200px' }}
          as="section"
          aria-label={t('ingestionMonitor.stats.totalIngestions')}
        >
          {statsCards.map((stat) => {
            const StatIcon = stat.icon
            return (
              <Box
                key={stat.title}
                as="article"
                bg="white"
                borderWidth="0.5px"
                borderColor="neutral.200"
                borderRadius="12px"
                boxShadow="default"
                px={4}
                py={6}
              >
                <Flex direction="column" gap={3}>
                  <Flex
                    h="40px"
                    w="40px"
                    align="center"
                    justify="center"
                    borderRadius="full"
                    bg={stat.iconBg}
                  >
                    <Icon as={StatIcon} boxSize={5} color={stat.iconColor} aria-hidden="true" />
                  </Flex>
                  <Flex direction="column" gap={1}>
                    <Text color="neutral.600" fontSize="14px">
                      {stat.title}
                    </Text>
                    <Text textStyle="h9">{stat.value}</Text>
                    <Text fontSize="14px" color={stat.subtitleColor || 'neutral.600'}>
                      {stat.subtitle}
                    </Text>
                  </Flex>
                </Flex>
              </Box>
            )
          })}
        </SimpleGrid>

        {/* Ingestion Success Rate Chart */}
        <Box
          as="section"
          aria-labelledby="chart-title"
          bg="white"
          borderWidth="0.5px"
          borderColor="neutral.200"
          borderRadius="16px"
          boxShadow="default"
          height={{ base: 'auto', md: '420px' }}
          py={6}
          px={4}
          mb={1}
        >
          <Heading as="h2" size="h3" fontWeight="400" mb={4} id="chart-title">
            {t('ingestionMonitor.charts.ingestionSuccessRate')}
          </Heading>
          <BarLineChart
            data={data.chartData}
            xKey="month"
            barKey="successfulIngestions"
            lineKey="failedIngestions"
            barColor="#3291D1"
            lineColor="#FFA100"
            height="326px"
            barLegendLabel={t('overview.charts.successfulIngestions')}
            lineLegendLabel={t('overview.charts.failedIngestions')}
          />
        </Box>

        {/* Detailed Ingestion Logs */}
        <Box
          as="section"
          aria-labelledby="logs-title"
          bg="white"
          borderWidth="0.5px"
          borderColor="neutral.200"
          borderRadius="12px"
          boxShadow="default"
          height={{ base: 'auto', md: '395px' }}
          py={4}
          px={{ base: 3, md: 4 }}
        >
          <Flex
            justify="space-between"
            align={{ base: 'stretch', sm: 'center' }}
            mb={3}
            flexDirection={{ base: 'column', sm: 'row' }}
            gap={{ base: 2, sm: 0 }}
          >
            <Heading as="h2" size="h3" fontWeight="400" id="logs-title">
              {t('ingestionMonitor.logs.title')}
            </Heading>
            <SearchableSelect
              options={STATUS_FILTER_OPTIONS}
              value={statusFilter}
              onChange={setStatusFilter}
              placeholder={t('common:statusLabel')}
              width={{ base: '100%', sm: '120px' }}
              fontSize="14px"
              height="32px"
              aria-label={t('common:statusLabel')}
            />
          </Flex>

          <Stack gap={0} role="list" aria-label={t('ingestionMonitor.logs.title')}>
            {filteredLogs.map((log, index) => (
              <Box
                key={log.id}
                as="article"
                role="listitem"
                py={3}
                borderTopWidth={index > 0 ? '1px' : '0'}
                borderColor="neutral.100"
              >
                <Flex
                  justify="space-between"
                  align={{ base: 'flex-start', md: 'center' }}
                  gap={4}
                  flexDirection={{ base: 'column', md: 'row' }}
                >
                  <Box width={{ base: 'auto', lg: '694px' }}>
                    <Text fontSize="16px" fontWeight="400" color="neutral.950" mb={0.5}>
                      {log.title}
                      {log.status === 'successful' && log.recordsProcessed && (
                        <Text as="span" fontWeight="400">
                          {' '}
                          {t('ingestionMonitor.logs.recordsProcessed', {
                            recordCount: log.recordsProcessed.toLocaleString(i18n.language),
                          })}
                        </Text>
                      )}
                    </Text>

                    <Text fontSize="16px" color="neutral.600" mb={0.5}>
                      {t('ingestionMonitor.logs.batchId')} {log.batchId}{' '}
                      {t('ingestionMonitor.logs.sourceSystem')} {log.sourceSystem}{' '}
                      {t('ingestionMonitor.logs.processingTime')} {log.processingTime}
                    </Text>

                    {log.status === 'successful' && (
                      <Text fontSize="16px" color="neutral.600">
                        {t('ingestionMonitor.logs.noAnomalies')}
                      </Text>
                    )}
                    {log.status === 'warning' && log.issueDetails && (
                      <Text fontSize="16px" color="neutral.600">
                        {t('ingestionMonitor.logs.issue')} {log.issueDetails}
                      </Text>
                    )}
                    {log.status === 'failed' && log.errorDetails && (
                      <Text fontSize="16px" color="neutral.600">
                        {t('ingestionMonitor.logs.error')} {log.errorDetails}
                      </Text>
                    )}
                  </Box>
                  <Flex
                    align="center"
                    flexDirection={{ base: 'row', md: 'row' }}
                    w={{ base: 'full', md: 'auto' }}
                  >
                    {getStatusBadge(log.status)}
                  </Flex>
                  <Flex
                    align="center"
                    flexDirection={{ base: 'row', md: 'row' }}
                    w={{ base: 'full', md: 'auto' }}
                  >
                    <Text fontSize="16px" color="neutral.600" whiteSpace="nowrap">
                      {formatTimestamp(log.timestamp)}
                    </Text>
                  </Flex>
                </Flex>
              </Box>
            ))}
          </Stack>
        </Box>
      </Stack>
    </Box>
  )
}

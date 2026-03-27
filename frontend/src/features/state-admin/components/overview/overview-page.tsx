import { useEffect, useState } from 'react'
import {
  Box,
  Flex,
  SimpleGrid,
  Text,
  Icon,
  Stack,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  Button,
  Heading,
  Spinner,
} from '@chakra-ui/react'
import { ChevronDownIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'
import i18n from '@/app/i18n'
import { useAuthStore } from '@/app/store'
import { getMockOverviewData } from '../../services/mock-data'
import { LineChart } from '@/shared/components/charts/line-chart'
import { AreaChart } from '@/shared/components/charts/area-chart'
import type { OverviewData } from '../../types/overview'
import { BsCheck2Circle, BsPerson } from 'react-icons/bs'
import { AiOutlineApi, AiOutlineWarning } from 'react-icons/ai'
import { BiMessageDetail } from 'react-icons/bi'

type MonthKey =
  | 'january'
  | 'february'
  | 'march'
  | 'april'
  | 'may'
  | 'june'
  | 'july'
  | 'august'
  | 'september'
  | 'october'
  | 'november'
  | 'december'

export function OverviewPage() {
  const { t } = useTranslation(['state-admin', 'common'])
  const user = useAuthStore((state) => state.user)
  const [data, setData] = useState<OverviewData | null>(null)
  const [selectedMonth, setSelectedMonth] = useState<MonthKey>('december')

  useEffect(() => {
    const pageTitle = user?.tenantId
      ? t('overview.title', { state: user.tenantId })
      : t('overview.titleFallback')
    document.title = `${pageTitle} | JalSoochak`
  }, [t, user?.tenantId])

  useEffect(() => {
    getMockOverviewData().then(setData)
  }, [])

  const monthOptions: MonthKey[] = ['december', 'november']

  if (!data) {
    return (
      <Flex
        h="64"
        align="center"
        justify="center"
        role="status"
        aria-live="polite"
        aria-busy="true"
      >
        <Spinner size="lg" color="primary.500" mr={3} />
        <Text color="neutral.600">{t('common:loading')}</Text>
      </Flex>
    )
  }

  const statsCards = [
    {
      title: t('overview.stats.pumpOperatorsSynced'),
      value: data.stats.pumpOperatorsSynced.toLocaleString(i18n.language),
      subtitle: t('overview.stats.outOf', { total: '3000' }),
      icon: BsPerson,
      iconBg: '#F1EEFF',
      iconColor: '#584C93',
    },
    {
      title: t('overview.stats.configurationStatus'),
      value: data.stats.configurationStatus,
      subtitle: t('overview.stats.allModulesConfigured'),
      icon: BsCheck2Circle,
      iconBg: '#E1FFEA',
      iconColor: '#079455',
    },
    {
      title: t('overview.stats.todayApiIngestion'),
      value: data.stats.todayApiIngestion,
      subtitle: t('overview.stats.successfullyIngested'),
      icon: AiOutlineApi,
      iconBg: '#EBF4FA',
      iconColor: '#3291D1',
    },
    {
      title: t('overview.stats.pendingDataSync'),
      value: data.stats.pendingDataSync.toLocaleString(i18n.language),
      subtitle: t('overview.stats.requiresAttention'),
      icon: AiOutlineWarning,
      iconBg: '#FFFBD7',
      iconColor: '#CA8A04',
    },
    {
      title: t('overview.stats.activeIntegrations'),
      value: data.stats.activeIntegrations.toLocaleString(i18n.language),
      subtitle: t('overview.stats.integrationNames'),
      icon: BiMessageDetail,
      iconBg: '#FBEAFF',
      iconColor: '#DC72F2',
    },
  ]

  return (
    <Box w="full">
      {/* Page Header */}
      <Box mb={5}>
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }}>
          {user?.tenantId
            ? t('overview.title', { state: user.tenantId })
            : t('overview.titleFallback')}
        </Heading>
      </Box>

      <Stack gap={{ base: 4, md: 6 }}>
        {/* Stats Cards */}
        <SimpleGrid
          as="section"
          aria-label={t('overview.aria.statsSection')}
          columns={{ base: 1, sm: 2, md: 3, lg: 5 }}
          spacing={{ base: 4, md: 7 }}
        >
          {statsCards.map((stat) => {
            const StatIcon = stat.icon
            return (
              <Box
                key={stat.title}
                bg="white"
                borderWidth="1px"
                borderColor="neutral.100"
                height={{ base: 'auto', xl: '200px' }}
                borderRadius="lg"
                boxShadow="default"
                p={4}
              >
                <Flex direction="column" gap={3}>
                  <Flex
                    h="40px"
                    w="40px"
                    align="center"
                    justify="center"
                    borderRadius="full"
                    bg={stat.iconBg}
                    aria-hidden="true"
                  >
                    <Icon as={StatIcon} boxSize={5} color={stat.iconColor} />
                  </Flex>
                  <Flex direction="column" gap={1}>
                    <Text color="neutral.600" fontSize={{ base: 'sm', md: 'md' }}>
                      {stat.title}
                    </Text>
                    <Text
                      textStyle="h9"
                      fontSize={{ base: 'xl', md: '2xl' }}
                      aria-label={`${stat.title}: ${stat.value}`}
                    >
                      {stat.value}
                    </Text>
                    <Text color="neutral.600" fontSize={{ base: 'sm', md: 'md' }}>
                      {stat.subtitle}
                    </Text>
                  </Flex>
                </Flex>
              </Box>
            )
          })}
        </SimpleGrid>

        {/* Demand vs Supply Chart */}
        <Box
          as="section"
          aria-labelledby="demand-supply-chart-heading"
          bg="white"
          borderWidth="1px"
          borderColor="neutral.100"
          borderRadius={{ base: 'lg', md: 'xl' }}
          boxShadow="default"
          height={{ base: 'auto', md: '534px' }}
          py={{ base: 4, md: 6 }}
          px={4}
        >
          <Heading
            as="h2"
            id="demand-supply-chart-heading"
            size="h3"
            fontWeight="400"
            mb={4}
            fontSize={{ base: 'md', md: 'xl' }}
          >
            {t('overview.charts.demandVsSupply')}
          </Heading>
          <LineChart
            data={data.demandSupplyData}
            xKey="period"
            yKeys={['Demand', 'Supply']}
            colors={['#3291D1', '#ADD3EB']}
            height="416px"
            xAxisLabel={t('overview.charts.Year')}
            legendLabels={[t('overview.charts.Demand'), t('overview.charts.Supply')]}
            yAxisLabel={t('overview.charts.Quantity')}
          />
        </Box>

        {/* Daily Ingestion Monitor */}
        <Box
          as="section"
          aria-labelledby="ingestion-chart-heading"
          bg="white"
          borderWidth="1px"
          borderColor="neutral.100"
          borderRadius={{ base: 'lg', md: '2xl' }}
          height={{ base: 'auto', md: '454px' }}
          boxShadow="default"
          py={{ base: 4, md: 6 }}
          px={4}
        >
          <Flex
            direction={{ base: 'column', sm: 'row' }}
            align={{ base: 'flex-start', sm: 'center' }}
            justify="space-between"
            gap={{ base: 3, sm: 0 }}
            mb={4}
          >
            <Heading
              as="h2"
              id="ingestion-chart-heading"
              size="h3"
              fontWeight="400"
              fontSize={{ base: 'md', md: 'xl' }}
            >
              {t('overview.charts.dailyIngestionMonitor')}
            </Heading>
            <Menu matchWidth>
              <MenuButton
                as={Button}
                aria-label={t('overview.aria.selectMonth')}
                h="32px"
                w={{ base: 'full', sm: '162px' }}
                px="12px"
                fontSize="14px"
                fontWeight="600"
                borderRadius="4px"
                borderColor="primary.500"
                borderWidth="1px"
                bg="white"
                color="primary.500"
                variant="outline"
                rightIcon={<ChevronDownIcon w={5} h={5} aria-hidden="true" />}
                _hover={{ bg: 'neutral.50' }}
                _active={{ bg: 'neutral.100' }}
                _focusVisible={{ boxShadow: 'outline' }}
                sx={{
                  '& svg': {
                    color: 'primary.500',
                  },
                }}
              >
                {t(`overview.months.${selectedMonth}`)}
              </MenuButton>
              <MenuList p={0} minW="162px" borderRadius="4px" borderColor="primary.500">
                {monthOptions.map((month) => {
                  const isSelected = month === selectedMonth

                  return (
                    <MenuItem
                      key={month}
                      h="32px"
                      px="12px"
                      fontSize="14px"
                      fontWeight={isSelected ? '600' : '400'}
                      color="neutral.950"
                      bg={isSelected ? 'primary.50' : 'white'}
                      _hover={{
                        bg: 'primary.50',
                      }}
                      _focus={{
                        bg: 'primary.50',
                      }}
                      onClick={() => setSelectedMonth(month)}
                      aria-selected={isSelected}
                    >
                      {t(`overview.months.${month}`)}
                    </MenuItem>
                  )
                })}
              </MenuList>
            </Menu>
          </Flex>
          <AreaChart
            data={data.dailyIngestionData}
            xKey="day"
            yKey="count"
            color="#FFA100"
            height="326px"
            legendLabel={t('overview.charts.successRate')}
          />
        </Box>
      </Stack>
    </Box>
  )
}

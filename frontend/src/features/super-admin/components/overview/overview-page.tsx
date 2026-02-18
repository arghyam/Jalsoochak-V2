import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Flex,
  SimpleGrid,
  Text,
  Icon,
  Stack,
  Button,
  Heading,
  Spinner,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { getMockSuperAdminOverviewData } from '../../services/mock-data'
import { BarLineChart } from '@/shared/components/charts/bar-line-chart'
import type { SuperAdminOverviewData } from '../../types/overview'
import { MdOutlinePlace } from 'react-icons/md'
import { BsCheck2Circle } from 'react-icons/bs'
import { IoCloseCircleOutline, IoAddOutline } from 'react-icons/io5'
import { ROUTES } from '@/shared/constants/routes'
import i18n from '@/app/i18n'

export function OverviewPage() {
  const { t } = useTranslation(['super-admin', 'common'])
  const navigate = useNavigate()
  const [data, setData] = useState<SuperAdminOverviewData | null>(null)

  useEffect(() => {
    document.title = `${t('overview.title')} | JalSoochak`
  }, [t])

  useEffect(() => {
    getMockSuperAdminOverviewData().then(setData)
  }, [])

  const formatTimestamp = (date: Date): string => {
    const locale = i18n.language === 'hi' ? 'hi-IN' : 'en-IN'
    return new Intl.DateTimeFormat(locale, {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(date)
  }

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
      title: t('overview.stats.totalStatesManaged'),
      value: data.stats.totalStatesManaged,
      icon: MdOutlinePlace,
      iconBg: '#EBF4FA',
      iconColor: '#3291D1',
    },
    {
      title: t('overview.stats.activeStates'),
      value: data.stats.activeStates,
      icon: BsCheck2Circle,
      iconBg: '#E1FFEA',
      iconColor: '#079455',
    },
    {
      title: t('overview.stats.inactiveStates'),
      value: data.stats.inactiveStates,
      icon: IoCloseCircleOutline,
      iconBg: '#FFFBD7',
      iconColor: '#CA8A04',
    },
  ]

  return (
    <Box w="full">
      {/* Page Header with Add Button */}
      <Flex
        direction={{ base: 'column', sm: 'row' }}
        justify="space-between"
        align={{ base: 'flex-start', sm: 'center' }}
        gap={{ base: 3, sm: 0 }}
        mb={5}
        minH={12}
      >
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }}>
          {t('overview.title')}
        </Heading>

        <Button
          variant="secondary"
          size={{ base: 'md', lg: 'sm' }}
          fontWeight="600"
          onClick={() => navigate(ROUTES.SUPER_ADMIN_STATES_UTS_ADD)}
          w={{ base: 'full', sm: 'auto' }}
          gap={1}
        >
          {<IoAddOutline size={24} />} {t('overview.addNewStateUt')}
        </Button>
      </Flex>

      <Stack gap={{ base: 4, md: 6 }}>
        {/* Stats Cards */}
        <SimpleGrid columns={{ base: 1, sm: 2, lg: 3 }} spacing={{ base: 4, md: 7 }}>
          {statsCards.map((stat) => {
            const StatIcon = stat.icon
            return (
              <Box
                key={stat.title}
                bg="white"
                borderWidth="0.5px"
                borderColor="neutral.200"
                borderRadius="12px"
                boxShadow="default"
                height={{ base: 'auto', md: '172px' }}
                px={4}
                py={{ base: 4, md: 6 }}
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
                  </Flex>
                </Flex>
              </Box>
            )
          })}
        </SimpleGrid>

        {/* Ingestion Success Rate Chart */}
        <Box
          as="section"
          aria-labelledby="chart-heading"
          bg="white"
          borderWidth="0.5px"
          borderColor="neutral.200"
          height={{ base: 'auto', md: '420px' }}
          borderRadius={{ base: '12px', md: '16px' }}
          boxShadow="default"
          py={{ base: 4, md: 6 }}
          px={4}
        >
          <Heading
            as="h2"
            id="chart-heading"
            size="h3"
            fontWeight="400"
            mb={4}
            fontSize={{ base: 'md', md: 'xl' }}
          >
            {t('overview.charts.ingestionSuccessRate')}
          </Heading>
          <BarLineChart
            data={data.ingestionData}
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

        {/* Notifications Section */}
        <Box
          as="section"
          aria-labelledby="notifications-heading"
          bg="white"
          borderWidth="0.5px"
          borderColor="neutral.200"
          borderRadius="12px"
          boxShadow="default"
          height={{ base: 'auto', md: '237px' }}
          py={{ base: 4, md: 6 }}
          px={4}
        >
          <Heading
            as="h2"
            id="notifications-heading"
            size="h3"
            fontWeight="400"
            mb={4}
            fontSize={{ base: 'md', md: 'xl' }}
          >
            {t('overview.notifications')}
          </Heading>
          <Stack as="ul" gap={{ base: 2, md: 4 }} listStyleType="none">
            {data.notifications.map((notification) => (
              <Flex
                as="li"
                key={notification.id}
                direction={{ base: 'column', sm: 'row' }}
                justify="space-between"
                align={{ base: 'flex-start', sm: 'center' }}
                gap={{ base: 1, sm: 4 }}
                py={2}
                borderBottomWidth="1px"
                borderColor="neutral.100"
                _last={{ borderBottomWidth: 0 }}
              >
                <Text fontSize={{ base: '14px', md: '16px' }} color="neutral.950" fontWeight="400">
                  {notification.message}
                </Text>
                <Text
                  fontSize={{ base: '14px', md: '16px' }}
                  color="neutral.600"
                  whiteSpace="nowrap"
                  fontWeight="400"
                >
                  {formatTimestamp(notification.timestamp)}
                </Text>
              </Flex>
            ))}
          </Stack>
        </Box>
      </Stack>
    </Box>
  )
}

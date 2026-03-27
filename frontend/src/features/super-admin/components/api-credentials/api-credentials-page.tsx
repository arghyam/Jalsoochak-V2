import { useEffect, useState } from 'react'
import {
  Box,
  Flex,
  SimpleGrid,
  Heading,
  Text,
  Button,
  Input,
  InputGroup,
  InputLeftElement,
  Badge,
  useBreakpointValue,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { SearchIcon } from '@chakra-ui/icons'
import { BiKey } from 'react-icons/bi'
import { getMockApiCredentialsData, generateApiKey, sendApiKey } from '../../services/mock-data'
import { SearchableSelect } from '@/shared/components/common'
import type { ApiCredentialsData, ApiCredential } from '../../types/api-credentials'
import { STATUS_FILTER_OPTIONS } from '../../types/api-credentials'

export function ApiCredentialsPage() {
  const { t } = useTranslation(['super-admin', 'common'])
  const [data, setData] = useState<ApiCredentialsData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [generatingKeyFor, setGeneratingKeyFor] = useState<string | null>(null)
  const [sendingKeyFor, setSendingKeyFor] = useState<string | null>(null)

  // Responsive values
  const searchInputWidth = useBreakpointValue({ base: 'full', md: '300px' }) ?? '300px'
  const gridColumns = useBreakpointValue({ base: 1, lg: 2 }) ?? 2

  useEffect(() => {
    document.title = `${t('apiCredentials.title')} | JalSoochak`
  }, [t])

  useEffect(() => {
    let isMounted = true

    const fetchData = async () => {
      setIsLoading(true)
      try {
        const result = await getMockApiCredentialsData()
        if (isMounted) {
          setData(result)
        }
      } catch (err) {
        console.error('Failed to fetch API credentials data:', err)
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
  }, [])

  const formatDate = (date: Date): string => {
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const year = date.getFullYear()

    return `${day}-${month}-${year}`
  }

  const formatLastUsed = (date: Date): string => {
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const year = date.getFullYear()
    let hours = date.getHours()
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const ampm = hours >= 12 ? 'pm' : 'am'

    hours = hours % 12
    hours = hours ? hours : 12
    return `${day}-${month}-${year}, ${hours}:${minutes}${ampm}`
  }

  const handleGenerateKey = async (credentialId: string) => {
    setGeneratingKeyFor(credentialId)
    try {
      const newKey = await generateApiKey(credentialId)
      setData((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          credentials: prev.credentials.map((cred) =>
            cred.id === credentialId ? { ...cred, apiKey: newKey } : cred
          ),
        }
      })
    } catch (err) {
      console.error('Failed to generate API key:', err)
    } finally {
      setGeneratingKeyFor(null)
    }
  }

  const handleSendKey = async (credentialId: string) => {
    setSendingKeyFor(credentialId)
    try {
      await sendApiKey(credentialId)
    } catch (err) {
      console.error('Failed to send API key:', err)
    } finally {
      setSendingKeyFor(null)
    }
  }

  const getStatusBadge = (status: ApiCredential['status']) => {
    const statusConfig = {
      active: {
        bg: '#E1FFEA',
        color: '#079455',
        label: t('common:status.active'),
      },
      inactive: {
        bg: '#FEE4E2',
        color: '#D92D20',
        label: t('common:status.inactive'),
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
        fontWeight="500"
        textTransform="capitalize"
        height="24px"
      >
        {config.label}
      </Badge>
    )
  }

  const filteredCredentials =
    data?.credentials.filter((cred) => {
      if (statusFilter !== 'all' && cred.status !== statusFilter) {
        return false
      }
      if (searchQuery && !cred.stateUtName.toLowerCase().includes(searchQuery.toLowerCase())) {
        return false
      }
      return true
    }) || []

  if (isLoading) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={5}>
          {t('apiCredentials.title')}
        </Heading>
        <Flex role="status" aria-live="polite" h="64" align="center" justify="center">
          <Text color="neutral.600">{t('common:loading')}</Text>
        </Flex>
      </Box>
    )
  }

  if (!data) {
    return (
      <Box w="full">
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }} mb={5}>
          {t('apiCredentials.title')}
        </Heading>
        <Flex h="64" align="center" justify="center">
          <Text color="neutral.600">{t('common:noDataAvailable')}</Text>
        </Flex>
      </Box>
    )
  }

  return (
    <Box w="full">
      {/* Page Header */}
      <Box mb={5}>
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }}>
          {t('apiCredentials.title')}
        </Heading>
      </Box>

      {/* Search and Filter Row */}
      <Flex
        justify="space-between"
        align="center"
        mb={6}
        h={{ base: 'auto', md: 16 }}
        bg="white"
        py={4}
        px={{ base: 3, md: 6 }}
        borderRadius={3}
        border="0.5px"
        borderColor="neutral.200"
        flexDirection={{ base: 'column', md: 'row' }}
        gap={{ base: 3, md: 0 }}
      >
        <InputGroup w={searchInputWidth}>
          <InputLeftElement
            pointerEvents="none"
            display="flex"
            alignItems="center"
            justifyContent="center"
            h="32px"
          >
            <SearchIcon color="neutral.400" boxSize={4} aria-hidden="true" />
          </InputLeftElement>
          <Input
            placeholder={t('apiCredentials.searchPlaceholder')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            aria-label={t('apiCredentials.searchPlaceholder')}
            borderColor="neutral.300"
            borderRadius="8px"
            fontSize="14px"
            h="32px"
            _hover={{ borderColor: 'neutral.400' }}
            _focus={{ borderColor: 'primary.500', boxShadow: 'none' }}
            _placeholder={{ color: 'neutral.400' }}
          />
        </InputGroup>
        <SearchableSelect
          options={STATUS_FILTER_OPTIONS}
          value={statusFilter}
          onChange={setStatusFilter}
          placeholder={t('common:statusLabel')}
          width={{ base: '100%', md: '140px' }}
          fontSize="14px"
          textColor="neutral.400"
          borderColor="neutral.400"
          borderRadius="4px"
          height="32px"
          aria-label={t('common:statusLabel')}
        />
      </Flex>

      {/* Credentials Grid */}
      <SimpleGrid columns={gridColumns} spacing={7}>
        {filteredCredentials.map((credential) => (
          <Box
            key={credential.id}
            as="article"
            aria-labelledby={`credential-${credential.id}-title`}
            bg="white"
            borderWidth="0.5px"
            borderColor="neutral.200"
            borderRadius="12px"
            boxShadow="default"
            py={6}
            px={4}
          >
            <Heading
              as="h2"
              size="h3"
              fontWeight="400"
              mb={4}
              id={`credential-${credential.id}-title`}
            >
              {credential.stateUtName}
            </Heading>

            <Flex align="center" gap={1} mb={3}>
              <BiKey aria-hidden="true" />
              <Text fontSize="14px">{credential.apiKey}</Text>
            </Flex>

            {/* Details Grid */}
            <SimpleGrid columns={2} gap={3} mb={3}>
              <Box>
                <Text fontSize="14px" fontWeight="500" mb={1}>
                  {t('apiCredentials.labels.lastUsed')}
                </Text>
                <Text fontSize="14px">{formatLastUsed(credential.lastUsed)}</Text>
              </Box>
              <Box>
                <Text fontSize="14px" mb={1} fontWeight="500">
                  {t('apiCredentials.labels.createdOn')}
                </Text>
                <Text fontSize="14px">{formatDate(credential.createdOn)}</Text>
              </Box>
              <Box>
                <Text fontSize="14px" mb={1} fontWeight="500">
                  {t('apiCredentials.labels.nextRotation')}
                </Text>
                <Text fontSize="14px">{formatDate(credential.nextRotation)}</Text>
              </Box>
              <Box>
                <Text fontSize="14px" mb={1} fontWeight="500">
                  {t('apiCredentials.labels.stateUtStatus')}
                </Text>
                {getStatusBadge(credential.status)}
              </Box>
            </SimpleGrid>

            {/* Action Buttons */}
            <Flex gap={3} flexDirection={{ base: 'column', sm: 'row' }}>
              <Button
                variant="secondary"
                size="md"
                flex={1}
                isLoading={generatingKeyFor === credential.id}
                onClick={() => handleGenerateKey(credential.id)}
                aria-label={`${t('apiCredentials.buttons.generateKey')} ${credential.stateUtName}`}
              >
                {t('apiCredentials.buttons.generateKey')}
              </Button>
              <Button
                variant="primary"
                size="md"
                flex={1}
                isLoading={sendingKeyFor === credential.id}
                onClick={() => handleSendKey(credential.id)}
                aria-label={`${t('apiCredentials.buttons.sendKey')} ${credential.stateUtName}`}
              >
                {t('apiCredentials.buttons.sendKey')}
              </Button>
            </Flex>
          </Box>
        ))}
      </SimpleGrid>

      {/* Empty State */}
      {filteredCredentials.length === 0 && (
        <Flex
          h="200px"
          align="center"
          justify="center"
          bg="white"
          borderWidth="0.5px"
          borderColor="neutral.200"
          borderRadius="12px"
          role="status"
        >
          <Text color="neutral.600">{t('apiCredentials.messages.noCredentialsFound')}</Text>
        </Flex>
      )}
    </Box>
  )
}

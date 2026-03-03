import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Heading,
  Text,
  Flex,
  Input,
  InputGroup,
  InputLeftElement,
  Button,
  IconButton,
  useBreakpointValue,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { SearchIcon, EditIcon } from '@chakra-ui/icons'
import { FiEye } from 'react-icons/fi'
import { IoAddOutline } from 'react-icons/io5'
import { DataTable, type DataTableColumn } from '@/shared/components/common'
import { getMockStatesUTsData } from '../../services/mock-data'
import type { StateUT } from '../../types/states-uts'
import { ROUTES } from '@/shared/constants/routes'

export function StatesUTsPage() {
  const { t } = useTranslation(['super-admin', 'common'])
  const navigate = useNavigate()
  const [states, setStates] = useState<StateUT[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')

  // Responsive values
  const showAddButtonText = useBreakpointValue({ base: false, sm: true }) ?? true

  useEffect(() => {
    document.title = `${t('statesUts.title')} | JalSoochak`
  }, [t])

  useEffect(() => {
    fetchStates()
  }, [])

  const fetchStates = async () => {
    setIsLoading(true)
    try {
      const data = await getMockStatesUTsData()
      setStates(data)
    } catch (error) {
      console.error('Failed to fetch states:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const formatTimestamp = (date: Date): string => {
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

  const filteredStates = states.filter((state) =>
    state.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const handleAddNew = () => {
    navigate(ROUTES.SUPER_ADMIN_STATES_UTS_ADD)
  }

  const handleView = (id: string) => {
    navigate(ROUTES.SUPER_ADMIN_STATES_UTS_VIEW.replace(':id', id))
  }

  const handleEdit = (id: string) => {
    navigate(ROUTES.SUPER_ADMIN_STATES_UTS_EDIT.replace(':id', id))
  }

  const columns: DataTableColumn<StateUT>[] = [
    {
      key: 'name',
      header: t('statesUts.table.stateUt'),
      sortable: false,
      render: (row) => (
        <Text textStyle="h10" fontWeight="400">
          {row.name}
        </Text>
      ),
    },
    {
      key: 'status',
      header: t('statesUts.table.status'),
      sortable: false,
      render: (row) => (
        <Box
          as="span"
          display="inline-block"
          px={2}
          py={0.5}
          borderRadius="16px"
          h={6}
          textStyle="h10"
          fontWeight="400"
          bg={row.status === 'active' ? 'success.50' : 'error.50'}
          color={row.status === 'active' ? 'success.500' : 'error.500'}
        >
          {row.status === 'active' ? t('common:status.active') : t('common:status.inactive')}
        </Box>
      ),
    },
    {
      key: 'lastSyncDate',
      header: t('statesUts.table.lastSyncDate'),
      sortable: true,
      render: (row) => (
        <Text textStyle="h10" fontWeight="400">
          {formatTimestamp(row.lastSyncDate)}
        </Text>
      ),
    },
    {
      key: 'totalDistricts',
      header: t('statesUts.table.totalDistricts'),
      sortable: true,
      render: (row) => (
        <Text textStyle="h10" fontWeight="400">
          {row.totalDistricts}
        </Text>
      ),
    },
    {
      key: 'actions',
      header: t('statesUts.table.actions'),
      render: (row) => (
        <Flex gap={1}>
          <IconButton
            aria-label={`${t('statesUts.aria.viewStateUt')} ${row.name}`}
            icon={<FiEye aria-hidden="true" size={20} />}
            variant="ghost"
            width={5}
            minW={5}
            height={5}
            color="neutral.950"
            fontWeight="400"
            onClick={() => handleView(row.id)}
            _hover={{ color: 'primary.500', bg: 'transparent' }}
          />
          <IconButton
            aria-label={`${t('statesUts.aria.editStateUt')} ${row.name}`}
            icon={<EditIcon aria-hidden="true" w={5} h={5} />}
            variant="ghost"
            width={5}
            minW={5}
            height={5}
            color="neutral.950"
            fontWeight="400"
            onClick={() => handleEdit(row.id)}
            _hover={{ color: 'primary.500', bg: 'transparent' }}
          />
        </Flex>
      ),
    },
  ]

  return (
    <Box w="full" maxW="100%" minW={0}>
      {/* Page Header */}
      <Box mb={5}>
        <Heading as="h1" size={{ base: 'h2', md: 'h1' }}>
          {t('statesUts.title')}
        </Heading>
      </Box>

      {/* Search and Add Button */}
      <Flex
        justify="space-between"
        align="center"
        mb={6}
        h={{ base: 'auto', md: 16 }}
        py={4}
        px={{ base: 3, md: 6 }}
        gap={{ base: 3, md: 4 }}
        flexDirection={{ base: 'column', md: 'row' }}
        borderWidth="0.5px"
        borderColor="neutral.200"
        borderRadius="12px"
        bg="white"
      >
        <InputGroup w={{ base: 'full', md: '320px' }}>
          <InputLeftElement pointerEvents="none" h={8}>
            <SearchIcon color="neutral.300" aria-hidden="true" />
          </InputLeftElement>
          <Input
            placeholder={t('common:search')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            aria-label={t('statesUts.searchPlaceholder')}
            bg="white"
            h={8}
            borderWidth="1px"
            borderRadius="4px"
            borderColor="neutral.300"
            _placeholder={{ color: 'neutral.300' }}
          />
        </InputGroup>
        <Button
          variant="secondary"
          size="sm"
          fontWeight="600"
          onClick={handleAddNew}
          gap={1}
          w={{ base: 'full', md: '178px' }}
          aria-label={t('statesUts.addNewStateUt')}
        >
          <IoAddOutline size={24} aria-hidden="true" />
          {showAddButtonText && t('statesUts.addNewStateUt')}
        </Button>
      </Flex>

      {/* Data Table */}
      <DataTable<StateUT>
        columns={columns}
        data={filteredStates}
        getRowKey={(row) => row.id}
        emptyMessage={t('statesUts.messages.noStatesFound')}
        isLoading={isLoading}
        pagination={{
          enabled: true,
          pageSize: 10,
          pageSizeOptions: [10, 25, 50],
        }}
      />
    </Box>
  )
}

import { useState } from 'react'
import {
  Box,
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  Text,
  Flex,
  HStack,
  Button,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  useBreakpointValue,
} from '@chakra-ui/react'
import { ChevronUpIcon, ChevronDownIcon } from '@chakra-ui/icons'
import { FaArrowLeft, FaArrowRight } from 'react-icons/fa'
import { useTranslation } from 'react-i18next'

export interface DataTableColumn<T> {
  key: string
  header: string
  sortable?: boolean
  render?: (row: T) => React.ReactNode
}

export interface PaginationConfig {
  enabled: boolean
  pageSize?: number
  pageSizeOptions?: number[]
}

export interface DataTableProps<T> {
  columns: DataTableColumn<T>[]
  data: T[]
  getRowKey: (row: T) => string | number
  emptyMessage?: string
  isLoading?: boolean
  pagination?: PaginationConfig
}

type SortDirection = 'asc' | 'desc' | null

export function DataTable<T extends object>({
  columns,
  data,
  getRowKey,
  emptyMessage,
  isLoading = false,
  pagination,
}: DataTableProps<T>) {
  const { t } = useTranslation('common')
  const [sortColumn, setSortColumn] = useState<string | null>(null)
  const [sortDirection, setSortDirection] = useState<SortDirection>(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [itemsPerPage, setItemsPerPage] = useState(pagination?.pageSize ?? 10)

  // Responsive values
  const showPaginationText = useBreakpointValue({ base: false, md: true }) ?? true

  const pageSizeOptions = pagination?.pageSizeOptions ?? [10, 25, 50]
  const displayEmptyMessage = emptyMessage ?? t('noDataAvailable')

  const handleSort = (columnKey: string, sortable?: boolean) => {
    if (!sortable) return

    if (sortColumn === columnKey) {
      // Cycle through: asc -> desc -> null
      if (sortDirection === 'asc') {
        setSortDirection('desc')
      } else if (sortDirection === 'desc') {
        setSortColumn(null)
        setSortDirection(null)
      }
    } else {
      setSortColumn(columnKey)
      setSortDirection('asc')
    }
  }

  const getSortedData = () => {
    if (!sortColumn || !sortDirection) return data

    return [...data].sort((a, b) => {
      const aValue = (a as Record<string, unknown>)[sortColumn]
      const bValue = (b as Record<string, unknown>)[sortColumn]

      // Handle different data types
      if (aValue instanceof Date && bValue instanceof Date) {
        return sortDirection === 'asc'
          ? aValue.getTime() - bValue.getTime()
          : bValue.getTime() - aValue.getTime()
      }

      if (typeof aValue === 'string' && typeof bValue === 'string') {
        return sortDirection === 'asc' ? aValue.localeCompare(bValue) : bValue.localeCompare(aValue)
      }

      if (typeof aValue === 'number' && typeof bValue === 'number') {
        return sortDirection === 'asc' ? aValue - bValue : bValue - aValue
      }

      // Fallback to string comparison
      return sortDirection === 'asc'
        ? String(aValue).localeCompare(String(bValue))
        : String(bValue).localeCompare(String(aValue))
    })
  }

  const sortedData = getSortedData()

  // Pagination logic
  const totalPages = pagination?.enabled
    ? Math.max(1, Math.ceil(sortedData.length / itemsPerPage))
    : 1

  const effectiveCurrentPage = Math.min(Math.max(1, currentPage), totalPages)

  const paginatedData = pagination?.enabled
    ? sortedData.slice(
        (effectiveCurrentPage - 1) * itemsPerPage,
        effectiveCurrentPage * itemsPerPage
      )
    : sortedData

  const handlePageChange = (page: number) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page)
    }
  }

  const handleItemsPerPageChange = (size: number) => {
    setItemsPerPage(size)
    setCurrentPage(1) // Reset to first page when changing page size
  }

  // Generate page numbers to display
  const getPageNumbers = (): (number | 'ellipsis')[] => {
    const pages: (number | 'ellipsis')[] = []
    const maxVisiblePages = 3

    if (totalPages <= maxVisiblePages + 2) {
      // Show all pages if total is small
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i)
      }
    } else {
      // Always show first page
      pages.push(1)

      if (effectiveCurrentPage > 3) {
        pages.push('ellipsis')
      }

      // Show pages around current page
      const start = Math.max(2, effectiveCurrentPage - 1)
      const end = Math.min(totalPages - 1, effectiveCurrentPage + 1)

      for (let i = start; i <= end; i++) {
        if (!pages.includes(i)) {
          pages.push(i)
        }
      }

      if (effectiveCurrentPage < totalPages - 2) {
        pages.push('ellipsis')
      }

      // Always show last page
      if (!pages.includes(totalPages)) {
        pages.push(totalPages)
      }
    }

    return pages
  }

  if (isLoading) {
    return (
      <Box
        bg="white"
        borderWidth="0.5px"
        borderColor="neutral.100"
        borderRadius="12px"
        w="full"
        pt="24px"
        pr="16px"
        pb="24px"
        pl="16px"
        textAlign="center"
        role="status"
        aria-live="polite"
        aria-busy="true"
      >
        <Text color="neutral.600">{t('loading')}</Text>
      </Box>
    )
  }

  if (data.length === 0) {
    return (
      <Box
        bg="white"
        borderWidth="0.5px"
        borderColor="neutral.100"
        borderRadius="12px"
        w="full"
        pt="24px"
        pr="16px"
        pb="24px"
        pl="16px"
        textAlign="center"
        role="status"
      >
        <Text color="neutral.600">{displayEmptyMessage}</Text>
      </Box>
    )
  }

  return (
    <Box w="full" maxW="100%">
      {/* Table Container */}
      <Box
        bg="white"
        borderWidth="0.5px"
        borderColor="neutral.100"
        borderRadius={pagination?.enabled ? '12px 12px 0 0' : '12px'}
        overflow="hidden"
        pt="24px"
        pb="24px"
      >
        <Box
          overflowX="auto"
          px={{ base: 2, md: 4 }}
          pb={2}
          sx={{
            WebkitOverflowScrolling: 'touch',
            '&::-webkit-scrollbar': {
              height: '8px',
            },
            '&::-webkit-scrollbar-track': {
              bg: 'neutral.50',
              borderRadius: '4px',
            },
            '&::-webkit-scrollbar-thumb': {
              bg: 'neutral.300',
              borderRadius: '4px',
            },
          }}
        >
          <Table size="sm" variant="simple" w="max-content" minW="100%">
            <Thead>
              <Tr>
                {columns.map((column) => {
                  const isSorted = sortColumn === column.key
                  const ariaSortValue = isSorted
                    ? sortDirection === 'asc'
                      ? 'ascending'
                      : 'descending'
                    : undefined

                  return (
                    <Th
                      key={column.key}
                      scope="col"
                      cursor={column.sortable ? 'pointer' : 'default'}
                      onClick={() => handleSort(column.key, column.sortable)}
                      onKeyDown={(e) => {
                        if (column.sortable && (e.key === 'Enter' || e.key === ' ')) {
                          e.preventDefault()
                          handleSort(column.key, column.sortable)
                        }
                      }}
                      tabIndex={column.sortable ? 0 : undefined}
                      userSelect="none"
                      _hover={column.sortable ? { bg: 'neutral.50' } : undefined}
                      bg="transparent"
                      h={10}
                      px={{ base: 3, md: 5 }}
                      py={3}
                      textTransform="none"
                      whiteSpace="nowrap"
                      aria-sort={column.sortable ? ariaSortValue : undefined}
                    >
                      <Flex align="center" gap={2} textStyle="h10">
                        <Text whiteSpace="nowrap">{column.header}</Text>
                        {column.sortable && (
                          <Box aria-hidden="true">
                            {isSorted ? (
                              sortDirection === 'asc' ? (
                                <ChevronUpIcon boxSize={4} />
                              ) : (
                                <ChevronDownIcon boxSize={4} />
                              )
                            ) : (
                              <Box opacity={0.3}>
                                <ChevronUpIcon boxSize={4} />
                              </Box>
                            )}
                          </Box>
                        )}
                      </Flex>
                    </Th>
                  )
                })}
              </Tr>
            </Thead>
            <Tbody>
              {paginatedData.map((row) => (
                <Tr key={getRowKey(row)} _hover={{ bg: 'neutral.25' }}>
                  {columns.map((column) => (
                    <Td
                      key={column.key}
                      borderTop="1px"
                      borderBottom="none"
                      borderColor="neutral.200"
                      px={{ base: 3, md: 5 }}
                      py={3}
                      h={12}
                      whiteSpace="nowrap"
                    >
                      {column.render
                        ? column.render(row)
                        : String((row as Record<string, unknown>)[column.key] ?? '')}
                    </Td>
                  ))}
                </Tr>
              ))}
            </Tbody>
          </Table>
        </Box>
      </Box>

      {/* Pagination Controls - Outside the table box */}
      {pagination?.enabled && totalPages > 0 && (
        <Flex
          as="nav"
          aria-label="Pagination"
          justify="space-between"
          align="center"
          px={{ base: 2, md: 4 }}
          py={4}
          h={{ base: 'auto', md: '66px' }}
          flexDirection={{ base: 'column', md: 'row' }}
          gap={{ base: 3, md: 0 }}
          bg="neutral.50"
          borderRadius="0 0 12px 12px"
        >
          {/* Items per page selector */}
          <HStack spacing={{ base: '6px', md: '10px' }}>
            {showPaginationText && (
              <Text fontSize={{ base: 'sm', md: 'md' }}>{t('table.itemsPerPage')}</Text>
            )}
            <Menu>
              <MenuButton
                as={Button}
                variant="outline"
                size="sm"
                rightIcon={<ChevronDownIcon aria-hidden="true" />}
                fontWeight="400"
                borderColor="#E9EAEB"
                borderRadius="8px"
                border="0.5px"
                py={2}
                px="15px"
                maxW="72px"
                bg="neutral.100"
                _hover={{ bg: 'white' }}
                _active={{ bg: 'neutral.100' }}
                aria-label={`${t('table.itemsPerPage')}: ${itemsPerPage}`}
              >
                {itemsPerPage}
              </MenuButton>
              <MenuList minW="80px">
                {pageSizeOptions.map((size) => (
                  <MenuItem
                    key={size}
                    onClick={() => handleItemsPerPageChange(size)}
                    fontWeight={itemsPerPage === size ? '600' : '400'}
                  >
                    {size}
                  </MenuItem>
                ))}
              </MenuList>
            </Menu>
          </HStack>

          {/* Page navigation */}
          <HStack spacing={{ base: 1, md: 2 }} h={8}>
            {/* Previous button */}
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handlePageChange(effectiveCurrentPage - 1)}
              isDisabled={effectiveCurrentPage === 1}
              leftIcon={<FaArrowLeft aria-hidden="true" />}
              fontWeight="400"
              color="neutral.600"
              _hover={{ bg: 'white' }}
              aria-label={t('table.previous')}
            >
              {showPaginationText && t('table.previous')}
            </Button>

            {/* Page numbers - hidden on mobile */}
            <HStack spacing={{ base: 1, md: 2 }} display={{ base: 'none', sm: 'flex' }}>
              {getPageNumbers().map((page, index) =>
                page === 'ellipsis' ? (
                  <Text key={`ellipsis-${index}`} px={2} color="neutral.400" aria-hidden="true">
                    ...
                  </Text>
                ) : (
                  <Button
                    key={page}
                    size="sm"
                    variant={effectiveCurrentPage === page ? 'solid' : 'ghost'}
                    onClick={() => handlePageChange(page)}
                    w="32px"
                    h="32px"
                    px={3}
                    py={2}
                    borderRadius="8px"
                    fontWeight={effectiveCurrentPage === page ? '600' : '400'}
                    bg={effectiveCurrentPage === page ? 'primary.500' : 'transparent'}
                    color={effectiveCurrentPage === page ? 'white' : 'neutral.600'}
                    _hover={{
                      bg: effectiveCurrentPage === page ? 'primary.600' : 'white',
                    }}
                    aria-label={t('table.goToPage', { page })}
                    aria-current={effectiveCurrentPage === page ? 'page' : undefined}
                  >
                    {page}
                  </Button>
                )
              )}
            </HStack>

            {/* Mobile page indicator */}
            <Text display={{ base: 'block', sm: 'none' }} fontSize="sm" color="neutral.600" px={2}>
              {effectiveCurrentPage} / {totalPages}
            </Text>

            {/* Next button */}
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handlePageChange(effectiveCurrentPage + 1)}
              isDisabled={effectiveCurrentPage === totalPages}
              rightIcon={<FaArrowRight aria-hidden="true" />}
              fontWeight="400"
              color="neutral.600"
              _hover={{ bg: 'white' }}
              aria-label={t('table.next')}
            >
              {showPaginationText && t('table.next')}
            </Button>
          </HStack>
        </Flex>
      )}
    </Box>
  )
}

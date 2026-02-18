import { useState } from 'react'
import { Box, Icon, Table, Tbody, Td, Th, Thead, Tr } from '@chakra-ui/react'
import { BiSortAlt2 } from 'react-icons/bi'
import type { PumpOperatorPerformanceData } from '../../types'

interface PumpOperatorsPerformanceTableProps {
  data: PumpOperatorPerformanceData[]
  title: string
  maxItems?: number
}

type SortColumn = 'reportingRate' | 'waterSupplied' | null
type SortDirection = 'asc' | 'desc' | null

export function PumpOperatorsPerformanceTable({
  data,
  title,
  maxItems,
}: PumpOperatorsPerformanceTableProps) {
  const [sortColumn, setSortColumn] = useState<SortColumn>(null)
  const [sortDirection, setSortDirection] = useState<SortDirection>(null)
  const safeMaxItems =
    typeof maxItems === 'number' && Number.isFinite(maxItems) ? Math.max(0, maxItems) : undefined
  const sortedRows =
    sortColumn && sortDirection
      ? [...data].sort((a, b) => {
          const aValue = a[sortColumn]
          const bValue = b[sortColumn]
          return sortDirection === 'asc' ? aValue - bValue : bValue - aValue
        })
      : data
  const rows = typeof safeMaxItems === 'number' ? sortedRows.slice(0, safeMaxItems) : sortedRows

  const handleSort = (column: Exclude<SortColumn, null>) => {
    if (sortColumn !== column) {
      setSortColumn(column)
      setSortDirection('desc')
      return
    }
    setSortDirection((current) => (current === 'desc' ? 'asc' : 'desc'))
  }

  return (
    <Box borderRadius="lg" overflow="hidden" w="full" minW={0}>
      <Box textStyle="bodyText3" fontWeight="400" mb="16px">
        {title}
      </Box>
      <Box
        maxH="330px"
        overflowY="auto"
        overflowX="auto"
        w="full"
        minW={0}
        pr={2}
        sx={{
          '&::-webkit-scrollbar': { width: '4px' },
          '&::-webkit-scrollbar-track': { bg: 'neutral.100', borderRadius: '999px' },
          '&::-webkit-scrollbar-thumb': {
            bg: 'neutral.300',
            borderRadius: '999px',
            minHeight: '165px',
          },
        }}
      >
        <Table size="sm" w="max-content" minW="100%">
          <Thead
            sx={{
              position: 'sticky',
              top: 0,
              zIndex: 1,
              bg: 'white',
              th: {
                textStyle: 'bodyText7',
                textTransform: 'none',
                fontWeight: '500',
                px: 3,
                py: 4,
              },
            }}
          >
            <Tr>
              <Th>Name</Th>
              <Th>Block</Th>
              <Th>Village</Th>
              <Th
                aria-sort={
                  sortColumn === 'reportingRate'
                    ? sortDirection === 'asc'
                      ? 'ascending'
                      : 'descending'
                    : undefined
                }
              >
                <Box
                  as="button"
                  type="button"
                  onClick={() => handleSort('reportingRate')}
                  display="inline-flex"
                  alignItems="center"
                  gap={1}
                  cursor="pointer"
                  textAlign="left"
                  width="100%"
                  bg="none"
                  border="none"
                  p={0}
                >
                  <Box as="span">Reporting Rate (%)</Box>
                  <Icon as={BiSortAlt2} boxSize="16px" color="neutral.500" aria-hidden />
                </Box>
              </Th>
              <Th
                aria-sort={
                  sortColumn === 'waterSupplied'
                    ? sortDirection === 'asc'
                      ? 'ascending'
                      : 'descending'
                    : undefined
                }
              >
                <Box
                  as="button"
                  type="button"
                  onClick={() => handleSort('waterSupplied')}
                  display="inline-flex"
                  alignItems="center"
                  gap={1}
                  cursor="pointer"
                  textAlign="left"
                  width="100%"
                  bg="none"
                  border="none"
                  p={0}
                >
                  <Box as="span">Water Supplied</Box>
                  <Icon as={BiSortAlt2} boxSize="16px" color="neutral.500" aria-hidden />
                </Box>
              </Th>
            </Tr>
          </Thead>
          <Tbody
            sx={{
              td: {
                textStyle: 'bodyText7',
                fontWeight: '400',
                px: 3,
                py: 0,
                height: '40px',
                lineHeight: '40px',
                whiteSpace: 'nowrap',
              },
            }}
          >
            {rows.map((operator) => (
              <Tr key={operator.id} _odd={{ bg: 'primary.25' }}>
                <Td>{operator.name}</Td>
                <Td>{operator.block}</Td>
                <Td>{operator.village}</Td>
                <Td>{operator.reportingRate}</Td>
                <Td>{operator.waterSupplied} LPCD</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </Box>
    </Box>
  )
}

import { useState } from 'react'
import { Box, Icon, Table, Tbody, Td, Th, Thead, Tr } from '@chakra-ui/react'
import { BiSortAlt2 } from 'react-icons/bi'
import type { EntityPerformance } from '../../types'

interface AllGramPanchayatsTableProps {
  data: EntityPerformance[]
  maxItems?: number
}

type SortColumn = 'coverage' | 'quantity' | 'regularity' | 'compositeScore' | null
type SortDirection = 'asc' | 'desc' | null

export function AllGramPanchayatsTable({ data, maxItems }: AllGramPanchayatsTableProps) {
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
    <Box borderRadius="lg" overflow="hidden">
      <Box
        maxH="416px"
        overflowY="auto"
        overflowX="auto"
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
        <Table size="sm">
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
                py: 5,
              },
            }}
          >
            <Tr>
              <Th>Gram Panchayat</Th>
              <Th
                aria-sort={
                  sortColumn === 'coverage'
                    ? sortDirection === 'asc'
                      ? 'ascending'
                      : 'descending'
                    : undefined
                }
              >
                <Box
                  as="button"
                  type="button"
                  onClick={() => handleSort('coverage')}
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
                  <Box as="span">Coverage (%)</Box>
                  <Icon as={BiSortAlt2} boxSize="16px" color="neutral.500" aria-hidden />
                </Box>
              </Th>
              <Th
                aria-sort={
                  sortColumn === 'quantity'
                    ? sortDirection === 'asc'
                      ? 'ascending'
                      : 'descending'
                    : undefined
                }
              >
                <Box
                  as="button"
                  type="button"
                  onClick={() => handleSort('quantity')}
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
                  <Box as="span">Quantity (LPCD)</Box>
                  <Icon as={BiSortAlt2} boxSize="16px" color="neutral.500" aria-hidden />
                </Box>
              </Th>
              <Th
                aria-sort={
                  sortColumn === 'regularity'
                    ? sortDirection === 'asc'
                      ? 'ascending'
                      : 'descending'
                    : undefined
                }
              >
                <Box
                  as="button"
                  type="button"
                  onClick={() => handleSort('regularity')}
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
                  <Box as="span">Regularity (%)</Box>
                  <Icon as={BiSortAlt2} boxSize="16px" color="neutral.500" aria-hidden />
                </Box>
              </Th>
              <Th
                aria-sort={
                  sortColumn === 'compositeScore'
                    ? sortDirection === 'asc'
                      ? 'ascending'
                      : 'descending'
                    : undefined
                }
              >
                <Box
                  as="button"
                  type="button"
                  onClick={() => handleSort('compositeScore')}
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
                  <Box as="span">Average (%)</Box>
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
            {rows.map((item) => (
              <Tr key={item.id} _odd={{ bg: 'primary.25' }}>
                <Td>{item.name}</Td>
                <Td>{item.coverage.toFixed(0)}%</Td>
                <Td>{item.quantity}</Td>
                <Td>{item.regularity.toFixed(0)}%</Td>
                <Td>{item.compositeScore.toFixed(0)}%</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </Box>
    </Box>
  )
}

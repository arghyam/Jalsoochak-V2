import { Box, Flex, Icon, Table, Tbody, Td, Th, Thead, Tr } from '@chakra-ui/react'
import { BiSortAlt2 } from 'react-icons/bi'
import type { EntityPerformance } from '../../types'

interface AllDistrictsTableProps {
  data: EntityPerformance[]
  maxItems?: number
}

export function AllDistrictsTable({ data, maxItems }: AllDistrictsTableProps) {
  const safeMaxItems =
    typeof maxItems === 'number' && Number.isFinite(maxItems) ? Math.max(0, maxItems) : undefined
  const rows = typeof safeMaxItems === 'number' ? data.slice(0, safeMaxItems) : data

  return (
    <Box borderRadius="lg" overflow="hidden">
      <Box
        maxH="416px"
        overflowY="auto"
        pr={2}
        sx={{
          '&::-webkit-scrollbar': { width: '4px' },
          '&::-webkit-scrollbar-track': { bg: 'neutral.100', borderRadius: '999px' },
          '&::-webkit-scrollbar-thumb': {
            bg: 'neutral.300',
            borderRadius: '999px',
            maxHeight: '80px',
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
              <Th>District</Th>
              <Th>
                <Flex align="center">
                  <Box as="span">Coverage (%)</Box>
                  <Icon as={BiSortAlt2} boxSize="16px" color="neutral.500" />
                </Flex>
              </Th>
              <Th>
                <Flex align="center">
                  <Box as="span">Quantity (LPCD)</Box>
                  <Icon as={BiSortAlt2} boxSize="16px" color="neutral.500" />
                </Flex>
              </Th>
              <Th>
                <Flex align="center">
                  <Box as="span">Regularity (%)</Box>
                  <Icon as={BiSortAlt2} boxSize="16px" color="neutral.500" />
                </Flex>
              </Th>
              <Th>
                <Flex align="center">
                  <Box as="span">Average (%)</Box>
                  <Icon as={BiSortAlt2} boxSize="16px" color="neutral.500" />
                </Flex>
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
            {rows.map((district) => (
              <Tr key={district.id} _odd={{ bg: 'primary.25' }}>
                <Td>{district.name}</Td>
                <Td>{district.coverage.toFixed(0)}%</Td>
                <Td>{district.quantity}</Td>
                <Td>{district.regularity.toFixed(0)}%</Td>
                <Td>{district.compositeScore.toFixed(0)}%</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </Box>
    </Box>
  )
}

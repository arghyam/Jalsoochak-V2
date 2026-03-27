import { Box, Flex, Icon, Table, Tbody, Td, Th, Thead, Tr } from '@chakra-ui/react'
import { BiSortAlt2 } from 'react-icons/bi'
import type { EntityPerformance } from '../../types'

interface AllStatesTableProps {
  data: EntityPerformance[]
  maxItems?: number
}

export function AllStatesTable({ data, maxItems }: AllStatesTableProps) {
  const safeMaxItems =
    typeof maxItems === 'number' && Number.isFinite(maxItems) ? Math.max(0, maxItems) : undefined
  const rows = typeof safeMaxItems === 'number' ? data.slice(0, safeMaxItems) : data

  return (
    <Box borderRadius="lg" overflow="visible" minW={0} w="full">
      <Box
        maxH="416px"
        overflowY="auto"
        overflowX="auto"
        w="full"
        maxW="100%"
        minW={0}
        pr={2}
        pb={2}
        sx={{
          WebkitOverflowScrolling: 'touch',
          '&::-webkit-scrollbar': { width: '4px', height: '4px' },
          '&::-webkit-scrollbar-track': { bg: 'neutral.100', borderRadius: '999px' },
          '&::-webkit-scrollbar-thumb': { bg: 'neutral.300', borderRadius: '999px' },
        }}
      >
        <Table size="sm" minW="720px" w="max-content">
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
                px: { base: 2, md: 3 },
                py: { base: 3, md: 5 },
                minW: '140px',
                whiteSpace: 'nowrap',
              },
            }}
          >
            <Tr>
              <Th>State/UT</Th>
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
              tr: {
                cursor: 'pointer',
              },
              td: {
                textStyle: 'bodyText7',
                fontWeight: '400',
                px: { base: 2, md: 3 },
                py: { base: 2, md: 0 },
                height: { base: 'auto', md: '40px' },
                lineHeight: { base: '20px', md: '40px' },
                minW: '140px',
                whiteSpace: 'nowrap',
              },
            }}
          >
            {rows.map((state) => (
              <Tr key={state.id} _odd={{ bg: 'primary.25' }}>
                <Td>{state.name}</Td>
                <Td>{state.coverage.toFixed(0)}%</Td>
                <Td>{state.quantity}</Td>
                <Td>{state.regularity.toFixed(0)}%</Td>
                <Td>{state.compositeScore.toFixed(0)}%</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </Box>
    </Box>
  )
}
